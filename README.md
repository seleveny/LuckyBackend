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
│   ├── OpenSearchConfig.java         # OpenSearch 客户端配置
│   └── SecurityConfig.java           # Spring Security 安全配置
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
│   └── dto/
│       └── UserInfo.java             # 用户信息 DTO
├── service/
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

## 日志

- 日志格式：`级别|时间 - 消息`
- 按小时滚动，保留 72 小时
- 应用日志级别：`DEBUG`
- 错误日志单独输出到 `logs/error.log`
- 每次请求自动生成 `traceId` 用于链路追踪
