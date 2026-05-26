# TechHub AGENTS.md — AI 协作开发规范

> 本文件为项目级 AI Agent 指令集。每位 AI 在开始任何工作前必须阅读并遵守。
> 所有开发者（人类与 AI）均需遵循以下规范。

---

## 1. 项目概述

**TechHub** 是一个前后端分离的轻量级技术社区论坛，兼具 AI 辅助阅读、智能推荐、神评机制、草稿保护、四级可见权限与版块公告管理。

- **后端**: Spring Boot 3.5.x + MyBatis-Plus 3.5.9 + MySQL 8.0 + Redis 7 + Spring Security + JWT
- **前端**: Vue 3 + TypeScript + Vite + Element Plus + Pinia + ofetch
- **AI 集成**: OpenAI / 国内大模型接口（可配置）

### 1.1 项目结构

```
TechHubBBS/
├── backend/                    # Spring Boot 后端
│   ├── docker-compose.yml      # MySQL + Redis
│   └── src/main/
│       ├── java/com/techhub/    # 后端代码（包结构见 §4）
│       └── resources/
│           ├── application.yml
│           └── db/              # 数据库初始化脚本
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/                 # API 请求层
│       ├── components/          # 组件
│       ├── views/               # 页面
│       ├── stores/              # Pinia stores
│       ├── router/              # 路由
│       └── composables/         # 组合函数
└── docs/                       # 项目文档（设计规范、开发文档、API 文档）
```

---

## 2. 分支策略

### 2.1 分支架构

```
main          ← 生产就绪代码，只接受来自 dev 的 merge
  └── dev     ← 开发集成分支，日常开发基分支
       ├── feature/*    ← 新功能开发
       ├── fix/*        ← Bug 修复
       ├── refactor/*   ← 重构（不改变功能）
       └── docs/*       ← 文档更新
```

### 2.2 规则

| 规则 | 说明 |
|------|------|
| **禁止直接 push 到 `main`** | `main` 只接受 `dev` 的 merge |
| **禁止直接 push 到 `dev`** | 所有变更通过 feature/fix 分支 PR 合入 |
| **分支命名** | `feature/<模块>-<简述>` 例：`feature/user-register`、`fix/post-visibility-404` |
| **一个分支一件事** | 每个分支只包含一个功能或修复，保持原子性 |
| **从 `dev` 拉分支** | `git checkout dev && git pull && git checkout -b feature/xxx` |

### 2.3 工作流

```
1. 从 dev 创建 feature 分支
2. 在 feature 分支上开发 + 本地测试
3. 开发完成 → merge 到 dev
4. dev 稳定后 → merge 到 main（打 tag）
```

---

## 3. Commit 规范

### 3.1 格式

```
<type>(<scope>): <subject>
```

### 3.2 Type 枚举

| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（不改变功能） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖 |
| `security` | 安全修复 |

### 3.3 Scope

后端: `auth`, `user`, `post`, `comment`, `category`, `notice`, `notification`, `recommendation`, `ai`, `draft`, `file`, `admin`

前端: `auth`, `user`, `post`, `comment`, `category`, `notice`, `notification`, `ai-panel`, `editor`, `layout`, `router`, `store`, `api`

### 3.4 示例

```
feat(user): add BCrypt password hashing in register flow
fix(post): correct visibility check for follower-only posts
refactor(recommendation): extract similarity calc to separate service
chore(deps): bump spring-boot to 3.5.14
security(auth): enforce JWT expiration check on every request
```

---

## 4. 后端开发规范

### 4.1 包结构（严格遵循）

```
com.techhub/
├── TechHubApplication.java           # 启动类
├── common/                           # 公共模块
│   ├── R.java                        # 统一响应体
│   ├── PageResult.java               # 分页结果
│   ├── ResultCode.java               # 状态码枚举
│   └── BusinessException.java        # 业务异常
├── config/                           # 配置类
│   ├── SecurityConfig.java
│   ├── MybatisPlusConfig.java
│   ├── RedisConfig.java
│   ├── WebMvcConfig.java
│   ├── JacksonConfig.java
│   ├── AiConfig.java
│   └── FileRecorderConfig.java
├── security/                         # 安全模块
│   ├── JwtTokenProvider.java         # JWT 生成与验证
│   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   ├── UserDetailsServiceImpl.java
│   └── SecurityUtils.java
├── controller/                       # 控制器层
│   └── admin/                        # 管理后台控制器
├── service/                          # 服务层接口
│   └── impl/                         # 服务实现
├── mapper/                           # MyBatis Mapper 接口
├── entity/                           # 实体类（对应数据库表）
├── dto/                              # 数据传输对象
│   ├── auth/                         # LoginRequest, RegisterRequest, LoginResponse
│   ├── user/                         # UserUpdateRequest, UserProfileVO
│   ├── post/                         # PostCreateRequest, PostVO 等
│   └── ...
├── enums/                            # 枚举
├── handler/                          # 全局异常处理器
│   └── GlobalExceptionHandler.java
├── interceptor/                      # 拦截器
├── util/                             # 工具类
└── scheduler/                        # 定时任务（推荐模型更新等）
```

