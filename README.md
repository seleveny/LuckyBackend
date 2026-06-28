# 幸运盒子后台 (LuckyBackend)

双色球彩票业务的后台管理系统，基于 Spring Boot 4.0.5 + WebFlux 响应式技术栈。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | |
| Spring Boot | 4.0.5 | |
| Spring Framework | 7.0.7 | |
| Spring Security | 7.0.5 | 安全认证 |
| Spring WebFlux | 响应式（非阻塞） | |
| R2DBC | 响应式 | MySQL 响应式数据库访问 |
| Flyway | - | 数据库版本迁移管理 |
| OpenSearch | 2.13.0 | 搜索引擎 |
| Redis | 7.2 | 缓存 / 会话管理 |
| MySQL | 8.4.0 | |
| Jackson | 3.1.1（tools.jackson） | JSON 序列化 |
| Maven | - | 构建工具 |

## 项目结构

```
src/main/java/com/yfqb/lucky/
├── LuckyBackendApplication.java      # 启动类
├── basic/
│   └── IResult.java                  # 统一响应封装
├── config/
│   ├── FlywayConfig.java             # Flyway 数据库迁移配置
│   ├── LotteryApiConfig.java         # 彩票 API 配置（pageSize、cron 等）
│   ├── OpenSearchConfig.java         # OpenSearch 客户端配置
│   ├── SchedulingConfig.java         # 定时任务配置（@EnableScheduling）
│   ├── SecurityConfig.java           # Spring Security 安全配置
│   ├── TransactionConfig.java        # 响应式事务管理器配置
│   └── WebClientConfig.java          # WebClient 配置（16MB 缓冲）
├── constant/
│   ├── CommonConstants.java          # 通用常量
│   └── LogContextConstants.java      # 日志上下文常量
├── controller/
│   ├── AuthController.java           # 认证控制器  (/auth)
│   ├── CommonController.java         # 通用控制器  (/common)
│   └── LotteryController.java        # 彩票控制器  (/lottery)
├── exception/
│   ├── BusinessException.java        # 自定义业务异常
│   └── GlobalExceptionHandler.java   # 全局异常处理器
├── model/
│   ├── po/
│   │   ├── LotteryDoubleBall.java       # 双色球开奖记录 PO
│   │   └── LotteryDoubleBallNumber.java # 双色球号码明细 PO
│   └── dto/
│       └── UserInfo.java             # 用户信息 DTO
├── repository/
│   ├── LotteryDoubleBallRepository.java       # 开奖记录 Repository
│   └── LotteryDoubleBallNumberRepository.java # 号码明细 Repository
├── scheduled/
│   └── LotterySyncTask.java          # 定时同步任务（首次全量 + 每日增量重试）
├── service/
│   ├── LotterySyncService.java       # 双色球数据同步服务
│   └── OpenSearchService.java        # OpenSearch 搜索服务
├── utils/
│   ├── CurrentPrincipal.java         # 当前用户工具
│   ├── JsonUtil.java                 # JSON 工具类
│   └── MDCUtil.java                  # MDC 日志链路工具
└── webflux/
    └── RequestResponseLoggingFilter.java  # 请求/响应日志过滤器

src/main/resources/
├── application.yaml                  # 基础配置
├── application-dev.yaml              # 开发环境配置
├── application-prod.yaml             # 生产环境配置
├── logback-spring.xml                # 日志配置
└── db/                               # Flyway 数据库迁移脚本
    └── V1_20260628__create_lottery_double_ball.sql  # 双色球开奖记录表
```

## 中间件（Docker Compose）

所有中间件通过 Docker Compose 管理，配置文件位于 `Middleware/docker-compose.yml`。

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| MySQL | `mysql:8.4.0` | 3306 | 数据库，密码 `Xu914939` |
| OpenSearch | `opensearchproject/opensearch:2.13.0` | 9200, 9300 | 搜索引擎，单节点，禁用安全认证 |
| Redis | `redis:7.2-alpine` | 6379 | 缓存/会话存储 |

### 启动中间件

```bash
cd /path/to/Middleware
docker-compose up -d
```

## 开发环境配置

**激活 profile**：启动时添加 `--spring.profiles.active=dev`

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 数据库连接

- **数据库名**：`lottery`
- **用户**：`root`
- **密码**：`Xu914939`
- **连接地址**：`r2dbc:mysql://localhost:3306/lottery`

