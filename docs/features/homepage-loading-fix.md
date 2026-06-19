# 修复：主页（/）偶尔无法正常加载

| 属性 | 值 |
|------|-----|
| 分支 | `fix/homepage-infinite-scroll` |
| 日期 | 2026-06-20 |
| 类型 | 修复 |
| 影响范围 | `useInfiniteScroll.ts` · `HomePage.vue` · `NoticeCarousel.vue` |
| 关联 Issue | [#7](https://github.com/my123m/TechHubBBS/issues/7) |

## 修复的缺陷

### D1 — IntersectionObserver 未响应条件渲染的 sentinel（根因）
`useInfiniteScroll` 在 `onMounted()` 中创建 `IntersectionObserver`，但 sentinel 元素位于 `v-else` 块内（数据到达后才渲染）。挂载时 `sentinelRef.value` 为 null → observer 从未创建 → 无限滚动不工作、首次加载后无法加载更多。

**修复**：用 `watch(sentinelRef, ..., { flush: 'post' })` 替代 `onMounted`，当 sentinel 进入/离开 DOM 时响应式创建/销毁 observer。`onUnmounted` 中调用 `stopWatch()` 清理。

### D2 — 未登录用户触发无谓推荐 API 调用
`onMounted` 无条件调用 `recLoadMore()`，未登录用户收到 401 → 产生无意义的 `recError` 状态。

**修复**：`onMounted` 中增加 `userStore.isLoggedIn` 条件守卫。

### D3 — NoticeCarousel API 失败无错误处理
`fetchNotices` 缺少 `catch`，若 API 失败或超时，组件持续显示骨架屏。

**修复**：添加 `catch` 块，失败时静默清空 `notices`（公告为非关键 UI）。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `frontend/src/composables/useInfiniteScroll.ts` — onMounted → watch |
| 修改 | `frontend/src/pages/home/HomePage.vue` — 登录守卫 |
| 修改 | `frontend/src/components/notice/NoticeCarousel.vue` — 错误处理 |

## 验证方式

- `pnpm build` 前端构建通过（Vite 构建成功）
- 手动验证：访问 `/` → 推荐/热门正常加载 → 滚动触发加载更多 → 切换 Tab 正常 → 未登录时推荐 Tab 显示登录提示（无 API 调用）
