package com.iot.qa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库条目表：问答引擎进行相似度检索的数据来源。
 */
@Data
@TableName("qa_knowledge")
public class QaKnowledge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类 */
    private Long categoryId;

    /** 标准问题 */
    private String question;

    /** 标准答案 */
    private String answer;

    /** 关键词，多个用英文逗号分隔，用于提升检索命中率 */
    private String keywords;

    /** 被命中次数，用于热门问题统计 */
    private Integer hitCount;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 分类名称，非数据库字段 */
    @TableField(exist = false)
    private String categoryName;
}