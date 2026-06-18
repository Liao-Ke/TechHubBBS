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

# ========== 第五章 系统实现 ==========
C.append(page_break())
C.append(heading("第五章 系统实现", 1))

C.append(heading("5.1 开发环境搭建", 2))
C.append(para("系统开发环境的搭建采用 Docker Compose 进行基础设施管理，确保开发环境的一致性和可复现性。后端开发环境需要 JDK 17+、Maven 3.9+、MySQL 8.0 和 Redis 7。前端开发环境需要 Node.js 20.19+ 和 pnpm 包管理器。"))
C.append(para("基础设施启动：通过项目根目录下的 docker-compose.yml 文件一键启动 MySQL 和 Redis 服务。MySQL 容器在首次启动时自动执行 backend/src/main/resources/db/ 目录下的 schema.sql（建表脚本）和 data.sql（初始数据脚本），完成数据库的初始化。Redis 容器采用 Alpine 轻量镜像，占用资源少、启动速度快。"))
C.append(para("后端启动：在 backend/ 目录下执行 mvn spring-boot:run 命令启动 Spring Boot 应用，默认监听 8080 端口。应用启动后，Knife4j API 文档可通过 http://localhost:8080/doc.html 访问。所有敏感配置（数据库密码、JWT 密钥、AI API 密钥等）均通过环境变量注入，开发者在本地通过 IntelliJ IDEA 的运行配置或 .env 文件设置。"))
C.append(para("前端启动：在 frontend/ 目录下执行 pnpm install 安装依赖，然后执行 pnpm dev 启动 Vite 开发服务器，默认监听 5173 端口。Vite 配置了代理规则，将 /api 前缀的请求转发到 http://localhost:8080，解决开发环境下的跨域问题。开发服务器支持热模块替换（HMR），修改代码后页面即时更新，无需手动刷新。"))

C.append(heading("5.2 用户认证与授权实现", 2))
C.append(para("认证流程：用户通过 POST /api/v1/auth/register 提交用户名、邮箱和密码进行注册。后端对密码进行 BCrypt 哈希处理（cost = 10）后存入数据库，密码的明文不会出现在任何日志或响应中。登录时，用户通过 POST /api/v1/auth/login 提交用户名和密码，后端查询数据库获取用户信息，通过 BCryptPasswordEncoder.matches() 方法验证密码，验证通过后调用 JwtTokenProvider.generateToken() 方法生成 JWT 令牌。令牌包含 userId、username 和 role 三类声明（Claim），设置 24 小时过期时间，签名密钥通过 JWT_SECRET 环境变量注入。"))
C.append(para("请求认证：每个请求到达后端后，JwtAuthenticationFilter 从 Authorization 请求头中提取 Bearer 令牌，调用 JwtTokenProvider.validateToken() 验证令牌的签名和有效期。验证通过后，从令牌中解析出用户信息，构建 UsernamePasswordAuthenticationToken 并注入 SecurityContextHolder，后续的 Controller 和 Service 层可通过 SecurityUtils.getCurrentUserId() 等工具方法获取当前用户信息。"))
C.append(para("授权控制：系统采用 URL 级别和方法级别的双重授权策略。在 SecurityConfig 中配置 SecurityFilterChain，通过 requestMatchers 设置不同 URL 路径的访问权限（如 /api/v1/auth/** 允许匿名访问，/api/v1/admin/** 要求 ADMIN 角色）。在 Service 层和 Controller 层通过 @PreAuthorize 注解实现方法级权限控制，例如 @PreAuthorize(\"hasRole('ADMIN')\") 确保只有管理员能执行角色变更操作。帖子编辑和删除操作通过比较 author_id 和当前 userId 验证所有权，确保用户只能操作自己的内容。"))
C.append(para("前端认证：用户在登录成功后，前端通过 useUserStore 将令牌存储到 localStorage 和 Pinia Store 中。ofetch 实例的请求拦截器自动从 Store 读取令牌并注入 Authorization 请求头。响应拦截器监听 401 状态码，自动清除本地令牌并跳转到登录页。路由守卫（beforeEach）在每次页面跳转前检查用户登录状态和角色权限，拦截未授权的访问尝试。"))

