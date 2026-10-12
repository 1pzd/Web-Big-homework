package com.iot.qa.controller.admin;

import com.iot.qa.common.Result;
import com.iot.qa.entity.SysConfig;
import com.iot.qa.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台：系统参数配置接口（问答模式、命中阈值、大模型密钥等）。
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class ConfigAdminController {

    private final ConfigService configService;

    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        return Result.success(configService.listAll());
    }

    @PutMapping("/save")
    public Result<Void> save(@RequestBody Map<String, String> body) {
        String key = body.get("configKey");
        if (key == null || key.isBlank()) {
            return Result.error("参数名不能为空");
        }
        configService.save(key, body.getOrDefault("configValue", ""));
        return Result.success("参数已保存", null);
    }
}