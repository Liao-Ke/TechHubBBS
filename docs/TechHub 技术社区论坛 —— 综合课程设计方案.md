# TechHub 技术社区论坛 —— 综合课程设计方案

## 一、项目概述

**项目名称**：TechHub 技术社区论坛  
**项目定位**：一个前后端分离、兼具智能推荐、社区治理、AI辅助阅读、草稿保护、细粒度可见权限与版块公告管理的轻量级技术论坛，用于系统整合《数据库原理与应用》《Java程序设计（Spring Boot）》《Web程序设计（Vue3）》三门课程的核心知识与工程实践。  
**核心目标**：
- 实现完整的用户、内容、互动、通知、管理功能；
- 引入个性化推荐算法提升内容分发效率；
- 构建神评（精华评论）机制激励高质量互动；
- 集成基于帖子原文的私有AI总结与问答功能；
- 实现帖子草稿自动保存，提升写作体验；
- 实现帖子可见权限控制（公开/登录可见/粉丝可见/私密四级）；
- 新增版块公告管理，支持管理员/版主发布版块须知与技术活动通知；
- 严格执行安全规范（密码加密、防XSS、统一响应）；
- 践行数据库规范（InnoDB、utf8mb4、雪花主键）；
- 全程遵循RESTful API设计与前后端分离协作模式。

**系统角色**：游客、普通用户、版主、管理员。

---

## 二、技术选型总览

| 层级       | 技术栈                                       | 说明                                 |
| ---------- | -------------------------------------------- | ------------------------------------ |
| **前端**   | Vue 3 + TypeScript + Vite                    | 组合式API，工程化构建                |
| UI组件库   | Element Plus                                 | 统一界面风格，公告时间轴/轮播等      |
| 状态管理   | Pinia                                        | 用户信息、未读通知数                 |
| 路由       | Vue Router 4                                 | 动态路由、导航守卫                   |
| HTTP客户端 | ofetch                                       | 基于Fetch，拦截器统一处理Token与错误 |
| Markdown   | markdown-it + DOMPurify                      | 安全解析与渲染                       |
| 代码高亮   | highlight.js                                 | 配合markdown-it                      |
| 图表       | ECharts                                      | 管理后台数据统计                     |
| **后端**   | Spring Boot 3.5.x                            | 自动配置、内嵌服务器                 |
| 安全框架   | Spring Security + JWT + BCrypt               | 无状态认证，密码强哈希，接口权限控制 |
| ORM        | MyBatis-Plus                                 | 雪花算法主键，条件构造器，分页插件   |
| 数据库     | MySQL 8.0 + HikariCP                         | InnoDB引擎，utf8mb4字符集            |
| 缓存       | Redis 7.x                                    | 热点数据缓存，推荐与高频查询加速     |
| AI服务集成 | OpenAI / 国内大模型接口（可配置）            | AI总结与问答，异步调用，超时兜底     |
| 中文分词   | jieba-analysis / ansj_seg                    | 帖子内容分词                         |
| API文档    | Knife4j (Swagger)                            | 在线接口调试                         |
| 工具库     | Lombok, Apache Commons Lang3, Spring自带工具 | 无Hutool                             |
| 文件存储   | dromara/x-file-storage（本地存储）            | 统一文件上传，本地文件系统存储，易扩展至OSS |
| 运行环境   | JDK 17+ / Node.js ^20.19.0 \|\| >=22.12.0    | 开发与部署运行时                     |

---

## 三、功能模块设计

### 3.1 用户与权限
- **游客**：浏览公开帖子、版块公告；无法互动、不可使用AI功能。
- **普通用户**：发帖、回帖、点赞、推荐神评（注册≥7天）、收藏、关注、接收通知、使用AI总结问答（私有数据）、保存草稿、设置帖子可见权限。
- **版主**：管理指定版块（加精、置顶、锁定、删除帖子，撤销神评），可发布/管理管辖版块的公告。
- **管理员**：全局用户管理、版块管理、系统配置、数据统计、神评强制干预，可管理全站公告，有权查看所有帖子（无视可见权限），但无权查看用户私有AI问答数据。