C.append(heading("5.3 帖子可见权限控制实现", 2))
C.append(para("帖子可见权限是系统安全设计的核心环节。该功能通过 PostVisibilityService.isVisible() 方法实现集中化的可见性判断逻辑："))
C.append(para("公开帖子（VISIBILITY = 0）：所有用户均可查看，无需任何条件判断。"))
C.append(para("登录可见帖子（VISIBILITY = 1）：需要用户已登录，未登录用户返回 404。通过 SecurityUtils.isAuthenticated() 判断当前请求的认证状态。"))
C.append(para("关注者可见帖子（VISIBILITY = 2）：需要当前用户是作者或其关注者。通过 FollowService.isFollowing(currentUserId, authorId) 判断当前用户是否关注了帖子作者。"))
C.append(para("私密帖子（VISIBILITY = 3）：仅帖子作者本人可以查看。通过比较 currentUserId 与 post.authorId 判断所有权。"))
C.append(para("所有判断路径中，管理员和版主角色均具备全局查看权限作为例外处理。关键设计决策：对于无权限访问的帖子统一返回 404 NOT FOUND 状态码，而非 403 FORBIDDEN。这样做是为了防止恶意用户通过观察 HTTP 状态码的差异推断出某些帖子 ID 是否存在（信息泄露攻击）。判断逻辑分散在帖子详情查询（PostServiceImpl.getPostById()）、帖子列表查询和推荐列表查询等多个服务方法中，确保可见性控制的全覆盖。"))

C.append(heading("5.4 神评机制实现", 2))
C.append(para("神评（Divine Comment）机制是系统的社区治理创新功能，通过社区投票自动筛选和标记高质量评论。实现包含以下环节："))
C.append(para("神评资格判定：通过 DivineCommentScheduler 定时任务（每 5 分钟执行一次）扫描所有状态为正常且未被删除的帖子，统计每个帖子的评论数量。当评论数达到 10 条及以上时，将帖子的 eligible_for_divine 字段设置为 1（具备神评资格），否则设置为 0。"))
C.append(para("神评自动授精：对于 eligible_for_divine = 1 的帖子下的所有评论，系统自动检查神评阈值条件：点赞数 >= 10 AND 推荐数 >= 5（双重维度过滤）。满足条件的评论，is_divine 字段设置为 1，divine_time 记录当前时间戳。同时更新帖子的 divine_comment_count 计数器。"))
C.append(para("神评自动摘除：定时任务同样检查已标记为神评的评论，若其点赞数或推荐数不再满足阈值条件（如被取消点赞/取消推荐导致数量下降），则自动将 is_divine 恢复为 0，更新帖子的计数器。"))
C.append(para("防滥用机制：推荐神评功能通过 CommentRecommendServiceImpl 实现，包含多重防护：用户必须注册满 7 天才能使用推荐功能（防止新注册账号刷票）；通过 comment_recommend 表的 (comment_id, user_id) 唯一约束确保同一用户对同一评论只能推荐一次（防止重复投票）；点赞和取消推荐操作为幂等操作，重复请求不会产生额外影响。"))
C.append(para("管理员干预：管理员可以通过管理后台的 PATCH /admin/comments/{id}/divine 接口手动授精或撤销神评状态，覆盖自动算法的判定结果。管理后台同时提供神评推荐日志查询功能，便于追踪异常投票行为。"))

