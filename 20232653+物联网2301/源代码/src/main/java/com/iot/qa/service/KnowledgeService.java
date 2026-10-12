package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.qa.common.BusinessException;
import com.iot.qa.common.ResultCode;
import com.iot.qa.dto.KnowledgeForm;
import com.iot.qa.engine.LocalRetrievalStrategy;
import com.iot.qa.entity.QaKnowledge;
import com.iot.qa.mapper.QaKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 知识库服务：知识条目的增删改查与命中计数。
 *
 * <p>知识条目变更后会调用 {@link LocalRetrievalStrategy#invalidate()}，
 * 让检索引擎的内存快照立即失效，保证新答案能马上被检索到。</p>
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService extends ServiceImpl<QaKnowledgeMapper, QaKnowledge> {

    private final LocalRetrievalStrategy localRetrievalStrategy;

    /** 后台分页查询（带分类名称、关键词过滤） */
    public IPage<QaKnowledge> pageWithCategory(long current, long size, String keyword, Long categoryId, Integer status) {
        Page<QaKnowledge> page = new Page<>(current, size);
        return baseMapper.selectPageWithCategory(page, keyword, categoryId, status);
    }

    /** 新增或修改知识条目 */
    @Transactional(rollbackFor = Exception.class)
    public QaKnowledge saveOrUpdateKnowledge(KnowledgeForm form) {
        QaKnowledge entity = new QaKnowledge();
        entity.setCategoryId(form.categoryId());
        entity.setQuestion(form.question().strip());
        entity.setAnswer(form.answer().strip());
        entity.setKeywords(form.keywords() == null ? "" : form.keywords().strip());
        entity.setStatus(form.status() == null ? 1 : form.status());

        if (form.id() == null) {
            entity.setHitCount(0);
            entity.setDeleted(0);
            save(entity);
        } else {
            entity.setId(form.id());
            if (getById(form.id()) == null) {
                throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
            }
            updateById(entity);
        }
        localRetrievalStrategy.invalidate();
        return entity;
    }

    /** 删除知识条目（逻辑删除） */
    @Transactional(rollbackFor = Exception.class)
    public void removeKnowledge(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        removeById(id);
        localRetrievalStrategy.invalidate();
    }

    /** 启用 / 停用 */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        QaKnowledge entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        entity.setStatus(status);
        updateById(entity);
        localRetrievalStrategy.invalidate();
    }

    /**
     * 命中计数 +1。
     * 使用 SQL 原子自增而非「查出来再写回去」，避免并发下的计数丢失。
     */
    public void incrementHit(Long id) {
        update(new UpdateWrapper<QaKnowledge>()
                .setSql("hit_count = hit_count + 1")
                .eq("id", id));
    }

    /** 按分类统计条目数 */
    public List<Map<String, Object>> countGroupByCategory() {
        return baseMapper.countGroupByCategory();
    }

    /** 热门条目 */
    public List<Map<String, Object>> topHit(int limit) {
        return baseMapper.selectTopHit(limit);
    }

    /** 统计知识条目总数 */
    public long countKnowledge() {
        return count(new LambdaQueryWrapper<QaKnowledge>());
    }
}