### 数据库迁移（Flyway）

应用启动时自动执行 `src/main/resources/db/` 目录下的迁移脚本，无需手动执行 SQL。

迁移脚本命名规范：`V{版本号}_{日期}__{描述}.sql`

```bash
# 示例
V1_20260628__create_lottery_double_ball.sql
V2_20260701__add_some_table.sql
```

### API 端点

| 路径 | 说明 | 认证 |
|------|------|------|
| `GET /common/ok` | 健康检查 | ❌ 无需认证 |
| `POST /auth/register` | 用户注册 | ✅ 需认证 |
| `POST /auth/login` | 用户登录 | ✅ 需认证 |
| `POST /auth/logout` | 用户登出 | ✅ 需认证 |
| `GET/POST /lottery/list` | 彩票列表 | ✅ 需认证 |
| `GET/POST /lottery/detail` | 彩票详情 | ✅ 需认证 |

## 构建与部署

```bash
# 编译
mvn clean compile

# 打包
mvn clean package

# 运行（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 运行（生产环境）
java -jar target/LuckyBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 双色球开奖数据同步

### 数据来源

调用中国福利彩票官方网站 JSON 接口（无需注册、无需 key）：

```
GET https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice
  ?name=ssq
  &pageNo={pageNo}
  &pageSize={pageSize}
  &systemType=PC
```

API 返回格式（单条开奖数据）：

```json
{
  "name": "双色球",
  "code": "2026072",
  "date": "2026-06-25(四)",
  "red": "07,08,12,15,17,21",
  "blue": "01",
  "sales": "348744958",
  "poolmoney": "2182909574",
  "prizegrades": [
    {"type": 1, "typenum": "4", "typemoney": "8287457"},
    {"type": 2, "typenum": "143", "typemoney": "367827"}
  ]
}
```

### 数据库表结构

| 表 | 说明 |
|------|------|
| `lottery_double_ball` | 双色球开奖记录主表，每期一条记录 |
| `lottery_double_ball_number` | 双色球号码明细表，每期 7 行（6 红球 + 1 蓝球） |

### 同步策略

#### 1. 首次全量同步（`syncHistory`）

服务首次启动时，通过 `@PostConstruct` 自动触发：

1. 查询数据库最新期号
2. **数据库为空** → 调用 `syncAllHistory()`，从第 1 页开始拉取全部历史数据（2003 年至今），逐页写入
3. **数据库已有数据** → 调用 `syncFromLatest()`，进入增量同步模式

#### 2. 增量同步（`syncFromLatest` → `syncPagesFrom`）

这是所有同步场景（定时、首次补全、手动）的核心逻辑：

```
syncFromLatest(latestPeriod)
  └─ syncPagesFrom(pageNo=1, latestPeriod)
       ├─ fetchPage(pageNo) → 请求福彩网 API
       ├─ stream.takeWhile(r → r.period > latestPeriod) → 过滤出比最新期号新的数据
       ├─ toSave 为空？ → 后续页更旧，停止翻页
       ├─ 逐个 concatMap 写入数据库（幂等）
       └─ 本页包含 latestPeriod 或已到最后一页？ → 停止 | 否则继续翻下一页
