package com.iot.qa.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回体：token 用于接口鉴权，同时写入 Cookie 以便页面跳转鉴权。
 */
@Data
public class LoginVO implements Serializable {

    private String token;

    private Long userId;

    private String username;

    private String nickname;

    private String role;
}