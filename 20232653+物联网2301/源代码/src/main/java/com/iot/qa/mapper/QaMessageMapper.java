package com.iot.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.qa.entity.QaMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 问答消息表 Mapper，同时承载后台看板的统计查询。
 */
@Mapper
public interface QaMessageMapper extends BaseMapper<QaMessage> {

    /**
     * 最近 N 天的提问量趋势。
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS day, COUNT(*) AS num "
            + "FROM qa_message WHERE role = 'user' AND deleted = 0 "
            + "AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) "
            + "GROUP BY day ORDER BY day")
    List<Map<String, Object>> countDailyTrend(@Param("days") int days);

    /**
     * 用户提问 TOP N（按原始问题文本聚合，用于发现知识库盲区）。
     */
    @Select("SELECT content AS question, COUNT(*) AS num FROM qa_message "
            + "WHERE role = 'user' AND deleted = 0 "
            + "GROUP BY content ORDER BY num DESC LIMIT #{limit}")
    List<Map<String, Object>> topUserQuestions(@Param("limit") int limit);

    /**
     * 按答案来源统计占比。
     */
    @Select("SELECT source AS source, COUNT(*) AS num FROM qa_message "
            + "WHERE role = 'assistant' AND deleted = 0 GROUP BY source")
    List<Map<String, Object>> countGroupBySource();

    /**
     * 统计反馈情况：有帮助 / 没帮助 / 未评价。
     */
    @Select("SELECT feedback AS feedback, COUNT(*) AS num FROM qa_message "
            + "WHERE role = 'assistant' AND deleted = 0 GROUP BY feedback")
    List<Map<String, Object>> countGroupByFeedback();

    /**
     * 按小时统计提问分布（0-23 时），用于分析用户活跃时段。
     */
    @Select("SELECT HOUR(create_time) AS hour, COUNT(*) AS num FROM qa_message "
            + "WHERE role = 'user' AND deleted = 0 GROUP BY hour ORDER BY hour")
    List<Map<String, Object>> countGroupByHour();
}