### 3.2 功能模块树
```
├── 用户模块
│   ├── 注册/登录/退出/修改密码（BCrypt加密）
│   ├── 个人信息编辑（头像、简介）
│   └── 个人主页（发帖、收藏、关注/粉丝列表）
├── 版块模块
│   ├── 管理员：版块CRUD，排序，启用/禁用
│   ├── 公告管理
│   │   ├── 版块公告列表（置顶优先，时间倒序）
│   │   ├── 发布公告（管理员/版主，选择版块，类型：须知/活动，Markdown）
│   │   ├── 编辑/删除公告
│   │   └── 公告展示：版块顶部横幅或独立公告栏
│   └── 用户：按版块浏览帖子
├── 帖子模块
│   ├── 发布帖子（Markdown，选择版块，设置可见权限[0公开/1登录可见/2粉丝可见/3私密]，自动保存草稿，自动分词提取关键词）
│   ├── 帖子列表（分页、版块筛选、排序，后端按可见权限过滤）
│   ├── 帖子详情（Markdown安全渲染，可见权限校验，无权限返回404）
│   ├── 帖子搜索（标题关键词，结果过滤可见性）
│   ├── 编辑/删除自己的帖子（软删除）
│   ├── 草稿管理
│   │   ├── 自动保存：编辑过程中定时自动保存到服务端
│   │   ├── 手动保存：提供保存草稿按钮
│   │   ├── 草稿恢复：新建或编辑时检测草稿，提示恢复
│   │   └── 草稿清理：发布成功后自动删除，支持手动放弃
│   └── 状态管理：普通、精华、置顶、锁定
├── 回复模块
│   ├── 对帖子回复（Markdown，可回复特定楼层）
│   ├── 点赞与推荐神评（自动判定逻辑）
│   └── 删除自己的回复，版主可删除违规回复
├── 互动模块
│   ├── 点赞：帖子/回复点赞（防重复）
│   ├── 收藏：收藏帖子
│   └── 关注：关注用户，粉丝列表（影响粉丝可见帖子）
├── 通知模块
│   ├── 被回复、被点赞、被关注、被选为神评时自动通知
│   └── 通知列表、已读/未读、全部标记已读
├── 推荐模块
│   ├── 首页个性化推荐流（“猜你喜欢”，过滤不可见帖子）
│   ├── 帖子详情页“相关帖子”推荐（过滤不可见）
│   └── 热门榜单（兜底推荐）
├── 神评模块
│   ├── 自动资格判定（帖子评论数≥10开启）
│   ├── 双维度达标（点赞≥10 且 推荐≥5）
│   ├── 防刷机制（注册天数限制、唯一约束）
│   ├── 神评专区展示（帖子顶部）
│   └── 动态调整（掉下阈值自动取消）
├── AI辅助模块
│   ├── AI总结生成：登录用户基于帖子原文生成结构化总结（私有存储）
│   ├── AI总结刷新：允许重新生成
│   ├── AI问答：需先生成总结，基于帖子原文回答，不脱离文本
│   ├── 问答历史：保存所有提问与回答
│   ├── 权限隔离：游客禁用，用户仅见自己数据，管理员无权查看
│   └── 异常控制：内容过短禁止生成，AI异常友好提示，无关问题统一回复
└── 管理后台
    ├── 用户管理（列表、搜索、封禁、分配角色）
    ├── 版块管理（增删改）
    ├── 帖子管理（全站帖子，批量操作）
    ├── 神评管理（查看推荐记录，强制撤销）
    ├── 公告管理（全站公告，筛选）
    └── 数据统计（用户增长、帖子量、活跃度、推荐效果）
```

---

## 四、数据库设计

### 4.1 设计规范（强制）
- **存储引擎**：所有表强制使用 **InnoDB**
- **字符集**：统一 **utf8mb4**，排序规则 **utf8mb4_unicode_ci**
- **主键**：全部使用 **BIGINT**，由应用层雪花算法生成
- **外键**：合理定义以维护参照完整性
- **时间字段**：统一 `DATETIME`，根据需要设置默认值与自动更新
- **索引**：为高频查询条件（版块+时间、用户+帖子等）建立联合索引；唯一约束用于防重
- **范式**：满足第三范式

