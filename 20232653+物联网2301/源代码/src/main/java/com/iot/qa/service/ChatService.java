package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.qa.common.BusinessException;
import com.iot.qa.common.ResultCode;
import com.iot.qa.dto.ChatRequest;
import com.iot.qa.engine.AnswerEngine;
import com.iot.qa.engine.AnswerResult;
import com.iot.qa.entity.QaMessage;
import com.iot.qa.entity.QaSession;
import com.iot.qa.mapper.QaMessageMapper;
import com.iot.qa.mapper.QaSessionMapper;
import com.iot.qa.util.TextUtils;
import com.iot.qa.vo.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答服务：串联「会话管理 → 消息落库 → 引擎求解 → 结果回写」的完整链路。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    /** 会话标题最大长度 */
    private static final int TITLE_MAX_LENGTH = 20;

    private final QaSessionMapper sessionMapper;

    private final QaMessageMapper messageMapper;

    private final AnswerEngine answerEngine;

    private final KnowledgeService knowledgeService;

    /**
     * 处理一次提问。
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(Long userId, ChatRequest request) {
        long start = System.currentTimeMillis();

        QaSession session = resolveSession(userId, request);

        // 1. 落库用户提问
        QaMessage userMessage = new QaMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setUserId(userId);
        userMessage.setRole(ROLE_USER);
        userMessage.setContent(request.question());
        userMessage.setDeleted(0);
        messageMapper.insert(userMessage);

        // 2. 交给问答引擎求解
        AnswerResult answerResult = answerEngine.ask(request.question());

        // 3. 落库系统回答
        int costMs = (int) (System.currentTimeMillis() - start);
        QaMessage aiMessage = new QaMessage();
        aiMessage.setSessionId(session.getId());
        aiMessage.setUserId(userId);
        aiMessage.setRole(ROLE_ASSISTANT);
        aiMessage.setContent(answerResult.getAnswer());
        aiMessage.setKnowledgeId(answerResult.getKnowledgeId());
        aiMessage.setScore(answerResult.getScore());
        aiMessage.setSource(answerResult.getSource());
        aiMessage.setFeedback(0);
        aiMessage.setCostMs(costMs);
        aiMessage.setDeleted(0);
        messageMapper.insert(aiMessage);

        // 4. 命中知识条目则累计热度
        if (answerResult.getKnowledgeId() != null) {
            knowledgeService.incrementHit(answerResult.getKnowledgeId());
        }

        // 5. 更新会话统计信息
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setLastTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getId());
        response.setSessionTitle(session.getTitle());
        response.setQuestion(request.question());
        response.setAnswer(answerResult.getAnswer());
        response.setSource(answerResult.getSource());
        response.setScore(answerResult.getScore());
        response.setKnowledgeId(answerResult.getKnowledgeId());
        response.setMatchedQuestion(answerResult.getMatchedQuestion());
        response.setCostMs(costMs);
        response.setCreateTime(LocalDateTime.now());
        return response;
    }

    /** 定位会话：sessionId 为空则新建，否则校验归属 */
    private QaSession resolveSession(Long userId, ChatRequest request) {
        if (request.sessionId() != null) {
            QaSession session = sessionMapper.selectById(request.sessionId());
            if (session == null) {
                throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
            }
            if (!session.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            return session;
        }
        QaSession session = new QaSession();
        session.setUserId(userId);
        session.setTitle(TextUtils.truncate(request.question(), TITLE_MAX_LENGTH));
        session.setMessageCount(0);
        session.setLastTime(LocalDateTime.now());
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    /** 我的会话列表 */
    public IPage<QaSession> pageSessions(Long userId, long current, long size) {
        return sessionMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<QaSession>()
                        .eq(QaSession::getUserId, userId)
                        .orderByDesc(QaSession::getLastTime));
    }

    /** 后台：全部会话分页 */
    public IPage<QaSession> pageAllSessions(long current, long size, String keyword) {
        LambdaQueryWrapper<QaSession> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(QaSession::getTitle, keyword);
        }
        wrapper.orderByDesc(QaSession::getLastTime);
        return sessionMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /** 会话内消息列表；userId 不为空时校验归属 */
    public List<QaMessage> listMessages(Long sessionId, Long userId) {
        QaSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        if (userId != null && !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        List<QaMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getSessionId, sessionId)
                .orderByAsc(QaMessage::getId));

        // 回填命中的标准问题，便于前端展示「依据」
        messages.stream()
                .filter(m -> m.getKnowledgeId() != null)
                .forEach(m -> {
                    var knowledge = knowledgeService.getById(m.getKnowledgeId());
                    if (knowledge != null) {
                        m.setMatchedQuestion(knowledge.getQuestion());
                    }
                });
        return messages;
    }

    /** 删除会话及其消息 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        QaSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.SESSION_NOT_FOUND);
        }
        if (userId != null && !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        sessionMapper.deleteById(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<QaMessage>().eq(QaMessage::getSessionId, sessionId));
    }

    /** 清空我的全部会话 */
    @Transactional(rollbackFor = Exception.class)
    public void clearSessions(Long userId) {
        List<QaSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<QaSession>()
                .eq(QaSession::getUserId, userId));
        for (QaSession session : sessions) {
            sessionMapper.deleteById(session.getId());
            messageMapper.delete(new LambdaQueryWrapper<QaMessage>().eq(QaMessage::getSessionId, session.getId()));
        }
    }

    /** 答案反馈：1 有帮助，-1 没帮助 */
    @Transactional(rollbackFor = Exception.class)
    public void feedback(Long messageId, Long userId, Integer feedback) {
        QaMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (userId != null && !message.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        message.setFeedback(feedback);
        messageMapper.updateById(message);
    }
}