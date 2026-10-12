package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.qa.entity.QaCategory;
import com.iot.qa.entity.QaMessage;
import com.iot.qa.entity.QaSession;
import com.iot.qa.mapper.QaMessageMapper;
import com.iot.qa.mapper.QaSessionMapper;
import com.iot.qa.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务：为后台数据看板聚合各项指标。
 *
 * <p>所有统计尽量下推到数据库完成（GROUP BY / COUNT），避免把明细数据捞到内存再计算。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserService userService;

    private final KnowledgeService knowledgeService;

    private final CategoryService categoryService;

    private final QaSessionMapper sessionMapper;

    private final QaMessageMapper messageMapper;

    /** 看板总览数据 */
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();

        vo.setUserCount(userService.countUsers());
        vo.setKnowledgeCount(knowledgeService.countKnowledge());
        vo.setSessionCount(sessionMapper.selectCount(new LambdaQueryWrapper<QaSession>()));
        vo.setMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getRole, "user")));
        vo.setTodayMessageCount(messageMapper.selectCount(new LambdaQueryWrapper<QaMessage>()
                .eq(QaMessage::getRole, "user")
                .ge(QaMessage::getCreateTime, LocalDate.now().atStartOfDay())));

        vo.setTrend(messageMapper.countDailyTrend(7));
        vo.setHotKnowledge(knowledgeService.topHit(10));
        vo.setHotQuestions(messageMapper.topUserQuestions(10));
        vo.setSourceStats(messageMapper.countGroupBySource());
        vo.setFeedbackStats(messageMapper.countGroupByFeedback());
        vo.setHourStats(messageMapper.countGroupByHour());
        vo.setCategoryStats(buildCategoryStats());

        vo.setHitRate(calculateHitRate(vo.getSourceStats()));
        vo.setSatisfiedRate(calculateSatisfiedRate(vo.getFeedbackStats()));
        return vo;
    }

    /** 分类知识量分布 */
    private List<Map<String, Object>> buildCategoryStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QaCategory category : categoryService.listWithCount()) {
            Map<String, Object> row = new HashMap<>(2);
            row.put("name", category.getName());
            row.put("num", category.getKnowledgeCount());
            result.add(row);
        }
        return result;
    }

    /** 命中率 = 知识库回答数 / 全部回答数 */
    private Double calculateHitRate(List<Map<String, Object>> sourceStats) {
        long total = 0L;
        long hit = 0L;
        for (Map<String, Object> row : sourceStats) {
            long num = ((Number) row.get("num")).longValue();
            total += num;
            if ("knowledge".equals(String.valueOf(row.get("source")))) {
                hit += num;
            }
        }
        return total == 0 ? 0D : Math.round(hit * 1000D / total) / 1000D;
    }

    /** 满意度 = 有帮助 / （有帮助 + 没帮助） */
    private Double calculateSatisfiedRate(List<Map<String, Object>> feedbackStats) {
        long positive = 0L;
        long negative = 0L;
        for (Map<String, Object> row : feedbackStats) {
            int feedback = ((Number) row.get("feedback")).intValue();
            long num = ((Number) row.get("num")).longValue();
            if (feedback == 1) {
                positive = num;
            } else if (feedback == -1) {
                negative = num;
            }
        }
        long rated = positive + negative;
        return rated == 0 ? 0D : Math.round(positive * 1000D / rated) / 1000D;
    }
}