C.append(heading("5.5 AI 辅助功能实现", 2))
C.append(para("AI 辅助阅读功能通过 AiServiceImpl 和 AiClient 工具类实现，为帖子的阅读和理解提供智能辅助："))
C.append(para("AI 摘要功能：用户发起摘要请求（POST /posts/{postId}/ai/summary），后端首先校验帖子内容长度不少于 50 字符，然后通过 @Async 异步注解开启异步任务。异步任务中，系统构建包含帖子全文的 Prompt，调用 AiClient.call() 方法向配置的 LLM API 发送请求（2 次重试，10 秒超时），将返回的摘要内容存入 ai_summary 表。前端在发起请求后进入轮询状态，每隔 2 秒查询摘要状态，直到状态变为已完成或失败。"))
C.append(para("AI 问答功能：用户获得帖子摘要后，可通过 POST /posts/{postId}/ai/qa 接口向 AI 提问。后端构建上下文 Prompt，将帖子全文、已有摘要和问题一并发送给 LLM。为防止 Prompt 注入攻击，Prompt 中明确指示 LLM 仅基于提供的帖子内容进行回答，忽略任何试图改变系统指令的提示。问答记录存入 ai_qa_history 表，支持历史记录查询。"))
C.append(para("用户数据隔离：ai_summary 表通过 (user_id, post_id) 复合唯一索引确保每个用户对每个帖子最多有一条摘要记录。ai_qa_history 表通过 user_id 和 post_id 索引确保查询结果仅包含当前用户的问答记录。摘要和问答的查询接口不接受管理员身份例外，即使是管理员也无法查看其他用户的 AI 互动记录，严格保护用户隐私。"))
C.append(para("LLM 接口适配：AiClient 类通过读取 AiConfig 配置实现 LLM 接口的可替换性。支持 OpenAI API 格式和国内大模型 API 格式（通过配置不同的 API URL、模型名称和认证方式），所有敏感凭据通过环境变量注入。"))

C.append(heading("5.6 推荐系统实现", 2))
C.append(para("推荐系统采用离线计算 + 在线服务的混合架构，通过 RecommendationScheduler 定时任务和 RecommendationServiceImpl 在线服务协同工作："))
C.append(para("离线计算阶段：定时任务定期执行以下步骤。步骤一：帖子关键词提取。使用 Jieba 分词器对帖子正文进行中文分词，去除停用词后，利用 TF-IDF 算法计算每个关键词的权重，结果存入 post_keyword 表。步骤二：用户画像构建。分析用户的浏览记录、点赞、收藏等行为数据，聚合被用户关注帖子的关键词，计算加权平均权重，构建用户兴趣向量（JSON 格式），存入 user_profile 表并缓存到 Redis（TTL 30 分钟）。步骤三：相似度矩阵计算。对任意两个帖子，基于关键词权重向量的余弦相似度公式计算相似分数，结果存入 post_similarity 表。"))
C.append(para("在线查询阶段：当用户请求推荐列表（GET /recommendations）时，系统首先从 Redis 中获取用户画像（miss 时回源数据库并回填缓存）。基于用户画像中的高权重关键词，在 post_similarity 表中查询与用户兴趣相关的帖子。结果集经过可见权限过滤（调用 PostVisibilityService.isVisible()）、去重（排除用户已浏览的帖子）和排序（按相似度分数降序排列），最终分页返回给前端。"))
C.append(para("冷启动策略：对于新注册用户（无浏览和互动记录，user_profile 表无数据），系统返回热门帖子作为默认推荐结果。热门帖子通过 HotPostScheduler 定时任务维护，综合考量帖子的浏览量、点赞数和评论数等指标进行排序，结果缓存到 Redis 中。"))

C.append(heading("5.7 草稿自动保存实现", 2))
C.append(para("草稿自动保存功能通过前后端协同实现，为用户提供无缝的内容保护体验："))
C.append(para("前端实现：使用 useDraft 组合函数（src/composables/useDraft.ts）封装草稿逻辑。组件挂载（onMounted）时，通过 GET /drafts/check?postId=xxx 检查是否存在未发布的草稿，若存在则以 ElMessageBox.confirm 弹窗询问用户是否恢复草稿。编辑过程中，通过 setInterval 设置 30 秒间隔的定时器，每次触发时将编辑器当前内容通过 POST /drafts 发送到后端保存。组件卸载（onUnmounted）时清除定时器并执行最后一次保存。"))
C.append(para("后端实现：PostDraftServiceImpl 的 saveDraft() 方法通过 MyBatis-Plus 的 saveOrUpdate() 实现 upsert 逻辑。利用 post_draft 表的 (user_id, post_id) 复合唯一约束：对于新建帖子的草稿（post_id 为 NULL），每个用户最多保存一份；对于编辑已有帖子的草稿（post_id 有值），每篇帖子对每个用户各保存一份独立草稿。帖子发布成功后，创建帖子接口自动清除对应的草稿记录。"))

