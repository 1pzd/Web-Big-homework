package com.iot.qa.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 后台数据看板聚合结果。
 */
@Data
public class DashboardVO implements Serializable {

    /** 注册用户数 */
    private Long userCount;

    /** 会话总数 */
    private Long sessionCount;

    /** 提问总数 */
    private Long messageCount;

    /** 知识条目数 */
    private Long knowledgeCount;

    /** 今日提问数 */
    private Long todayMessageCount;

    /** 知识库命中率（0~1） */
    private Double hitRate;

    /** 有帮助评价占比（0~1） */
    private Double satisfiedRate;

    /** 近 7 天提问趋势：[{day, num}] */
    private List<Map<String, Object>> trend;

    /** 热门知识条目 TOP10：[{question, hitCount}] */
    private List<Map<String, Object>> hotKnowledge;

    /** 用户提问 TOP10：[{question, num}] */
    private List<Map<String, Object>> hotQuestions;

    /** 答案来源分布：[{source, num}] */
    private List<Map<String, Object>> sourceStats;

    /** 反馈分布：[{feedback, num}] */
    private List<Map<String, Object>> feedbackStats;

    /** 24 小时活跃分布：[{hour, num}] */
    private List<Map<String, Object>> hourStats;

    /** 分类知识量分布：[{name, num}] */
    private List<Map<String, Object>> categoryStats;
}