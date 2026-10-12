package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.qa.common.BusinessException;
import com.iot.qa.common.JwtUtil;
import com.iot.qa.common.ResultCode;
import com.iot.qa.dto.LoginRequest;
import com.iot.qa.dto.PasswordForm;
import com.iot.qa.dto.RegisterRequest;
import com.iot.qa.entity.SysUser;
import com.iot.qa.mapper.SysUserMapper;
import com.iot.qa.util.PasswordUtil;
import com.iot.qa.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务：注册、登录、资料与密码维护，以及后台用户管理。
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final JwtUtil jwtUtil;

    /** 注册 */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (existsByUsername(request.username())) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(PasswordUtil.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setRole("USER");
        user.setStatus(STATUS_ENABLED);
        user.setDeleted(0);
        save(user);
    }

    /** 登录，成功返回 token 与用户信息 */
    public LoginVO login(LoginRequest request) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("LIMIT 1"));
        if (user == null || !PasswordUtil.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    public boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
    }

    /** 查询用户，不存在则抛业务异常 */
    public SysUser getByIdOrThrow(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** 修改个人资料 */
    public void updateProfile(Long userId, String nickname, String email) {
        SysUser user = getByIdOrThrow(userId);
        user.setNickname(nickname);
        user.setEmail(email);
        updateById(user);
    }

    /** 修改密码 */
    public void changePassword(Long userId, PasswordForm form) {
        SysUser user = getByIdOrThrow(userId);
        if (!PasswordUtil.matches(form.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        user.setPassword(PasswordUtil.encode(form.newPassword()));
        updateById(user);
    }

    /** 后台分页查询用户 */
    public IPage<SysUser> pageUsers(long current, long size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getId);
        return page(new Page<>(current, size), wrapper);
    }

    /** 启用 / 禁用用户 */
    public void updateStatus(Long userId, Integer status) {
        SysUser user = getByIdOrThrow(userId);
        user.setStatus(status == null ? STATUS_ENABLED : status);
        updateById(user);
    }

    /** 管理员重置密码 */
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getByIdOrThrow(userId);
        user.setPassword(PasswordUtil.encode(newPassword));
        updateById(user);
    }

    /** 统计用户总数 */
    public long countUsers() {
        return count();
    }
}