import json

PROJECT_FILE = "E:/User/Desktop/TechHubBBS/thesis/thesis-project.json"

with open(PROJECT_FILE, "r", encoding="utf-8") as f:
    project = json.load(f)

def heading(text, level=1):
    return {"type": "heading", "level": level, "text": text, "style": {}}

def para(text, bold=False, font_size="12pt", alignment="justify"):
    style = {}
    style["font_size"] = font_size
    style["alignment"] = alignment
    if bold:
        style["bold"] = True
    return {"type": "paragraph", "text": text, "style": style}

def page_break():
    return {"type": "page_break"}

def empty_line():
    return para("")

C = project["content"]

# ========== 第三章 系统需求分析 ==========
C.append(page_break())
C.append(heading("第三章 系统需求分析", 1))

C.append(heading("3.1 功能需求概述", 2))
C.append(para("TechHub 技术社区论坛系统旨在构建一个集内容创作、互动交流、智能辅助于一体的综合技术交流平台。经过详细的需求调研和分析，系统需要实现以下核心功能模块：用户管理模块、帖子管理模块、评论管理模块、版块分类模块、公告管理模块、通知推送模块、AI 辅助模块、推荐系统模块、草稿管理模块、管理后台模块以及文件上传模块。各模块之间通过 RESTful API 进行数据交互，前端通过统一的 API 层进行调用，确保系统的松耦合和高内聚。"))

C.append(heading("3.2 用户角色分析", 2))
C.append(para("系统定义了三种用户角色，每种角色拥有不同的权限范围："))
C.append(para("游客（Guest）：未登录的访问者。可以浏览公开帖子、查看版块分类列表、阅读公告内容、查看用户公开资料。无法发布内容、点赞评论或使用 AI 辅助功能。"))
C.append(para("普通用户（User）：已注册并登录的用户。除游客权限外，还可以发布和编辑帖子、发表评论、点赞和收藏内容、关注其他用户、使用 AI 摘要和问答功能、保存和管理草稿、接收实时通知。帖子可见权限受作者设置的四级可见性控制。"))
C.append(para("管理员（Admin）：系统最高权限角色。除普通用户权限外，还拥有版块管理、公告管理、用户封禁与角色变更、帖子加精置顶与强制删除、神评人工干预以及数据统计分析等管理后台全部功能。"));
C.append(para("此外，系统还支持版主（Moderator）角色作为中间权限层级，版主可以管理指定版块的帖子和公告，但不能执行用户角色变更等全局管理操作。"))

C.append(heading("3.3 核心功能需求", 2))

C.append(heading("3.3.1 用户管理模块", 3))
C.append(para("用户管理模块是系统的基础模块，负责用户身份的全生命周期管理。具体功能包括：用户注册（用户名、邮箱、密码，密码经过 BCrypt 哈希后存储）、用户登录（验证凭据后签发 24 小时有效期的 JWT 令牌）、用户个人资料管理（头像上传、个人简介编辑）、用户间关注与取消关注（支持互相关注检测）以及用户主页展示（展示用户基本信息、发布的帖子列表）。"))

C.append(heading("3.3.2 帖子管理模块", 3))
C.append(para("帖子管理模块是系统的核心内容模块。用户可以创建帖子（选择版块分类、设置可见权限、编写 Markdown 正文）、编辑自己的帖子、软删除帖子。帖子支持四种可见权限级别：公开（所有人可见）、登录可见（仅登录用户可见）、关注者可见（仅作者关注者可见）、私密（仅作者本人和管理员可见）。管理员可以查看所有帖子（不受可见权限限制），并对帖子进行加精、置顶、锁定和强制删除等管理操作。帖子列表支持按分类筛选、关键词搜索和排序（最新发布 / 热门帖子）。"))

