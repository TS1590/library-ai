package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.BorrowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅管理业务服务
 */
@Service
public class BorrowService {

    /** 最大借阅数量 */
    private static final int MAX_BORROW_COUNT = 5;

    /** 默认借阅天数 */
    private static final int BORROW_DAYS = 30;

    /** 最大续借次数 */
    private static final int MAX_RENEW_COUNT = 2;

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private BookMapper bookMapper;

    /**
     * 借阅图书（事务操作）
     */
    @Transactional
    public String borrowBook(Long userId, Long bookId) {
        // 1. 检查用户当前借阅数量
        int currentCount = borrowMapper.countCurrentBorrows(userId);
        if (currentCount >= MAX_BORROW_COUNT) {
            return "借阅失败：您当前已借阅 " + currentCount + " 本书，达到最大借阅数量 " + MAX_BORROW_COUNT + " 本";
        }

        // 2. 检查图书是否可借
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() == 0) {
            return "借阅失败：图书不存在或已下架";
        }
        if (book.getAvailableCopies() <= 0) {
            return "借阅失败：图书库存不足，当前可借数量为 0";
        }

        // 3. 检查是否已借过同一本书（未归还）
        List<Borrow> userBorrows = borrowMapper.selectByUserWithBook(userId);
        boolean alreadyBorrowed = userBorrows.stream()
                .anyMatch(b -> b.getBookId().equals(bookId) && b.getReturnTime() == null);
        if (alreadyBorrowed) {
            return "借阅失败：您已借阅过《" + book.getTitle() + "》，请先归还后再借";
        }

        // 4. 创建借阅记录
        Borrow borrow = new Borrow();
        borrow.setUserId(userId);
        borrow.setBookId(bookId);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueTime(LocalDateTime.now().plusDays(BORROW_DAYS));
        borrow.setStatus(1); // 借阅中
        borrowMapper.insert(borrow);

        // 5. 减少图书可借库存
        bookMapper.decreaseAvailable(bookId);

        return "借阅成功：《" + book.getTitle() + "》，请于 "
                + borrow.getDueTime().toLocalDate() + " 前归还";
    }

    /**
     * 归还图书（事务操作）
     */
    @Transactional
    public String returnBook(Long borrowId, Long userId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null) {
            return "归还失败：借阅记录不存在";
        }
        if (!borrow.getUserId().equals(userId)) {
            return "归还失败：非法操作";
        }
        if (borrow.getReturnTime() != null) {
            return "归还失败：该图书已归还";
        }

        // 更新借阅记录
        borrow.setReturnTime(LocalDateTime.now());
        borrow.setStatus(0); // 已归还
        borrowMapper.updateById(borrow);

        // 增加图书可借库存
        bookMapper.increaseAvailable(borrow.getBookId());

        // 检查是否逾期归还
        if (borrow.getDueTime() != null && LocalDateTime.now().isAfter(borrow.getDueTime())) {
            return "归还成功（注意：逾期归还，请遵守借阅规则）";
        }

        return "归还成功";
    }

    /**
     * 续借图书
     */
    @Transactional
    public String renewBook(Long borrowId, Long userId) {
        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null || !borrow.getUserId().equals(userId)) {
            return "续借失败：借阅记录不存在";
        }
        if (borrow.getReturnTime() != null) {
            return "续借失败：该图书已归还";
        }
        if (borrow.getRenewCount() != null && borrow.getRenewCount() >= MAX_RENEW_COUNT) {
            return "续借失败：已达到最大续借次数 " + MAX_RENEW_COUNT + " 次";
        }

        // 更新续借次数和到期时间
        borrow.setRenewCount((borrow.getRenewCount() == null ? 0 : borrow.getRenewCount()) + 1);
        borrow.setDueTime(LocalDateTime.now().plusDays(BORROW_DAYS));
        borrowMapper.updateById(borrow);

        return "续借成功：到期时间延长至 " + borrow.getDueTime().toLocalDate();
    }

    /**
     * 查询用户借阅记录（含图书信息）
     */
    public List<Borrow> getUserBorrows(Long userId) {
        return borrowMapper.selectByUserWithBook(userId);
    }

    /**
     * 查询所有借阅记录（管理员）
     */
    public List<Borrow> listAllBorrows() {
        return borrowMapper.selectAllWithDetails();
    }

    /**
     * 获取用户在借数量
     */
    public int countCurrentBorrows(Long userId) {
        return borrowMapper.countCurrentBorrows(userId);
    }

    /**
     * 更新逾期状态
     */
    public void updateOverdueStatus() {
        borrowMapper.updateOverdueStatus();
    }
}
