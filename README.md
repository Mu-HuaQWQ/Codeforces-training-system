# 竞赛训练管理系统

基于 Spring Boot + React 的 Codeforces & Luogu 学生训练追踪平台。

## 功能

- 🧑‍🎓 **学生管理** — 添加/删除学生，记录姓名、平台用户名
- 🔄 **自动爬取** — 高并发爬虫从 Codeforces / Luogu 获取做题数据和 Rating
- 📊 **雷达图** — 10 维度能力可视化（DP、贪心、数学、数据结构、字符串、图论、树、几何、构造、二分）
- 📈 **Rating 变化** — 历史 Rating 折线图，支持时间范围筛选
- 🏆 **比赛记录** — 自动拉取正式比赛，显示排名、过题数、Rating 变化（15条/页分页）
- 📋 **详细统计** — AC 数、提交数、通过率、连续 AC 天数

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | React 19 + TypeScript + Vite 8 + Chart.js |
| 后端 | Spring Boot 3.3.5 + Java 23 |
| 数据库 | H2（内嵌，零安装） |
| 爬虫 | OkHttp + 令牌桶限流 + 生产者-消费者模式 |

## 项目结构

```
pachong/                     # 后端 Spring Boot
├── src/main/java/com/pachong/
│   ├── controller/          # REST API
│   ├── service/             # 业务逻辑
│   ├── entity/              # JPA 实体
│   ├── repository/          # 数据访问
│   ├── model/               # 数据模型
│   ├── crawler/             # 爬虫实现（策略模式）
│   ├── concurrent/          # 并发控制（令牌桶 + 线程池）
│   ├── analysis/            # 数据分析 + 雷达图生成
│   └── storage/             # JSON 文件存储
│
frontend/                    # 前端 React
├── src/
│   ├── pages/               # StudentList, StudentDetail
│   ├── components/          # RatingChart, RadarPanel, AddStudentModal ...
│   ├── api/                 # API 调用
│   └── types/               # TypeScript 类型定义
│
docs/                        # 设计文档
```

## 快速开始

```bash
# 1. 启动后端（需要 Java 23 + Maven）
cd pachong
mvn spring-boot:run

# 2. 启动前端（需要 Node.js 22+）
cd frontend
npm install
npm run dev

# 3. 打开 http://localhost:5173
```

## 设计模式

| 模式 | 应用场景 |
|------|---------|
| 策略模式 | `CrawlerStrategy` → CodeforcesCrawler / LuoguCrawler |
| 生产者-消费者 | CrawlerOrchestrator + BlockingQueue + ThreadPoolExecutor |
| MVC | Controller → Service → Repository |
| DTO | Java Record 用于 API 数据传输 |

## API 端点

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/students` | 学生列表 |
| POST | `/api/students` | 添加学生 |
| DELETE | `/api/students/{id}` | 删除学生 |
| GET | `/api/students/{id}` | 学生详情（含爬取、雷达图） |
| POST | `/api/crawl/refresh-all` | 刷新全部学生 |
| POST | `/api/crawl` | 爬取指定用户 |
| GET | `/api/crawl/progress` | 爬取进度 |
