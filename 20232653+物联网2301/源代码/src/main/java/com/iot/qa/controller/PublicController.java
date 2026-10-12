package com.iot.qa.controller;

import com.iot.qa.common.Result;
import com.iot.qa.mapper.QaKnowledgeMapper;
import com.iot.qa.service.ConfigService;
import com.iot.qa.service.KnowledgeService;
import com.iot.qa.service.StatsService;
import com.iot.qa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 无需登录即可访问的公开接口，供首页展示站点信息与推荐问题。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ConfigService configService;

    private final QaKnowledgeMapper knowledgeMapper;

    private final KnowledgeService knowledgeService;

    private final UserService userService;

    private final StatsService statsService;

    /** 站点信息 */
    @GetMapping("/site")
    public Result<Map<String, String>> site() {
        Map<String, String> data = new HashMap<>(2);
        data.put("name", configService.get("site.name", "智问 · AI 智能问答平台"));
        data.put("notice", configService.get("site.notice", ""));
        return Result.success(data);
    }

    /** 推荐问题（猜你想问） */
    @GetMapping("/hot-questions")
    public Result<List<String>> hotQuestions(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(knowledgeMapper.selectHotQuestions(Math.min(limit, 20)));
    }

    /** 首页展示用的平台规模数据 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>(4);
        data.put("knowledgeCount", knowledgeService.countKnowledge());
        data.put("userCount", userService.countUsers());
        data.put("hitRate", statsService.dashboard().getHitRate());
        return Result.success(data);
    }
}