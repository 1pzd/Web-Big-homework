package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.qa.common.BusinessException;
import com.iot.qa.common.ResultCode;
import com.iot.qa.dto.CategoryForm;
import com.iot.qa.entity.QaCategory;
import com.iot.qa.entity.QaKnowledge;
import com.iot.qa.mapper.QaCategoryMapper;
import com.iot.qa.mapper.QaKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识分类服务。
 */
@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<QaCategoryMapper, QaCategory> {

    private final QaKnowledgeMapper knowledgeMapper;

    /** 查询全部分类，并附带各分类下的知识条目数量 */
    public List<QaCategory> listWithCount() {
        List<QaCategory> categories = list(new LambdaQueryWrapper<QaCategory>()
                .orderByAsc(QaCategory::getSort)
                .orderByAsc(QaCategory::getId));

        Map<Long, Long> countMap = knowledgeMapper.countGroupByCategory().stream()
                .filter(row -> row.get("categoryId") != null && row.get("num") != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("categoryId")).longValue(),
                        row -> ((Number) row.get("num")).longValue(),
                        (a, b) -> a));

        categories.forEach(c -> c.setKnowledgeCount(
                countMap.getOrDefault(c.getId(), 0L).intValue()));
        return categories;
    }

    /** 新增或修改分类 */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateCategory(CategoryForm form) {
        if (form.id() == null) {
            QaCategory entity = new QaCategory();
            entity.setName(form.name().strip());
            entity.setSort(form.sort() == null ? 0 : form.sort());
            entity.setDescription(form.description());
            entity.setIcon(form.icon() == null ? "icon-default" : form.icon());
            entity.setDeleted(0);
            save(entity);
        } else {
            QaCategory entity = getById(form.id());
            if (entity == null) {
                throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
            }
            entity.setName(form.name().strip());
            entity.setSort(form.sort() == null ? 0 : form.sort());
            entity.setDescription(form.description());
            if (form.icon() != null) {
                entity.setIcon(form.icon());
            }
            updateById(entity);
        }
    }

    /** 删除分类，分类下仍有知识条目时禁止删除 */
    @Transactional(rollbackFor = Exception.class)
    public void removeCategory(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        long knowledgeCount = knowledgeMapper.selectCount(new LambdaQueryWrapper<QaKnowledge>()
                .eq(QaKnowledge::getCategoryId, id));
        if (knowledgeCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_IN_USE);
        }
        removeById(id);
    }

    /** 分类下拉选项 */
    public List<QaCategory> options() {
        return list(new LambdaQueryWrapper<QaCategory>()
                .select(QaCategory::getId, QaCategory::getName)
                .orderByAsc(QaCategory::getSort));
    }
}