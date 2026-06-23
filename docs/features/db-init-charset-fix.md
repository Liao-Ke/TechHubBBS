# 修复：数据库初始化会话字符集设为 utf8mb4,消除中文乱码

| 属性 | 值 |
|------|-----|
| 分支 | `fix/db-init-charset` |
| 日期 | 2026-06-22 |
| 类型 | Bug 修复 |
| 影响范围 | 后端数据库初始化脚本、docker-compose 配置 |

## 根因分析

### 现象

`docker-compose up -d` 启动 MySQL 后,后端接口返回的中文数据全部为乱码。

### 根因

MySQL 官方 Docker 镜像的 `docker-entrypoint.sh` 在首次启动时执行 `/docker-entrypoint-initdb.d/*.sql` 中的初始化脚本。执行时 `mysql` 客户端连接的会话字符集(`character_set_client` / `character_set_connection` / `character_set_results`)由客户端与服务端协商决定,**不依赖** `--character-set-server` 启动参数。

`--character-set-server=utf8mb4` 只设置**服务端**字符集,不影响客户端连接协商。当客户端未显式声明字符集时,部分镜像版本/连接场景下会话字符集可能回退为 `latin1`。

UTF-8 编码的 `schema.sql` / `data.sql` 中的中文(表注释、种子数据)被按 `latin1` 解码后写入 InnoDB(`utf8mb4` 列),字节流在「客户端编码 → 连接编码 → 列编码」的转换链中被错误重编码,从源头损坏。

之后 JDBC 连接即使配置了 `characterEncoding=UTF-8`,读出来的也是已经损坏的字节,表现为接口返回乱码。

### 转换链错误示意

```
data.sql (UTF-8 字节)
  → mysql 客户端按 latin1 解码 (错误)
  → 连接层 latin1 → utf8mb4 转换 (二次错误编码)
  → InnoDB utf8mb4 列存储 (已损坏的字节)
  → JDBC utf8mb4 读取 (读出损坏字节)
  → 接口返回乱码
```

## 修复方案

采用双重保险:SQL 脚本显式声明 + 客户端配置文件兜底。

### 1. 初始化 SQL 脚本头部加 `SET NAMES utf8mb4`

`SET NAMES utf8mb4` 同时设置三个会话变量,确保后续 DDL/DML 的中文按 utf8mb4 写入:

- `character_set_client = utf8mb4`
- `character_set_connection = utf8mb4`
- `character_set_results = utf8mb4`

这是最可靠、不依赖镜像版本和连接协商的方案。

### 2. 挂载客户端字符集配置文件

新增 `backend/mysql-conf/client-charset.cnf`,通过 docker volume 挂载到 `/etc/mysql/conf.d/client-charset.cnf`,确保 `mysql` 客户端默认使用 utf8mb4,即使 entrypoint 未显式传 `--default-character-set` 参数。

### 为什么需要双重保险

- `SET NAMES` 在 SQL 层生效,覆盖所有通过 `mysql` 客户端执行初始化脚本的路径
- 配置文件在客户端连接层生效,覆盖后续 `docker exec` 手动执行 SQL 等场景
- 两者互不冲突,共同保证任何路径下会话字符集都是 utf8mb4

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `backend/src/main/resources/db/schema.sql` — 头部加 `SET NAMES utf8mb4;` |
| 修改 | `backend/src/main/resources/db/data.sql` — 头部加 `SET NAMES utf8mb4;` |
| 新增 | `backend/mysql-conf/client-charset.cnf` — `[client]`/`[mysql]` 默认字符集 utf8mb4 |
| 修改 | `backend/docker-compose.yml` — mysql 服务 volumes 挂载 client-charset.cnf 到 `/etc/mysql/conf.d/` |
| 新增 | `docs/features/db-init-charset-fix.md` |

## 验证方式

### 1. 清理旧数据卷(关键)

旧数据卷中如果是乱码数据,改配置不会自动修复,必须重建:

```bash
cd backend
podman-compose down -v   # -v 删除数据卷
podman-compose up -d mysql redis
```

### 2. 等待 MySQL healthy 后检查字符集变量

```bash
podman exec techhub-mysql mysql -uroot -proot -e "SHOW VARIABLES LIKE 'character%';"
```

预期 `character_set_client`、`character_set_connection`、`character_set_results` 均为 `utf8mb4`。

### 3. 检查种子数据中文

```bash
podman exec techhub-mysql mysql -uroot -proot techhub -e "SELECT id, username, bio FROM user LIMIT 3;"
podman exec techhub-mysql mysql -uroot -proot techhub -e "SELECT id, name, description FROM category LIMIT 3;"
```

预期中文正常显示,无 `?` 或乱码。

### 4. 接口层验证

启动后端后调用:

| 接口 | 预期 |
|---|---|
| `GET /api/v1/categories` | 200,中文版块名正常 |
| `GET /api/v1/posts?size=2` | 200,中文标题/正文正常 |
| `GET /api/v1/posts/30` | 200,Markdown 正文中文正常 |
| `GET /api/v1/notices?categoryId=10` | 200,公告中文正常 |
| `GET /api/v1/notifications`(未登录) | 401,"未登录或登录已过期" 中文正常 |

### 实测结果

全部通过:字符集变量均为 utf8mb4,数据库种子数据中文正常,5 个接口响应中文正常。

## 已知限制

- **需要 `down -v` 重建数据卷**:已存在的乱码数据无法通过配置修改修复,必须删除数据卷重新初始化
- **仅覆盖本地 Docker 初始化场景**:生产环境若使用外部 MySQL 或其他部署方式,需另行确保会话字符集为 utf8mb4(通常 JDBC URL 的 `characterEncoding=UTF-8` 已足够,因为 JDBC 驱动会显式声明字符集)
- **配置文件路径绑定 MySQL 官方镜像**:挂载到 `/etc/mysql/conf.d/` 是 MySQL 官方镜像约定的配置目录,若更换为基础镜像(如 mariadb)需调整挂载路径
