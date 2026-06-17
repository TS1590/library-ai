package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书 Mapper 接口
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 查询图书列表（联表查分类名）
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.status = 1 " +
            "ORDER BY b.create_time DESC")
    List<Book> selectBookListWithCategory();

    /**
     * 根据分类查图书
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.category_id = #{categoryId} AND b.status = 1")
    List<Book> selectByCategory(@Param("categoryId") Long categoryId);

    /**
     * 搜索图书（按书名/作者）
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.status = 1 AND (b.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR b.author LIKE CONCAT('%',#{keyword},'%'))")
    List<Book> searchBooks(@Param("keyword") String keyword);

    /**
     * 借书：减少可借数量
     */
    @Update("UPDATE book SET available_copies = available_copies - 1 " +
            "WHERE id = #{bookId} AND available_copies > 0")
    int decreaseAvailable(@Param("bookId") Long bookId);

    /**
     * 还书：增加可借数量
     */
    @Update("UPDATE book SET available_copies = available_copies + 1 " +
            "WHERE id = #{bookId} AND available_copies < total_copies")
    int increaseAvailable(@Param("bookId") Long bookId);

    /**
     * 查询热门图书（按借阅次数排序）
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b " +
            "LEFT JOIN category c ON b.category_id = c.id " +
            "LEFT JOIN borrow br ON b.id = br.book_id " +
            "WHERE b.status = 1 " +
            "GROUP BY b.id " +
            "ORDER BY COUNT(br.id) DESC " +
            "LIMIT #{limit}")
    List<Book> selectPopularBooks(@Param("limit") int limit);

    /**
     * 分页查询图书（含分类名，用于管理后台，显示所有状态的图书）
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b LEFT JOIN category c ON b.category_id = c.id " +
            "ORDER BY b.status DESC, b.create_time DESC")
    IPage<Book> selectBookPageWithCategory(Page<Book> page);

    /**
     * 查询最新上架图书
     */
    @Select("SELECT b.*, c.name AS category_name " +
            "FROM book b LEFT JOIN category c ON b.category_id = c.id " +
            "WHERE b.status = 1 " +
            "ORDER BY b.create_time DESC LIMIT #{limit}")
    List<Book> selectLatestBooks(@Param("limit") int limit);
}
