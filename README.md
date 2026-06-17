# 校园图书管理系统（含AI智能体）

基于 Spring Boot + Thymeleaf + MyBatis-Plus + DeepSeek AI 的校园图书管理系统。

## 技术栈

| 层面 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7.18 |
| 数据访问 | MyBatis-Plus 3.5.3.1 |
| 数据库 | MySQL 8.0 |
| 前端模板 | Thymeleaf |
| 权限控制 | 拦截器（Interceptor） |
| AI集成 | DeepSeek API（HTTP直接调用，兼容OpenAI格式） |
| 构建工具 | Maven |
| JDK | 1.8+ |

## 功能模块

### 基础功能
- 用户注册/登录（MD5密码加密）
- 管理员/读者双角色权限
- 图书管理（录入、编辑、下架、搜索）
- 借阅管理（借书、还书、续借、逾期处理）
- 分类管理
- 用户管理（管理员）
- 个人中心（个人信息修改、密码修改、借阅记录）

### AI智能体功能 🤖
- **智能聊天机器人**：页面右下角悬浮对话框，流式输出，多轮对话记忆
- **AI图书推荐**：根据借阅历史，DeepSeek分析偏好，个性化推荐
- **AI图书分析**：生成AI摘要、关键词标签、读者群体分析、难度等级

## 快速开始

### 1. 环境准备
- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- IntelliJ IDEA（推荐）
- DeepSeek API Key（注册地址：https://platform.deepseek.com）

### 2. 数据库初始化
```bash
# 创建数据库并导入测试数据
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 配置
修改 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    username: root      # 改为你的MySQL用户名
    password: root      # 改为你的MySQL密码

ai:
  deepseek:
    api-key: sk-xxxxxx  # 改为你的DeepSeek API Key（或设置环境变量 DEEPSEEK_API_KEY）
```

### 4. 启动
```bash
mvn spring-boot:run
# 或在 IDEA 中直接运行 LibraryApplication.main()
```

访问：http://localhost:8080

### 5. 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| reader1 | 123456 | 读者 |
| reader2 | 123456 | 读者 |

## 项目结构

```
library-ai/
├── pom.xml
├── src/main/java/com/example/library/
│   ├── LibraryApplication.java          # 启动类
│   ├── config/                           # 配置类
│   │   ├── AIConfig.java                 # AI配置
│   │   ├── DeepSeekProperties.java       # DeepSeek属性
│   │   ├── MyBatisPlusConfig.java        # MyBatis-Plus配置
│   │   ├── WebConfig.java                # Web/拦截器配置
│   │   └── MetaObjectHandlerConfig.java  # 字段自动填充
│   ├── controller/                       # 控制器
│   │   ├── HomeController.java           # 首页/图书浏览
│   │   ├── AuthController.java           # 登录/注册
│   │   ├── BookController.java           # 图书管理
│   │   ├── AdminController.java          # 管理后台
│   │   ├── UserController.java           # 用户中心
│   │   └── AIController.java             # AI接口
│   ├── entity/                           # 实体类
│   ├── mapper/                           # MyBatis数据访问
│   ├── service/                          # 业务服务
│   │   └── ai/                           # AI服务
│   │       ├── DeepSeekClient.java       # DeepSeek HTTP客户端
│   │       ├── ChatSessionManager.java   # 会话管理
│   │       ├── BookRecommendService.java # AI推荐
│   │       └── BookAnalysisService.java  # AI分析
│   └── interceptor/                      # 拦截器
└── src/main/resources/
    ├── application.yml                   # 配置文件
    ├── db/schema.sql                     # 数据库脚本
    ├── static/css/                       # 样式
    └── templates/                        # Thymeleaf模板
        ├── fragments/                    # 布局片段
        ├── book/                         # 图书页面
        ├── admin/                        # 管理后台
        ├── ai/                           # AI页面
        └── user/                         # 用户中心
```

## AI功能配置

```yaml
ai:
  enabled: true                    # AI功能开关（答辩时可关闭）
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}   # 建议使用环境变量
    model: deepseek-chat           # 模型名称
    max-tokens: 2048               # 最大输出长度
    temperature: 0.7               # 创造性参数(0-2)
```

AI聊天接口：
- `GET /ai/chat/stream?message=你好&sessionId=xxx` — SSE流式对话
- `POST /ai/chat/clear` — 清除会话
- `GET /ai/recommend` — AI推荐页面
- `GET /ai/analyze-book/{bookId}` — AI图书分析
- `GET /ai/status` — 检查AI服务状态

## 评分要点对应

| 评分项 | 得分 | 实现 |
|--------|------|------|
| 业务需求 (10分) | ✓ | 完整的图书馆管理业务场景 |
| 数据库使用 (10分) | ✓ | MySQL 8.0 + 7张表 + MyBatis-Plus |
| 数据库连接 (15分) | ✓ | Mapper接口 + 自定义SQL + 事务 |
| 网站部署 (10分) | ✓ | Thymeleaf模板 + 前后端整合 |
| 业务实现 (20分) | ✓ | 登录/注册/首页/安全/借阅/推荐 |
| 系统健壮性 (15分) | ✓ | 权限拦截/数据校验/异常降级 |
| **AI创新亮点** | **加分** | DeepSeek集成/聊天机器人/推荐/分析 |