### 4.2 完整表结构概要

#### 用户表 `user`
- `id` BIGINT（雪花）
- `username`, `password`(BCrypt), `email`, `avatar_url`, `bio`, `role`(USER/MODERATOR/ADMIN), `status`, `create_time`, `update_time`

#### 版块表 `category`
- `id` BIGINT
- `name`(UNIQUE), `description`, `sort_order`, `status`, `create_time`

#### 帖子表 `post`
- `id` BIGINT
- `title`, `content`(TEXT), `category_id`, `author_id`, `type`(0普通/1精华/2置顶), `status`(1正常/0锁定/2删除), `visibility`(0公开/1登录可见/2粉丝可见/3私密), `view_count`, `like_count`, `comment_count`, `divine_comment_count`, `eligible_for_divine`, `create_time`, `update_time`
- 索引：`(category_id, create_time)`, `author_id`

#### 回复表 `comment`
- `id` BIGINT
- `content`, `post_id`(FK CASCADE), `user_id`, `parent_id`, `reply_to_user_id`, `like_count`, `recommend_count`, `is_divine`, `divine_time`, `create_time`
- 索引：`(post_id, create_time)`, `user_id`

#### 点赞表 `user_like`
- `id` BIGINT
- `user_id`, `target_type`(POST/COMMENT), `target_id`, `create_time`
- 唯一约束：`(user_id, target_type, target_id)`

#### 收藏表 `favorite`
- `id` BIGINT
- `user_id`, `post_id`, `create_time`
- 唯一约束：`(user_id, post_id)`

#### 关注表 `follow`
- `id` BIGINT
- `follower_id`, `followee_id`, `create_time`
- 唯一约束：`(follower_id, followee_id)`

#### 通知表 `notification`
- `id` BIGINT
- `user_id`, `type`(REPLY/LIKE/FOLLOW/DIVINE/SYSTEM), `source_id`, `content`, `is_read`, `create_time`

#### 神评推荐记录表 `comment_recommend`
- `id` BIGINT
- `comment_id`, `user_id`, `create_time`
- 唯一约束：`(comment_id, user_id)`

#### 推荐相关表
- `user_profile`：`user_id`, `keyword_weights`(JSON), `last_update_time`
- `post_keyword`：`post_id`, `keyword`, `tfidf_weight`
- `post_similarity`：`post_id_a`, `post_id_b`, `similarity_score`
- `user_behavior`（可选）

#### AI总结表 `ai_summary`
- `id` BIGINT
- `user_id`, `post_id`, `content`(TEXT), `status`(0生成中/1成功/2失败), `error_message`, `create_time`, `update_time`
- 唯一约束：`(user_id, post_id)`

#### AI问答记录表 `ai_qa_history`
- `id` BIGINT
- `user_id`, `post_id`, `question`(TEXT), `answer`(TEXT), `create_time`
- 索引：`(user_id, post_id)`

#### 帖子草稿表 `post_draft`
- `id` BIGINT
- `user_id`, `post_id`(可为NULL表示新帖草稿), `title`, `content`(LONGTEXT), `category_id`, `visibility`, `last_saved_at`, `create_time`, `update_time`
- 唯一索引：允许 `post_id` 为 NULL 时重复，业务层控制每个用户只有一个新帖草稿；有值时 `(user_id, post_id)` 唯一
- 索引：`idx_user_id`

#### 版块公告表 `category_notice`
- `id` BIGINT
- `category_id`(FK), `title`, `content`(TEXT), `type`(0须知/1活动), `author_id`(FK), `is_pinned`, `status`(1已发布/0草稿), `create_time`, `update_time`
- 索引：`(category_id, status, is_pinned, create_time)`

所有表均遵循 InnoDB + utf8mb4，主键 BIGINT。

---

## 五、RESTful API 设计

