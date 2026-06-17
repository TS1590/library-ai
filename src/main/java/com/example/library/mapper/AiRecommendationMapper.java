package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.AiRecommendation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI推荐记录 Mapper 接口
 */
@Mapper
public interface AiRecommendationMapper extends BaseMapper<AiRecommendation> {

    /**
     * 查询用户最近的推荐记录（含图书信息）
     */
    @Select("SELECT ar.*, b.title AS book_title, b.author AS book_author, b.cover_url AS book_cover_url " +
            "FROM ai_recommendation ar LEFT JOIN book b ON ar.book_id = b.id " +
            "WHERE ar.user_id = #{userId} ORDER BY ar.create_time DESC LIMIT #{limit}")
    List<AiRecommendation> selectByUserWithBook(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 删除用户的旧推荐记录
     */
    @Delete("DELETE FROM ai_recommendation WHERE user_id = #{userId} AND is_read = 1 " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    void deleteOldRecommendations(@Param("userId") Long userId);
}