### 4.2 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 实体类 | 驼峰，单数 | `User`, `Post`, `Comment` |
| DTO | 动作+Request/Response/VO | `PostCreateRequest`, `UserProfileVO` |
| Service 接口 | 实体名+Service | `UserService`, `PostService` |
| Service 实现 | 实体名+ServiceImpl | `UserServiceImpl` |
| Mapper | 实体名+Mapper | `UserMapper` |
| Controller | 实体名+Controller | `UserController` |
| 枚举 | 含义+Enum | `RoleEnum`, `VisibilityEnum` |

### 4.3 禁止事项

- **禁止使用 Hutool** — 项目明确排除，用 Apache Commons Lang3 + Spring 自带工具替代
- **禁止在 Controller 中写业务逻辑** — 只做参数校验和调用 Service
- **禁止直接操作数据库** — 必须通过 Mapper（MyBatis-Plus）
- **禁止硬编码魔法值** — 使用枚举 `RoleEnum`、`VisibilityEnum` 等
- **禁止 `SELECT *`** — 按需查询字段
- **禁止 `@ts-ignore` / `as any`** — 零容忍

### 4.4 必须遵循

- 所有接口返回 `R<T>` 统一响应体
- 所有异常由 `GlobalExceptionHandler` 统一处理
- 密码使用 `BCrypt`（cost=10），不可逆哈希
- 主键使用 MyBatis-Plus `ASSIGN_ID`（雪花算法）
- 删除操作使用逻辑删除（`deleted` 字段）
- Jackson 序列化雪花 ID 为字符串（防 JS 精度丢失）
- 文件上传使用 `dromara/x-file-storage`，禁止直接写 `MultipartFile` 处理逻辑

---

## 5. 前端开发规范

### 5.1 目录结构

```
src/
├── api/
│   ├── index.ts                    # ofetch 实例 + 拦截器
│   ├── modules/                    # 按模块拆分的 API
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── post.ts
│   │   ├── comment.ts
│   │   ├── category.ts
│   │   ├── notification.ts
│   │   ├── recommendation.ts
│   │   ├── ai.ts
│   │   ├── draft.ts
│   │   ├── notice.ts
│   │   ├── file.ts
│   │   └── admin.ts
│   └── types/                      # 接口响应类型
├── components/
│   ├── common/                     # 通用组件
│   ├── markdown/                   # Markdown 编辑器/阅读器
│   ├── post/                       # 帖子相关
│   ├── user/                       # 登录/注册
│   ├── notice/                     # 公告组件
│   ├── ai/                         # AI 面板
│   └── admin/                      # 管理后台组件
├── views/                          # 路由页面
├── stores/                         # Pinia stores
│   ├── user.ts                     # 用户信息 + token
│   └── notification.ts             # 通知未读数
├── router/
│   └── index.ts                    # 路由 + 导航守卫
├── composables/                    # 组合函数
└── directives/                     # 自定义指令（权限指令等）
```

### 5.2 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 组件文件 | PascalCase | `PostCard.vue`, `MdEditor.vue` |
| 页面文件 | kebab-case | `post-detail.vue`, `user-profile.vue` |
| TS 文件 | kebab-case | `post-card.ts`, `use-auth.ts` |
| API 函数 | 动词+名词 | `createPost()`, `fetchComments()` |
| Store | 名词 | `useUserStore` |
| Composable | `use`前缀 | `useAuth`, `usePagination` |

### 5.3 强制规范

- **所有组件使用 `<script setup lang="ts">`**（组合式 API）
- **使用 `@/` 路径别名**导入（禁止深层相对路径 `../../../../`）
- **API 调用必须通过 `src/api/modules/` 层**，禁止在组件中直接调用 ofetch
- **Markdown 渲染必须用 `MdViewer.vue`**（内置 DOMPurify XSS 防护）
- **Token 由 ofetch 拦截器自动注入**，禁止手动拼接 Authorization header
- **所有页面必须处理四种状态**：加载中（骨架屏）、正常、空数据、错误
- **暗色模式优先** — 新组件必须同时适配亮/暗色主题

