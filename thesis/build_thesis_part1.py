import json
import os

PROJECT_FILE = "E:/User/Desktop/TechHubBBS/thesis/thesis-project.json"

with open(PROJECT_FILE, "r", encoding="utf-8") as f:
    project = json.load(f)

# Clear existing content
project["content"] = []

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

# ========== TITLE PAGE ==========
C.append(heading("TechHub 技术社区论坛的设计与实现", 1))
C.append(empty_line())
C.append(para("基于 Spring Boot + Vue 3 的前后端分离技术社区平台", font_size="14pt", alignment="center"))
C.append(empty_line())
C.append(empty_line())

# ========== 中文摘要 ==========
C.append(page_break())
C.append(heading("摘要", 1))
C.append(para("随着互联网技术的快速发展和开发者社区的蓬勃兴起，技术论坛作为知识分享与交流的重要平台，在软件开发领域扮演着越来越重要的角色。本文设计并实现了一个名为 TechHub 的前后端分离技术社区论坛系统，旨在为开发者提供一个集内容发布、互动交流、AI 辅助阅读与个性化推荐于一体的综合性技术交流平台。"))
C.append(para("系统后端采用 Spring Boot 3.5 框架，结合 MyBatis-Plus ORM 框架实现数据持久化，使用 Spring Security 与 JWT 实现无状态认证授权，通过 Redis 缓存提升系统性能。前端采用 Vue 3 + TypeScript 技术栈，使用 Vite 构建工具、Element Plus 组件库和 Pinia 状态管理，构建了响应式用户界面并支持暗色模式。系统使用 MySQL 8.0 作为关系型数据库，Redis 7 作为缓存中间件，并通过 Docker Compose 实现一键部署。"))
C.append(para("系统实现了用户注册登录、帖子发布与浏览、评论互动、关注收藏、实时通知等基础社区功能。在此基础上，创新性地实现了四大特色功能：四级帖子可见权限控制（公开、登录可见、关注者可见、私密），基于双重维度（点赞数 + 推荐数）的神评社区治理机制，30 秒间隔的草稿自动保存与恢复功能，以及用户数据隔离的 AI 私密摘要与问答系统。此外，系统还集成了基于 Jieba 分词与 TF-IDF 的内容推荐算法，以及功能完善的管理后台。"))
C.append(para("经过全面的功能测试与性能验证，系统运行稳定，各项功能指标均达到预期设计目标，为技术社区类 Web 应用的开发提供了完整的工程实践参考。"))
C.append(empty_line())
C.append(para("关键词：技术社区；Spring Boot；Vue.js；前后端分离；AI 辅助阅读；推荐系统", bold=True))

# ========== 英文摘要 ==========
C.append(page_break())
C.append(heading("Abstract", 1))
C.append(para("With the rapid development of Internet technologies and the flourishing of developer communities, technical forums play an increasingly important role as platforms for knowledge sharing and communication in the software development field. This paper designs and implements a full-stack technical community forum system called TechHub, aiming to provide developers with a comprehensive technical communication platform integrating content publishing, interactive communication, AI-assisted reading, and personalized recommendations."))
C.append(para("The backend of the system adopts the Spring Boot 3.5 framework, combined with the MyBatis-Plus ORM framework for data persistence, uses Spring Security and JWT for stateless authentication and authorization, and improves system performance through Redis caching. The frontend adopts the Vue 3 + TypeScript technology stack, using Vite as the build tool, Element Plus component library, and Pinia state management, building a responsive user interface with dark mode support. The system uses MySQL 8.0 as the relational database, Redis 7 as the caching middleware, and implements one-click deployment through Docker Compose."))
C.append(para("The system implements basic community functions such as user registration and login, post publishing and browsing, comment interaction, following and favorites, and real-time notifications. On this basis, it innovatively implements four distinctive features: four-level post visibility control (public, login-only, followers-only, private), a divine comment community governance mechanism based on dual dimensions (like count + recommendation count), draft auto-save and recovery with 30-second interval, and an AI private summary and Q&A system with user data isolation. In addition, the system integrates a content recommendation algorithm based on Jieba segmentation and TF-IDF, as well as a fully functional admin dashboard."))
C.append(para("After comprehensive functional testing and performance verification, the system runs stably, and all functional indicators meet the expected design goals, providing a complete engineering practice reference for the development of technical community web applications."))
C.append(empty_line())
C.append(para("Keywords: Technical Community; Spring Boot; Vue.js; Full-Stack Development; AI-Assisted Reading; Recommendation System", bold=True))

