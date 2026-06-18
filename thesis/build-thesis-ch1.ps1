$cli = "C:\Users\29640\AppData\Local\Packages\PythonSoftwareFoundation.Python.3.13_qbz5n2kfra8p0\LocalCache\local-packages\Python313\Scripts\cli-anything-wps.exe"
$project = "E:\User\Desktop\TechHubBBS\thesis\thesis-project.json"

# Remove placeholder heading
& $cli --project $project --json writer remove 0
Write-Output "Removed placeholder."

# Title
& $cli --project $project --json writer add-heading -t "TechHub 技术社区论坛的设计与实现" -l 1
& $cli --project $project --json writer add-paragraph -t "" --font-size "12pt"
& $cli --project $project --json writer add-paragraph -t "基于 Spring Boot + Vue 3 的前后端分离技术社区平台" --font-size "14pt" --alignment center
& $cli --project $project --json writer add-paragraph -t "" --font-size "12pt"

Write-Output "Title done."

# Chinese Abstract
& $cli --project $project --json writer add-page-break
& $cli --project $project --json writer add-heading -t "摘要" -l 1

$abs1 = "随着互联网技术的快速发展和开发者社区的蓬勃兴起，技术论坛作为知识分享与交流的重要平台，在软件开发领域扮演着越来越重要的角色。本文设计并实现了一个名为 TechHub 的前后端分离技术社区论坛系统，旨在为开发者提供一个集内容发布、互动交流、AI 辅助阅读与个性化推荐于一体的综合性技术交流平台。"
& $cli --project $project --json writer add-paragraph -t $abs1 --font-size "12pt" --alignment justify

$abs2 = "系统后端采用 Spring Boot 3.5 框架，结合 MyBatis-Plus ORM 框架实现数据持久化，使用 Spring Security 与 JWT 实现无状态认证授权，通过 Redis 缓存提升系统性能。前端采用 Vue 3 + TypeScript 技术栈，使用 Vite 构建工具、Element Plus 组件库和 Pinia 状态管理，构建了响应式用户界面并支持暗色模式。系统使用 MySQL 8.0 作为关系型数据库，Redis 7 作为缓存中间件，并通过 Docker Compose 实现一键部署。"
& $cli --project $project --json writer add-paragraph -t $abs2 --font-size "12pt" --alignment justify

$abs3 = "系统实现了用户注册登录、帖子发布与浏览、评论互动、关注收藏、实时通知等基础社区功能。在此基础上，创新性地实现了四大特色功能：四级帖子可见权限控制，基于双重维度的神评社区治理机制，30 秒间隔的草稿自动保存与恢复功能，以及用户数据隔离的 AI 私密摘要与问答系统。此外，系统还集成了基于 Jieba 分词与 TF-IDF 的内容推荐算法，以及功能完善的管理后台。"
& $cli --project $project --json writer add-paragraph -t $abs3 --font-size "12pt" --alignment justify

$abs4 = "经过全面的功能测试与性能验证，系统运行稳定，各项功能指标均达到预期设计目标，为技术社区类 Web 应用的开发提供了完整的工程实践参考。"
& $cli --project $project --json writer add-paragraph -t $abs4 --font-size "12pt" --alignment justify

& $cli --project $project --json writer add-paragraph -t "" --font-size "12pt"
& $cli --project $project --json writer add-paragraph -t "关键词：技术社区；Spring Boot；Vue.js；前后端分离；AI 辅助阅读；推荐系统" --font-size "12pt" --bold

Write-Output "Chinese abstract done."
