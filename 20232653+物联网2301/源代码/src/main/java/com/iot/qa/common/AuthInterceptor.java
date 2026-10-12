package com.iot.qa.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录与权限拦截器。
 *
 * <p>token 优先从请求头读取，其次从 Cookie 读取，这样页面跳转（服务端渲染）
 * 与 AJAX 接口调用可以共用同一套凭证。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${ai.jwt.header}")
    private String headerName;

    @Value("${ai.jwt.prefix}")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        LoginUser loginUser = jwtUtil.parseToken(token);

        if (loginUser == null) {
            reject(request, response, ResultCode.UNAUTHORIZED);
            return false;
        }

        // 管理端接口与页面需要 ADMIN 角色
        String path = request.getRequestURI();
        if ((path.startsWith("/admin") || path.startsWith("/api/admin")) && !loginUser.isAdmin()) {
            reject(request, response, ResultCode.FORBIDDEN);
            return false;
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(headerName);
        if (header != null && header.startsWith(tokenPrefix)) {
            return header.substring(tokenPrefix.length()).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, ResultCode code) throws Exception {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(code.getCode() == 401 ? 401 : 403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\":%d,\"message\":\"%s\",\"data\":null}", code.getCode(), code.getMessage()));
        } else {
            String target = request.getRequestURI().startsWith("/admin") ? "/admin/login" : "/login";
            response.sendRedirect(target);
        }
    }
}