# ========== 第一章 绪论 ==========
C.append(page_break())
C.append(heading("第一章 绪论", 1))

C.append(heading("1.1 项目背景与意义", 2))
C.append(para("在当今数字化时代，互联网技术已经成为推动社会进步和经济发展的重要力量。技术社区论坛作为开发者之间知识共享、问题讨论和技术交流的核心平台，在软件开发生态中占据着不可替代的位置。无论是初学者寻求学习资源，还是资深开发者探讨前沿技术，技术社区都发挥着桥梁和纽带的作用。然而，传统的技术论坛系统往往存在功能单一、用户体验欠佳、缺乏智能化辅助等问题，难以满足现代开发者日益增长的多元化需求。"))
C.append(para("基于此，本课题设计并实现了一个名为 TechHub 的前后端分离技术社区论坛系统。该系统不仅涵盖了用户管理、内容发布、评论互动、通知推送等基础论坛功能，还创新性地引入了 AI 辅助阅读、个性化内容推荐、神评社区治理机制、草稿自动保护以及四级帖子可见权限等特色功能，旨在打造一个现代化、智能化、安全可靠的技术交流平台。"))
C.append(para("本课题具有重要的理论意义和实际应用价值。在理论层面，系统综合运用了软件工程、数据库原理、Web 编程技术和人工智能等多学科知识，为前后端分离架构的设计与实现提供了完整的工程实践案例。在应用层面，系统可直接部署使用，为开发者提供一个高效、安全、智能的技术交流环境，同时系统采用的推荐算法和 AI 集成方案可为同类系统的开发提供参考。"))

C.append(heading("1.2 国内外研究现状", 2))
C.append(para("在国外，技术社区平台发展较为成熟。以 Stack Overflow 为代表的问答型社区通过积分和徽章系统激励用户贡献高质量内容，已成为全球开发者首选的编程问答平台。Reddit 的 r/programming 子频道以投票排序机制聚集了大量技术讨论。GitHub Discussions 则为开源项目提供了内嵌的社区交流空间。这些平台在用户体验、内容治理和社区运营方面积累了丰富的经验，但在 AI 辅助阅读和个性化推荐方面仍有提升空间。"))
C.append(para("在国内，掘金、SegmentFault、CSDN、博客园等技术社区各具特色。掘金主打高质量技术文章分享，通过内容审核和推荐算法保障内容质量；SegmentFault 聚焦技术问答，形成了较为活跃的中文开发者社区；CSDN 作为老牌 IT 社区，拥有庞大的用户基础和丰富的内容资源。然而，这些平台在前后端分离架构的开放性、AI 辅助功能的深度集成以及细粒度的隐私控制方面仍存在不足。"))
C.append(para("综合分析国内外技术社区的现状，本课题所设计的 TechHub 系统在以下方面具有创新性：一是实现了四级帖子可见权限的细粒度隐私控制，解决了传统论坛隐私设置单一的问题；二是引入了基于双重维度的神评机制，通过社区共治的方式提升内容质量；三是将 AI 摘要与问答功能深度集成到帖子阅读流程中，且实现了用户数据的严格隔离；四是提供了 30 秒间隔的草稿自动保存功能，有效防止内容丢失。"))

C.append(heading("1.3 论文组织结构", 2))
C.append(para("本论文共分为七章，各章节内容安排如下："))
C.append(para("第一章 绪论：介绍项目的背景与意义，分析国内外技术社区的发展现状，阐述本课题的创新点，并概述论文的整体结构。"))
C.append(para("第二章 相关技术概述：详细介绍系统开发所涉及的关键技术，包括前端 Vue 3 技术栈、后端 Spring Boot 框架、数据存储技术、AI 集成方案以及容器化部署工具。"))
C.append(para("第三章 系统需求分析：从功能需求和非功能需求两个维度对系统进行全面分析，明确用户角色划分和核心业务流程。"))
C.append(para("第四章 系统设计：详细阐述系统的架构设计、数据库设计、API 接口设计和安全设计，展示系统的整体设计方案。"))
C.append(para("第五章 系统实现：重点介绍系统核心功能模块的实现细节，包括用户认证授权、帖子可见权限控制、神评机制、AI 辅助功能、推荐系统和草稿管理等。"))
C.append(para("第六章 系统测试：介绍系统的测试方案，包括功能测试、性能测试和安全测试的结果与分析。"))
C.append(para("第七章 总结与展望：总结本课题的主要工作成果，分析系统的不足之处，并对未来的改进方向进行展望。"))

