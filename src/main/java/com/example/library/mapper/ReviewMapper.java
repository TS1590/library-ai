package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 图书评价 Mapper 接口
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 查询图书的所有评价
     */
    @Select("SELECT r.*, u.username, u.real_name AS user_real_name " +
            "FROM review r LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.book_id = #{bookId} ORDER BY r.create_time DESC")
    List<Review> selectByBookId(@Param("bookId") Long bookId);

    /**
     * 计算图书平均评分
     */
    @Select("SELECT COALESCE(AVG(rating), 0) FROM review WHERE book_id = #{bookId}")
    double avgRating(@Param("bookId") Long bookId);
}
