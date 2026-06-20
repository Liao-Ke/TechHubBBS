# 修复：DraftServiceImpl.getById() 缺少所有权检查 (IDOR)

| 属性 | 值 |
|------|-----|
| 分支 | `fix/draft-idor-getById` |
| 日期 | 2026-06-20 |
| 类型 | 安全修复 |
| Issue | [#15](https://github.com/my123m/TechHubBBS/issues/15) |
| 影响范围 | 后端 DraftServiceImpl.java |

## 根因分析

`DraftServiceImpl.getById(Long id)` 方法直接按 ID 查询并返回草稿，未校验当前登录用户是否拥有该草稿。任何登录用户只需知道草稿 ID 即可查看任意用户的草稿内容，属于典型的 **IDOR（Insecure Direct Object Reference）** 漏洞。

对比同文件中的 `deleteDraft()` 方法已正确执行所有权检查，两者行为不一致是此漏洞根源。

## 修复方案

在 `getById()` 方法中增加所有权检查，遵循与 `deleteDraft()` 完全一致的安全模式：

1. 调用 `SecurityUtils.getCurrentUserId()` 获取当前用户 ID
2. 未认证时抛出 `UNAUTHORIZED`
3. 按 ID 查询草稿，不存在时抛出 `NOT_FOUND`
4. 校验 `draft.getUserId().equals(userId)`，不匹配时抛出 `FORBIDDEN`（不区分"草稿不存在"和"无权查看"，防止 ID 枚举）

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../service/impl/DraftServiceImpl.java` — `getById()` 增加 userId 获取和所有权校验 |
| 修改 | `backend/.../controller/DraftControllerTest.java` — 新增 `getDraft_NotOwner_Forbidden` 测试 |

## 验证方式

- `mvn test -Dtest="com.techhub.controller.DraftControllerTest"` — 9 个测试全部通过
- 手动验证：账号 A 创建草稿 → 账号 B 无法通过 `GET /api/v1/drafts/{id}` 查看

## 已知限制

- 无。管理员如需查看用户草稿，后续可扩展角色豁免逻辑（当前所有非所有者均被拒绝）
