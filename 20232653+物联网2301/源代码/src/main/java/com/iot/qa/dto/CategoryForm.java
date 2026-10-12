package com.iot.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类新增 / 修改参数。
 */
public record CategoryForm(
        Long id,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 30, message = "分类名称最长 30 个字符")
        String name,

        Integer sort,

        @Size(max = 200, message = "分类描述最长 200 个字符")
        String description,

        String icon
) {
}