package com.iot.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.iot.qa.entity.QaKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 知识库条目 Mapper。
 */
@Mapper
public interface QaKnowledgeMapper extends BaseMapper<QaKnowledge> {

    /**
     * 分页查询知识条目（关联分类名称），SQL 见 resources/mapper/QaKnowledgeMapper.xml。
     */
    IPage<QaKnowledge> selectPageWithCategory(IPage<QaKnowledge> page,
                                              @Param("keyword") String keyword,
                                              @Param("categoryId") Long categoryId,
                                              @Param("status") Integer status);

    /**
     * 一次性载入全部启用条目，供本地检索引擎使用。
     * 引擎内部会做内存缓存，避免每轮问答都查库。
     */
    @Select("SELECT id, category_id, question, answer, keywords, hit_count, status "
            + "FROM qa_knowledge WHERE deleted = 0 AND status = 1 ORDER BY hit_count DESC")
    List<QaKnowledge> selectAllEnabled();

    /**
     * 热门问题排行。
     */
    @Select("SELECT question AS question, hit_count AS hitCount FROM qa_knowledge "
            + "WHERE deleted = 0 AND hit_count > 0 ORDER BY hit_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectTopHit(@Param("limit") int limit);

    /**
     * 按分类统计知识条目数量。
     */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS num FROM qa_knowledge "
            + "WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countGroupByCategory();

    /**
     * 推荐问题（猜你想问）：按热度取启用状态的标准问题。
     */
    @Select("SELECT question FROM qa_knowledge WHERE deleted = 0 AND status = 1 "
            + "ORDER BY hit_count DESC, id ASC LIMIT #{limit}")
    List<String> selectHotQuestions(@Param("limit") int limit);
}