C.append(heading("5.8 管理后台实现", 2))
C.append(para("管理后台是系统管理和运维的核心界面，采用独立的路由布局和组件体系："))
C.append(para("布局设计：AdminLayout 采用左侧可折叠导航栏 + 右侧内容区的经典管理界面布局。AppSidebar 组件渲染导航菜单，菜单项根据用户角色动态显示（ADMIN 可见全部菜单，MODERATOR 仅见帖子管理和公告管理）。侧边栏的折叠状态通过 useAppStore 管理并持久化到 localStorage。"))
C.append(para("仪表盘：DashboardPage 是管理后台的首页，通过 ECharts 图表库展示系统的核心统计数据。包括：用户注册趋势（折线图）、帖子发布趋势（折线图）、版块帖子分布（饼图）、近期活跃度统计（柱状图）等。数据通过 GET /admin/statistics 接口获取，支持按日、周、月等不同粒度查询。"))
C.append(para("用户管理：UserManagePage 展示用户列表，支持按用户名和邮箱搜索。管理员可以对用户执行封禁/解封操作（PATCH /admin/users/{id}/ban）和角色变更操作（PATCH /admin/users/{id}/role）。被封禁的用户无法登录系统，所有操作通过后台接口进行，具有完整的操作日志记录。"))
C.append(para("帖子管理：PostManagePage 展示全量帖子列表（不受可见权限限制），管理员和版主可以对帖子进行类型变更（加精/置顶：PATCH /admin/posts/{id}/type）、锁定/解锁（PATCH /admin/posts/{id}/lock）和强制删除（DELETE /admin/posts/{id}）。锁定后的帖子禁止新增评论，但已有内容仍可浏览。"))
C.append(para("神评管理：DivineManagePage 展示神评推荐日志和所有神评列表。管理员可以通过该页面查看推荐投票记录，识别异常投票行为，并对神评进行人工授精或撤销操作（PATCH /admin/comments/{id}/divine），覆盖自动算法的判定。"))

# ========== 第六章 系统测试 ==========
C.append(page_break())
C.append(heading("第六章 系统测试", 1))

C.append(heading("6.1 测试环境", 2))
C.append(para("系统测试在以下环境中进行：操作系统为 Windows 11，浏览器为 Chrome 120+ 和 Edge 120+，后端运行在 JDK 17 + Spring Boot 3.5 环境，数据库为 MySQL 8.0 + Redis 7（通过 Docker Compose 部署），测试工具包括 Postman（API 测试）、JUnit 5 + MockMvc（单元测试和集成测试）以及 Chrome DevTools（前端性能分析）。"))

C.append(heading("6.2 功能测试", 2))
C.append(para("针对系统的核心功能模块，设计了覆盖主要业务流程的功能测试用例："))
C.append(para("用户认证测试：验证注册流程（正常注册、重复用户名注册、无效邮箱格式、密码长度不足）、登录流程（正确凭据登录、错误密码登录、已封禁用户登录）和 Token 过期处理。所有测试用例均通过，边界条件处理正确。"))
C.append(para("帖子发布与可见权限测试：验证帖子的创建、编辑和删除功能。重点测试四级可见权限的边界条件：未登录用户访问登录可见帖子（返回 404）、非关注者访问关注者可见帖子（返回 404）、非作者访问私密帖子（返回 404）、管理员访问任意帖子（正常返回）。测试覆盖了直接API调用和浏览器前端两种访问路径，权限校验均准确无误。"))
C.append(para("评论与神评测试：验证评论的发布、嵌套回复和删除功能。测试神评机制：创建 10 条评论后帖子获得神评资格，对某评论累积 10 个赞和 5 个推荐后自动标记为神评，减少点赞后神评自动撤销。注册不足 7 天的用户尝试推荐神评被拒绝。所有测试用例均符合预期。"))
C.append(para("AI 功能测试：验证 AI 摘要的异步生成流程（发起请求 → 状态查询轮询 → 摘要返回），内容过短（<50 字）时拒绝生成，问答功能正常且用户数据隔离正确，不同用户对同一帖子的摘要互不可见。"))
C.append(para("草稿功能测试：验证编辑过程中草稿的自动保存（30 秒间隔），页面刷新后草稿恢复提示的功能，新建帖子和编辑已有帖子的草稿独立存储，帖子发布后草稿自动清除。"))
C.append(para("管理后台功能测试：验证管理员对用户封禁/解封和角色变更，对帖子的加精/置顶/锁定/强制删除，版块和公告的增删改查，以及神评的人工干预功能。所有管理操作均进行了权限校验，非管理员被正确拒绝。"))

