package com.iot.qa.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户信息（由 JWT 解析得到，非数据库实体）。
 */
@Data
public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private String role;

    /** 是否为管理员 */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}