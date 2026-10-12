package com.iot.qa.common;

import lombok.Getter;

/**
 * 业务状态码枚举。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    USERNAME_EXISTS(1001, "用户名已被注册"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),
    OLD_PASSWORD_ERROR(1005, "原密码不正确"),
    KNOWLEDGE_NOT_FOUND(2001, "知识条目不存在"),
    CATEGORY_NOT_FOUND(2002, "分类不存在"),
    CATEGORY_IN_USE(2003, "该分类下仍有知识条目，无法删除"),
    SESSION_NOT_FOUND(3001, "会话不存在"),
    ENGINE_ERROR(4001, "问答引擎调用失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}