### 5.4 依赖清单（禁止添加未授权依赖）

| 用途 | 允许 | 禁止 |
|------|------|------|
| UI 框架 | Element Plus | Ant Design, Naive UI |
| HTTP | ofetch | axios |
| 图表 | ECharts | Chart.js, D3 |
| 状态管理 | Pinia | Vuex |
| Markdown | markdown-it + DOMPurify | marked, showdown |
| 代码高亮 | highlight.js | Prism.js |
| 图标 | @element-plus/icons-vue | Font Awesome, Material Icons |

---

## 6. API 规范

### 6.1 路由前缀

所有 API 以 `/api/v1` 为前缀。

### 6.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 6.3 RESTful 规范

```
GET    /api/v1/posts              # 帖子列表（分页）
GET    /api/v1/posts/{id}         # 帖子详情
POST   /api/v1/posts              # 发布帖子
PUT    /api/v1/posts/{id}         # 编辑帖子
DELETE /api/v1/posts/{id}         # 删除帖子（软删除）
```

### 6.4 认证

- Token 通过 `Authorization: Bearer <token>` header 传递
- Token 有效期 24 小时
- 登录接口无需认证，其余接口需携带 Token

---

## 7. 数据库规范

### 7.1 强制

- 引擎: **InnoDB**
- 字符集: **utf8mb4**
- 排序规则: **utf8mb4_unicode_ci**
- 主键: **BIGINT + 雪花算法**（MyBatis-Plus ASSIGN_ID）
- 软删除: 统一使用 `deleted` 字段（0=未删除，1=已删除）
- 时间字段: `create_time`, `update_time`（MyBatis-Plus 自动填充）

### 7.2 禁止

- 禁止使用外键约束（应用层保证一致性）
- 禁止使用 `TEXT` 类型（用 `LONGTEXT` 替代以支持大文章）
- 禁止存储明文密码

---

## 8. 安全规范（零容忍）

| 规则 | 违规后果 |
|------|----------|
| **绝不硬编码密码/密钥** | 必须通过环境变量注入 |
| **JWT Secret >= 256 bits** | 使用强随机密钥，定期轮换 |
| **BCrypt cost=10** | 不可逆哈希存储密码 |
| **防 XSS** | Markdown → DOMPurify 清洗，后端二次清洗 |
| **防 SQL 注入** | 使用 MyBatis-Plus 参数化查询，禁止拼接 SQL |
| **可见权限严格校验** | 无权限帖子返回 404（防信息泄露），管理员除外 |
| **分页限制** | `pageSize` 上限 50 |
| **文件上传白名单** | 仅允许头像（jpg/png/gif/webp）和帖子图片，限制 5MB |
| **CORS 白名单** | 仅允许配置的域名，禁止 `*` |

---

## 9. 测试要求

- **后端**: JUnit 5 + MockMvc（接口测试）
- **前端**: 组件测试 + E2E（可选 Playwright）
- **覆盖要求**: 核心业务逻辑（认证、权限、推荐算法）必须有单元测试
- **PR 前置**: 所有测试通过后方可合并

---

## 10. AI 协作规则

### 10.1 工作前必须

1. 阅读本文件全部内容
2. 阅读 `docs/` 下相关设计文档：
   - `TechHub 技术方案文档.md` — 架构决策
   - `TechHub 后端开发文档.md` — 后端实现模式
   - `TechHub API开发文档.md` — 接口规范
   - `TechHub 前端开发文档.md` — 前端实现模式
   - `TechHub 前端页面设计规范.md` — 视觉设计系统
3. 确认当前分支是否正确（feature/fix/refactor 分支）
4. 理解改动对前后端的影响范围

### 10.2 代码要求

- **保持一致性**: 严格遵循现有代码风格和命名模式
- **最小变更**: 只修改必要代码，不做无关重构
- **不引入新依赖**: 除非明确授权
- **不忽略类型错误**: 禁止 `as any`、`@ts-ignore`、`@SuppressWarnings`
- **不删除测试**: 永远不删测试来"修复"构建失败
- **不包含 TODO**: 提交的代码中不允许包含 `TODO` 注释

### 10.3 完成前必须

- [ ] 本地构建通过（`mvn compile` / `pnpm build`）
- [ ] 相关测试通过
- [ ] Lint/Format 检查通过
- [ ] 手动验证功能可用
- [ ] 检查未引入安全漏洞（密码明文、SQL 注入、XSS）
- [ ] Commit message 符合 §3 规范

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-26 | 初始版本 — 分支策略、前后端规范、AI 协作规则 |
