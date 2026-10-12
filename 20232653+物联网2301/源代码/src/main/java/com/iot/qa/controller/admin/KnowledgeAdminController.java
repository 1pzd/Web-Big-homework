package com.iot.qa.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iot.qa.common.PageResult;
import com.iot.qa.common.Result;
import com.iot.qa.dto.KnowledgeForm;
import com.iot.qa.entity.QaKnowledge;
import com.iot.qa.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台：知识库管理接口。
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class KnowledgeAdminController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/page")
    public Result<PageResult<QaKnowledge>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Integer status) {
        IPage<QaKnowledge> page = knowledgeService.pageWithCategory(current, size, keyword, categoryId, status);
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/save")
    public Result<QaKnowledge> save(@Valid @RequestBody KnowledgeForm form) {
        return Result.success("保存成功", knowledgeService.saveOrUpdateKnowledge(form));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        knowledgeService.removeKnowledge(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        knowledgeService.updateStatus(id, status);
        return Result.success("状态已更新", null);
    }
}