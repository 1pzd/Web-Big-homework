package com.iot.qa.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一轮问答的返回结果。
 */
@Data
public class ChatResponse implements Serializable {

    /** 会话 ID（新会话时由服务端创建） */
    private Long sessionId;

    /** 会话标题 */
    private String sessionTitle;

    /** 用户提问 */
    private String question;

    /** 系统回答 */
    private String answer;

    /** 答案来源：knowledge / llm / fallback */
    private String source;

    /** 相似度得分 */
    private Double score;

    /** 命中的知识条目 ID */
    private Long knowledgeId;

    /** 命中的标准问题 */
    private String matchedQuestion;

    /** 引擎耗时（毫秒） */
    private Integer costMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}