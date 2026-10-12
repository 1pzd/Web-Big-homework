package com.iot.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识条目新增 / 修改参数。
 */
public record KnowledgeForm(
        Long id,

        Long categoryId,

        @NotBlank(message = "标准问题不能为空")
        @Size(max = 200, message = "标准问题最长 200 个字符")
        String question,

        @NotBlank(message = "标准答案不能为空")
        String answer,

        @Size(max = 200, message = "关键词最长 200 个字符")
        String keywords,

        Integer status
) {
}