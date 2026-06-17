package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 借阅记录实体类
 */
@TableName("borrow")
public class Borrow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    /** 状态: 0=已归还, 1=借阅中, 2=逾期 */
    private Integer status;
    /** 续借次数 */
    private Integer renewCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 非DB字段
    @TableField(exist = false)
    private String bookTitle;
    @TableField(exist = false)
    private String bookAuthor;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String userRealName;

    public Borrow() {}

    public boolean isOverdue() {
        return returnTime == null && dueTime != null && LocalDateTime.now().isAfter(dueTime);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }
    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRenewCount() { return renewCount; }
    public void setRenewCount(Integer renewCount) { this.renewCount = renewCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserRealName() { return userRealName; }
    public void setUserRealName(String userRealName) { this.userRealName = userRealName; }
}
