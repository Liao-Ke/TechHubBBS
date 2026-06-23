# 修复：移除 JWT 密钥硬编码默认回退，缺失 JWT_SECRET 时 fail-fast 启动

| 属性 | 值 |
|------|-----|
| 分支 | `fix/jwt-secret-no-default` |
| 日期 | 2026-06-23 |
| 类型 | 安全修复（Critical） |
| Issue | [#61](https://github.com/my123m/TechHubBBS/issues/61) |
| 影响范围 | 后端 `application.yml`、`JwtTokenProvider.java`、`backend/.env.example`；文档 `README.md`、`TechHub 后端开发文档.md`、`TechHub 技术方案文档.md` |

## 根因分析

`application.yml` 与 `JwtTokenProvider` 构造器对 `jwt.secret` 占位符各保留了一个**公开的硬编码默认回退值**：

- `application.yml:74`：`${JWT_SECRET:TechHubSecretKeyForJWTTokenGeneration...}`（明文回退）
- `JwtTokenProvider.java:25`：`@Value("${jwt.secret:dGVzdC1...}")`（Base64 编码回退）

部署时若未通过环境变量注入 `JWT_SECRET`，应用使用这两个公开密钥签发/校验 JWT。攻击者可直接从源码仓库取得密钥，自行签发含任意 `sub`、`role`（含 `ADMIN`）的令牌，携带 `Authorization: Bearer <token>` 即可绕过 Spring Security 全部角色鉴权，访问/操作管理员接口、伪造任意用户身份。

`JwtTokenProvider` 第 27 行对 secret 执行 `Decoders.BASE64.decode(secret)` 后作为 HMAC-SHA 密钥，两个默认值均能正常解码为有效密钥字节，因此应用可无报错启动，漏洞无可见异常。

违反 `AGENTS.md §8`「绝不硬编码密钥 / JWT Secret >=256 bits」零容忍安全条款。

## 修复方案

彻底移除两处默认回退，让缺失 `JWT_SECRET` 时 Spring 启动解析占位符即抛 `IllegalArgumentException: Could not resolve placeholder 'jwt.secret'`，达到「拒绝启动 + 明确报错」。密钥仅能通过环境变量（或外部密钥管理服务）注入。

具体改动：

| 文件 | 改动 |
|------|------|
| `backend/src/main/resources/application.yml:74` | `secret: ${JWT_SECRET:TechHubSecretKey...}` → `secret: ${JWT_SECRET}`（去回退），并补充安全说明注释 |
| `backend/src/main/java/com/techhub/security/JwtTokenProvider.java:25` | `@Value("${jwt.secret:dGVzdC1...}")` → `@Value("${jwt.secret}")`（去回退） |
| `backend/.env.example` | 新增 `JWT_SECRET` 必填项说明 + 本地示例值（与测试 profile 同一 base64 串，仅供本地调试） |
| `README.md` | 环境变量表 `JWT_SECRET` 改为「无默认值（必填）」并附生成/本地配置说明；启动步骤加入 `.env` 准备；安全规范补「密钥绝不硬编码」 |
| `docs/TechHub 后端开发文档.md` | §7.1 代码片段改为无默认 `@Value`；§11.4 `docker run` 示例补 `-e JWT_SECRET=...` 必填项 |
| `docs/TechHub 技术方案文档.md` | §10 风险矩阵「JWT Secret 泄露」应对措施补「配置与代码均无默认回退，缺失即 fail-fast 拒绝启动」 |

## 行为变化

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| 生产部署未配置 `JWT_SECRET` | 以公开默认密钥**静默启动**，可被伪造任意身份 | 启动期抛 `IllegalArgumentException`，**拒绝启动** |
| 本地 `mvn spring-boot:run` | 直接可跑 | 需先 `cp .env.example .env`（spring-dotenv 自动加载），否则拒绝启动 |
| 测试 `mvn test` | 通过 | 通过（`test/resources/application-test.yml` 已显式提供 `jwt.secret`，不受默认值移除影响） |
| docker compose `--profile full` | 启动 | 不传 `JWT_SECRET` 即拒绝启动（compose 透传 `${JWT_SECRET:-}` 为空，占位符无法解析） |

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/resources/application.yml` |
| 修改 | `backend/src/main/java/com/techhub/security/JwtTokenProvider.java` |
| 修改 | `backend/.env.example` |
| 修改 | `README.md` |
| 修改 | `docs/TechHub 后端开发文档.md` |
| 修改 | `docs/TechHub 技术方案文档.md` |
| 新增 | `docs/features/jwt-secret-no-default.md`（本文件） |

## 验证方式

- `mvn -q -pl backend compile` — 编译通过
- `mvn -q -pl backend test` — 全部测试通过（含 354 集成测试），无新增失败
- 手动 fail-fast：临时清空 `backend/.env` 中的 `JWT_SECRET` 后 `mvn spring-boot:run`，确认启动即抛 `Could not resolve placeholder 'jwt.secret'` 并退出；恢复 `.env` 后正常启动
- 构建检查：`pnpm` 前端不涉及本次改动，无需构建

## 已知限制

- 本地开发 DX 略降：必须先配置 `backend/.env`。已通过 `spring-dotenv`（`backend/pom.xml` 既有依赖）自动加载缓解，且 `.env` 已被 gitignore。
- 未接入外部密钥管理服务（Vault/KMS）。当前密钥仍以环境变量形式注入；后续可扩展为读取 KMS 解析后的密钥 ID（已在该 yml 注释中标注升级路径，标记为 `ponytail:`）。
- 未实现密钥轮换机制（双密钥并行窗口）。属后续运维增强，不在本 issue 范围。