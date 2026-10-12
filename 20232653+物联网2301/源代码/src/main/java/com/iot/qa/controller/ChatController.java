package com.iot.qa.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iot.qa.common.PageResult;
import com.iot.qa.common.Result;
import com.iot.qa.common.UserContext;
import com.iot.qa.dto.ChatRequest;
import com.iot.qa.entity.QaMessage;
import com.iot.qa.entity.QaSession;
import com.iot.qa.mapper.QaKnowledgeMapper;
import com.iot.qa.service.ChatService;
import com.iot.qa.vo.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 前台问答接口。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final QaKnowledgeMapper knowledgeMapper;

    /** 提问 */
    @PostMapping("/ask")
    public Result<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(UserContext.getUserId(), request));
    }

    /** 我的会话列表 */
    @GetMapping("/sessions")
    public Result<PageResult<QaSession>> sessions(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size) {
        IPage<QaSession> page = chatService.pageSessions(UserContext.getUserId(), current, size);
        return Result.success(PageResult.of(page));
    }

    /** 会话内消息 */
    @GetMapping("/messages/{sessionId}")
    public Result<List<QaMessage>> messages(@PathVariable Long sessionId) {
        return Result.success(chatService.listMessages(sessionId, UserContext.getUserId()));
    }

    /** 删除会话 */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId, UserContext.getUserId());
        return Result.success("会话已删除", null);
    }

    /** 清空我的会话 */
    @DeleteMapping("/sessions")
    public Result<Void> clearSessions() {
        chatService.clearSessions(UserContext.getUserId());
        return Result.success("已清空全部会话", null);
    }

    /** 答案反馈 */
    @PostMapping("/feedback")
    public Result<Void> feedback(@RequestBody Map<String, Object> body) {
        Long messageId = Long.valueOf(String.valueOf(body.get("messageId")));
        Integer feedback = Integer.valueOf(String.valueOf(body.get("feedback")));
        chatService.feedback(messageId, UserContext.getUserId(), feedback);
        return Result.success("感谢反馈", null);
    }

    /** 猜你想问 */
    @GetMapping("/guess")
    public Result<List<String>> guess(@RequestParam(defaultValue = "6") int limit) {
        return Result.success(knowledgeMapper.selectHotQuestions(limit));
    }
}