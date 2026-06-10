# CP Crawler 前后端分离设计方案

**日期:** 2026-06-03
**状态:** 已确认

## 1. 目标

将现有 Java Swing 桌面应用改造为前后端分离架构：
- 后端：Spring Boot REST API，复用现有爬虫/分析/存储代码
- 前端：Vite + React + TypeScript，Chart.js 雷达图，简洁美观

## 2. 架构总览

```
前端 (Vite + React + TS)   localhost:5173
        │
        │ REST API (JSON)
        ▼
后端 (Spring Boot)          localhost:8080
        │
        ├── Controller (REST endpoints)
        ├── Service (编排 + 分析)
        └── 复用现有爬虫/模型/存储代码
```

## 3. 后端 API 设计

### 3.1 依赖变更

新增 Spring Boot Starter Web (spring-boot-starter-web)，其余依赖不变。

### 3.2 REST Endpoints

| 方法 | 路径 | 请求体 | 返回 |
|------|------|--------|------|
| POST | `/api/crawl` | `{ "users": [{"handle":"tourist","platform":"CODEFORCES"}] }` | `{ "taskId": "uuid" }` |
| GET | `/api/crawl/progress` | - | `{ "completed":2, "failed":0, "total":3, "done":false }` |
| GET | `/api/crawl/results` | - | `[{ "handle":"tourist", "stats": {...}, "profile": {...} }]` |
| GET | `/api/data/users` | - | 已缓存的用户列表 |
| GET | `/api/data/stats/{handle}` | - | UserStatistics JSON |

### 3.3 包结构调整

```
com.pachong
├── Main.java                  → Spring Boot 启动类
├── controller/
│   └── CrawlController.java   → REST endpoints
├── service/
│   └── CrawlService.java      → 爬取编排（单例，管理后台任务）
├── concurrent/                → 不变
├── crawler/                   → 不变
├── model/                     → 不变（加 Jackson 注解）
├── analysis/                  → 不变
├── storage/                   → 不变
└── util/                      → 不变
```

删除 `gui/` 包（Swing GUI 不再需要）。

## 4. 前端设计

### 4.1 技术栈

- Vite 5 + React 18 + TypeScript 5
- Chart.js 4 + react-chartjs-2（雷达图）
- CSS Modules（样式隔离）

### 4.2 组件树

```
App
├── Header              （标题栏：CP Crawler）
├── InputPanel          （用户名输入 + 平台选择 + 添加/移除 + 用户列表 + 开始按钮）
└── ResultTabs          （Tab 切换：进度 | 雷达图 | 统计）
    ├── ProgressPanel   （进度条 + 日志流）
    ├── RadarPanel      （Chart.js 雷达图 Canvas）
    └── StatsPanel      （统计表格）
```

### 4.3 数据流

1. 用户在 InputPanel 添加用户（纯本地状态）
2. 点击"开始爬取" → POST /api/crawl
3. 前端轮询 GET /api/crawl/progress（每秒一次），更新 ProgressPanel
4. 爬取完成后 GET /api/crawl/results → 更新 RadarPanel + StatsPanel

### 4.4 视觉风格

- 配色：主色 `#1a73e8`，绿色 `#2ea043`（操作按钮），`#f44336`（失败），浅灰背景 `#f5f5f5`
- 卡片式布局，圆角 8px，轻微阴影
- 进度条带百分比文字
- 日志区等宽字体（Consolas / monospace），深色背景
- 雷达图支持 hover tooltip，图例点击切换
- 响应式：最小宽度 800px，桌面优先

### 4.5 目录结构

```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── App.css
    ├── api/
    │   └── index.ts           （fetch 封装）
    ├── components/
    │   ├── Header.tsx
    │   ├── InputPanel.tsx
    │   ├── ProgressPanel.tsx
    │   ├── RadarPanel.tsx
    │   └── StatsPanel.tsx
    ├── types/
    │   └── index.ts           （TS 类型定义）
    └── vite-env.d.ts
```

## 5. 任务清单

1. 后端改造：pom.xml 加 Spring Boot 依赖，新建 Main/CrawlController/CrawlService
2. 删除 gui/ 包
3. 前端初始化：Vite + React + TS 项目
4. 前端实现：InputPanel → ProgressPanel → RadarPanel → StatsPanel
5. 前后端联调验证
6. 最终测试

## 6. 不做的事

- 不做 WebSocket / SSE（轮询足够简单）
- 不做用户认证
- 不做移动端适配
- 不做历史数据管理页面
- 不做国际化