全局前缀 `/api/v1`，统一响应体：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 5.1 认证与用户
- `POST /api/v1/auth/register` – 注册
- `POST /api/v1/auth/login` – 登录，返回JWT
- `GET /api/v1/users/me` – 当前用户信息
- `PATCH /api/v1/users/me` – 更新个人信息
- `GET /api/v1/users/{id}` – 用户公开信息
- `GET /api/v1/users/{id}/posts` – 用户帖子列表（可见性过滤）

### 5.2 版块与公告
- `GET /api/v1/categories` – 启用版块列表
- `POST /api/v1/categories` – 新建（管理员）
- `PATCH /api/v1/categories/{id}` – 编辑（管理员）
- `DELETE /api/v1/categories/{id}` – 删除（管理员）
- `GET /api/v1/categories/{categoryId}/notices` – 公告列表（公开，可筛选type）
- `POST /api/v1/categories/{categoryId}/notices` – 发布公告（管理员/管辖版主）
- `GET /api/v1/notices/{id}` – 公告详情
- `PATCH /api/v1/notices/{id}` – 编辑公告（发布者/版主/管理员）
- `DELETE /api/v1/notices/{id}` – 删除公告
- `GET /api/v1/admin/notices` – 管理全站公告（管理员）

### 5.3 帖子
- `GET /api/v1/posts` – 帖子列表（可见性动态过滤，分页，筛选）
- `POST /api/v1/posts` – 发布帖子（含visibility字段）
- `GET /api/v1/posts/{id}` – 详情（可见性校验，无权限404）
- `PATCH /api/v1/posts/{id}` – 编辑（作者，可改visibility）
- `DELETE /api/v1/posts/{id}` – 删除（作者/管理员）
- `POST /api/v1/posts/{id}/likes` / `DELETE` – 点赞/取消
- `POST /api/v1/posts/{id}/favorites` / `DELETE` – 收藏/取消
- `GET /api/v1/posts/{id}/comments` – 回复列表
- `POST /api/v1/posts/{id}/comments` – 发表回复
- `GET /api/v1/posts/{id}/divine-comments` – 神评列表

### 5.4 回复与神评
- `DELETE /api/v1/comments/{id}` – 删除回复（作者/版主）
- `POST /api/v1/comments/{id}/likes` / `DELETE` – 点赞回复
- `POST /api/v1/comments/{id}/recommend` – 推荐神评（注册≥7天）
- `DELETE /api/v1/comments/{id}/recommend` – 取消推荐

### 5.5 通知
- `GET /api/v1/notifications` – 通知列表
- `PATCH /api/v1/notifications/{id}/read` – 标记已读
- `PATCH /api/v1/notifications/read-all` – 全部已读
- `GET /api/v1/notifications/unread-count` – 未读数量

### 5.6 推荐
- `GET /api/v1/recommendations` – 个性化推荐（过滤不可见帖子）
- `GET /api/v1/posts/{id}/related` – 相关帖子（过滤）

### 5.7 AI辅助
- `POST /api/v1/posts/{postId}/ai/summary` – 生成/刷新总结（需登录，内容长度检查）
- `GET /api/v1/posts/{postId}/ai/summary` – 获取总结（私有）
- `POST /api/v1/posts/{postId}/ai/qa` – 提问（需先有总结）
- `GET /api/v1/posts/{postId}/ai/qa/history` – 问答历史（私有）

### 5.8 草稿
- `POST /api/v1/drafts` – 保存草稿（upsert）
- `GET /api/v1/drafts` – 草稿列表
- `GET /api/v1/drafts/{draftId}` – 草稿详情
- `GET /api/v1/drafts/check?postId={postId}` – 检查草稿存在
- `DELETE /api/v1/drafts/{draftId}` – 删除草稿
- `DELETE /api/v1/drafts?postId={postId}` – 按帖子删除草稿

