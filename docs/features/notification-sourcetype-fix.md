# notification-sourcetype-fix — 通知 sourceType 与评论跳转修复

## 修改范围

- **Issue**: [#18](https://github.com/my123m/TechHubBBS/issues/18)
- **分支**: `fix/notification-comment-sourcetype`
- **影响模块**: 后端通知服务、前端通知页面、前端帖子详情页

## 问题描述

`NotificationServiceImpl.toVO()` 通过 `type→sourceType` 的 switch 推导来源类型，但 `LIKE` 类型统一映射为 `sourceType="post"`。评论点赞通知的 `sourceId` 是评论 ID，前端用评论 ID 当帖子 ID 跳转 → 404。

**同源问题一并修复**：REPLY（回复评论）和 DIVINE（神评）通知同样存在 `sourceId=commentId` 但无 parentId 的问题。

## 解决方案

`notification` 表新增两列，显式存储来源类型与上级帖子 ID：

### 数据库

```sql
-- notification 表新增
ALTER TABLE `notification`
    ADD COLUMN `source_type` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '来源类型 POST/COMMENT/USER' AFTER `source_id`,
    ADD COLUMN `parent_id`   BIGINT      DEFAULT NULL               COMMENT '上级帖子ID（来源为评论时记录所属帖子ID）' AFTER `source_type`;
```

### 后端

| 文件 | 改动 |
|---|---|
| `Notification.java` | 新增 `sourceType`, `parentId` 字段 |
| `NotificationVO.java` | 新增 `parentId`（String，雪花 ID 防 JS 精度丢失） |
| `NotificationService.java` | `create()` 签名加 `sourceType`、`parentId` 参数 |
| `NotificationServiceImpl.java` | `create()` 写入新字段；`toVO()` 用存储值输出大写 + parentId，移除 type→sourceType switch |
| `CommentServiceImpl.java` | REPLY → `"COMMENT"`, `postId` |
| `InteractionServiceImpl.java` | likePost→`"POST"/null`，likeComment→`"COMMENT"/comment.getPostId()` |
| `DivineCommentServiceImpl.java` | DIVINE → `"COMMENT"/comment.getPostId()` |
| `FollowServiceImpl.java` | FOLLOW → `"USER"/null` |

### 前端

| 文件 | 改动 |
|---|---|
| `api/types/notification.ts` | `NotificationVO` 新增 `parentId?: string \| null` |
| `NotificationPage.vue` | COMMENT 分支改为 `router.push(`/posts/${parentId}#comment-${sourceId}`)` |
| `PostDetailPage.vue` | 评论项加 `:id="`comment-${id}`"`，监听 `route.hash` 滚动定位（带重试） |

## 验证方式

| 方式 | 命令 |
|---|---|
| 后端编译 | `mvn compile -pl backend` |
| 后端单元测试 | `mvn test -Punit-tests -pl backend` |
| 前端类型检查 | `pnpm type-check` |
| 前端 Lint | `pnpm lint` |
| 前端测试 | `pnpm test:unit` |
| 手动验证 | 点赞评论 → 通知 sourceType=COMMENT, parentId=帖子ID → 点击跳转到帖子并滚动到评论 |

## 已知限制

- 已部署数据库需手动执行上述 `ALTER TABLE` 语句
- 评论锚点滚动仅在第一页评论内生效；被点赞评论若在第二页，滚动将失败（父帖子页面默认加载第一页评论）
- `SYSTEM` 类型通知无调用方，`sourceType` 默认为 `"UNKNOWN"`，前端不会跳转（符合预期）
