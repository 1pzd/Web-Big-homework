package com.iot.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答消息表：保存每一轮对话的用户提问与系统回答。
 */
@Data
@TableName("qa_message")
public class QaMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    /** 角色：user 提问者，assistant 回答者 */
    private String role;

    /** 消息正文 */
    private String content;

    /** 命中的知识条目 ID，未命中为 null */
    private Long knowledgeId;

    /** 相似度得分，0~1 */
    private Double score;

    /** 答案来源：knowledge 知识库 / llm 大模型 / fallback 兜底 */
    private String source;

    /** 用户反馈：1 有帮助，-1 没帮助，0 未评价 */
    private Integer feedback;

    /** 引擎耗时（毫秒），用于性能观测 */
    private Integer costMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 命中的标准问题，非数据库字段，便于前端展示依据 */
    @TableField(exist = false)
    private String matchedQuestion;
}