# ========== 第二章 相关技术概述 ==========
C.append(page_break())
C.append(heading("第二章 相关技术概述", 1))

C.append(heading("2.1 前端技术栈", 2))
C.append(para("TechHub 系统前端采用 Vue 3 作为核心框架。Vue 3 是 Vue.js 框架的最新主版本，引入了 Composition API（组合式 API），提供了比 Options API 更灵活的代码组织方式，特别适合构建复杂的前端应用。TypeScript 作为 JavaScript 的超集，提供了静态类型检查和强大的 IDE 支持，显著提升了代码质量和开发效率。本系统所有前端代码均采用 TypeScript 编写，并通过 <script setup lang=\"ts\"> 语法糖简化组合式 API 的使用。"))
C.append(para("Vite 作为新一代前端构建工具，利用浏览器原生 ES Module 支持实现了极速的冷启动和热模块替换（HMR），大幅提升了开发体验。Element Plus 是基于 Vue 3 的桌面端组件库，提供了丰富的 UI 组件，如表格、表单、对话框、导航菜单等，支持暗色模式切换，与本系统面向开发者的定位高度契合。"))
C.append(para("在状态管理方面，系统采用 Pinia 替代传统的 Vuex。Pinia 提供了更简洁的 API、完整的 TypeScript 类型推导以及模块化设计。本系统定义了四个 Pinia Store：用户信息 Store（管理登录状态、Token 和用户资料）、通知 Store（管理未读通知数量）、草稿 Store（管理当前编辑帖子的草稿状态）和应用 Store（管理侧边栏折叠等 UI 状态）。"))
C.append(para("在 HTTP 通信方面，系统采用 ofetch 替代传统的 Axios。ofetch 是基于原生 Fetch API 的轻量级 HTTP 客户端，打包体积更小，API 设计更加简洁。通过在 ofetch 实例上配置请求拦截器和响应拦截器，系统实现了 Token 自动注入、401 状态码自动跳转登录页以及统一错误处理等功能。"))
C.append(para("在 Markdown 处理方面，系统采用 markdown-it 作为 Markdown 解析引擎，支持代码语法高亮（highlight.js）、表格、任务列表等扩展语法。为避免 XSS 攻击，所有用户生成内容在渲染前均经过 DOMPurify 清洗，去除潜在的恶意脚本。MdEditor 编辑器组件支持实时预览、快捷键操作和工具栏辅助，为用户提供流畅的编辑体验。"))

C.append(heading("2.2 后端技术栈", 2))
C.append(para("系统后端基于 Spring Boot 3.5 框架构建。Spring Boot 是 Java 生态中最流行的微服务开发框架，通过自动配置和起步依赖（Starter）显著简化了 Spring 应用的搭建和开发过程。本系统利用 Spring Boot 的内嵌 Tomcat 容器实现独立运行，无需额外部署应用服务器。"))
C.append(para("在数据持久化方面，系统采用 MyBatis-Plus 3.5.9 作为 ORM 框架。MyBatis-Plus 在 MyBatis 的基础上提供了强大的增强功能，包括自动生成 CRUD 操作、分页插件、逻辑删除、乐观锁和动态表名等。系统利用其内置的雪花算法（Snowflake）生成全局唯一的分布式主键 ID，解决了分布式系统中的 ID 冲突问题。同时，通过 Jackson 序列化配置将 Long 类型 ID 转换为字符串，避免了 JavaScript 中大数精度丢失的问题。"))
C.append(para("在安全认证方面，系统采用 Spring Security 结合 JWT（JSON Web Token）实现无状态认证授权。Spring Security 提供了完整的认证（Authentication）和授权（Authorization）框架，通过过滤器链对请求进行拦截和校验。JWT 令牌包含用户 ID、用户名和角色信息，设置 24 小时有效期，通过 Authorization 请求头传递。用户密码采用 BCrypt 算法进行不可逆哈希存储（cost = 10），确保即使数据库泄露也不会暴露用户明文密码。"))
C.append(para("在文件存储方面，系统采用 dromara/x-file-storage 框架，该框架提供了统一的文件上传、下载和管理接口，支持本地存储和多种云存储平台，降低了文件管理的开发复杂度。系统对上传文件进行白名单校验（仅允许 JPG、PNG、GIF、WebP 格式），并限制文件大小不超过 5MB。"))
C.append(para("在 API 文档方面，系统集成 Knife4j（基于 springdoc-openapi），自动生成 OpenAPI 3.0 规范的接口文档，提供可视化的 Swagger UI 界面，方便前后端协作开发和接口调试。"))

