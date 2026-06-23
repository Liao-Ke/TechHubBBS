# 修复：计数器竞态条件与 updateById 全量字段覆盖问题

| 属性 | 值 |
|------|-----|
| 分支 | `fix/counter-race-and-updateById-overwrite` |
| 日期 | 2026-06-23 |
| 类型 | Bug 修复 |
| Issue | [#63](https://github.com/my123m/TechHubBBS/issues/63), [#77](https://github.com/my123m/TechHubBBS/issues/77) |
| 影响范围 | 后端 InteractionServiceImpl, CommentServiceImpl, DivineCommentServiceImpl, PostServiceImpl, DivineCommentScheduler |

## 根因分析

### Issue #63：计数器读-改-写竞态条件

帖子/评论的点赞数（likeCount）、帖子评论数（commentCount）、评论推荐数（recommendCount）在多处使用「读实体 → 内存加减 → updateById」模式更新，并发下存在竞态条件导致计数丢失（lost update）。

并发场景：
```
T1: read likeCount=5  →  T1: write likeCount=6
T2: read likeCount=5  →                →  T2: write likeCount=6  (应为7)
```

项目此前已为浏览量（viewCount）修复同类问题（#21 → #53 原子 SQL `view_count = view_count + 1`），但其余计数器均未同步修复。

### Issue #77：updateById 写回全量字段覆盖并发变更

`PostServiceImpl.updatePost()` 先 `selectById` 读取帖子全量字段到内存，仅修改 title/content/visibility 三个字段后，调用 `postMapper.updateById(post)` 将**所有非 null 字段写回数据库**。由于从 DB 加载的实体所有字段均非 null，`updateById` 会把 read 时刻的 viewCount、likeCount、commentCount、divineCommentCount 等计数值一并写回，覆盖在此期间其他线程通过原子 SQL 完成的计数递增。

同类模式存在于：
- `DivineCommentServiceImpl.promoteToDivine()` / `demoteFromDivine()` — `postMapper.updateById(post)` 覆盖并发计数
- `DivineCommentScheduler.promoteComment()` / `demoteComment()` — 同上

## 修复方案

### 1. 计数器递增/递减改用原子 SQL

使用 `LambdaUpdateWrapper.setSql()` 生成单条原子 UPDATE 语句，由数据库行锁保证原子性：

```java
// 帖子点赞数 +1
postMapper.update(null, new LambdaUpdateWrapper<Post>()
        .eq(Post::getId, postId)
        .setSql("like_count = like_count + 1"));

// 帖子评论数 -1（带下界保护）
postMapper.update(null, new LambdaUpdateWrapper<Post>()
        .eq(Post::getId, postId)
        .gt(Post::getCommentCount, 0)
        .setSql("comment_count = comment_count - 1"));
```

### 2. 非计数器字段更新改用 LambdaUpdateWrapper

对于 `updatePost`、`promoteToDivine`、`demoteFromDivine` 等需要更新非计数器字段的场景，使用 `LambdaUpdateWrapper` 仅 set 目标字段，避免 `updateById` 写回全量字段：

```java
// updatePost: 仅更新 title/content/visibility
LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<Post>()
        .eq(Post::getId, postId);
if (request.getTitle() != null) {
    wrapper.set(Post::getTitle, request.getTitle());
}
// ...
postMapper.update(null, wrapper);

// promoteToDivine: 仅更新 isDivine 和 divineTime
commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
        .eq(Comment::getId, comment.getId())
        .set(Comment::getIsDivine, 1)
        .set(Comment::getDivineTime, LocalDateTime.now()));
```

## 文件变更

| 操作 | 文件 | 变更说明 |
|------|------|----------|
| 修改 | `InteractionServiceImpl.java` | likePost/unlikePost/likeComment/unlikeComment 改用原子 SQL |
| 修改 | `CommentServiceImpl.java` | create/delete 中的 commentCount 改用原子 SQL |
| 修改 | `DivineCommentServiceImpl.java` | recommend/cancelRecommend 改用原子 SQL；promoteToDivine/demoteFromDivine 改用 LambdaUpdateWrapper |
| 修改 | `PostServiceImpl.java` | updatePost 改用 LambdaUpdateWrapper 仅更新目标字段 |
| 修改 | `DivineCommentScheduler.java` | promoteComment/demoteComment 改用 LambdaUpdateWrapper + 原子 SQL |
| 修改 | `PostServiceTest.java` | updatePost 测试 mock/verify 从 `updateById` 改为 `update(null, wrapper)` |
| 修改 | `InteractionServiceImplTest.java` | 点赞测试 mock/verify 从 `updateById` 改为 `update(null, wrapper)` |
| 修改 | `DivineCommentServiceTest.java` | 神评相关测试 mock/verify 从 `updateById` 改为 `update(null, wrapper)` |
| 修改 | `DivineCommentSchedulerTest.java` | 定时任务测试 mock/verify 从 `updateById` 改为 `update(null, wrapper)` |
| 新增 | `docs/features/counter-race-and-updatebyid-fix.md` | 功能记录 |

## 验证方式

- `mvn compile -q` — 编译通过
- `mvn test` — 所有单元测试通过
- 集成测试 `DivineCommentIntegrationTest` 验证端到端行为

## 已知限制

- `setSql` 直接写入 SQL 片段，表名/列名变更时需手动维护
- 原子 SQL 更新后内存中的实体对象不会自动刷新，调用方如需最新值需重新查询或手动同步
