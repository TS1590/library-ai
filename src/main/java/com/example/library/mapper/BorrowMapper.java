package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Borrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 借阅记录 Mapper 接口
 */
@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {

    /**
     * 查询用户当前在借的图书数量
     */
    @Select("SELECT COUNT(*) FROM borrow WHERE user_id = #{userId} AND status IN (1, 2)")
    int countCurrentBorrows(@Param("userId") Long userId);

    /**
     * 查询用户的借阅记录（含图书信息）
     */
    @Select("SELECT br.*, b.title AS book_title, b.author AS book_author " +
            "FROM borrow br LEFT JOIN book b ON br.book_id = b.id " +
            "WHERE br.user_id = #{userId} ORDER BY br.create_time DESC")
    List<Borrow> selectByUserWithBook(@Param("userId") Long userId);

    /**
     * 查询所有借阅记录（管理员视图）
     */
    @Select("SELECT br.*, b.title AS book_title, b.author AS book_author, " +
            "u.username, u.real_name AS user_real_name " +
            "FROM borrow br " +
            "LEFT JOIN book b ON br.book_id = b.id " +
            "LEFT JOIN user u ON br.user_id = u.id " +
            "ORDER BY br.create_time DESC")
    List<Borrow> selectAllWithDetails();

    /**
     * 查询单个用户最近借阅的图书类别（用于AI推荐）
     */
    @Select("SELECT DISTINCT b.category_id FROM borrow br " +
            "JOIN book b ON br.book_id = b.id " +
            "WHERE br.user_id = #{userId} " +
            "ORDER BY br.create_time DESC LIMIT #{limit}")
    List<Long> selectRecentCategories(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 更新逾期状态
     */
    @Update("UPDATE borrow SET status = 2 WHERE status = 1 AND due_time < NOW()")
    int updateOverdueStatus();
}