```

关键设计：

| 特性 | 说明 |
|------|------|
| **翻页方向** | 从第 1 页（最新数据）往回翻，写入顺序严格从新到旧 |
| **每页 100 条** | 覆盖约 8 个月数据，绝大多数场景只需请求第 1 页 |
| **`takeWhile` 过滤** | 只保留比 `latestPeriod` 大的数据，遇到 ≤ 的立即截断 |
| **提前停止** | `toSave.isEmpty()` → 本页第一条已 ≤ `latestPeriod`，后续页更旧，立即停止 |
| **幂等写入** | `saveLotteryData()` 先按 `period` 查库，已存在则跳过 |

示例：数据库最新是 `2026050`，第 1 页返回 `[2026072 ... 2026001]`
→ `takeWhile` 写入 `2026072 → ... → 2026051`（比 `2026050` 新的）
→ 遇到 `2026050` → 停止

#### 3. 开奖日定时同步 — 带重试机制

由 `LotterySyncTask` 管理，只在周二、四、日执行：

```
21:30 ─── syncLatestFirst() ─── 首次尝试
21:31 ─── syncLatestRetry()  ─── 第 1 次重试（若未拉到当天数据）
21:32 ─── syncLatestRetry()  ─── 第 2 次重试
...         ...                   ...
22:00 ─── syncLatestRetry()  ─── 第 30 次重试（最后一次，到 22:00 截止）
```

每次尝试调用 `LotterySyncService.syncLatest()`：

1. 请求 API 第 1 页，取第 1 条数据
2. 判断 `drawDate == today`？
   - **是** → 写入数据库（幂等），返回 `true`，标记当天已完成
   - **否** → 返回 `false`，等待下次重试

| 结果 | 含义 | 后续行为 |
|------|------|----------|
| `true` | API 最新一期日期 == 今天，已拉取到当天开奖数据 | `todaySynced = true`，后续执行跳过 |
| `false` | API 最新一期日期 ≠ 今天，官网尚未更新 | 下一分钟继续重试 |
| 异常 | 网络超时、API 报错、解析失败 | 下一分钟继续重试 |

**容错说明**：

| 场景 | 处理方式 |
|------|----------|
| 网络抖动 / API 超时 | 下一分钟自动重试，成功/失败均计入重试次数，最多 30 次（到 22:00） |
| 官网 21:15 未及时更新数据 | 后续重试轮次中官网数据就绪后即可拉取 |
| 当天 30 次重试全部失败 | 下次开奖日 21:30 会自动拉取（但会丢失该期）。**更可靠的兜底**：服务重启时 `initHistory()` 会自动补全所有缺失期数 |
| 服务器宕机错过多期 | 重启后 `@PostConstruct initHistory()` 自动从数据库最新一期之后翻页拉取所有缺失数据 |
| 重启时历史补全与定时任务冲突 | `historyRunning` 标记控制，历史补全期间定时任务跳过，避免并发调用 API 和写入数据库 |

#### 4. 按期号查询同步（`syncByPeriod`）

手动调用，指定期号（如 `"2026072"`）拉取单期数据：

1. 请求 API 第 1 页（最新 100 条）
2. 过滤匹配的期号
3. 未找到 → 抛出异常
4. 找到 → 幂等写入

### 事务与幂等

- **事务粒度**：`saveLotteryData()` 内部使用 `transactionalOperator::transactional`，每条数据在独立事务中写入，确保主表 `lottery_double_ball` 和明细表 `lottery_double_ball_number` 同时写入或同时回滚
- **幂等写入**：写入前先按 `period` 查询，已存在的期号自动跳过
- **星期字段**：从 API 返回的 `date` 字段（如 `"2026-06-25(四)"`）中提取括号内简写，通过 `convertWeekday()` 转换为完整中文星期（如 `星期四`）；若提取失败，兜底使用 Java `DayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)`

### 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `lottery.api.page-size` | `100` | 每页条数 |
| `lottery.api.history-enable` | `true` | 是否启用首次启动历史数据补全 |

### 相关类

| 类 | 说明 |
|------|------|
| `LotterySyncService` | 同步核心逻辑：分页拉取、JSON 解析、幂等写入 |
| `LotterySyncTask` | 定时任务调度：首次全量补全 + 开奖日 21:30 同步 + 每分钟重试到 22:00 |
| `LotteryApiConfig` | API 配置参数（name、pageSize、cron、historyEnable） |
| `LotteryDoubleBall` | 双色球开奖记录 PO（period、drawDate、weekday、红球、蓝球、奖池等） |
| `LotteryDoubleBallNumber` | 双色球号码明细 PO（period、drawDate、number、type） |
| `LotteryDoubleBallRepository` | 开奖记录 Repository（按 period 查、按日期范围查、查最新） |
| `LotteryDoubleBallNumberRepository` | 号码明细 Repository（按号码查出现次数、最近出现等） |
| `WebClientConfig` | WebClient 配置（16MB 内存缓冲） |
| `TransactionConfig` | 响应式事务管理器 + TransactionalOperator Bean |
| `SchedulingConfig` | 启用 @Scheduled 定时任务支持 |

## 日志

- 日志格式：`级别|时间 - 消息`
- 按小时滚动，保留 72 小时
- 应用日志级别：`DEBUG`
- 错误日志单独输出到 `logs/error.log`
- 每次请求自动生成 `traceId` 用于链路追踪
