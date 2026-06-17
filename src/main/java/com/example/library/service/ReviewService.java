package com.example.library.service;

import com.example.library.entity.Review;
import com.example.library.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书评价业务服务
 */
@Service
public class ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    /**
     * 添加评价
     */
    public boolean addReview(Review review) {
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return false;
        }
        return reviewMapper.insert(review) > 0;
    }

    /**
     * 查询图书所有评价
     */
    public List<Review> getBookReviews(Long bookId) {
        return reviewMapper.selectByBookId(bookId);
    }

    /**
     * 获取图书平均评分
     */
    public double getAvgRating(Long bookId) {
        return reviewMapper.avgRating(bookId);
    }
}
