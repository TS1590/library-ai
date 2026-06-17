package com.example.library.service;

import com.example.library.entity.Category;
import com.example.library.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书分类业务服务
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> listAll() {
        return categoryMapper.selectList(null);
    }

    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    public boolean save(Category category) {
        return categoryMapper.insert(category) > 0;
    }

    public boolean update(Category category) {
        return categoryMapper.updateById(category) > 0;
    }

    public boolean delete(Long id) {
        return categoryMapper.deleteById(id) > 0;
    }
}
