package com.iot.qa.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iot.qa.common.PageResult;
import com.iot.qa.common.Result;
import com.iot.qa.common.UserContext;
import com.iot.qa.entity.SysUser;
import com.iot.qa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台：用户管理接口。
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String role) {
        IPage<SysUser> page = userService.pageUsers(current, size, keyword, role);
        return Result.success(PageResult.of(page));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (id.equals(UserContext.getUserId())) {
            return Result.error("不能禁用当前登录的管理员账号");
        }
        userService.updateStatus(id, status);
        return Result.success("状态已更新", null);
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.success("密码已重置", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        if (id.equals(UserContext.getUserId())) {
            return Result.error("不能删除当前登录的管理员账号");
        }
        userService.removeById(id);
        return Result.success("用户已删除", null);
    }
}