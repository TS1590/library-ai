package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.Book;
import com.example.library.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书业务服务
 */
@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 查询所有在架图书（含分类名）
     */
    public List<Book> listAvailable() {
        return bookMapper.selectBookListWithCategory();
    }

    /**
     * 根据分类查询图书
     */
    public List<Book> listByCategory(Long categoryId) {
        return bookMapper.selectByCategory(categoryId);
    }

    /**
     * 搜索图书
     */
    public List<Book> search(String keyword) {
        return bookMapper.searchBooks(keyword);
    }

    /**
     * 根据ID查询图书
     */
    public Book getById(Long id) {
        return bookMapper.selectById(id);
    }

    /**
     * 根据ISBN查询图书
     */
    public Book getByIsbn(String isbn) {
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("isbn", isbn);
        return bookMapper.selectOne(wrapper);
    }

    /**
     * 添加图书
     */
    public boolean add(Book book) {
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }
        if (book.getStatus() == null) {
            book.setStatus(1);
        }
        return bookMapper.insert(book) > 0;
    }

    /**
     * 更新图书
     */
    public boolean update(Book book) {
        return bookMapper.updateById(book) > 0;
    }

    /**
     * 删除图书（软删除，下架）
     */
    public boolean delete(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) return false;
        book.setStatus(0);
        return bookMapper.updateById(book) > 0;
    }

    /**
     * 查询热门图书
     */
    public List<Book> listPopular(int limit) {
        return bookMapper.selectPopularBooks(limit);
    }

    /**
     * 查询最新上架图书
     */
    public List<Book> listLatest(int limit) {
        return bookMapper.selectLatestBooks(limit);
    }

    /**
     * 获取图书总数
     */
    public long countTotal() {
        return bookMapper.selectCount(null);
    }

    /**
     * 分页查询图书（管理员用，含分类名）
     */
    public IPage<Book> listByPage(int pageNum, int pageSize) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        return bookMapper.selectBookPageWithCategory(page);
    }

    /**
     * 减少可借库存
     */
    public boolean decreaseAvailable(Long bookId) {
        return bookMapper.decreaseAvailable(bookId) > 0;
    }

    /**
     * 增加可借库存
     */
    public boolean increaseAvailable(Long bookId) {
        return bookMapper.increaseAvailable(bookId) > 0;
    }
}