C.append(heading("3.3.3 评论与神评机制", 3))
C.append(para("评论模块支持用户对帖子发表评论，支持嵌套回复（通过 parent_id 形成评论树）。用户可以给自己的评论或他人的评论点赞，也可以推荐优质评论为神评候选。系统通过神评机制自动筛选高质量评论：当帖子评论数达到 10 条以上时，帖子获得神评资格；当某条评论同时满足点赞数不低于 10 和推荐数不低于 5 时，该评论被自动标记为神评。为防止刷票滥用，推荐神评功能要求用户注册时间不少于 7 天，且同一用户对同一评论不能重复推荐。系统每 5 分钟执行一次定时任务，对不符合阈值的神评进行自动摘除。"))

C.append(heading("3.3.4 AI 辅助阅读", 3))
C.append(para("AI 辅助阅读是系统的创新功能之一。用户可以针对任意帖子（正文不少于 50 字）请求 AI 生成个性化摘要。摘要采用异步生成模式：用户发起请求后系统异步调用 LLM API，前端轮询查询生成状态（处理中 / 已完成 / 失败）。摘要生成后，用户可以基于帖子内容向 AI 提出相关问题，AI 根据帖子内容和对话历史进行回答。为保障用户隐私，AI 摘要和问答记录通过 (user_id, post_id) 唯一约束进行用户隔离，每位用户只能看到自己的 AI 互动记录。同时，系统实施了 Prompt 注入防护策略，要求 LLM 仅基于帖子内容作答。"))

C.append(heading("3.3.5 个性化推荐", 3))
C.append(para("推荐系统模块负责为登录用户提供个性化的帖子推荐。系统采用离线计算 + 在线查询的混合架构：离线阶段通过 Jieba 分词器对帖子内容进行中文分词，利用 TF-IDF 算法提取关键词并计算权重，将结果存入 post_keyword 数据表；同时根据用户的浏览和互动行为构建用户画像（JSON 格式的关键词权重向量），存入 user_profile 数据表；基于关键词权重计算帖子间的余弦相似度，生成 post_similarity 相似度矩阵。在线阶段，系统从 Redis 缓存中获取用户画像，匹配相似度矩阵，经过可见权限过滤和去重后返回推荐帖子列表。对于新用户（无历史行为数据），系统采用冷启动策略，返回热门帖子作为推荐结果。"))

C.append(heading("3.3.6 通知系统", 3))
C.append(para("通知系统负责在用户互动事件发生时向相关用户推送实时通知。系统支持五种通知类型：回复通知（有人回复了你的帖子或评论）、点赞通知（有人点赞了你的帖子或评论）、关注通知（有人关注了你）、神评通知（你的评论被标记为神评）和系统通知（管理员公告等）。前端通过导航栏的通知铃铛图标实时显示未读通知数量，用户可以在通知页面查看详细列表，支持单条标记已读和一键全部已读。"))

C.append(heading("3.3.7 草稿管理", 3))
C.append(para("草稿管理模块为帖子编辑提供自动保存和恢复功能。用户在编辑帖子时，前端利用 useDraft 组合函数每 30 秒自动将编辑器内容保存到后端。草稿数据通过 (user_id, post_id) 唯一约束进行 upsert，新建帖子的草稿以 post_id 为 NULL 存储（每个用户最多一个新建草稿），编辑已有帖子的草稿则关联具体的 post_id。当用户再次进入编辑器时，系统自动检测是否存在未发布的草稿，并提示用户是否恢复上次编辑的内容。帖子发布成功后，关联的草稿自动删除。"))

C.append(heading("3.3.8 管理后台", 3))
C.append(para("管理后台为管理员和版主提供系统管理功能。主要包括：仪表盘（展示用户总数、帖子总数、近期活跃度等统计数据的 ECharts 图表）、用户管理（用户列表查询、封禁/解封、角色变更）、帖子管理（全量帖子查看、加精/置顶/锁定/强制删除）、版块管理（版块分类的增删改查）、公告管理（版块公告和活动通知的发布与维护）和神评管理（推荐日志查看、神评的人工授精/撤销）。管理后台采用独立的路由布局，左侧可折叠的侧边栏导航，权限通过路由守卫和 v-permission 指令双重控制。"))

