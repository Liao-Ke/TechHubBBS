# 修复：Markdown 编辑器窗口随内容变宽

| 属性 | 值 |
|------|-----|
| 分支 | `fix/md-editor-width` |
| 日期 | 2026-06-19 |
| 类型 | 修复 |
| 影响范围 | 前端 MdEditor.vue · markdown.scss |
| 关联 Issue | [#8](https://github.com/my123m/TechHubBBS/issues/8) |

## 修复的缺陷

Markdown 编辑器在输入长 URL、长代码块或无空格长文本时，编辑器整体宽度超出页面容器，左右出现水平滚动条，破坏页面布局。

**根因**：flex 容器未裁剪溢出、textarea 缺少长词换行、预览区仅限制纵向滚动。

## 修复内容

### MdEditor.vue

| 选择器 | 变更 |
|--------|------|
| `.md-editor` | 新增 `width: 100%` — 确保填满父容器宽度 |
| `.md-editor__panes` | 新增 `overflow: hidden` — 防止子元素撑开 flex 容器 |
| `.md-editor__edit` | 新增 `overflow-x: auto` — 内容溢出时在编辑区内部水平滚动 |
| `:deep(.el-textarea__inner)` | 新增 `word-break: break-all; overflow-wrap: break-word; overflow-x: hidden` — 强制长词换行 |
| `.md-editor__preview` | `overflow-y: auto` → `overflow: auto` — 覆盖双轴滚动 |

### markdown.scss

| 选择器 | 变更 |
|--------|------|
| `.markdown-body` | `word-wrap: break-word` → `overflow-wrap: break-word`，追加 `word-break: break-word` |

## 验证方式

- `pnpm build` 前端构建通过
- 手动验证：访问 `/posts/create`，在 Markdown 编辑器中粘贴长 URL / 无空格长文本 / 多行代码块，确认编辑器宽度不再变化，溢出内容在编辑区和预览区内部显示滚动条

## 已知限制

- `word-break: break-all` 在编辑区可能将英文单词从中截断，影响代码编辑体验，但对中文输入场景无影响
