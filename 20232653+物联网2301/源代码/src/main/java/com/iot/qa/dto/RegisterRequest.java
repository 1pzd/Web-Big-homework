package com.iot.qa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求参数。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名需为 4~20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需为 6~32 位")
        String password,

        @NotBlank(message = "请再次输入密码")
        String confirmPassword,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 20, message = "昵称最长 20 个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        String email
) {
}