C.append(heading("3.4 非功能需求", 2))
C.append(para("安全性需求：系统需实现严格的认证授权机制，密码采用 BCrypt 不可逆哈希存储，JWT 密钥长度不低于 256 位，所有密钥和密码通过环境变量注入。需防范 XSS 攻击（Markdown 内容经 DOMPurify 清洗）、SQL 注入（MyBatis-Plus 参数化查询）和 CSRF 攻击（无状态 REST API）。帖子可见权限需在后端严格校验，无权限访问统一返回 404 状态码防止信息泄露。"))
C.append(para("性能需求：帖子列表查询支持分页且每页不超过 50 条，热点数据通过 Redis 缓存降低数据库压力，HTTP 接口响应时间应在 500ms 以内（AI 调用除外）。系统需支持至少 100 并发用户的正常访问。"))
C.append(para("可用性需求：前端界面需支持暗色模式（默认）和亮色模式切换，所有页面需处理加载中、正常、空数据和错误四种状态，关键操作需提供确认对话框防止误操作。移动端需支持响应式布局，确保在手机和平板设备上的基本可用性。"))
C.append(para("可维护性需求：代码需遵循统一的命名规范和分包结构，后端 Controller 层仅负责参数接收和校验（业务逻辑全在 Service 层），前端 API 调用统一通过 api/modules/ 层。接口文档通过 Knife4j 自动生成，方便团队协作和后继维护。"))

# ========== 第四章 系统设计 ==========
C.append(page_break())
C.append(heading("第四章 系统设计", 1))

C.append(heading("4.1 系统架构设计", 2))
C.append(para("TechHub 系统采用典型的前后端分离架构，前端和后端通过 HTTP/HTTPS 协议进行数据交互，使用 JSON 作为数据交换格式。整体架构分为四层："))
C.append(para("展示层（Presentation Layer）：运行在用户浏览器中的 Vue 3 单页应用（SPA），负责页面渲染、用户交互和前端路由。通过 Vite 构建工具打包为静态资源文件，可部署到 Nginx 或 CDN。"))
C.append(para("网关层（Gateway Layer）：由 Vite 开发服务器（开发环境）或 Nginx（生产环境）提供反向代理和静态资源服务，将 /api 前缀的请求转发到后端服务。"))
C.append(para("业务层（Business Layer）：基于 Spring Boot 框架构建的后端服务，采用分层架构设计（Controller → Service → Mapper），提供 RESTful API 接口。Controller 层负责接收请求和参数校验，Service 层封装核心业务逻辑，Mapper 层基于 MyBatis-Plus 实现数据持久化操作。Spring Security 过滤器链在请求到达 Controller 之前完成 JWT 令牌的验证和权限校验。"))
C.append(para("数据层（Data Layer）：由 MySQL 8.0 关系型数据库和 Redis 7 缓存数据库组成。MySQL 负责持久化存储所有业务数据，通过 InnoDB 引擎保障事务一致性和参照完整性。Redis 负责缓存热点数据（用户画像、帖子排行等），提供高性能的数据读取能力，同时用作分布式锁和会话管理。"))
C.append(para("此外，系统还通过 Docker Compose 实现三容器编排：mysql 容器（端口 3306）、redis 容器（端口 6379）和 app 容器（端口 8080）。app 容器依赖于 mysql 和 redis 容器的健康状态，通过健康检查机制确保依赖服务就绪后才启动应用。"))

C.append(heading("4.2 数据库设计", 2))

C.append(heading("4.2.1 实体关系分析", 3))
C.append(para("根据系统功能需求，数据库设计共包含 18 张核心数据表，可归纳为以下实体关系："))
C.append(para("用户（User）是系统的核心实体，与其他实体建立多重关系：用户可以发布多篇帖子（Post），可以发表多条评论（Comment），可以对帖子和评论点赞（UserLike），可以收藏帖子（Favorite），可以关注其他用户（Follow），可以接收多条通知（Notification），可以保存草稿（PostDraft），以及进行 AI 摘要和问答（AiSummary、AiQaHistory）。"))
C.append(para("帖子（Post）是内容的核心载体，归属于某个版块分类（Category），由用户发布，可以有多条评论，可以被点赞、收藏，包含关键词信息（PostKeyword），与其他帖子存在相似度关系（PostSimilarity）。"))
C.append(para("版块分类（Category）包含多个帖子，每个版块可以有多条公告（CategoryNotice）。评论（Comment）属于某个帖子，支持嵌套回复（通过 parent_id 自引用），可以被推荐为神评（CommentRecommend）。"))
C.append(para("所有表均采用 BIGINT 类型的主键（通过雪花算法生成），使用 InnoDB 存储引擎，字符集为 utf8mb4，支持软删除（deleted 字段），时间字段（create_time、update_time）由 MyBatis-Plus 自动填充。关键关联表定义了外键约束以维护参照完整性。"))

