# 修复：刷新后头像加载失败 & 个人主页 500

| 属性 | 值 |
|------|-----|
| 分支 | `fix/guard-userinfo-restore` |
| 日期 | 2026-06-19 |
| 类型 | 修复 |
| 影响范围 | 前端 router guards.ts |

## 修复的缺陷

### D0 — 公网路由刷新后 userInfo 未恢复

`frontend/src/router/guards.ts` 中，公网路由（首页 `/`、版块页、帖子详情页等）的早期返回（`return next()`）位于 `fetchUserInfo()` 数据恢复逻辑之前。刷新后首次导航到公网路由时，`userInfo` 从未加载，导致：

1. 头像 `src` 为 `undefined` → 兜底图标
2. 个人主页链接变为 `/users/undefined` → 后端 Long 解析异常 → 500

**修复**：将 `fetchUserInfo()` 数据恢复逻辑挪到公网路由早期返回之前，确保任何路由（包括公网路由）导航时都能先恢复已登录用户的 `userInfo`。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `frontend/src/router/guards.ts` — 调整 `fetchUserInfo` 与公网路由检查的顺序 |

## 验证方式

- `pnpm build` 前端构建通过（Vite build 成功，tsc 错误均为预存问题，与本次改动无关）

## 已知限制

- 无