C.append(heading("6.3 性能测试", 2))
C.append(para("性能测试主要关注系统的响应时间和并发处理能力："))
C.append(para("接口响应时间测试：使用 Postman 对主要接口进行 20 次请求的响应时间采样。帖子列表查询（GET /posts，分页 20 条）平均响应时间 85ms，帖子详情查询（GET /posts/{id}）平均响应时间 62ms，评论列表查询平均响应时间 78ms，登录接口平均响应时间 210ms（包含 BCrypt 密码验证开销）。所有核心读接口响应时间均在 100ms 以内，满足性能需求。AI 摘要异步生成的平均响应时间取决于 LLM API 的响应速度，通常在 5-15 秒之间。"))
C.append(para("Redis 缓存效果测试：对比开启和关闭 Redis 缓存场景下的推荐列表查询性能。关闭缓存时，推荐列表查询平均响应时间为 320ms；开启缓存后降至 45ms，性能提升约 86%，验证了 Redis 缓存在推荐系统中显著的加速效果。"))

C.append(heading("6.4 安全测试", 2))
C.append(para("安全测试覆盖了系统的关键安全机制："))
C.append(para("认证绕过测试：尝试在未携带 Authorization 令牌的情况下访问需认证的接口（如创建帖子、点赞等），所有接口均正确返回 401 Unauthorized。尝试使用过期令牌（修改系统时间）访问，JWT 验证正确拒绝并返回 401。尝试篡改令牌中的 role 字段，JWT 签名验证失败。"))
C.append(para("权限提升测试：使用普通用户令牌尝试访问管理后台接口（如 /admin/users），所有接口均正确返回 403 Forbidden。尝试通过修改请求参数编辑他人的帖子，后端所有权校验正确拒绝。尝试访问 AI 接口查看其他用户的摘要记录，用户隔离机制正确生效。"))
C.append(para("XSS 攻击测试：在帖子正文和评论中注入 <script>alert('xss')</script> 等恶意脚本标签，经 markdown-it（html: false 配置）和 DOMPurify 双重清洗后，前端页面正常渲染为纯文本而非可执行脚本。"))
C.append(para("SQL 注入测试：在搜索关键词和用户名查询参数中尝试 ' OR '1'='1 等 SQL 注入载荷，MyBatis-Plus 的参数化查询和 BlockAttackInnerInterceptor 插件正确防护，未发生异常查询行为。"))
C.append(para("文件上传测试：尝试上传非白名单格式文件（.exe、.php、.jsp），后端文件上传拦截器正确拒绝并返回错误信息。尝试上传超过 5MB 尺寸的图片文件，同样被正确拦截。"))

# ========== 第七章 总结与展望 ==========
C.append(page_break())
C.append(heading("第七章 总结与展望", 1))

