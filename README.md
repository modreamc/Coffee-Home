# 咖啡港湾 COFFEE HARBOR

游戏 / 动漫爱好者的轻量论坛：Spring Boot 3 + MyBatis-Plus + MySQL 后端，Vue 3 + Vite + Element Plus 前端。

```
forum-backend/   Spring Boot 3.2.5 / Java 21 / MyBatis-Plus / Spring Security + JWT
forum-frontend/  Vue 3 / Vite 5 / Pinia / Element Plus
docs/            接口文档
tools/           一次性运维脚本（初始化种子数据、检查库结构等）
deploy/          上线参考配置（nginx）
```

## 1. 快速开始

### 1.1 准备数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS forum DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
mysql -u root -p forum < forum-backend/sql/schema.sql
```

建表脚本是幂等的（全部 `CREATE TABLE IF NOT EXISTS`，不会删数据）。应用启动时还会由
`DatabaseUpgrader` 幂等补齐后续新增的列与表。

### 1.2 启动后端

```bash
cd forum-backend
mvn spring-boot:run
# 默认 http://localhost:8080
```

### 1.3 启动前端

```bash
cd forum-frontend
npm install
npm run dev
# 默认 http://localhost:3000，/api、/picture 由 Vite 代理到 8080
```

首次启动会自动创建管理员账号：

- 设置了 `ADMIN_PASSWORD` 时使用该密码；
- 未设置时生成随机密码，并**只打印一次**在启动日志里（搜索 `随机管理员密码`）；
- 仓库不内置任何默认密码或 JWT 密钥，公开部署时请务必通过环境变量注入。

### 1.4 灌入示例长文与配图（可选）

`tools/seed-data/*.txt` 里有 66 篇长文种子（36 篇单机/独立游戏 + 30 篇动漫），
`tools/SeedContent.java` 会按标题 upsert 到 `posts`，并从 Steam / Kitsu / Bing 图搜下载配图到 `picture/`：

```bash
APP_UPLOAD_DIR=forum-backend/picture \
java -cp "<mysql-connector-j.jar>" tools/SeedContent.java
```

- 可重复执行：已下载的图片会跳过，帖子按标题更新（不会重复插入），点赞/收藏/评论按固定种子重建；
- 同一个游戏的第二、三篇帖子会自动换用不同官方素材，避免列表里出现重复封面；
- 图片来自 Steam 商店官方图、Kitsu 动画视觉图与 Bing 图搜兜底，**仅用于本地演示**，商用请替换为已授权素材。

## 2. 配置

所有敏感配置都通过环境变量注入，仓库里不保存可用的真实密钥：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/forum?...` | 数据库连接串 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `0000` | 本地开发默认值，**生产必须覆盖** |
| `DB_POOL_SIZE` | `10` | Hikari 连接池大小 |
| `JWT_SECRET` | 空 | 未设置时启动生成临时随机密钥（重启后旧 Token 失效）；生产必须设置至少 32 字节随机串 |
| `JWT_EXPIRATION` | `86400000` | token 有效期（毫秒，默认 24h） |
| `APP_UPLOAD_DIR` | `./picture` | 图片物理目录 |
| `APP_CORS_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` | 允许的前端来源，逗号分隔；支持 `*` 通配（Vite 换端口不影响），生产建议写死域名 |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / 空 | 未设置密码时生成随机密码并打印到日志；**生产必须显式设置** |
| `ADMIN_AUTO_INIT` | `true` | 是否自动创建管理员 |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_WINDOW_SECONDS` / `LOGIN_LOCK_SECONDS` | 5 / 900 / 900 | 登录失败限流 |
| `LOG_LEVEL` | `INFO` | `com.forum` 日志级别 |

配置模板见 [`.env.example`](.env.example)（Spring 不会自动读取 `.env`，请在 IDE 运行配置或 shell 里设置）。

PowerShell 示例：

```powershell
$env:DB_PASSWORD="your-db-password"
$env:JWT_SECRET="a-random-secret-at-least-32-bytes-long"
$env:ADMIN_PASSWORD="a-strong-admin-password"
mvn spring-boot:run
```

### 图片目录放在哪？

`picture/` 属于**后端**——它是用户上传的运行时数据，不是前端源码：

- 上传走 `POST /api/images/upload`，由后端校验类型、生成文件名并落盘，URL 也由后端返回；
- 前端 `dist/` 是纯静态、可重复构建的产物，不应混入运行时写入的文件；
- 开发期由 Vite 把 `/picture` 代理到 8080；生产由 nginx 反代到后端，或直接指向同一目录；
- 部署时把 `APP_UPLOAD_DIR` 指到持久化卷（如 `/var/lib/forum/picture`），前端容器保持只读；
- 数据量/并发上来后，换成对象存储（OSS/S3）+ CDN，数据库只存 URL。

前端只放**构建期**资源：`src/assets`（参与打包、带 hash）和 `public/`（原样拷贝，如 favicon）。

## 3. 安全设计

- **认证**：JWT（HS 系列，密钥由 `JWT_SECRET` 提供）。过滤器每次请求都回查数据库，
  用户被删除或角色被降级后旧 token 立即失效；角色以数据库为准，不信任 token 里的 claim。
- **未登录/越权**：返回 401 / 403 的 JSON（`SecurityErrorHandler`），前端据此清会话并跳登录。
- **登录限流**：按 `IP|用户名` 维度统计失败次数，超限返回 429；用户不存在时也执行一次
  BCrypt 比对，避免账号枚举与时序侧信道。
- **越权访问**：`/api/admin/**` 仅 ADMIN；`/api/images/upload` 需登录（避免被当免费图床）。
- **文件上传**：只按文件头魔数识别 JPG / PNG / GIF / WEBP，扩展名由服务端生成，
  不信任客户端文件名（防 `x.html` 型同源 XSS 与路径穿越）。
- **富文本 XSS**：帖子正文入库前用 jsoup 白名单过滤（`HtmlSanitizer`），
  允许基础排版与图片/链接，移除 script/iframe/事件属性/`javascript:` 协议；前端渲染前再做一层
  DOMParser 兜底（覆盖历史脏数据）。
- **敏感字段**：管理端用户列表使用 `AdminUserView` DTO，绝不返回 `password` 哈希。
- **CORS**：白名单来源 + `allowCredentials`，不再使用 `*`。

## 4. 数据一致性

- 发帖 / 改帖 / 删帖 / 评论 / 点赞 / 收藏 / 签到 / 删用户等跨表写操作都加了 `@Transactional`。
- 计数（点赞、收藏、评论、话题帖子数）改为 SQL 原子自增（`GREATEST(x + delta, 0)`），
  并发下不会读-改-写丢更新；唯一键冲突（重复点赞）由 `DuplicateKeyException` 兜底。
- 删除帖子会级联清理评论（含楼中楼）、点赞、收藏、通知，并删除不再被引用的图片文件。
- 删除用户会级联清理其帖子、评论、点赞、收藏、签到与通知；不能删自己，也不能删最后一个管理员。
- 计数漂移可用 `POST /api/admin/stats/rebuild`（管理后台"重算计数"按钮）按真实记录重算。

## 5. 接口行为要点

- 分页参数统一收敛：`page >= 1`，`size` 上限 50（`Paging`）。
- 搜索 `GET /api/posts/search?keyword=&page=&size=` 返回分页结构（不再整表加载）。
- 评论 `GET /api/comments/{postId}`：按**主评论**分页，`list` 中同时带回这些主评论下的全部子孙回复，
  避免跨页回复丢失；`total` 为主评论数。
- 图片上传返回 `/picture/{uuid}.{ext}`，静态访问公开。
- 视频：帖子可附带最多 3 个视频链接（`videos` 字段），后端按平台白名单校验，并对 B站链接自动抓取标题/封面/时长
  （封面下载到 `picture/video-<BV号>.jpg`，`b23.tv` 短链由服务端解析），前端对 B站 / YouTube / 腾讯视频 / 优酷 使用官方 iframe 播放器；
  抖音/西瓜没有公开嵌入能力，降级为跳转卡片。老帖子只有链接时，详情页会按需补抓一次封面。
- 详细字段与错误码见 `docs/接口文档.md`。

## 6. 视觉主题（Lo-Fi Café）

前端是一套自绘的 Lo-Fi 咖啡馆主题，无图片素材依赖，全部由 CSS 绘制：

| 模式 | 氛围 | 主要元素 |
|---|---|---|
| 白天「午后」 | 奶油纸感 + 焦糖咖啡 + 斜射阳光 | 窗外的暖阳、窗帘、串灯、黑胶机、冒着热气的咖啡杯、纸胶带卡片 |
| 夜晚「雨夜」 | 深烘可可 + 琥珀灯光 + 窗外雨丝 | 月亮与雨、常亮的落地灯、浮尘光点、暗色纸张与便利贴评论 |

- 两种模式共用同一套布局、圆角与琥珀色高光，只在 `[data-theme]` 下替换"光线与材质"。
- 主题开关在右上角；也可用 `?theme=light` / `?theme=dark` 直接锁定主题（方便演示和截图）。
- 动效包括：页面切换的淡入推进、列表错峰进场、咖啡热气、串灯闪烁、雨丝、黑胶旋转、均衡器、卡片悬停微倾、按钮掠过式高光。
- 全局遵循 `prefers-reduced-motion`，系统开启"减少动效"时动画会自动关闭。

## 7. 构建与部署

```bash
# 后端可执行 jar
cd forum-backend && mvn clean package
java -jar target/forum-backend-1.0.0.jar

# 前端静态资源
cd forum-frontend && npm run build   # 产物在 dist/
```

前端与后端同源部署（推荐）参考 `deploy/nginx.conf`：`/api`、`/picture` 反代到 8080，
其余路径回退到 `index.html`（history 路由）。注意 `client_max_body_size` 要大于 10MB。

## 8. 测试

```bash
cd forum-backend && mvn test
```

覆盖：HTML 白名单过滤、JWT 签发/过期/篡改/换密钥、登录限流、分页收敛、标签序列化、等级边界。

## 9. 已知取舍

- 登录限流是单机内存实现，多实例部署需换成 Redis。
- 数据库升级仍由 `DatabaseUpgrader` 在启动时执行（单实例方案），正式环境建议迁移到 Flyway/Liquibase。
- 图片存本地磁盘，多实例/容器化部署需换成对象存储或共享卷。
- 全站暂未引入缓存层，热榜按 SQL 计算，数据量上来后建议加 Redis 缓存或物化热榜表。
