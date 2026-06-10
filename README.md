
# 竞赛训练管理系统

> 基于 Spring Boot + React 的 Codeforces & Luogu 学生训练追踪平台

<img src="./assets/wps2.png" alt="img" style="zoom: 120%;" /> 


### 实 验 报 告

- 课  程  名   高级程序设计  
- 课  程  号      C01102         
- 学 生 姓  名            
- 学 生 学 号            
- 专 业 班 级            
- 所 在 学 院    计算分院         
- 指 导 老 师     郭鸣        

 



 实验报告日期：  2024  年  6 月 20 日





## 期末项目

 每个人都要提交项目报告。

内容包括代码，本说明文件

- 有 ReadME，项目简介

- 有 doc ，设计说明

- 有 src 

- 如果有 test ，包括测试

  

## 项目简介

本系统是一个**竞赛训练管理系统**，面向 ACM 竞赛教练和学生，提供多平台（Codeforces、Luogu）数据聚合与训练追踪功能。

**主要功能：**
- 🧑‍🎓 **学生管理**：添加/删除学生，记录姓名、平台用户名、当前 Rating
- 🔄 **自动爬取**：高并发爬虫自动从 Codeforces 和 Luogu 获取学生做题数据、Rating 变化
- 📊 **雷达图**：基于 10 个维度（DP、贪心、数学、数据结构、字符串、图论、树、几何、构造、二分）的能力雷达图
- 📈 **Rating 变化曲线**：历史 Rating 追踪，支持时间范围筛选（近1月/3月/6月/1年/全部）
- 🏆 **比赛记录**：自动提取学生参与过的比赛列表
- 📋 **统计表格**：AC 数、总提交数、通过率、连续 AC 天数等详细统计

**技术架构：**
- **前端**：React 19 + TypeScript + Vite 8 + Chart.js（雷达图 / 折线图）
- **后端**：Spring Boot 3.3.5 + Java 23 + H2 数据库
- **爬虫**：OkHttp 高并发爬取 + 令牌桶限流 + 生产者-消费者模式
- **存储**：H2 内嵌数据库（学生、Rating历史、比赛记录）+ JSON 文件（原始爬取数据）


## 设计说明

详细设计文档见 `docs/superpowers/specs/2026-06-07-training-management-system-design.md`。

### 系统架构

```
React 前端 (Vite, port 5173)
    │  /api 代理
    ▼
Spring Boot 后端 (port 8080)
    │
    ├── StudentController   — 学生 CRUD API
    ├── CrawlController     — 爬虫控制 API
    ├── StudentService      — 业务逻辑
    ├── CrawlService        — 爬虫编排
    └── CrawlerOrchestrator — 高并发爬虫引擎
         │
         ├── CodeforcesCrawler (CF API)
         ├── LuoguCrawler      (Luogu XHR)
         ├── RateLimiter       (令牌桶)
         └── ThreadPoolExecutor
              │
              ▼
         H2 Database + JSON 文件存储
```

### 数据库设计

| 表 | 用途 |
|---|---|
| `student` | 学生基本信息 (name, handle, platform) |
| `rating_history` | Rating 变化历史 (student_id, rating, recorded_at) |
| `contest_record` | 比赛记录 (student_id, contest_name, date) |

### 设计模式

| 模式 | 应用 |
|------|------|
| **策略模式** | `CrawlerStrategy` 接口 → `CodeforcesCrawler` / `LuoguCrawler` |
| **生产者-消费者** | `CrawlerOrchestrator` + `BlockingQueue` + `ThreadPoolExecutor` |
| **单例模式** | `ConfigLoader`、`HttpUtils` 的 OkHttpClient |
| **DTO 模式** | Java Record 用于 API 数据传输 |
| **MVC 模式** | Controller → Service → Repository 分层 |


## 技术特点

- ✅ **高并发爬虫**：线程池（core=4, max=8）+ 令牌桶限流（CF 3 req/s, Luogu 2 req/s）
- ✅ **指数退避重试**：最多 4 次重试，检测 404/429/502/503
- ✅ **前端路由**：react-router-dom 实现 SPA，学生列表 ↔ 详情页
- ✅ **响应式设计**：最大宽度 1200px，适配桌面端
- ✅ **H2 持久化**：数据重启不丢失，无需安装外部数据库
- ✅ **自动去重**：Rating 历史仅保存变化，比赛记录自动去重


## 项目结构

```
pachong/                    # 后端
├── src/main/java/com/pachong/
│   ├── Main.java           # Spring Boot 入口
│   ├── controller/
│   │   ├── CrawlController.java
│   │   └── StudentController.java
│   ├── service/
│   │   ├── CrawlService.java
│   │   └── StudentService.java
│   ├── entity/             # JPA 实体
│   │   ├── Student.java
│   │   ├── RatingHistory.java
│   │   └── ContestRecord.java
│   ├── repository/         # JPA Repository
│   ├── model/              # 数据模型 + 枚举
│   ├── crawler/            # 爬虫实现
│   ├── concurrent/         # 并发控制
│   ├── analysis/           # 数据分析
│   └── storage/            # JSON 存储
│
frontend/                   # 前端
├── src/
│   ├── App.tsx             # 路由入口
│   ├── api/index.ts        # API 调用
│   ├── types/index.ts      # TypeScript 类型
│   ├── pages/
│   │   ├── StudentList.tsx  # 学生列表页
│   │   └── StudentDetail.tsx# 学生详情页
│   └── components/
│       ├── Header.tsx
│       ├── AddStudentModal.tsx
│       ├── RatingChart.tsx
│       ├── RadarPanel.tsx
│       ├── StatsPanel.tsx
│       └── ProgressPanel.tsx
│
docs/                       # 文档
├── superpowers/specs/      # 设计文档
└── superpowers/plans/      # 实施计划
```