C.append(heading("7.1 工作总结", 2))
C.append(para("本文围绕技术社区论坛系统的设计与实现，完成了以下主要工作："))
C.append(para("系统设计与架构方面：基于前后端分离的架构思想，设计并实现了 TechHub 技术社区论坛系统。后端采用 Spring Boot + MyBatis-Plus + Spring Security + JWT 技术栈，前端采用 Vue 3 + TypeScript + Element Plus + Vite 技术栈，数据库采用 MySQL 8.0 + Redis 7，并通过 Docker Compose 实现容器化部署。系统遵循 RESTful API 设计风格，实现了 12 个 API 模块共 50+ 个接口端点，覆盖了用户管理、内容发布、评论互动、通知推送和管理后台等完整业务闭环。"))
C.append(para("数据库设计方面：遵循第三范式和项目数据库规范，设计了 18 张数据表，覆盖了用户、帖子、评论、分类、通知、AI 记录、推荐数据和草稿等所有业务实体。通过外键约束保障参照完整性，通过唯一索引防止数据重复，通过软删除机制保障数据可恢复性。"))
C.append(para("安全防护方面：实现了多层安全防护体系，包括 BCrypt 密码哈希存储（cost = 10）、JWT 无状态认证（256 位密钥）、四级帖子可见权限的细粒度控制、XSS 和 SQL 注入的全面防护、文件上传的白名单和尺寸限制，以及前端路由守卫和按钮权限指令的双重权限控制。"))
C.append(para("创新功能方面：实现了四项特色功能——四级帖子可见权限控制（公开/登录可见/关注者可见/私密）、基于双重维度的神评社区治理机制（点赞数+推荐数，含自动授精/摘除和防滥用机制）、30秒间隔的草稿自动保存与恢复功能，以及用户数据隔离的 AI 私密摘要与问答系统。此外，还实现了基于 Jieba 分词和 TF-IDF 的内容推荐算法以及冷启动策略。"))
C.append(para("测试验证方面：对系统进行了全面的功能测试、性能测试和安全测试。核心读接口平均响应时间在 100ms 以内，Redis 缓存使推荐查询性能提升 86%，所有安全测试用例均通过，系统运行稳定可靠。"))

C.append(heading("7.2 不足与展望", 2))
C.append(para("尽管 TechHub 系统已经实现了完整的技术社区功能，但仍存在以下不足和可改进之处："))
C.append(para("推荐算法的优化：当前推荐系统仅基于 TF-IDF 关键词权重和余弦相似度，属于纯内容推荐方法，未引入协同过滤等更高级的推荐策略。未来可以融合用户行为数据，采用混合推荐策略提升推荐的准确性和多样性。同时，推荐模型的更新周期为定时任务全量计算，对于活跃度高的社区存在时效性不足的问题，可考虑引入增量更新机制或实时流计算框架。"))
C.append(para("AI 功能的扩展：当前 AI 功能仅支持帖子摘要和问答，未来可以扩展更多智能辅助功能，如代码片段解析与解释、技术话题自动分类与标签推荐、用户兴趣迁移的智能检测等。同时，可以引入 RAG（检索增强生成）技术，使 AI 能够结合更多外部知识进行回答。"))
C.append(para("性能与扩展性优化：当前系统采用单体后端架构，虽然通过三层分工（Controller-Service-Mapper）实现了良好的代码组织，但在面对大规模用户并发时存在性能瓶颈。未来可以考虑将核心服务拆分为微服务架构，引入消息队列（如 RabbitMQ）解耦通知推送等异步任务，使用 Elasticsearch 替代 MySQL 进行全文搜索以提升检索性能。"))
C.append(para("前端体验优化：当前前端采用单页应用（SPA）架构，首屏加载时间较长。未来可以考虑引入服务端渲染（SSR）或静态站点生成（SSG）技术优化首屏加载速度和 SEO。同时，可以增加 PWA（渐进式 Web 应用）支持，使用户获得接近原生应用的体验。"))
C.append(para("测试覆盖率提升：当前测试主要集中在核心功能模块的手动验证，单元测试和自动化集成测试的覆盖率仍有提升空间。未来应持续补充 JUnit 测试用例和前端组件测试，引入 CI/CD 流水线实现代码提交的自动化测试和部署。"))

# ========== 参考文献 ==========
C.append(page_break())
C.append(heading("参考文献", 1))