C.append(heading("4.2.2 核心表结构设计", 3))
C.append(para("以下介绍系统的核心数据表结构："))
C.append(para("user 表：存储用户基本信息，包含 id（雪花ID）、username（唯一用户名）、password（BCrypt 哈希密码）、email（邮箱）、avatar_url（头像地址）、bio（个人简介）、role（角色：USER/MODERATOR/ADMIN）、status（状态：正常/封禁）、create_time 和 update_time 等字段。"))
C.append(para("post 表：存储帖子信息，包含 id、title（标题）、content（LONGTEXT 正文）、category_id（所属版块）、author_id（作者）、type（类型：普通/精华/置顶）、status（状态：正常/锁定/草稿）、visibility（可见性：0-3 四级）、view_count（浏览量）、like_count（点赞数）、comment_count（评论数）、divine_comment_count（神评数）、eligible_for_divine（神评资格）、deleted（软删除标记）等字段。外键关联 category 表和 user 表。"))
C.append(para("comment 表：存储评论信息，包含 id、content（TEXT 正文）、post_id（所属帖子）、user_id（评论者）、parent_id（父评论ID，NULL 表示顶级评论）、reply_to_user_id（被回复用户）、like_count（点赞数）、recommend_count（推荐数）、is_divine（是否神评）、divine_time（神评时间）等字段。外键关联 post 表和 user 表，post_id 外键设置 ON DELETE CASCADE。"))
C.append(para("post_keyword 表：存储帖子关键词及 TF-IDF 权重，主键为 (post_id, keyword) 联合主键，包含 post_id、keyword（关键词）和 tfidf_weight（权重值）。外键关联 post 表并设置 ON DELETE CASCADE。"))
C.append(para("post_similarity 表：存储帖子间的余弦相似度，主键为 (post_id_a, post_id_b) 联合主键，包含 similarity_score（相似度分数）。外键关联 post 表。"))
C.append(para("user_profile 表：存储用户画像数据，以 user_id 为主键，包含 keyword_weights（JSON 格式的关键词权重向量）和 last_update_time（最后更新时间）。外键关联 user 表。"))
C.append(para("ai_summary 表：存储 AI 摘要记录，包含 id、user_id、post_id、content（LONGTEXT 摘要内容）、status（状态：0-处理中，1-已完成，2-失败）。通过 (user_id, post_id) 唯一约束实现用户数据隔离。外键关联 user 表和 post 表。"))
C.append(para("post_draft 表：存储帖子草稿，包含 id、user_id、post_id（NULL 表示新建帖子的草稿）、title、content（LONGTEXT）、category_id、visibility。通过 (user_id, post_id) 唯一约束实现 upsert 逻辑。外键关联 user 表。"))
C.append(para("其余表包括：category（版块分类）、category_notice（版块公告）、notification（通知）、user_like（点赞记录）、favorite（收藏记录）、follow（关注记录）、comment_recommend（神评推荐记录）、ai_qa_history（AI 问答历史）、file_detail 和 file_part_detail（文件存储记录）。每张表均遵循统一的命名和设计规范。"))

