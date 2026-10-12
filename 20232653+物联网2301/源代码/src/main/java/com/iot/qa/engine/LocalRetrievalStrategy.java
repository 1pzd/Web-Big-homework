package com.iot.qa.engine;

import com.iot.qa.entity.QaKnowledge;
import com.iot.qa.mapper.QaKnowledgeMapper;
import com.iot.qa.service.ConfigService;
import com.iot.qa.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 本地知识库检索策略。
 *
 * <p>核心思路：把用户问题与知识库中每个「标准问题 + 关键词」做加权相似度打分，
 * 取得分最高且超过阈值的条目作为答案，从而实现不依赖外部服务、离线可用的问答。</p>
 *
 * <p>打分公式：<br>
 * score = 0.45 × 关键词命中率 + 0.40 × 问题文本相似度 + 0.15 × 关键词文本相似度</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalRetrievalStrategy implements AnswerStrategy {

    /** 知识库内存快照有效期，单位毫秒 */
    private static final long SNAPSHOT_TTL_MS = 60_000L;

    /** 关键词命中多少个别视为满分 */
    private static final int EXPECTED_KEYWORD_HITS = 3;

    private final QaKnowledgeMapper knowledgeMapper;

    private final ConfigService configService;

    private volatile List<QaKnowledge> snapshot = Collections.emptyList();

    private volatile long snapshotLoadedAt = 0L;

    private final Object snapshotLock = new Object();

    @Override
    public String mode() {
        return "local";
    }

    @Override
    public AnswerResult answer(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        double threshold = configService.getDouble("ai.engine.threshold", 0.25D);
        List<QaKnowledge> items = currentSnapshot();
        if (items.isEmpty()) {
            return null;
        }

        QaKnowledge best = null;
        double bestScore = 0D;
        for (QaKnowledge item : items) {
            double score = score(question, item);
            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }

        if (best == null || bestScore < threshold) {
            log.debug("本地检索未命中，最高分={}, 阈值={}", bestScore, threshold);
            return null;
        }
        return AnswerResult.ofKnowledge(best.getAnswer(), round(bestScore), best.getId(), best.getQuestion());
    }

    /** 单条知识打分 */
    private double score(String question, QaKnowledge item) {
        double keywordRate = TextUtils.keywordHitRate(question, item.getKeywords(), EXPECTED_KEYWORD_HITS);
        double questionSim = TextUtils.similarity(question, item.getQuestion());
        double keywordSim = TextUtils.similarity(question, item.getKeywords());
        return 0.45D * keywordRate + 0.40D * questionSim + 0.15D * keywordSim;
    }

    private double round(double value) {
        return Math.round(value * 1000D) / 1000D;
    }

    /**
     * 获取知识库快照，超过有效期才重新查库。
     * 问答属于高频读操作，用内存快照把「每次提问一次全表扫描」降为「每分钟一次」。
     */
    private List<QaKnowledge> currentSnapshot() {
        if (System.currentTimeMillis() - snapshotLoadedAt < SNAPSHOT_TTL_MS) {
            return snapshot;
        }
        synchronized (snapshotLock) {
            if (System.currentTimeMillis() - snapshotLoadedAt < SNAPSHOT_TTL_MS) {
                return snapshot;
            }
            try {
                snapshot = knowledgeMapper.selectAllEnabled();
                snapshotLoadedAt = System.currentTimeMillis();
            } catch (Exception e) {
                log.warn("加载知识库快照失败，沿用旧数据: {}", e.getMessage());
                snapshotLoadedAt = System.currentTimeMillis();
            }
            return snapshot;
        }
    }

    /** 知识条目发生变化时由业务层调用，使快照立即失效 */
    public void invalidate() {
        snapshotLoadedAt = 0L;
    }
}