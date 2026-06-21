# 修复：UserServiceImpl.getUserPosts() 未过滤可见性导致私密帖子泄露

| 属性 | 值 |
|------|-----|
| 分支 | `fix/user-posts-visibility` |
| 日期 | 2026-06-21 |
| 类型 | 安全修复 |
| Issue | [#16](https://github.com/my123m/TechHubBBS/issues/16) |
| 影响范围 | 后端 UserService.java, UserServiceImpl.java, UserController.java, UserControllerTest, VisibilityMatrixIntegrationTest |

## 根因分析

### 可见性过滤缺失

`UserServiceImpl.getUserPosts()` 方法按作者 ID 分页查询帖子时，仅过滤了 `deleted=0`，未调用 `PostVisibilityService.isVisible()` 进行可见性过滤。`SecurityConfig.java:60` 将 `GET /api/v1/users/{id}/posts` 设为 `permitAll()`，导致任意用户（包括未登录游客）可查看目标用户的全部帖子，无视帖子四级可见权限设置（公开/登录可见/粉丝可见/私密）。

对比 `PostServiceImpl.listPosts()` 已正确使用 `postVisibilityService.isVisible()` 过滤，两者行为不一致是此漏洞根源。

## 修复方案

### 服务层修复

将 `getUserPosts()` 签名与 `PostService.listPosts(query, currentUserId)` 对齐，在 DB 查询后增加可见性过滤：

1. `UserService` 接口 `getUserPosts` 增加 `Long currentUserId` 参数
2. `UserServiceImpl` 注入 `PostVisibilityService`
3. `UserServiceImpl.getUserPosts()` 在查询后对每条记录调用 `postVisibilityService.isVisible(post, currentUserId, isLoggedIn, isAdmin)` 过滤
4. `isAdmin` 通过 `SecurityUtils.getCurrentRole()` 判断（与 `PostServiceImpl` 一致）

### Controller 层

`UserController.getUserPosts()` 使用 `SecurityUtils.getCurrentUserId()` 获取当前用户 ID 并传入服务层。

### 测试

- `UserControllerTest` mock 签名更新为三参数版本
- `VisibilityMatrixIntegrationTest` 新增 `UserPostsListVisibility` 嵌套类，覆盖：
  - 游客 → 仅公开帖 (1 条)
  - 陌生人 → 公开 + 登录可见 (2 条)
  - 粉丝 → 公开 + 登录可见 + 粉丝可见 (3 条)
  - 作者 → 全部帖子含私密 (4 条)
  - 管理员 → 权限豁免全部可见 (4 条)

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../service/UserService.java` — `getUserPosts` 签名增加 `Long currentUserId` |
| 修改 | `backend/.../service/impl/UserServiceImpl.java` — 注入 `PostVisibilityService`；`getUserPosts()` 增加可见性过滤；新增 `isAdmin()` 辅助方法 |
| 修改 | `backend/.../controller/UserController.java` — `getUserPosts()` 传入 `SecurityUtils.getCurrentUserId()` |
| 修改 | `backend/.../controller/UserControllerTest.java` — mock 签名更新为三参数 |
| 修改 | `backend/.../integration/VisibilityMatrixIntegrationTest.java` — 新增 `UserPostsListVisibility`（5 个测试）|
| 新增 | `docs/features/user-posts-visibility-fix.md` |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test -Dtest="UserControllerTest"` — 8/9 通过（1 个预存失败：`updateProfile_Success` 断言消息不匹配，与本次改动无关）
- `mvn test -Dtest="VisibilityMatrixIntegrationTest"` — 26 个测试全部通过（含新增 5 个）

## 已知限制

- 分页 `total` 基于 DB 查询结果，过滤后 `records` 数量可能小于 `total`（与 `PostServiceImpl.listPosts()` 行为一致，属于应用层过滤的固有现象）
- `PostListQuery.@Max(50)` 在 `UserController.getUserPosts()` 中未生效（缺少 `@Valid`），但不在本次修复范围内