C.append(heading("4.3 API 接口设计", 2))
C.append(para("系统遵循 RESTful API 设计风格，所有接口以 /api/v1 为统一前缀，使用 JSON 格式进行数据交换。统一响应体结构为 { \"code\": 200, \"message\": \"success\", \"data\": { ... } }，通过状态码枚举类（ResultCode）规范化管理响应状态。"))
C.append(para("系统共设计了 12 个 API 模块，涵盖约 50+ 个接口端点。主要模块包括："))
C.append(para("认证模块（/auth）：提供注册（POST /auth/register）和登录（POST /auth/login）接口，登录成功返回 JWT 令牌和用户基本信息。"))
C.append(para("用户模块（/users）：提供当前用户信息查询（GET /users/me）、资料更新（PATCH /users/me）、用户公开资料查看（GET /users/{id}）、用户帖子列表（GET /users/{id}/posts）、关注/取消关注（POST/DELETE /users/{id}/follow）和关注状态检查（GET /users/{id}/follow）。"))
C.append(para("帖子模块（/posts）：提供帖子列表分页查询（GET /posts，支持分类筛选、关键词搜索、排序）、帖子创建（POST /posts）、详情查看（GET /posts/{id}，含可见权限校验和浏览量自增）、编辑（PATCH /posts/{id}）、软删除（DELETE /posts/{id}）、点赞/取消点赞（POST/DELETE /posts/{id}/likes）和收藏/取消收藏（POST/DELETE /posts/{id}/favorites）接口。"))
C.append(para("评论模块（/posts/{id}/comments 和 /comments）：提供评论列表查询（GET，继承帖子可见权限）、评论创建（POST，支持嵌套回复）、评论删除（DELETE /comments/{id}）、评论点赞（POST /comments/{id}/likes）、神评推荐（POST /comments/{id}/recommend）和神评列表查询（GET /posts/{postId}/divine-comments）。"))
C.append(para("通知模块（/notifications）：提供通知列表（GET）、单条标记已读（PATCH /notifications/{id}/read）、全部已读（PATCH /notifications/read-all）和未读数查询（GET /notifications/unread-count）。"))
C.append(para("推荐模块（/recommendations）：提供个性化推荐列表（GET /recommendations，支持冷启动）和相关帖子推荐（GET /posts/{id}/related）。"))
C.append(para("AI 模块（/posts/{id}/ai）：提供摘要生成（POST /posts/{postId}/ai/summary）、摘要查询（GET，用户隔离）、提问（POST /posts/{postId}/ai/qa）和问答历史查询（GET /posts/{postId}/ai/qa/history）。"))
C.append(para("草稿模块（/drafts）：提供草稿保存（POST，upsert）、列表查询（GET）、详情查询（GET /drafts/{draftId}）、存在性检查（GET /drafts/check）和删除（DELETE）。"))
C.append(para("管理模块（/admin）：提供用户管理、帖子管理、版块管理、公告管理、神评管理和数据统计接口，所有管理接口均需 ADMIN 或 MODERATOR 角色权限。"))
C.append(para("此外，还包括版块分类模块（/categories）、公告模块（/notices 和 /categories/{id}/notices）和文件上传模块（/files/upload）。所有接口的请求参数和响应格式均通过 DTO 类进行严格约束，确保 API 的一致性和可维护性。"))

C.append(heading("4.4 安全设计", 2))
C.append(para("系统的安全设计遵循纵深防御原则，从网络层、应用层和数据层三个层面构建安全防护体系："))
C.append(para("认证与授权：采用 Spring Security + JWT 的无状态认证方案。用户登录成功后，后端签发包含 userId、username 和 role 的 JWT 令牌（24 小时有效期，密钥长度不低于 256 位）。后续请求通过 JwtAuthenticationFilter 解析 Authorization 请求头中的 Bearer 令牌，验证签名和有效期后，将认证信息注入 SecurityContextHolder。授权通过 @PreAuthorize 注解和 SecurityFilterChain 的 URL 匹配规则实现，支持方法级和 URL 级的权限控制。"))
C.append(para("密码安全：用户密码采用 BCrypt 算法进行不可逆哈希存储，cost 因子设置为 10。即使数据库被非法访问，也无法还原用户的明文密码。登录验证时通过 BCryptPasswordEncoder.matches() 方法比对用户输入的密码与数据库中的哈希值。"))
C.append(para("防护攻击：防范 SQL 注入方面，使用 MyBatis-Plus 的参数化查询，禁止拼接 SQL 字符串，并启用 BlockAttackInnerInterceptor 插件拦截潜在的注入攻击。防范 XSS 攻击方面，前端 Markdown 内容经 markdown-it（禁用 HTML 标签）渲染后，再通过 DOMPurify 白名单机制二次清洗；后端对存储的内容进行 HTML 实体编码。防范 CSRF 攻击方面，系统采用 RESTful 无状态 API 架构，不依赖 Cookie-Session 机制，天然免疫 CSRF 攻击。"))
C.append(para("可见权限控制：帖子可见权限通过 PostVisibilityService 集中校验。对于公开帖子，所有用户均可访问；登录可见帖子，未登录用户返回 404；关注者可见帖子，非关注者返回 404；私密帖子，仅作者本人和管理员可访问。使用 404 而非 403 状态码防止通过状态码差异推断帖子是否存在。管理员和版主具有全局可见权限，但 AI 互动记录不受管理员查看。"))
C.append(para("前端安全：通过路由守卫（beforeEach）控制页面级权限，未登录用户访问需要认证的页面时自动跳转到登录页。通过 v-permission 自定义指令控制按钮级权限，根据用户角色动态显示或隐藏操作按钮。Token 令牌通过 ofetch 拦截器自动注入请求头，禁止前端代码手动拼接 Authorization 头。"))

