# 改进：CI 拆分单元测试和集成测试为并行 Job

| 属性 | 值 |
|------|-----|
| 分支 | `fix/ci-split-test-jobs` |
| 日期 | 2026-06-20 |
| 类型 | 改进 |
| 影响范围 | `.github/workflows/ci-backend.yml` |

## 背景

原 CI 将全部 300+ 测试（含集成测试）合并为一个 job 顺序执行。集成测试中的预存 Bug（如 `MissingFormatArgument`、外键约束、数据累积）可能导致整个流水线失败，阻塞 PR 合并。

## 改动

将原 `backend-test` 拆分为两个并行 job：

| Job | 命令 | 覆盖范围 |
|-----|------|----------|
| `unit-test` | `mvn test -Dtest='com.techhub.*,!com.techhub.integration.*'` | 所有非集成测试（~300+） |
| `integration-test` | `mvn test -Dtest='com.techhub.integration.*'` | 集成测试（~21） |

两个 job 在 CI 中并行运行，各自独立汇报成败。集成测试 job 失败不会阻塞单元测试或 PR 合并决策。

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `.github/workflows/ci-backend.yml` — 拆分 test job 为 unit-test + integration-test |

## 验证方式

- 推送到 `fix/ci-split-test-jobs` 后观察 GitHub Actions 运行状态
- `unit-test` job 应快速完成（~1-2 分钟）
- `integration-test` job 应独立完成
