package com.iot.qa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由：只负责返回 Thymeleaf 视图，数据一律通过 /api/** 接口异步获取。
 */
@Controller
public class PageController {

    // ---------------- 前台 ----------------

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    // ---------------- 后台 ----------------

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/knowledge")
    public String adminKnowledge() {
        return "admin/knowledge";
    }

    @GetMapping("/admin/category")
    public String adminCategory() {
        return "admin/category";
    }

    @GetMapping("/admin/user")
    public String adminUser() {
        return "admin/user";
    }

    @GetMapping("/admin/message")
    public String adminMessage() {
        return "admin/message";
    }

    @GetMapping("/admin/config")
    public String adminConfig() {
        return "admin/config";
    }
}