C.append(heading("4.5 前端架构设计", 2))
C.append(para("前端应用采用 Vue 3 组合式 API 的单页应用架构，整体设计遵循组件化、模块化和类型安全原则："))
C.append(para("路由设计：采用 Vue Router 4 的懒加载（动态 import）策略，将页面组件按需加载，减少首屏打包体积。路由结构分为三类布局：默认布局（DefaultLayout，包含顶部导航栏和底部页脚，适用于大部分公共页面和登录用户页面）、管理布局（AdminLayout，包含可折叠的左侧导航菜单，适用于管理后台页面）和认证布局（AuthLayout，简洁的居中布局，适用于登录注册页面）。通过路由元信息（meta）配置页面标题和所需角色权限，配合全局路由守卫实现权限校验。"))
C.append(para("状态管理：使用 Pinia 进行全局状态管理，定义了四个 Store：useUserStore（管理 Token、用户信息、登录状态和角色判断）、useNotificationStore（管理未读通知数量和新通知标记）、useDraftStore（管理当前编辑帖子的草稿状态）和 useAppStore（管理侧边栏折叠、移动端菜单等 UI 状态）。Store 之间保持松耦合，通过组合式 API 在组件中按需引用。"))
C.append(para("组件设计：组件按照功能域划分为多个目录：common/（通用组件，如 AppHeader、AppFooter、AppSidebar、EmptyState、LoadingSkeleton）、markdown/（Markdown 编辑器和阅读器）、post/（帖子卡片和列表）、user/（登录和注册表单）、notice/（公告横幅和轮播）、ai/（AI 摘要和问答面板）、notification/（通知铃铛）、admin/（统计图表和数据表格）、icons/（自定义图标组件）。所有组件使用 <script setup lang=\"ts\"> 语法，通过 defineProps 和 defineEmits 定义类型安全的组件接口。"))
C.append(para("API 层设计：通过 src/api/index.ts 创建统一的 ofetch 实例，配置请求拦截器（自动注入 Authorization 令牌）和响应拦截器（统一错误处理、401 自动跳转登录页）。API 接口按模块拆分到 src/api/modules/ 目录下（如 auth.ts、post.ts、comment.ts 等），每个模块导出类型安全的异步函数，确保类型系统的端到端覆盖。"))
C.append(para("样式设计：采用 SCSS 预处理器，定义全局 CSS 变量（通过 var() 实现主题色、背景色、文字色等动态切换）。系统支持暗色模式（默认）和亮色模式，通过 Element Plus 的暗色模式支持和自定义 CSS 变量共同实现。页面遵循 12 列栅格布局，内容区最大宽度 1200px 居中显示。"))

# Save
with open(PROJECT_FILE, "w", encoding="utf-8") as f:
    json.dump(project, f, ensure_ascii=False, indent=2)

print("Chapters 3-4 written successfully.")
print(f"Total content items: {len(project['content'])}")
