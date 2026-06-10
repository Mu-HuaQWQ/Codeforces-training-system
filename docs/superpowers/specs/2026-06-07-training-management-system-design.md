# Training Management System 设计文档

## Context

将现有的"CP Crawler 爬虫查看器"改造为**训练管理系统**。当前系统仅支持临时输入用户、爬取数据、查看雷达图，数据存在内存和 JSON 文件中，重启即丢失。用户需要一个持久化的学生管理系统，能自动追踪学生各平台的做题情况和 rating 变化。

## Architecture

```
前端 React 19 + Vite (port 5173)
├── /          学生列表主页
├── /:id       学生详情页
└── 弹窗        添加/编辑学生

后端 Spring Boot 3.3.5 (port 8080)
├── /api/students       学生 CRUD
├── /api/crawl          爬虫控制 + 一键更新全部学生
├── /api/rating-history rating 历史查询
└── /api/contests       比赛记录查询
```

前端通过 Vite proxy 将 `/api` 代理到 `localhost:8080`。

## Data Model (H2 Database)

使用 Spring Data JPA，H2 内嵌数据库，文件持久化到 `./data/h2`。

### student
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT (PK, AUTO) | 主键 |
| name | VARCHAR(100) | 学生姓名 |
| handle | VARCHAR(100) | 平台用户名 |
| platform | VARCHAR(20) | CODEFORCES / LUOGU |
| created_at | TIMESTAMP | 添加时间 |

唯一约束：(handle, platform)

### rating_history
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT (PK, AUTO) | 主键 |
| student_id | BIGINT (FK→student) | 关联学生 |
| rating | INT | 当时 rating |
| recorded_at | TIMESTAMP | 记录时间 |

### contest_record
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT (PK, AUTO) | 主键 |
| student_id | BIGINT (FK→student) | 关联学生 |
| contest_name | VARCHAR(500) | 比赛名称 |
| contest_id | VARCHAR(100) | 平台比赛 ID |
| rank | INT | 最终排名 |
| old_rating | INT | 赛前 rating |
| new_rating | INT | 赛后 rating |
| contest_date | TIMESTAMP | 比赛日期 |

## Backend Changes

### 新建
- `Student` entity + `StudentRepository` (JPA)
- `RatingHistory` entity + `RatingHistoryRepository`
- `ContestRecord` entity + `ContestRecordRepository`
- `StudentController` — REST CRUD
- `StudentService` — 业务逻辑

### 增强
- `CrawlController` 新增 `POST /api/crawl/refresh-all` — 遍历所有学生，逐一爬取，保存 rating 快照和比赛记录
- `CrawlService` 爬取完成后自动调用 `saveRatingSnapshot()` 和 `saveContestRecords()`
- 爬虫增加比赛记录爬取：CF `contest.ratingChanges` API，Luogu 用已爬取的 submission 反推比赛参与

### 「当前分数」和「这周过题数」
- 每次爬取时实时计算，从最新数据中获取
- 当前分数 = 最新一次 rating_history 的 rating
- 这周过题数 = 爬取的 submission 中近 7 天的 AC 数量

## Frontend Changes

### 新增依赖
- `react-router-dom` — 页面路由

### 新建组件
- `StudentList.tsx` — 学生列表主页（表格 + 操作按钮）
- `StudentDetail.tsx` — 学生详情页（rating 曲线 + 比赛记录 + 雷达图）
- `AddStudentModal.tsx` — 添加学生弹窗（姓名、用户名、平台）
- `RatingChart.tsx` — Rating 变化折线图 (Chart.js)

### 修改组件
- `App.tsx` — 改为路由入口
- `Header.tsx` — 标题改为「训练管理系统」
- `RadarPanel.tsx` — 保留，用于详情页
- `StatsPanel.tsx` — 保留，用于详情页统计
- `InputPanel.tsx` — 废弃或合并到 AddStudentModal

## API Endpoints Summary

| Method | Path | Body | Response | Description |
|---|---|---|---|---|
| GET | /api/students | — | Student[] | 获取所有学生（含实时分数和本周过题数） |
| POST | /api/students | {name, handle, platform} | Student | 添加学生 |
| DELETE | /api/students/{id} | — | — | 删除学生 |
| GET | /api/students/{id} | — | StudentDetail | 学生详情（含 profile, stats, radarData） |
| GET | /api/students/{id}/rating-history | — | RatingHistory[] | rating 历史 |
| GET | /api/students/{id}/contests | — | ContestRecord[] | 比赛记录 |
| POST | /api/crawl/refresh/{id} | — | — | 刷新单个学生数据 |
| POST | /api/crawl/refresh-all | — | — | 刷新全部学生数据 |
| GET | /api/crawl/progress | — | ProgressInfo | 爬取进度（保留） |

## Verification

1. 启动后端 `mvn spring-boot:run`，确认 H2 表自动创建
2. `curl POST /api/students` 添加测试学生
3. `curl POST /api/crawl/refresh/{id}` 触发爬取，检查返回
4. `curl GET /api/students/{id}` 确认数据已更新
5. 前端 `npm run dev`，访问 `localhost:5173`
6. 在主页添加学生、点击详情、查看 rating 曲线和比赛记录
