package com.iot.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提问请求参数。
 */
public record ChatRequest(
        /** 会话 ID，为空表示开启新会话 */
        Long sessionId,

        @NotBlank(message = "问题内容不能为空")
        @Size(max = 500, message = "单个问题最长 500 个字符")
        String question
) {
}