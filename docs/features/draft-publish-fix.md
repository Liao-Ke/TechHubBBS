# 修复：发布帖子后内容错误出现在草稿箱

| 属性 | 值 |
|------|-----|
| 分支 | `fix/draft-publish-logic` |
| 日期 | 2026-06-19 |
| 类型 | 修复 |
| Issue | [#6](https://github.com/my123m/TechHubBBS/issues/6) |
| 影响范围 | 前端 useDraft.ts / PostCreatePage.vue / 后端 DraftController.java |

## 根因分析

发布帖子与草稿自动保存之间存在 **3 个关联缺陷**，组合导致发布成功的帖子内容以草稿形式残留：

### B1 — 发布中自动保存未禁用（核心）
`useDraft` 的 2s 防抖定时器在 `handleSubmit()` 执行期间可能正好触发，导致 `saveDraft()` 与 `postApi.create()` 并发执行。即使发布成功，`stopAutoSave()` 在组件卸载时检测到 `isDirty = true`（因 `saveDraft()` 从未完成），触发最终保存把已发布的帖子内容又存为草稿。

**时序**：
1. 用户输入 → `isDirty = true`，启动 2s 防抖
2. 用户在 2s 内点「发布帖子」
3. `postApi.create()` 成功，`discardDraft()` 因 `currentDraftId` 为 null 直接返回
4. `router.push()` 导航离开 → 组件 unmount → `stopAutoSave()` → `isDirty` 仍为 `true` → **调用 `saveDraft()` 创建草稿**

### B2 — `checkExistingDraft()` 双重解包
`checkDraft()` 已返回 `PostDraft`（API 响应的 `.data` 部分），但 `checkExistingDraft()` 又对其取 `.data`，导致 `draft?.id` 永久为 `undefined`，草稿恢复对话框从不弹出。

### B3 — 发布成功后 `isDirty` 未重置
即使 B1 的竞态被规避，`handleSubmit()` 成功后没有将 `isDirty` 设为 `false`，导致 `stopAutoSave()` 仍可能触发多余保存。

## 修复方案

| 缺陷 | 文件 | 修复 |
|------|------|------|
| B1 | `useDraft.ts` | 新增 `disabled: Ref<boolean>` 参数。`startAutoSave()` 的 watch 回调、debounce 回调、interval 回调、`stopAutoSave()` 的最终保存均检查 `disabled?.value`，为 true 时跳过 |
| B1 | `PostCreatePage.vue` | 传入 `submitting` 作为 `disabled` 标志：`useDraft(undefined, submitting)` |
| B2 | `PostCreatePage.vue` | `checkExistingDraft()` 直接使用 `checkDraft()` 返回值：`const draft = await checkDraft()` 替代原双重解包 |
| B3 | `PostCreatePage.vue` | `handleSubmit()` 成功后添加 `isDirty.value = false; draftData.value = {}`，防止 unmount 时残留保存 |

## 附带修复

| 缺陷 | 文件 | 修复 |
|------|------|------|
| B4 | `DraftController.java` | `@RequestParam Long postId` → `@RequestParam(required = false) Long postId`，使新建帖子也能正常检测草稿 |

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/.../controller/DraftController.java` — postId 参数改为可选 |
| 修改 | `frontend/src/composables/useDraft.ts` — 新增 `disabled` 参数，所有保存路径检查该标志 |
| 修改 | `frontend/src/pages/post/PostCreatePage.vue` — 传入 `submitting`、修复双重解包、发布后重置 isDirty |

## 验证方式

- `mvn compile` 后端编译通过
- `pnpm build` 前端构建通过
- 启动应用后，登录 → 访问 `/posts/create` → 填写标题/内容/版块 → 点击「发布帖子」→ 确认跳转到帖子详情页 → 访问 `/drafts` 确认无残留草稿

## 已知限制

- 若 `postApi.create()` 本身失败（网络/服务端错误），auto-save 保护的草稿仍然有效，错误提示「发布失败，草稿已保存」是正确行为