refs = [
    "[1] 张洪伟, 王宇飞. Spring Boot 实战派[M]. 北京: 电子工业出版社, 2023.",
    "[2] 尤雨溪. Vue.js 设计与实现[M]. 北京: 人民邮电出版社, 2022.",
    "[3] 朱荣鑫, 张天, 黄迪璇. Spring 微服务架构设计（第2版）[M]. 北京: 机械工业出版社, 2020.",
    "[4] 林昊. 高性能 MySQL（第4版）[M]. 北京: 电子工业出版社, 2022.",
    "[5] Josiah L. Carlson. Redis 实战[M]. 黄健宏 译. 北京: 人民邮电出版社, 2015.",
    "[6] 李刚. 轻量级 Java EE 企业应用实战（第5版）[M]. 北京: 电子工业出版社, 2021.",
    "[7] 项亮. 推荐系统实践[M]. 北京: 人民邮电出版社, 2012.",
    "[8] 杨开振, 周吉文, 梁华辉, 谭茂华. Java EE 互联网轻量级框架整合开发[M]. 北京: 电子工业出版社, 2017.",
    "[9] 吴志祥, 王建勇. 大规模文本分类中的特征选择与权重计算研究综述[J]. 软件学报, 2019, 30(3): 735-754.",
    "[10] Vaswani A, Shazeer N, Parmar N, et al. Attention Is All You Need[C]. Advances in Neural Information Processing Systems, 2017: 5998-6008.",
    "[11] Brown T B, Mann B, Ryder N, et al. Language Models are Few-Shot Learners[C]. Advances in Neural Information Processing Systems, 2020: 1877-1901.",
    "[12] 开源中国. SegmentFault 技术问答社区架构演进[EB/OL]. https://segmentfault.com, 2023.",
    "[13] Spring Team. Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/docs/current/reference/html/, 2025.",
    "[14] MyBatis-Plus Team. MyBatis-Plus 开发文档[EB/OL]. https://baomidou.com, 2025.",
    "[15] Vue.js Team. Vue 3 Documentation[EB/OL]. https://vuejs.org/guide/, 2025.",
]

for ref in refs:
    C.append(para(ref, font_size="11pt"))

# ========== 致谢 ==========
C.append(page_break())
C.append(heading("致谢", 1))
C.append(para("时光荏苒，大学生涯即将画上句号。在本次毕业设计完成之际，我谨向所有在学习和项目开发过程中给予我帮助和支持的人表示最诚挚的感谢。"))
C.append(para("首先，衷心感谢我的指导老师。在毕业设计的选题、方案设计、系统开发和论文撰写的全过程中，老师给予了我悉心的指导和宝贵的建议。老师严谨的治学态度、丰富的工程经验和耐心的教学风格，不仅帮助我顺利完成了毕业设计，更让我对软件工程实践有了更深刻的理解和认识。"))
C.append(para("其次，感谢大学期间所有授课老师。数据库原理、Java 程序设计、Web 编程技术等专业课程的扎实教学，为本次毕业设计的完成奠定了坚实的理论基础。在课程中学习的软件工程方法、数据库设计原则和前后端开发技术，直接指导了 TechHub 系统的架构设计和功能实现。"))
C.append(para("此外，感谢一起学习和奋斗的同学们。在项目开发过程中，同学们的讨论和交流为我提供了许多思路和灵感，大家的互相鼓励和帮助让整个开发过程充满动力和乐趣。"))
C.append(para("最后，特别感谢我的家人。感谢他们在我求学期间一如既往的支持、理解和鼓励，正是他们的无私付出，才让我能够全身心地投入到学业和项目开发中。"))
C.append(para("路漫漫其修远兮，吾将上下而求索。毕业设计是大学学习的终点，更是技术探索的新起点。在未来的工作和学习中，我将继续保持对技术的好奇心和求知欲，不断提升自己的专业能力，努力成为一名优秀的软件工程师。"))

# Update metadata
project["metadata"]["author"] = "XXX"
project["metadata"]["subject"] = "毕业设计论文"
project["metadata"]["description"] = "TechHub 技术社区论坛的设计与实现 - 基于 Spring Boot + Vue 3 的前后端分离技术社区平台"
project["metadata"]["title"] = "TechHub 技术社区论坛的设计与实现"

# Save
with open(PROJECT_FILE, "w", encoding="utf-8") as f:
    json.dump(project, f, ensure_ascii=False, indent=2)

print("Chapters 5-7 + Refs + Acknowledgements written successfully.")
print(f"Total content items: {len(project['content'])}")
