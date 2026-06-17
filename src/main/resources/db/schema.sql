-- =============================================
-- 校园图书管理系统 数据库初始化脚本
-- 数据库: library_ai
-- =============================================

CREATE DATABASE IF NOT EXISTS library_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE library_ai;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码（MD5加密）',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0=读者, 1=管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
    `avatar_url`  VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 图书分类表
-- =============================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- =============================================
-- 3. 图书表
-- =============================================
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    `isbn`        VARCHAR(20)  DEFAULT NULL COMMENT 'ISBN号',
    `title`       VARCHAR(200) NOT NULL COMMENT '书名',
    `author`      VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `publisher`   VARCHAR(100) DEFAULT NULL COMMENT '出版社',
    `category_id` BIGINT       DEFAULT NULL COMMENT '分类ID',
    `description` TEXT         DEFAULT NULL COMMENT '图书简介',
    `cover_url`   VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
    `total_copies` INT         NOT NULL DEFAULT 0 COMMENT '总副本数',
    `available_copies` INT     NOT NULL DEFAULT 0 COMMENT '可借副本数',
    `location`    VARCHAR(100) DEFAULT NULL COMMENT '馆藏位置',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=下架, 1=在架',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_isbn` (`isbn`),
    KEY `idx_title` (`title`),
    KEY `idx_author` (`author`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- =============================================
-- 4. 借阅记录表
-- =============================================
DROP TABLE IF EXISTS `borrow`;
CREATE TABLE `borrow` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '借阅ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`      BIGINT   NOT NULL COMMENT '图书ID',
    `borrow_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    `due_time`     DATETIME NOT NULL COMMENT '应还时间（默认借阅后30天）',
    `return_time`  DATETIME DEFAULT NULL COMMENT '实际归还时间',
    `status`       TINYINT  NOT NULL DEFAULT 1 COMMENT '状态: 0=已归还, 1=借阅中, 2=逾期',
    `renew_count`  INT      DEFAULT 0 COMMENT '续借次数',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- =============================================
-- 5. 图书评价表（新增 - AI推荐数据基础）
-- =============================================
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `book_id`     BIGINT   NOT NULL COMMENT '图书ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `rating`      INT      NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content`     TEXT     DEFAULT NULL COMMENT '评价内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `chk_rating` CHECK (`rating` >= 1 AND `rating` <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评价表';

-- =============================================
-- 6. AI聊天历史表（新增）
-- =============================================
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `session_id`  VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色: user/assistant',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天历史表';

-- =============================================
-- 7. AI推荐记录表（新增）
-- =============================================
DROP TABLE IF EXISTS `ai_recommendation`;
CREATE TABLE `ai_recommendation` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '推荐ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT   NOT NULL COMMENT '推荐图书ID',
    `reason`      TEXT     DEFAULT NULL COMMENT 'AI生成的推荐理由',
    `score`       DOUBLE   DEFAULT 0.0 COMMENT '推荐分数',
    `is_read`     TINYINT  DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI推荐记录表';

-- =============================================
-- 初始化测试数据
-- =============================================

-- 测试账号（密码使用MD5加密）
-- 管理员: admin / admin123  (MD5: 0192023a7bbd73250516f069df18b500)
-- 读者1: reader1 / 123456  (MD5: e10adc3949ba59abbe56e057f20f883e)
-- 读者2: reader2 / 123456  (MD5: e10adc3949ba59abbe56e057f20f883e)
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 1, 1),
('reader1', 'e10adc3949ba59abbe56e057f20f883e', '张三', 0, 1),
('reader2', 'e10adc3949ba59abbe56e057f20f883e', '李四', 0, 1);

-- 图书分类
INSERT INTO `category` (`name`, `description`) VALUES
('计算机科学', '计算机编程、算法、人工智能等'),
('文学小说', '中外文学名著、小说'),
('历史地理', '历史、地理、传记类'),
('自然科学', '数学、物理、化学、生物等'),
('社会科学', '经济、哲学、心理学等'),
('艺术设计', '绘画、设计、音乐类');

-- 示例图书（封面默认为NULL，管理员可通过后台「添加图书」上传真实封面）
INSERT INTO `book` (`isbn`, `title`, `author`, `publisher`, `category_id`, `description`, `cover_url`, `total_copies`, `available_copies`, `location`) VALUES
('978-7-111-66666-1', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', 1, 'Java程序设计经典著作，全面深入讲解Java语言核心概念。', NULL, 5, 5, 'A区-01-01'),
('978-7-111-66666-2', 'Spring Boot实战', 'Craig Walls', '人民邮电出版社', 1, '全面介绍Spring Boot框架的使用，从入门到进阶。', NULL, 3, 3, 'A区-01-02'),
('978-7-111-66666-3', '深入理解MySQL', 'Sasha Pachev', '中国电力出版社', 1, 'MySQL数据库原理与性能优化实战。', NULL, 4, 4, 'A区-01-03'),
('978-7-111-66666-4', '三体', '刘慈欣', '重庆出版社', 2, '科幻小说巨作，雨果奖获奖作品。', NULL, 6, 6, 'B区-02-01'),
('978-7-111-66666-5', '活着', '余华', '作家出版社', 2, '一个普通人在时代变迁中的苦难与坚韧。', NULL, 4, 4, 'B区-02-02'),
('978-7-111-66666-6', '人类简史', '尤瓦尔·赫拉利', '中信出版社', 3, '从认知革命到人工智能，人类文明的宏大叙事。', NULL, 3, 3, 'C区-03-01'),
('978-7-111-66666-7', '算法导论', 'Thomas H. Cormen', '机械工业出版社', 1, '计算机算法领域的经典教材。', NULL, 3, 3, 'A区-01-04'),
('978-7-111-66666-8', '百年孤独', '加西亚·马尔克斯', '南海出版公司', 2, '魔幻现实主义文学代表作。', NULL, 2, 2, 'B区-02-03'),
('978-7-111-66666-9', '设计模式', 'Erich Gamma', '机械工业出版社', 1, '面向对象软件设计模式经典著作。', NULL, 3, 3, 'A区-01-05'),
('978-7-111-66667-0', '人工智能简史', '尼克', '人民邮电出版社', 1, '人工智能发展历程与技术演进。', NULL, 2, 2, 'A区-02-01');
