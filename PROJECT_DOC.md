# 校园图书管理系统（含 AI 智能体）— 项目文档

> **适用场景**：课程答辩、组员上手、项目交接  
> **最后更新**：2026-06-17

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [系统架构](#3-系统架构)
4. [数据库设计](#4-数据库设计)
5. [模块详解（按分工）](#5-模块详解)
   - [5.1 姚晓飞：整体架构 + AI 智能体](#51-姚晓飞整体架构--ai-智能体)
   - [5.2 郑博涵：用户管理 + 图书管理](#52-郑博涵用户管理--图书管理)
   - [5.3 马李发：借阅管理 + 管理后台](#53-马李发借阅管理--管理后台)
   - [5.4 涂恩恺：前端模板 + 系统测试](#54-涂恩恺前端模板--系统测试)
6. [API 接口文档](#6-api-接口文档)
7. [部署与运行](#7-部署与运行)
8. [测试指南](#8-测试指南)
9. [FAQ / 已知问题](#9-faq--已知问题)

---

## 1. 项目概述

**校园图书管理系统**是一个基于 Spring Boot 的 Web 应用，面向校园图书馆场景，提供图书管理、借阅管理、用户管理等基础功能，并集成 **AI 智能体（通义千问大模型）**，实现智能聊天、个性化图书推荐、AI 图书分析等创新功能。

### 核心功能

| 功能模块 | 说明 |
|----------|------|
| 用户认证 | 注册、登录、MD5 密码加密、角色权限（读者/管理员） |
| 图书管理 | 录入、编辑、下架、搜索、分类筛选、封面上传 |
| 借阅管理 | 借书、还书、续借、逾期标记（事务保护） |
| 管理后台 | 仪表盘统计、用户管理、借阅管理、分类管理 |
| AI 聊天机器人 | 悬浮对话框，Function Calling 让 AI 直接操作图书馆（借书/还书/搜索） |
| AI 图书推荐 | 基于借阅历史，DeepSeek 分析偏好，个性化推荐 |
| AI 图书分析 | 生成 AI 摘要、关键词标签、读者群体、难度等级 |

---

## 2. 技术栈

| 层面 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.18 |
| 数据访问 | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 前端模板 | Thymeleaf | (Spring Boot 内置) |
| 前端交互 | 原生 JavaScript (fetch + AJAX) | — |
| AI 大模型 | 通义千问 Plus (qwen-plus) / 阿里云百炼平台 | — |
| AI 通信 | HTTP REST 调用（兼容 OpenAI Chat Completions 格式） | — |
| 构建工具 | Maven | 3.6+ |
| JDK | 1.8 | — |
| 安全 | 拦截器 + Session + MD5 | — |

---

## 3. 系统架构

### 3.1 总体分层架构

```
┌──────────────────────────────────────────────────────┐
│                    浏览器 (Thymeleaf)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │ 首页/图书 │ │ 登录注册 │ │ 管理后台 │ │ AI聊天   │ │
│  │ 浏览/详情 │ │          │ │          │ │ 机器人   │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP
┌──────────────────────┴───────────────────────────────┐
│                 Spring Boot 后端                       │
│                                                       │
│  ┌─────────────┐                                     │
│  │ Interceptor  │  ← 登录拦截 + 管理员权限拦截        │
│  └──────┬──────┘                                     │
│         │                                             │
│  ┌──────┴──────┐                                     │
│  │  Controller │  ← 6 个控制器（含 AI 控制器）        │
│  └──────┬──────┘                                     │
│         │                                             │
│  ┌──────┴──────┐                                     │
│  │   Service   │  ← 业务逻辑 + AI 服务层              │
│  └──────┬──────┘                                     │
│         │                                             │
│  ┌──────┴──────┐      ┌─────────────────┐            │
│  │   Mapper    │      │ DeepSeekClient  │            │
│  │ (MyBatis+)  │      │ (HTTP → AI API) │            │
│  └──────┬──────┘      └────────┬────────┘            │
└─────────┼──────────────────────┼─────────────────────┘
          │                      │
    ┌─────┴─────┐      ┌────────┴────────┐
    │   MySQL   │      │  通义千问 API    │
    │   8.0     │      │  (阿里云百炼)    │
    └───────────┘      └─────────────────┘
```

### 3.2 请求处理流程

```
浏览器请求 → WebConfig (拦截器链)
  ├─ 白名单路径 (/login, /register, /css/**, /js/** 等) → 直接放行
  ├─ /admin/** 路径 → LoginInterceptor → AdminInterceptor → Controller
  └─ 其他路径 → LoginInterceptor → Controller
```

### 3.3 项目文件结构（关键文件）

```
library-ai/
├── pom.xml                                    # Maven 依赖
├── README.md                                  # 快速开始指南
├── PROJECT_DOC.md                             # 本文档
├── upload/covers/                             # 用户上传的图书封面
├── src/main/java/com/example/library/
│   ├── LibraryApplication.java                # Spring Boot 启动入口
│   ├── config/
│   │   ├── AIConfig.java                      # RestTemplate Bean
│   │   ├── DeepSeekProperties.java            # AI 配置属性
│   │   ├── MyBatisPlusConfig.java             # MyBatis-Plus 分页插件
│   │   ├── WebConfig.java                     # 拦截器注册 + 静态资源映射
│   │   └── MetaObjectHandlerConfig.java       # 自动填充 createTime/updateTime
│   ├── controller/
│   │   ├── HomeController.java                # 首页、图书列表、搜索、详情
│   │   ├── AuthController.java                # 登录/注册/登出
│   │   ├── BookController.java                # 图书 CRUD + 封面图片上传 + 借阅/归还/续借 AJAX
│   │   ├── AdminController.java               # 管理后台（仪表盘/用户/借阅/分类）
│   │   ├── UserController.java                # 个人中心（资料修改/密码修改）
│   │   └── AIController.java                  # AI 聊天/推荐/分析接口
│   ├── entity/
│   │   ├── User.java                          # 用户实体
│   │   ├── Book.java                          # 图书实体
│   │   ├── Category.java                      # 分类实体
│   │   ├── Borrow.java                        # 借阅记录实体
│   │   ├── Review.java                        # 评价实体
│   │   ├── ChatHistory.java                   # 聊天历史实体
│   │   └── AiRecommendation.java              # AI 推荐记录实体
│   ├── mapper/                                # 7 个 Mapper 接口（MyBatis-Plus）
│   ├── service/
│   │   ├── BookService.java                   # 图书业务
│   │   ├── BorrowService.java                 # 借阅业务（@Transactional）
│   │   ├── UserService.java                   # 用户业务（MD5 加密）
│   │   ├── CategoryService.java               # 分类业务
│   │   ├── ReviewService.java                 # 评价业务
│   │   └── ai/
│   │       ├── DeepSeekClient.java            # AI HTTP 客户端（核心）
│   │       ├── ChatSessionManager.java        # 会话管理（内存）
│   │       ├── BookRecommendService.java      # AI 推荐服务
│   │       ├── BookAnalysisService.java       # AI 分析服务（含缓存）
│   │       └── LibraryToolService.java        # Function Calling 工具集
│   └── interceptor/
│       ├── LoginInterceptor.java              # 登录检查
│       └── AdminInterceptor.java              # 管理员权限检查
└── src/main/resources/
    ├── application.yml                        # 应用配置（数据库/AI）
    ├── db/schema.sql                          # 数据库建表 + 种子数据
    ├── static/css/                            # 样式表
    │   ├── style.css
    │   └── chatbot.css
    └── templates/                             # Thymeleaf 模板
        ├── index.html                         # 首页
        ├── login.html / register.html         # 登录/注册
        ├── fragments/
        │   ├── layout.html                    # 公共布局（导航栏/页脚/聊天组件）
        │   └── chatbot.html                   # AI 聊天机器人组件（内联 JS）
        ├── book/                              # 图书相关页面
        │   ├── list.html                      # 图书列表
        │   ├── detail.html                    # 图书详情
        │   ├── add.html                       # 添加图书（管理员）
        │   └── edit.html                      # 编辑图书（管理员）
        ├── admin/                             # 管理后台页面
        │   ├── dashboard.html                 # 仪表盘
        │   ├── books.html                     # 图书管理
        │   ├── users.html                     # 用户管理
        │   ├── borrows.html                   # 借阅管理
        │   └── categories.html                # 分类管理
        ├── ai/recommend.html                  # AI 推荐页面
        └── user/profile.html                  # 个人中心
```

---

## 4. 数据库设计

### 4.1 ER 关系

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│     user     │       │     book     │       │   category   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │──┐    │ id (PK)      │──┐    │ id (PK)      │
│ username     │  │    │ isbn         │  │    │ name         │
│ password     │  │    │ title        │  │    │ description  │
│ real_name    │  │    │ author       │  │    └──────────────┘
│ role         │  │    │ publisher    │  │            ↑
│ status       │  │    │ category_id  │──┼────────────┘
└──────────────┘  │    │ description  │  │
       │          │    │ cover_url    │  │
       │          │    │ total_copies │  │
       │          │    │ available_copies│
       │          │    │ location     │  │
       │          │    │ status       │  │
       │          │    └──────────────┘  │
       │          │           │          │
       │    ┌─────┴───────────┴──────────┴──────┐
       │    │              borrow                │
       │    ├────────────────────────────────────┤
       └────│ user_id (FK)                       │
            │ book_id (FK)                       │
            │ borrow_time / due_time / return_time│
            │ status (0=已还/1=在借/2=逾期)       │
            │ renew_count                        │
            └────────────────────────────────────┘

┌──────────────┐       ┌──────────────────────┐
│    review    │       │  ai_recommendation    │
├──────────────┤       ├──────────────────────┤
│ id (PK)      │       │ id (PK)              │
│ book_id (FK) │       │ user_id (FK)         │
│ user_id (FK) │       │ book_id (FK)         │
│ rating (1-5) │       │ reason               │
│ content      │       │ score                │
└──────────────┘       └──────────────────────┘

┌──────────────┐
│ chat_history │
├──────────────┤
│ id (PK)      │
│ user_id (FK) │
│ session_id   │
│ role         │
│ content      │
└──────────────┘
```

### 4.2 7 张数据表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | username(UK), password(MD5), role(0读者/1管理员), status(0禁用/1正常) |
| `category` | 图书分类 | name(UK), description |
| `book` | 图书表 | isbn(UK), title, author, category_id(FK), cover_url, total/available_copies, status |
| `borrow` | 借阅记录 | user_id(FK), book_id(FK), borrow_time, due_time, return_time, status, renew_count |
| `review` | 图书评价 | book_id(FK), user_id(FK), rating(1-5), content |
| `chat_history` | AI 聊天历史 | user_id(FK), session_id, role(user/assistant), content |
| `ai_recommendation` | AI 推荐记录 | user_id(FK), book_id(FK), reason, score, is_read |

### 4.3 种子数据

- **3 个测试用户**：admin(管理员)、reader1(读者)、reader2(读者)
- **6 个图书分类**：计算机科学、文学小说、历史地理、自然科学、社会科学、艺术设计
- **10 本示例图书**：涵盖各分类，封面初始为 NULL（显示 📖 emoji 占位符）

---

## 5. 模块详解

### 5.1 姚晓飞：整体架构 + AI 智能体

#### 负责范围

- 项目整体架构设计（分层结构、配置管理）
- 数据库设计（7 张表的 DDL、索引、种子数据）
- AI 智能体模块全部开发
- 项目报告撰写

#### 关键文件清单

```
config/
├── AIConfig.java                 # RestTemplate Bean 定义
├── DeepSeekProperties.java       # AI 配置属性（api-key/model/temperature等）
├── MyBatisPlusConfig.java        # MyBatis-Plus 分页插件
├── WebConfig.java                # 拦截器注册 + 静态资源路径映射
└── MetaObjectHandlerConfig.java  # 自动填充 createTime/updateTime

service/ai/
├── DeepSeekClient.java           # AI HTTP 客户端 ★★★
├── ChatSessionManager.java       # 对话会话管理（内存 ConcurrentHashMap）
├── BookRecommendService.java     # AI 个性化图书推荐
├── BookAnalysisService.java      # AI 图书分析（含缓存）
└── LibraryToolService.java       # Function Calling 工具集 ★★★

controller/AIController.java      # AI 接口（聊天/推荐/分析/状态）
entity/（所有 7 个实体类）
resources/db/schema.sql           # 数据库建表脚本
resources/application.yml         # 主配置文件（数据源 + AI 参数）
```

#### 核心设计决策

**① AI 调用方式 — HTTP 直连（非 SDK）**

```java
// DeepSeekClient.callWithTools() — 核心方法
// 构建 OpenAI 兼容的 JSON 请求体：
{
  "model": "qwen-plus",
  "messages": [...],
  "tools": [...],          // Function Calling 工具定义
  "tool_choice": "auto"
}
// POST → https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
```

选择用 `RestTemplate` 直接构建 HTTP 请求而非 SDK，原因：
- DeepSeek / 通义千问 API 完全兼容 OpenAI 格式
- 避免额外的 SDK 依赖
- 更灵活地控制请求参数

**② Function Calling 设计 — AI 直接操作图书馆**

```java
// AIController.chat() 中的核心循环
for (int round = 0; round < 3; round++) {
    1. 发送消息 + 工具定义 → DeepSeek
    2. 检查响应中是否有 tool_calls
    3. 如果有 → executeTool() 执行实际操作
    4. 将工具执行结果返回给 AI
    5. 如果没有 → 纯文本回复，结束
}
```

AI 可以执行的 6 个工具：
| 工具名 | 功能 | 触发场景 |
|--------|------|----------|
| `search_books` | 搜索图书 | 用户问"有哪些Java书" |
| `borrow_book` | 借阅图书 | 用户说"帮我借《三体》" |
| `return_book` | 归还图书 | 用户说"帮我还书" |
| `get_my_borrows` | 查借阅记录 | 用户问"我借了哪些书" |
| `get_book_info` | 查图书详情 | 用户问某本书的详情 |
| `get_recommendations` | 获取推荐 | 用户要推荐 |

**③ AI 推荐策略**

```
用户借阅历史（最近5本） + 全部在架图书（前20本）
     ↓
构建 Prompt → DeepSeek API
     ↓
AI 返回 JSON: [{bookId, reason, score}]
     ↓
解析 + 验证（图书ID存在性检查）
     ↓
不足 3 本 → 热门图书兜底
     ↓
写入 ai_recommendation 表
```

**④ AI 分析缓存**

`BookAnalysisService` 使用 `ConcurrentHashMap` 缓存分析结果，同一本书不重复调用 API。返回结果包含：
- `summary`: AI 生成的精彩摘要（100字内）
- `tags`: 5 个关键词标签
- `targetReaders`: 适合的读者群体
- `difficulty`: 难度等级（入门/适中/进阶/专业）

---

### 5.2 郑博涵：用户管理 + 图书管理

#### 负责范围

- 用户管理模块（登录、注册、密码修改、个人信息）
- 图书管理模块（CRUD + 封面图片上传）

#### 关键文件清单

```
controller/AuthController.java     # 登录/注册/登出
controller/BookController.java     # 图书 CRUD + 封面图片上传 + 借阅/归还 AJAX
controller/UserController.java     # 个人中心

service/UserService.java           # 用户业务（MD5加密、登录校验）
service/BookService.java           # 图书业务

entity/User.java                   # 用户实体
entity/Book.java                   # 图书实体

templates/login.html               # 登录页
templates/register.html            # 注册页
templates/user/profile.html        # 个人中心
templates/book/add.html            # 添加图书页（含封面上传）
templates/book/edit.html           # 编辑图书页
templates/book/list.html           # 图书列表页
templates/book/detail.html         # 图书详情页
```

#### 核心逻辑说明

**① 用户注册流程**（`UserService.register()`）

```
1. 检查用户名长度 (3-20 字符)
2. 检查密码长度 (≥6 位)
3. 查重：findByUsername() 检查用户名是否已存在
4. MD5 加密密码：DigestUtils.md5DigestAsHex()
5. 设置默认角色(0=读者) 和状态(1=正常)
6. 插入数据库
```

**② 用户登录流程**（`UserService.login()`）

```
1. 根据用户名查询用户
2. 检查用户是否存在
3. 检查账号是否被禁用 (status == 0)
4. MD5 加密输入密码 → 与数据库密文比对
5. 登录成功 → 清除密码字段 → 存入 HttpSession
6. 管理员跳转 /admin/dashboard，读者跳转首页
```

**③ 封面上传功能**（`BookController.saveCoverImage()`）

```
1. 检查上传文件 MIME 类型（仅允许 jpg/png/gif/webp）
2. 创建 upload/covers/ 目录（不存在则自动创建）
3. 生成 UUID 文件名（保留原扩展名）
4. 保存到 ./upload/covers/{uuid}.{ext}
5. 返回路径：/upload/covers/{uuid}.{ext}
6. 写入 book.cover_url 字段
```

**④ 图书搜索**（`BookMapper.searchBooks()`）

```sql
SELECT b.*, c.name AS category_name
FROM book b LEFT JOIN category c ON b.category_id = c.id
WHERE b.status = 1
  AND (b.title LIKE '%keyword%' OR b.author LIKE '%keyword%')
```

---

### 5.3 马李发：借阅管理 + 管理后台

#### 负责范围

- 借阅管理模块（借书、还书、续借、逾期处理）
- 管理后台（仪表盘统计、用户管理、借阅查看、分类管理）

#### 关键文件清单

```
controller/BookController.java     # 借阅/归还/续借 POST 接口（@ResponseBody）
controller/AdminController.java    # 管理后台所有页面

service/BorrowService.java         # 借阅业务（核心，带 @Transactional）

mapper/BorrowMapper.java           # 借阅 SQL（联表查询）
mapper/UserMapper.java             # 用户统计

entity/Borrow.java                 # 借阅记录实体

templates/admin/dashboard.html     # 管理仪表盘
templates/admin/books.html         # 图书管理列表
templates/admin/users.html         # 用户管理列表
templates/admin/borrows.html       # 借阅记录管理
templates/admin/categories.html    # 分类管理
```

#### 核心逻辑说明

**① 借阅流程**（`BorrowService.borrowBook()` — `@Transactional` 事务保护）

```
借阅规则：每人最多 5 本，每本借期 30 天，不可重复借同一本

1. COUNT(user_id) → 检查当前在借数量是否 ≥ 5
2. SELECT book → 检查图书是否存在、是否在架、是否有库存
3. 检查是否已借过同一本书且未归还
4. INSERT borrow 记录（borrow_time=now, due_time=now+30天, status=1）
5. UPDATE book SET available_copies = available_copies - 1
   ↑ 两步在同一事务中，保证数据一致性
```

**② 归还流程**（`BorrowService.returnBook()` — `@Transactional`）

```
1. SELECT borrow → 校验记录存在、属于当前用户、未归还
2. UPDATE borrow SET return_time=now, status=0
3. UPDATE book SET available_copies = available_copies + 1
4. 检查是否逾期 → 逾期提示
```

**③ 续借流程**（`BorrowService.renewBook()`）

```
续借规则：最多续借 2 次，每次延长 30 天

1. 校验借阅记录存在、属于当前用户、未归还
2. 检查 renew_count < 2
3. renew_count += 1
4. due_time = now + 30 天
```

**④ 管理后台仪表盘统计**

```
总图书数：bookService.countTotal()
读者数：userService.countReaders()
当前在借数：borrowService.listAllBorrows() → filter(未归还)
最近借阅：最近 10 条借阅记录
```

**⑤ 逾期状态自动更新**

```sql
-- BorrowMapper.updateOverdueStatus()
UPDATE borrow SET status = 2 WHERE status = 1 AND due_time < NOW()
```

在管理员查看借阅记录时自动触发。

---

### 5.4 涂恩恺：前端模板 + 系统测试

#### 负责范围

- 所有 Thymeleaf 前端模板开发
- 页面样式设计（CSS）
- AI 聊天机器人前端交互
- 系统功能测试、兼容性测试

#### 关键文件清单

```
templates/
├── fragments/
│   ├── layout.html        # 公共布局：导航栏 + 页脚 + 聊天组件
│   └── chatbot.html       # AI 聊天机器人悬浮组件（内联 JS）★★★
├── index.html             # 首页（热门图书 + 最新上架 + AI推荐 + 统计）
├── login.html             # 登录页
├── register.html          # 注册页
├── book/
│   ├── list.html          # 图书列表（含搜索、分类筛选）
│   ├── detail.html        # 图书详情（含借阅按钮、AI推荐）
│   ├── add.html           # 添加图书（含封面上传预览）
│   └── edit.html          # 编辑图书（含封面预览）
├── admin/
│   ├── dashboard.html     # 管理仪表盘
│   ├── books.html         # 图书管理表格
│   ├── users.html         # 用户管理表格
│   ├── borrows.html       # 借阅记录表格
│   └── categories.html    # 分类管理
├── ai/recommend.html      # AI 推荐展示页
└── user/profile.html      # 个人中心

static/css/
├── style.css              # 主样式
└── chatbot.css            # 聊天机器人样式
```

#### 核心实现说明

**① Thymeleaf 布局复用**

所有页面通过 `th:replace` 引入公共片段，避免代码重复：

```html
<!-- 每个页面顶部 -->
<link rel="stylesheet" href="/css/style.css">
<link rel="stylesheet" href="/css/chatbot.css">

<!-- 导航栏 -->
<div th:replace="fragments/layout :: navbar"></div>

<!-- 聊天机器人（所有页面都有） -->
<div th:replace="fragments/layout :: chatbot"></div>

<!-- 页脚 -->
<div th:replace="fragments/layout :: footer"></div>
```

**② AI 聊天机器人前端**（`chatbot.html` 内联脚本）

```
用户输入 → fetch POST /ai/chat（非流式）
         ↓
    显示 "思考中..."
         ↓
    AI 返回 JSON {success, reply}
         ↓
    更新气泡内容
```

关键特性：
- 悬浮按钮 🤖 切换聊天窗口
- 会话 ID 管理（`_chatSessionId`）支持多轮对话记忆
- 清除会话按钮（POST /ai/chat/clear）
- 防重复提交（`_chatProcessing` 锁）
- 自动滚动到最新消息

**③ 封面图片预览**（添加/编辑图书页）

```html
<input type="file" name="coverImage" accept="image/*"
       onchange="previewImage(this)">
```
上传前即可预览封面，编辑页显示当前封面。

**④ 响应式 / 无图片降级**

```html
<!-- 有封面 → 显示图片 -->
<img th:if="${book.coverUrl}" th:src="${book.coverUrl}" th:alt="${book.title}">
<!-- 无封面 → 显示 emoji 占位符 -->
<span th:unless="${book.coverUrl}">📖</span>
```

---

## 6. API 接口文档

### 6.1 页面路由

| 路径 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/` | GET | 首页 | 所有 |
| `/login` | GET | 登录页 | 匿名 |
| `/register` | GET | 注册页 | 匿名 |
| `/books` | GET | 图书列表（支持 ?keyword= & ?categoryId=） | 所有 |
| `/book/detail?id=` | GET | 图书详情 | 所有 |
| `/search?keyword=` | GET | 图书搜索 | 所有 |
| `/user/profile` | GET | 个人中心 | 登录 |
| `/ai/recommend` | GET | AI 推荐页 | 登录 |
| `/admin/dashboard` | GET | 管理仪表盘 | 管理员 |
| `/admin/books` | GET | 图书管理 | 管理员 |
| `/admin/users` | GET | 用户管理 | 管理员 |
| `/admin/borrows` | GET | 借阅管理 | 管理员 |
| `/admin/categories` | GET | 分类管理 | 管理员 |
| `/book/add` | GET | 添加图书页 | 管理员 |
| `/book/edit/{id}` | GET | 编辑图书页 | 管理员 |

### 6.2 表单提交接口

| 路径 | 方法 | 说明 | 参数 |
|------|------|------|------|
| `/doLogin` | POST | 登录 | username, password |
| `/doRegister` | POST | 注册 | username, password, realName, phone?, email? |
| `/logout` | GET | 登出 | — |
| `/book/doAdd` | POST | 添加图书 | Book 表单 + coverImage(文件) |
| `/book/doEdit` | POST | 编辑图书 | Book 表单 + coverImage?(文件) |
| `/book/delete/{id}` | GET | 下架图书 | — |
| `/user/updateProfile` | POST | 修改个人信息 | realName, phone?, email? |
| `/user/changePassword` | POST | 修改密码 | oldPassword, newPassword |
| `/admin/user/toggle/{id}` | GET | 禁用/启用用户 | — |

### 6.3 AJAX 接口（@ResponseBody）

| 路径 | 方法 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| `/book/borrow` | POST | 借阅图书 | bookId | `{success, message}` |
| `/book/return` | POST | 归还图书 | borrowId | `{success, message}` |
| `/book/renew` | POST | 续借图书 | borrowId | `{success, message}` |

### 6.4 AI 接口

| 路径 | 方法 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| `/ai/chat` | POST | AI 对话（含 Function Calling） | message, sessionId? | `{success, sessionId, reply}` |
| `/ai/chat/clear` | POST | 清除会话 | sessionId | `{success, message}` |
| `/ai/analyze-book/{bookId}` | GET | AI 图书分析 | — | `{success, summary, tags, targetReaders, difficulty}` |
| `/ai/status` | GET | AI 服务状态 | — | `{enabled: true/false}` |

### 6.5 AI 对话示例

**请求：**
```
POST /ai/chat
Content-Type: application/x-www-form-urlencoded

message=帮我借一本《三体》&sessionId=1_abc12345
```

**返回：**
```json
{
  "success": true,
  "sessionId": "1_abc12345",
  "reply": "借阅成功：《三体》，请于 2026-07-17 前归还"
}
```

---

## 7. 部署与运行

### 7.1 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 8+ | 编译和运行 |
| Maven | 3.6+ | 构建 |
| MySQL | 8.0+ | 数据库 |
| IntelliJ IDEA | 2020+ | 推荐 IDE |
| DeepSeek/通义千问 API Key | — | AI 功能需要 |

### 7.2 部署步骤

```bash
# 1. 初始化数据库
mysql -u root -p < src/main/resources/db/schema.sql

# 2. 修改配置（数据库密码 + API Key）
# 编辑 src/main/resources/application.yml
#   spring.datasource.password → 你的 MySQL 密码
#   ai.deepseek.api-key → 你的通义千问 API Key（或设置环境变量）

# 3. 启动项目
mvn spring-boot:run

# 4. 访问
# http://localhost:8080
```

### 7.3 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| reader1 | 123456 | 读者 |
| reader2 | 123456 | 读者 |

### 7.4 AI 功能开关

在 `application.yml` 中设置：
```yaml
ai:
  enabled: false   # 答辩/演示时如果 API 不可用，可关闭
```

关闭后：
- 聊天机器人提示 "AI服务暂未开启"
- AI 推荐页显示 "AI推荐服务暂时不可用"
- 其他功能（借阅、图书管理等）不受影响

---

## 8. 测试指南

### 8.1 功能测试路径

#### 基础流程测试

```
1. 注册新读者账号 → 登录 → 浏览首页
2. 搜索图书 "Java" → 查看图书详情 → 点击借阅
3. 进入个人中心 → 查看借阅记录 → 归还图书
4. 修改个人信息 → 修改密码
```

#### 管理员流程测试

```
1. 用 admin 登录 → 进入管理后台
2. 查看仪表盘统计数据
3. 添加一本新书（上传封面图片）
4. 编辑图书信息
5. 查看用户列表 → 禁用/启用用户
6. 查看借阅记录 → 检查逾期标记
```

#### AI 功能测试

```
1. 点击右下角 🤖 → 打开聊天窗口
2. 输入 "帮我推荐几本书" → 查看回复
3. 输入 "搜索Java" → AI 调用 search_books
4. 输入 "帮我借《活着》" → AI 调用 borrow_book
5. 输入 "我借了哪些书" → AI 调用 get_my_borrows
6. 进入 AI 推荐页面 → 查看个性化推荐
7. 查看图书详情 → 触发 AI 分析（首次慢，后续缓存）
```

#### 边界条件测试

```
1. 借阅第 6 本书 → 应提示"达到最大借阅数量"
2. 借阅已借过的书 → 应提示"已借阅过"
3. 借阅库存为 0 的书 → 应提示"库存不足"
4. 续借第 3 次 → 应提示"达到最大续借次数"
5. 未登录访问 /user/profile → 应重定向到登录页
6. 读者访问 /admin/dashboard → 应返回 403
7. 禁用账号登录 → 应提示"账号已被禁用"
```

### 8.2 AI 降级测试

```yaml
# 关闭 AI
ai:
  enabled: false
```

验证：
- 聊天窗口应提示 AI 不可用
- 首页 AI 推荐不显示但不报错
- 其他功能完全正常

---

## 9. FAQ / 已知问题

### Q1: 启动报错 "Communications link failure"？
A: MySQL 未启动或 `application.yml` 中数据库密码不正确。确认 MySQL 8.0 运行中，且已执行 `schema.sql`。

### Q2: AI 聊天返回 "AI服务暂时不可用"？
A: 检查：
1. `ai.enabled` 是否为 `true`
2. `ai.deepseek.api-key` 是否有效
3. 网络能否访问 `dashscope.aliyuncs.com`

### Q3: 封面上传后图片不显示？
A: 确认 `upload/covers/` 目录存在且有读写权限。`WebConfig` 已添加 `/upload/**` → `file:./upload/` 的静态资源映射。

### Q4: 密码安全吗？
A: 当前使用 MD5 加密（课程项目够用）。如果部署到生产环境，建议改用 BCrypt（Spring Security 提供）。

### Q5: 会话（Session）多久过期？
A: AI 聊天会话在内存中保存，30 分钟无操作自动过期。用户登录 Session 默认 30 分钟（Servlet 标准）。

### Q6: `DROP TABLE IF EXISTS` 会清空数据吗？
A: 是的。`schema.sql` 每次执行都会重建表。如果已有重要数据，请先备份。

### Q7: Function Calling 最多几轮？
A: 最多 3 轮（`MAX_TOOL_ROUNDS = 3`）。防止 AI 陷入死循环。

### Q8: AI 分析为什么第二次访问变快了？
A: `BookAnalysisService` 使用 `ConcurrentHashMap` 内存缓存，同一本书只分析一次。

---

> **文档维护者**：姚晓飞（组长）  
> **组员**：郑博涵、马李发、涂恩恺
