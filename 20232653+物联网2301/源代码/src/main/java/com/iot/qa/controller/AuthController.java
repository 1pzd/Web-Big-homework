package com.iot.qa.controller;

import com.iot.qa.common.JwtUtil;
import com.iot.qa.common.Result;
import com.iot.qa.common.UserContext;
import com.iot.qa.dto.LoginRequest;
import com.iot.qa.dto.PasswordForm;
import com.iot.qa.dto.RegisterRequest;
import com.iot.qa.entity.SysUser;
import com.iot.qa.service.UserService;
import com.iot.qa.vo.LoginVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：注册、登录、注销、当前用户、修改密码。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功，请登录", null);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginVO vo = userService.login(request);
        writeTokenCookie(response, vo.getToken());
        return Result.success("登录成功", vo);
    }

    /** 后台管理员登录：复用同一套校验，但要求 ADMIN 角色 */
    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginVO vo = userService.login(request);
        if (!"ADMIN".equalsIgnoreCase(vo.getRole())) {
            return Result.error(403, "该账号不是管理员，无法登录后台");
        }
        writeTokenCookie(response, vo.getToken());
        return Result.success("登录成功", vo);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return Result.success("已退出登录", null);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        SysUser user = userService.getByIdOrThrow(UserContext.getUserId());
        Map<String, Object> data = new HashMap<>(6);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("createTime", user.getCreateTime());
        return Result.success(data);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        userService.updateProfile(UserContext.getUserId(), body.get("nickname"), body.get("email"));
        return Result.success("资料已更新", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordForm form) {
        userService.changePassword(UserContext.getUserId(), form);
        return Result.success("密码修改成功，请重新登录", null);
    }

    /** 把 JWT 写入 Cookie，页面跳转与 AJAX 请求共用 */
    private void writeTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(jwtUtil.getExpire().intValue());
        response.addCookie(cookie);
    }
}