### 5.9 管理后台
- `GET /api/v1/admin/users` – 用户管理
- `PATCH /api/v1/admin/users/{id}/ban` – 封禁/解封
- `PATCH /api/v1/admin/users/{id}/role` – 修改角色
- `GET /api/v1/admin/posts` – 帖子管理（全部可见）
- `PATCH /api/v1/admin/posts/{id}/type` – 设置精华/置顶
- `PATCH /api/v1/admin/posts/{id}/lock` – 锁定/解锁
- `DELETE /api/v1/admin/posts/{id}` – 强制删除
- `GET /api/v1/admin/comments/recommend-log` – 神评推荐记录
- `PATCH /api/v1/admin/comments/{id}/divine` – 强制设置/取消神评
- `GET /api/v1/admin/statistics` – 站点统计

### 5.10 文件上传
- `POST /api/v1/files/upload` – 上传文件（头像、帖子内嵌图片等），返回文件URL

---

## 六、后端实现要点

### 6.1 基础框架
- Spring Boot 3.x，整合 MyBatis-Plus，雪花ID，逻辑删除，分页插件。
- 统一响应体 `R<T>`，全局异常处理。
- Spring Security + JWT + BCrypt 实现认证。
- 接口权限注解：`@PreAuthorize("hasRole('ADMIN')")` 及自定义注解。

### 6.2 关键业务逻辑
- **可见权限过滤**：`PostVisibilityService` 根据用户角色、登录状态、关注关系动态返回是否可查看帖子，列表查询时动态拼接SQL，详情接口404处理。
- **草稿保存**：使用 upsert 逻辑，自动保存由前端定时触发（30秒），发布成功后自动清理对应草稿。
- **AI总结问答**：调用大模型API，通过 Prompt 限定基于原文，总结状态管理，私有存储，用户隔离，内容过短禁止生成。
- **神评机制**：帖子评论数≥10可推荐，双维度达标自动加神评，定时任务检查掉标，管理员可干预。
- **版块公告**：版主仅能管理管辖版块的公告，管理员全局管理。
- **推荐算法**：内容+协同过滤，定时更新用户画像和帖子相似度，推荐结果过滤不可见帖子，缓存优化。

### 6.3 安全性
- 密码 BCrypt 加密。
- 所有ID序列化为字符串避免 JavaScript 精度丢失。
- Markdown 内容存储原文，前端渲染时 DOMPurify 清洗。
- AI 接口防止 Prompt 注入，固定回答边界。

### 6.4 文件上传
- 使用 **dromara/x-file-storage**（Maven 坐标 `org.dromara.x-file-storage:x-file-storage-spring`）统一处理文件上传。
- 启动类添加 `@EnableFileStorage` 注解启用服务，注入 `FileStorageService` 即可使用。
- `application.yml` 配置本地存储平台：
  ```yaml
  dromara:
    x-file-storage:
      default-platform: local-1
      local:
        - platform: local-1
          enable-storage: true
          base-path: /data/uploads/
          domain: http://localhost:8080/file/
  ```
- 上传调用：`fileStorageService.of(file).setPath("avatar/").upload()`，返回 `FileInfo` 含访问 URL。
- 用途：用户头像上传、帖子 Markdown 内嵌图片上传等，基于本地文件系统存储，后续可无缝切换至 OSS/ MinIO 等云存储平台。

---

## 七、前端实现要点

### 7.1 架构与状态
- Vite + Vue3 + TypeScript，组合式API。
- Pinia 管理用户信息、未读通知数。
- Vue Router 动态路由，导航守卫校验角色权限。
- 自定义指令 `v-permission` 控制按钮级权限。

### 7.2 关键交互
- **帖子编辑器**：集成可见性选择器，草稿自动保存定时器，脏标记控制，恢复提示。
- **帖子列表**：展示版块公告栏（公告轮播），帖子卡片，可见性图标提示（如锁状图标表示私密）。
- **帖子详情**：无权限显示404友好页面；神评专区高亮；AI总结面板（先检查内容长度，登录后生成）；推荐流和底部相关帖子。
- **管理后台**：公告管理、用户管理、数据统计图表（ECharts）。
- **Markdown 安全**：使用 markdown-it 配合 DOMPurify，禁用原始HTML，代码高亮用 highlight.js。

