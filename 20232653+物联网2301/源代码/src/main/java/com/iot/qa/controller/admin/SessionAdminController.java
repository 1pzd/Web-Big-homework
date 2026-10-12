package com.iot.qa.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iot.qa.common.PageResult;
import com.iot.qa.common.Result;
import com.iot.qa.entity.QaMessage;
import com.iot.qa.entity.QaSession;
import com.iot.qa.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台：问答记录管理接口。
 */
@RestController
@RequestMapping("/api/admin/session")
@RequiredArgsConstructor
public class SessionAdminController {

    private final ChatService chatService;

    @GetMapping("/page")
    public Result<PageResult<QaSession>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        IPage<QaSession> page = chatService.pageAllSessions(current, size, keyword);
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<List<QaMessage>> messages(@PathVariable Long sessionId) {
        return Result.success(chatService.listMessages(sessionId, null));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> remove(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId, null);
        return Result.success("会话已删除", null);
    }
}