## 启动方式

```bash
# 1. 启动后端（需要 Java 23 + Maven）
cd pachong
mvn spring-boot:run

# 2. 启动前端（需要 Node.js）
cd frontend
npm install
npm run dev

# 3. 访问 http://localhost:5173
```


## 心得体会
- 困难 1 — **Codeforces API 稳定性**：CF API 偶尔返回 502/503，爬虫需要健壮的指数退避重试机制。通过分析 CF 官方 API 文档，设计了 4 次重试 + 多状态码检测的方案。
- 困难 2 — **前后端类型同步**：Java 后端 DTO 与 TypeScript 前端类型需要一一对应。采用「先定义后端 DTO，前端按 JSON 结构反推类型」的工作流，减少手动同步错误。
- 困难 3 — **爬虫限流与并发平衡**：CF 频率限制严格（~5req/s），太快会被封，太慢用户体验差。使用令牌桶算法实现精确限流。
- 收获：理解了全栈项目的完整开发流程，掌握了 Spring Boot JPA 持久化、React Router 路由设计、Chart.js 图表集成等技能。最大的体会是**架构设计比编码更重要**——好的模块划分能让后续迭代事半功倍。

​     

## 项目技术评价❗（修改填写）

| 功能/技术 | 备注 | 优  | 良  | 中  | 及格 |
| ---- | -------- | --- | --- | --- | --- |
| 学生管理 CRUD | 添加/删除/查询学生 | √ ||||
| 多平台爬虫 | Codeforces + Luogu 自动爬取 | √ ||||
| 雷达图 | 10维度能力可视化 | √ ||||
| Rating 变化图 | 历史追踪 + 时间范围筛选 | √ ||||
| 比赛记录 | 自动提取参赛记录 | √ ||||
| 设计模式 | 策略模式、生产者-消费者、单例、DTO、MVC | √ ||||
| 多线程 | ThreadPoolExecutor + BlockingQueue 高并发爬虫 | √ ||||
| 令牌桶限流 | RateLimiter 基于令牌桶算法 | √ ||||
| 重试机制 | 指数退避，最多4次，多状态码检测 | √ ||||
| JPA 持久化 | H2 + Spring Data JPA，自动建表 | √ ||||
| 前端路由 | react-router-dom SPA | √ ||||
| 响应式设计 | 桌面端 1200px 布局 | √ ||||
| 测试驱动开发 | API 联调 + TypeScript 类型检查 | | √ | | |
|  | | | | | |


## 小组分工

- 本人
  - 工作内容
    - 后端开发：Spring Boot + JPA + 爬虫引擎
    - 前端开发：React + TypeScript + Chart.js
    - 数据库设计：H2 表结构 + JPA Entity
    - 文档编写：设计文档 + 实验报告
    - API 测试与联调
  
- 另一成员
  - 工作内容
    - 文档编写
    - 测试程序
    - 需求分析
      
  

- 权重分配表：  ❗（修改填写）

| 本人  | 另一成员  |
| ---- | ---- |
| 0.9  | 0.9  |

1组人权重 最高 1.0

2人组权重 和最高 1.8



## 评分表❗（修改填写）


| 评分标准 | 总分 | 说明 | 自评计分❗ | 教师计分 |
| :---: | :---: | :---: | ----- | ----- |
| **代码质量** | 30分 | 代码结构清晰，易于理解；代码注释充足，有助于理解代码功能；代码无冗余，遵循DRY原则 | 27 |  |
| **功能实现** | 40分 | 所有要求的功能都已实现并正确运行；程序无明显错误或bug | 36 |  |
| **创新性** | 10分 | 作业展示了独特的创新思维；作业中实现的功能超出了基本要求 | 9 |  |
| **用户体验** | 10分 | 程序界面友好，易于操作；程序反馈清晰，易于理解 | 9 |  |
| **文档和报告** | 10分 | 提交了完整的文档，包括设计思路、代码解释和用户指南；提交了清晰的项目报告，包括项目概述、实现过程和遇到的问题及解决方案 | 9 |  |
| 总分 | 100分 |  | 90 |  |

 




（以下请勿修改）

## 指导教师评语

 

优： "此份大作业表现出色，展示了深入的课程内容理解和应用。项目设计结构清晰，模块划分合理，显示出良好的软件工程素养。各项功能都能准确地识别和处理各种输入，这是非常值得赞扬的。代码实现部分也做得很好，实现了一些高级的技术。总的来说，此份作业完成得非常好，期待看到在编程项目方面的进一步成果。"

---

良： "此份大作业完成得很好，展示了对课程内容的理解和应用。项目设计结构清晰，模块划分合理。各项功能都能处理大部分的输入，这是值得赞扬的。然而，代码实现部分还有一些提升的空间。建议在未来的学习中，可以更深入地研究这些高级技术。"

---

中： "此份大作业完成得一般，展示了对课程内容的基本理解。项目设计有一定的结构，但模块划分还有待改进。各项功能能处理一部分的输入，但还有一些错误需要修正。代码实现部分还有很大的提升空间。建议在未来的学习中，加强对**编程**的理解和应用。"

---

及格： "此份大作业完成得基本合格，但还有很多需要改进的地方。项目设计需要更清晰的结构和更合理的模块划分。各项功能还有很多错误需要修正。代码实现部分几乎没有亮点。建议在未来的学习中，加强对**编程**的理解和应用，提高编程技能。"

  

  实验报告评分（百分制）：   分

 

  指导教师签名：<img src="./assets/签名.jpg" style="zoom:15%;" />

  日     期：2024年6  月 27 日
