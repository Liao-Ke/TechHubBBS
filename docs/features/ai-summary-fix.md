# 修复：帖子生成AI摘要功能端到端不可用

| 属性 | 值 |
|------|-----|
| 分支 | `fix/ai-summary-functionality` |
| 日期 | 2026-06-19 |
| 类型 | 修复 |
| 影响范围 | 后端 AiServiceImpl / AiClient / AiConfig / pom.xml · 前端 AiSummaryPanel / ai.ts |

## 修复的缺陷

### D0 — 缺少 commons-pool2 依赖（启动失败）
`x-file-storage` 隐式依赖 `org.apache.commons.pool2.impl.GenericObjectPoolConfig`，但 pom.xml 未声明。
**修复**：添加 `commons-pool2` 依赖。

### D1 — .env 不被 Spring Boot 加载（根因）
Spring Boot 不支持 `.env` 文件。pom.xml 缺少 dotenv 库。
**修复**：添加 `me.paulschwarz:spring-dotenv:4.0.0` 依赖。`backend/.env` 已配置即可正常注入 `${AI_API_KEY}` 等占位符。

### D2 — 前后端接口类型不匹配
后端 `POST /summary` 返回 `R<Void>`，前端声明 `R<AiSummaryResponse>` 并访问 `res.data.content`（始终 null）。
**修复**：前端 `ai.ts` 将 `generateSummary` 返回类型改为 `R<null>`，`AiSummaryPanel.vue` 改为 POST 触发 + 轮询 GET 模式。

### D3 — 前端缺少轮询机制
触发生成后不查询状态。
**修复**：`AiSummaryPanel.vue` 新增 `generateSummary` → POST 触发 → `setInterval` 轮询 GET（2s 间隔 / 30 次上限 / 60s 超时），根据 status 切换 UI 状态。`onBeforeUnmount` 清理定时器。

### D4 — @Async 自调用失效
`AiServiceImpl.generateSummary()` 内部 `this.executeSummaryGeneration()` 绕过 AOP 代理，异步变同步。
**修复**：提取 `AiSummaryAsyncExecutor` 独立组件，`@Async` 方法移至新组件，由 Spring 代理正确拦截。

### D5 — toSummaryResponse 未设 postId
`AiSummaryResponse.postId` 始终为 null。
**修复**：`toSummaryResponse` 添加 `resp.setPostId(summary.getPostId().toString())`。

### D6 — AiClient 入口不校验空配置
apiKey 为空时传 `Authorization: Bearer ` 导致难以诊断的认证错误。
**修复**：`callLlm` 开头添加 apiKey / apiUrl null-or-blank 检查并返回 null + 记录 error 日志。

## 文件变更

| 操作 | 文件 |
|------|------|
| 新增 | `backend/src/main/java/com/techhub/service/impl/AiSummaryAsyncExecutor.java` |
| 修改 | `backend/pom.xml` — 添加 spring-dotenv + commons-pool2 |
| 修改 | `backend/src/main/java/com/techhub/service/impl/AiServiceImpl.java` — 注入 asyncExecutor, 移除旧 @Async 方法, 修复 toSummaryResponse |
| 修改 | `backend/src/main/java/com/techhub/util/AiClient.java` — callLlm 入口空值检查 |
| 修改 | `frontend/src/api/modules/ai.ts` — generateSummary 返回类型 R\<null\> |
| 修改 | `frontend/src/components/ai/AiSummaryPanel.vue` — 轮询机制 |
| 修改 | `backend/src/test/java/com/techhub/service/impl/AiServiceImplTest.java` — @Mock asyncExecutor |

## 验证方式

- `mvn compile` 编译通过
- `mvn test -Dtest="AiServiceImplTest,AiConfigTest"` 单元测试通过
- `pnpm build` 前端构建通过
- 启动应用后，登录 → 打开帖子详情页 → 点击"生成 AI 总结" → 观察到"AI 正在总结..." → 轮询后显示摘要内容

## 已知限制

- 轮询在组件卸载时停止，跨路由导航后需重新触发
- 若 LLM 服务响应超过 60 秒，前端显示超时错误（可调大 POLL_MAX_ATTEMPTS）