### 7.3 可见权限UI
- 发布/编辑时可见性下拉：公开、登录可见、粉丝可见、私密（仅自己可见）。
- 帖子列表中，根据后端过滤后的数据渲染，无需额外处理；详情页若返回404，展示“帖子不存在或无权查看”。

---

## 八、实施步骤

| 阶段 | 内容                                                         | 预计时间 |
| ---- | ------------------------------------------------------------ | -------- |
| 一   | **需求与设计**：用例图、E-R图，API文档，前端组件树，算法细节。 | 2 天     |
| 二   | **数据库实现**：建表，索引，视图，种子数据。                 | 1.5 天   |
| 三   | **后端基础框架**：项目初始化，整合安全框架、ORM、API文档，统一响应，注册登录。 | 2 天     |
| 四   | **后端核心业务**：用户、版块、帖子（含可见权限）、回复、互动、通知、神评、推荐算法、AI总结问答、草稿、公告，管理后台，单元测试。 | 8 天     |
| 五   | **前端基础与组件**：项目搭建，公共组件，Markdown编辑器与渲染器，路由与状态。 | 2 天     |
| 六   | **前端业务页面**：所有页面开发，AI面板、草稿恢复、可见性选择、公告展示、权限控制。 | 6.5 天   |
| 七   | **联调测试与优化**：全功能联调，权限组合测试，异常场景测试，性能优化，部署。 | 2.5 天   |

---

## 九、课程知识点覆盖矩阵

| 课程                            | 核心知识点                     | 项目应用                                               |
| ------------------------------- | ------------------------------ | ------------------------------------------------------ |
| **数据库原理与应用**            | E-R模型、范式、索引优化、事务  | 全部表设计，联合索引，复合查询，事务保证点赞、神评操作 |
|                                 | 存储引擎、字符集、分布式主键   | InnoDB，utf8mb4，雪花ID                                |
|                                 | 视图、触发器、存储过程         | 热门帖子视图，级联删除触发器                           |
| **Java程序设计（Spring Boot）** | 起步依赖、自动配置             | 项目快速整合各类组件                                   |
|                                 | RESTful API设计                | 全部接口资源化设计，HTTP方法语义                       |
|                                 | Spring Security + JWT + BCrypt | 无状态认证，密码强哈希，接口权限                       |
|                                 | MyBatis-Plus高级特性           | 雪花ID，条件构造器，逻辑删除，分页                     |
|                                 | AOP、全局异常、统一响应        | 日志记录，异常处理，R封装                              |
|                                 | 定时任务、外部API调用          | 推荐模型更新，神评检查，AI服务集成                     |
| **Web程序设计（Vue3）**         | 组合式API、组件化              | 全部页面组件，公告、AI面板、编辑器等                   |
|                                 | Vue Router、Pinia              | 动态路由，导航守卫，状态管理                           |
|                                 | ofetch、拦截器                 | 统一请求处理，Token携带                                |
|                                 | 安全渲染、指令、响应式         | markdown-it+DOMPurify，v-permission，条件渲染          |
|                                 | 用户体验优化                   | 草稿自动保存、恢复提示、404友善页面                    |
| **软件工程与安全**              | 数据隐私、最小权限             | AI数据私有，帖子可见权限隔离，管理员不可见AI内容       |
|                                 | 防XSS、密码加密                | 前端DOMPurify，后端BCrypt                              |
|                                 | 模块化设计                     | 各功能模块清晰分离，服务层设计                         |

---

## 十、总结

本最终方案完整融合了TechHub技术社区论坛的所有核心功能：用户与权限管理、版块及公告系统、支持Markdown的帖子发布与草稿自动保存、细粒度的四级可见权限控制、神评社区治理机制、个性化内容推荐，以及完全私有的AI总结问答服务。系统严格遵循数据库设计规范、RESTful API风格、前后端分离架构和安全最佳实践，覆盖了三门课程的核心理论与工程实践要求。通过该项目的实施，学生将深入掌握从需求分析、数据库建模、后端业务开发到前端交互设计的全栈技能，同时理解复杂系统中权限、隐私和用户体验的综合考量，完全达到课程设计的综合性教学目标。