C.append(heading("2.3 数据存储技术", 2))
C.append(para("系统采用 MySQL 8.0 作为关系型数据库。MySQL 是全球最流行的开源关系型数据库管理系统，具有性能优异、稳定性高、生态完善等优点。本系统所有数据表均采用 InnoDB 存储引擎，支持事务、行级锁和外键约束。字符集统一设置为 utf8mb4，排序规则为 utf8mb4_unicode_ci，确保对 Emoji 等特殊字符的正确存储。关键关联表之间定义了外键约束，部分外键设置了 ON DELETE CASCADE 级联删除规则以维护数据的参照完整性。"))
C.append(para("系统采用 Redis 7 作为高性能缓存中间件。Redis 是开源的键值对内存数据库，具有极高的读写性能和丰富的数据结构支持。在本系统中，Redis 主要用于以下场景：缓存热点帖子和用户画像数据（TTL 30 分钟），加速推荐算法的在线查询；存储用户 Token 黑名单，支持登出功能；以及作为定时任务的分布式锁，防止多实例环境下的重复执行。"))
C.append(para("数据库设计遵循第三范式（3NF），共定义了 18 张数据表，涵盖用户、帖子、评论、分类、通知、AI 记录、推荐数据、草稿、文件记录等核心业务实体。所有表均使用 BIGINT 类型作为主键，通过雪花算法生成全局唯一 ID。时间相关字段（create_time、update_time）由 MyBatis-Plus 的自动填充机制在插入和更新时自动维护。"))

C.append(heading("2.4 AI 集成技术", 2))
C.append(para("系统集成了大语言模型（LLM）API，为帖子阅读提供 AI 摘要和智能问答功能。AI 模块采用可配置的接口设计，支持 OpenAI API 和国内大模型接口的灵活切换，通过 application.yml 配置文件中的环境变量指定 API 地址、密钥和模型名称。"))
C.append(para("AI 摘要和问答功能的实现遵循用户数据隔离原则。每个用户的 AI 摘要和问答记录通过 (user_id, post_id) 唯一约束独立存储，确保用户之间的数据互不可见，即使管理员也无权访问其他用户的 AI 互动记录。在安全性方面，系统在向 LLM 发送请求时进行 Prompt 注入防护，要求模型仅基于帖子内容进行回答，避免被恶意提示词操纵。同时，AI 调用设置了 2 次重试机制和 10 秒超时限制，确保服务的稳定性和可靠性。"))

C.append(heading("2.5 容器化部署", 2))
C.append(para("系统采用 Docker Compose 实现多服务的一键编排部署。docker-compose.yml 配置文件定义了三个服务容器：MySQL 8.0 数据库容器（端口 3306）、Redis 7 Alpine 缓存容器（端口 6379）和 Spring Boot 应用容器（端口 8080）。应用容器通过 depends_on 配置项设置了对 MySQL 和 Redis 容器的依赖关系，并配合健康检查机制确保依赖服务完全就绪后再启动应用。"))
C.append(para("所有敏感配置（数据库密码、Redis 密码、JWT 密钥、AI API 密钥等）均通过环境变量注入，不硬编码在配置文件或代码中，遵循安全开发的最佳实践。前端应用的开发环境通过 Vite 的代理配置将 /api 请求转发到本地后端服务，生产环境则由 Nginx 或后端服务直接托管前端静态资源。"))

# Save
with open(PROJECT_FILE, "w", encoding="utf-8") as f:
    json.dump(project, f, ensure_ascii=False, indent=2)

print("Chapters 1-2 + Abstracts written successfully.")
print(f"Total content items: {len(project['content'])}")
