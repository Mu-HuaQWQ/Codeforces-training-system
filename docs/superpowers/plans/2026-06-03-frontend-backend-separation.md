# CP Crawler 前后端分离实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Java Swing 桌面应用改造为 Spring Boot REST API + Vite React TypeScript 前端

**Architecture:** 后端 Spring Boot 暴露 REST API，复用现有爬虫/分析/存储代码；前端 Vite + React + TS 通过 fetch 调用 API，Chart.js 画雷达图

**Tech Stack:** Spring Boot 3, Vite 5, React 18, TypeScript 5, Chart.js 4 + react-chartjs-2

---

### Task 1: 后端 — 添加 Spring Boot 依赖

**Files:**
- Modify: `pachong/pom.xml`

- [ ] **Step 1: 加 spring-boot-starter-parent 和 web starter**

在 pom.xml 的 `<properties>` 之前加入 parent：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>
```

在 `<dependencies>` 中加入：

```xml
<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

添加 spring-boot-maven-plugin：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

- [ ] **Step 2: 验证编译**

```bash
cd pachong && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pachong/pom.xml
git commit -m "build: add Spring Boot starter web dependency"
```

---

### Task 2: 后端 — 创建 Spring Boot 启动类和配置

**Files:**
- Modify: `pachong/src/main/java/com/pachong/Main.java`
- Create: `pachong/src/main/resources/application.properties`

- [ ] **Step 1: 添加 CORS 和端口配置**

创建 `pachong/src/main/resources/application.properties`：

```properties
server.port=8080
spring.jackson.serialization.write-dates-as-timestamps=false
```

- [ ] **Step 2: 改写 Main.java 为 Spring Boot 启动类**

```java
package com.pachong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
```

- [ ] **Step 3: 验证启动**

```bash
cd pachong && mvn spring-boot:run
```

Expected: 看到 "Started Main in X seconds"，浏览器访问 http://localhost:8080 返回 404（正常，还没写 Controller）

- [ ] **Step 4: Commit**

```bash
git add pachong/src/main/java/com/pachong/Main.java pachong/src/main/resources/application.properties
git commit -m "feat: Spring Boot application entry point"
```

---

### Task 3: 后端 — 创建 CrawlService

**Files:**
- Create: `pachong/src/main/java/com/pachong/service/CrawlService.java`

- [ ] **Step 1: 创建 Service 类，封装后台爬取任务管理和分析**

```java
package com.pachong.service;

import com.pachong.analysis.SubmissionAnalyzer;
import com.pachong.concurrent.CrawlTask;
import com.pachong.concurrent.CrawlerOrchestrator;
import com.pachong.model.*;
import com.pachong.storage.DataRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CrawlService {

    private volatile CrawlerOrchestrator orchestrator;
    private final SubmissionAnalyzer analyzer = new SubmissionAnalyzer();
    private final DataRepository repository = new DataRepository();
    private volatile boolean crawling = false;
    private volatile List<CrawlTask.CrawlResult> lastResults;

    public synchronized void startCrawl(List<UserRequest> users) {
        orchestrator = new CrawlerOrchestrator();
        for (UserRequest u : users) {
            orchestrator.addUser(u.handle(), u.platform());
        }
        crawling = true;
        new Thread(() -> {
            try {
                lastResults = orchestrator.startCrawl();
            } finally {
                crawling = false;
            }
        }).start();
    }

    public ProgressInfo getProgress() {
        if (orchestrator == null) {
            return new ProgressInfo(0, 0, 0, false);
        }
        return new ProgressInfo(
            orchestrator.getCompletedCount(),
            orchestrator.getFailedCount(),
            orchestrator.getTotalSubmitted(),
            crawling
        );
    }

    public List<UserStatsResponse> getResults() {
        if (lastResults == null) return List.of();
        List<UserStatsResponse> responses = new ArrayList<>();
        for (CrawlTask.CrawlResult r : lastResults) {
            if (!r.isSuccess()) {
                responses.add(new UserStatsResponse(
                    r.getHandle(), r.getPlatform().getDisplayName(),
                    null, null, true, r.getErrorMessage()
                ));
                continue;
            }
            UserStatistics stats = analyzer.analyze(r.getHandle(), r.getPlatform(), r.getSubmissions());
            repository.saveStatistics(r.getHandle(), stats);
            responses.add(new UserStatsResponse(
                r.getHandle(), r.getPlatform().getDisplayName(),
                r.getProfile(), stats, false, null
            ));
        }
        return responses;
    }

    public List<RadarData> getComparisonRadarData(List<String> handles) {
        if (lastResults == null) return List.of();
        var userTagMap = new LinkedHashMap<String, Map<String, Integer>>();
        for (CrawlTask.CrawlResult r : lastResults) {
            if (!r.isSuccess() || r.getSubmissions() == null) continue;
            if (!handles.contains(r.getHandle())) continue;
            List<Submission> acOnly = r.getSubmissions().stream()
                .filter(Submission::isAccepted).toList();
            userTagMap.put(r.getHandle(), analyzer.computeTagAcceptedCount(acOnly));
        }
        return analyzer.generateComparisonRadarData(userTagMap);
    }

    // === DTOs ===

    public record UserRequest(String handle, Platform platform) {}

    public record ProgressInfo(int completed, int failed, int total, boolean done) {}

    public record UserStatsResponse(
        String handle, String platform,
        UserProfile profile, UserStatistics stats,
        boolean failed, String error
    ) {}
}
```

- [ ] **Step 2: 验证编译**

```bash
cd pachong && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pachong/src/main/java/com/pachong/service/CrawlService.java
git commit -m "feat: CrawlService for background crawling and analysis"
```

---

### Task 4: 后端 — 创建 REST Controller

**Files:**
- Create: `pachong/src/main/java/com/pachong/controller/CrawlController.java`

- [ ] **Step 1: 创建 REST Controller**

```java
package com.pachong.controller;

import com.pachong.model.RadarData;
import com.pachong.service.CrawlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CrawlController {

    private final CrawlService crawlService;

    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostMapping("/crawl")
    public String startCrawl(@RequestBody CrawlRequest request) {
        crawlService.startCrawl(request.users());
        return "{\"status\":\"started\"}";
    }

    @GetMapping("/crawl/progress")
    public CrawlService.ProgressInfo getProgress() {
        return crawlService.getProgress();
    }

    @GetMapping("/crawl/results")
    public List<CrawlService.UserStatsResponse> getResults() {
        return crawlService.getResults();
    }

    @PostMapping("/stats/compare")
    public List<RadarData> compareUsers(@RequestBody CompareRequest request) {
        return crawlService.getComparisonRadarData(request.handles());
    }

    // === Request DTOs ===

    public record CrawlRequest(List<CrawlService.UserRequest> users) {}

    public record CompareRequest(List<String> handles) {}
}
```

- [ ] **Step 2: 验证编译**

```bash
cd pachong && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pachong/src/main/java/com/pachong/controller/CrawlController.java
git commit -m "feat: REST API controller for crawl endpoints"
```

---

### Task 5: 后端 — 删除 GUI 包

**Files:**
- Delete: `pachong/src/main/java/com/pachong/gui/InputPanel.java`
- Delete: `pachong/src/main/java/com/pachong/gui/MainWindow.java`
- Delete: `pachong/src/main/java/com/pachong/gui/ProgressPanel.java`
- Delete: `pachong/src/main/java/com/pachong/gui/RadarChartPanel.java`

- [ ] **Step 1: 删除 GUI 文件**

```bash
rm pachong/src/main/java/com/pachong/gui/*.java
rmdir pachong/src/main/java/com/pachong/gui
```

- [ ] **Step 2: 验证编译（确认没有引用 gui 包的代码）**

```bash
cd pachong && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git rm pachong/src/main/java/com/pachong/gui/*.java
git commit -m "refactor: remove Swing GUI, replaced by REST API"
```

---

### Task 6: 前端 — 初始化 Vite + React + TS 项目

**Files:**
- Create: `frontend/` 整个项目目录

- [ ] **Step 1: 用 Vite 创建项目**

```bash
cd /e/program/java
npm create vite@latest frontend -- --template react-ts
```

- [ ] **Step 2: 安装依赖**

```bash
cd frontend
npm install
npm install chart.js react-chartjs-2
```

- [ ] **Step 3: 配置 Vite 代理**

修改 `frontend/vite.config.ts`：

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 4: 验证启动**

```bash
cd frontend && npm run dev
```

Expected: 看到 Vite 启动在 localhost:5173

- [ ] **Step 5: Commit**

```bash
git add frontend/
git commit -m "feat: initialize Vite + React + TypeScript frontend"
```

---

### Task 7: 前端 — TypeScript 类型定义

**Files:**
- Create: `frontend/src/types/index.ts`

- [ ] **Step 1: 定义所有 TS 类型**

```typescript
export type Platform = 'CODEFORCES' | 'LUOGU';

export interface UserInput {
  handle: string;
  platform: Platform;
}

export interface ProgressInfo {
  completed: number;
  failed: number;
  total: number;
  done: boolean;
}

export interface UserProfile {
  handle: string;
  platform: string;
  rating: number | null;
  maxRating: number | null;
  rank: string | null;
  contribution: number | null;
}

export interface TagCount {
  [tag: string]: number;
}

export interface UserStatistics {
  handle: string;
  platform: string;
  totalSubmissions: number;
  acceptedCount: number;
  acceptanceRate: number;
  tagAcceptedCount: TagCount;
  tagTotalCount: TagCount;
  difficultyCount: TagCount;
  maxStreak: number;
}

export interface RadarData {
  handle: string;
  labels: string[];
  values: number[];
}

export interface UserStatsResponse {
  handle: string;
  platform: string;
  profile: UserProfile | null;
  stats: UserStatistics | null;
  failed: boolean;
  error: string | null;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/types/index.ts
git commit -m "feat: TypeScript type definitions for API data"
```

---

### Task 8: 前端 — API 层

**Files:**
- Create: `frontend/src/api/index.ts`

- [ ] **Step 1: 封装 fetch 请求**

```typescript
import type {
  ProgressInfo,
  RadarData,
  UserInput,
  UserStatsResponse,
} from '../types';

const BASE = '/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
  }
  return res.json();
}

export function startCrawl(users: UserInput[]): Promise<{ status: string }> {
  return request('/crawl', {
    method: 'POST',
    body: JSON.stringify({ users }),
  });
}

export function getProgress(): Promise<ProgressInfo> {
  return request('/crawl/progress');
}

export function getResults(): Promise<UserStatsResponse[]> {
  return request('/crawl/results');
}

export function getComparison(handles: string[]): Promise<RadarData[]> {
  return request('/stats/compare', {
    method: 'POST',
    body: JSON.stringify({ handles }),
  });
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/index.ts
git commit -m "feat: API fetch layer"
```

---

### Task 9: 前端 — App 主布局和样式

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/App.css`
- Modify: `frontend/src/index.css`

- [ ] **Step 1: 清空并写入全局样式**

`frontend/src/index.css`：

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  background: #f0f2f5;
  color: #333;
  min-height: 100vh;
}

#root {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}
```

- [ ] **Step 2: App.tsx 主布局**

```tsx
import { useState } from 'react';
import Header from './components/Header';
import InputPanel from './components/InputPanel';
import ProgressPanel from './components/ProgressPanel';
import RadarPanel from './components/RadarPanel';
import StatsPanel from './components/StatsPanel';
import type { UserInput, UserStatsResponse, ProgressInfo, RadarData } from './types';
import { startCrawl, getProgress, getResults, getComparison } from './api';
import './App.css';

type Tab = 'progress' | 'radar' | 'stats';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('progress');
  const [crawling, setCrawling] = useState(false);
  const [progress, setProgress] = useState<ProgressInfo>({ completed: 0, failed: 0, total: 0, done: false });
  const [log, setLog] = useState<string[]>([]);
  const [results, setResults] = useState<UserStatsResponse[]>([]);
  const [radarData, setRadarData] = useState<RadarData[]>([]);

  const appendLog = (msg: string) => setLog(prev => [...prev, msg]);

  const handleStart = async (users: UserInput[]) => {
    setLog([]);
    setResults([]);
    setRadarData([]);
    setCrawling(true);
    setActiveTab('progress');
    appendLog(`开始爬取 ${users.length} 个用户...`);

    try {
      await startCrawl(users);

      const timer = setInterval(async () => {
        try {
          const p = await getProgress();
          setProgress(p);
          if (!p.done) return;

          clearInterval(timer);
          setCrawling(false);
          appendLog('=== 爬取完成 ===');

          const res = await getResults();
          setResults(res);
          for (const r of res) {
            if (r.failed) {
              appendLog(`[FAIL] ${r.handle}: ${r.error}`);
            } else if (r.stats) {
              appendLog(
                `${r.handle}: ${r.stats.acceptedCount} AC / ${r.stats.totalSubmissions} 提交, ` +
                `通过率 ${r.stats.acceptanceRate.toFixed(1)}%, 连续AC ${r.stats.maxStreak} 天`
              );
            }
          }

          const handles = res.filter(r => !r.failed).map(r => r.handle);
          if (handles.length > 0) {
            const radar = await getComparison(handles);
            setRadarData(radar);
          }
        } catch (e) {
          clearInterval(timer);
          appendLog(`错误: ${e}`);
          setCrawling(false);
        }
      }, 500);
    } catch (e) {
      appendLog(`请求失败: ${e}`);
      setCrawling(false);
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: 'progress', label: '进度' },
    { key: 'radar', label: '雷达图' },
    { key: 'stats', label: '统计' },
  ];

  return (
    <div className="app">
      <Header />
      <InputPanel onStart={handleStart} disabled={crawling} />
      <div className="result-area">
        <div className="tabs">
          {tabs.map(t => (
            <button
              key={t.key}
              className={`tab ${activeTab === t.key ? 'active' : ''}`}
              onClick={() => setActiveTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>
        <div className="tab-content">
          {activeTab === 'progress' && (
            <ProgressPanel progress={progress} log={log} />
          )}
          {activeTab === 'radar' && (
            <RadarPanel data={radarData} />
          )}
          {activeTab === 'stats' && (
            <StatsPanel results={results} />
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
```

- [ ] **Step 3: App.css 样式**

```css
.app {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-area {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.tabs {
  display: flex;
  border-bottom: 2px solid #e8e8e8;
}

.tab {
  padding: 12px 24px;
  border: none;
  background: none;
  font-size: 14px;
  cursor: pointer;
  color: #666;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.2s, border-color 0.2s;
}

.tab:hover {
  color: #1a73e8;
}

.tab.active {
  color: #1a73e8;
  border-bottom-color: #1a73e8;
  font-weight: 600;
}

.tab-content {
  padding: 24px;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/App.tsx frontend/src/App.css frontend/src/index.css
git commit -m "feat: App layout with tabs and crawl orchestration"
```

---

### Task 10: 前端 — Header 组件

**Files:**
- Create: `frontend/src/components/Header.tsx`

- [ ] **Step 1: 创建 Header 组件**

```tsx
function Header() {
  return (
    <header style={styles.header}>
      <h1 style={styles.title}>🚀 CP Crawler</h1>
      <span style={styles.subtitle}>Codeforces & Luogu 竞赛数据分析</span>
    </header>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex',
    alignItems: 'baseline',
    gap: 16,
    padding: '16px 0',
  },
  title: {
    fontSize: 24,
    fontWeight: 700,
    color: '#1a73e8',
  },
  subtitle: {
    fontSize: 14,
    color: '#999',
  },
};

export default Header;
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/Header.tsx
git commit -m "feat: Header component"
```

---

### Task 11: 前端 — InputPanel 组件

**Files:**
- Create: `frontend/src/components/InputPanel.tsx`

- [ ] **Step 1: 创建 InputPanel 组件**

```tsx
import { useState } from 'react';
import type { UserInput, Platform } from '../types';

interface Props {
  onStart: (users: UserInput[]) => void;
  disabled: boolean;
}

function InputPanel({ onStart, disabled }: Props) {
  const [handle, setHandle] = useState('');
  const [platform, setPlatform] = useState<Platform>('CODEFORCES');
  const [users, setUsers] = useState<UserInput[]>([]);

  const addUser = () => {
    const trimmed = handle.trim();
    if (!trimmed) return;
    setUsers(prev => [...prev, { handle: trimmed, platform }]);
    setHandle('');
  };

  const removeUser = (idx: number) => {
    setUsers(prev => prev.filter((_, i) => i !== idx));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') addUser();
  };

  return (
    <div style={styles.panel}>
      <h3 style={styles.heading}>添加用户</h3>
      <div style={styles.row}>
        <input
          style={styles.input}
          placeholder="用户名 handle"
          value={handle}
          onChange={e => setHandle(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />
        <select
          style={styles.select}
          value={platform}
          onChange={e => setPlatform(e.target.value as Platform)}
          disabled={disabled}
        >
          <option value="CODEFORCES">Codeforces</option>
          <option value="LUOGU">Luogu</option>
        </select>
        <button style={styles.addBtn} onClick={addUser} disabled={disabled}>
          添加
        </button>
      </div>
      {users.length > 0 && (
        <div style={styles.userList}>
          {users.map((u, i) => (
            <span key={i} style={styles.tag}>
              {u.handle} ({u.platform === 'CODEFORCES' ? 'CF' : 'LG'})
              <button style={styles.removeBtn} onClick={() => removeUser(i)}>
                ×
              </button>
            </span>
          ))}
        </div>
      )}
      <button
        style={styles.startBtn}
        onClick={() => onStart(users)}
        disabled={disabled || users.length === 0}
      >
        {disabled ? '爬取中...' : '▶ 开始爬取'}
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    background: '#fff',
    borderRadius: 8,
    padding: 20,
    boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
  },
  heading: {
    fontSize: 14,
    fontWeight: 600,
    color: '#333',
    marginBottom: 12,
  },
  row: {
    display: 'flex',
    gap: 8,
    marginBottom: 12,
  },
  input: {
    flex: 1,
    padding: '8px 12px',
    border: '1px solid #d9d9d9',
    borderRadius: 6,
    fontSize: 14,
    outline: 'none',
  },
  select: {
    padding: '8px 12px',
    border: '1px solid #d9d9d9',
    borderRadius: 6,
    fontSize: 14,
    background: '#fff',
    cursor: 'pointer',
  },
  addBtn: {
    padding: '8px 16px',
    background: '#1a73e8',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    cursor: 'pointer',
    fontWeight: 500,
  },
  userList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  tag: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 4,
    padding: '4px 10px',
    background: '#e8f0fe',
    color: '#1a73e8',
    borderRadius: 16,
    fontSize: 13,
  },
  removeBtn: {
    background: 'none',
    border: 'none',
    color: '#999',
    cursor: 'pointer',
    fontSize: 16,
    lineHeight: 1,
    padding: '0 2px',
  },
  startBtn: {
    width: '100%',
    padding: '10px 0',
    background: '#2ea043',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 16,
    fontWeight: 600,
    cursor: 'pointer',
  },
};

export default InputPanel;
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/InputPanel.tsx
git commit -m "feat: InputPanel component"
```

---

### Task 12: 前端 — ProgressPanel 组件

**Files:**
- Create: `frontend/src/components/ProgressPanel.tsx`

- [ ] **Step 1: 创建 ProgressPanel 组件**

```tsx
import { useEffect, useRef } from 'react';
import type { ProgressInfo } from '../types';

interface Props {
  progress: ProgressInfo;
  log: string[];
}

function ProgressPanel({ progress, log }: Props) {
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [log]);

  const done = progress.completed + progress.failed;
  const pct = progress.total > 0 ? Math.round((done / progress.total) * 100) : 0;

  return (
    <div style={styles.panel}>
      <div style={styles.status}>
        {progress.total === 0
          ? '等待开始...'
          : progress.done
            ? `完成: ${progress.completed} 成功, ${progress.failed} 失败`
            : `进行中: ${progress.completed} 完成, ${progress.failed} 失败, ${progress.total} 总数`}
      </div>
      <div style={styles.barWrapper}>
        <div style={styles.barTrack}>
          <div style={{ ...styles.barFill, width: `${pct}%` }} />
        </div>
        <span style={styles.barText}>{pct}%</span>
      </div>
      <div style={styles.logBox}>
        {log.length === 0 ? (
          <span style={styles.logPlaceholder}>日志将在此显示...</span>
        ) : (
          log.map((line, i) => (
            <div
              key={i}
              style={{
                ...styles.logLine,
                color: line.startsWith('[FAIL]') ? '#f44336' : '#333',
              }}
            >
              {line}
            </div>
          ))
        )}
        <div ref={logEndRef} />
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  panel: {
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
  },
  status: {
    fontSize: 15,
    fontWeight: 600,
    color: '#333',
  },
  barWrapper: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
  },
  barTrack: {
    flex: 1,
    height: 24,
    background: '#e8e8e8',
    borderRadius: 12,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    background: 'linear-gradient(90deg, #1a73e8, #4da3ff)',
    borderRadius: 12,
    transition: 'width 0.3s ease',
  },
  barText: {
    fontSize: 13,
    color: '#666',
    minWidth: 36,
    textAlign: 'right',
  },
  logBox: {
    maxHeight: 300,
    overflowY: 'auto',
    background: '#1e1e1e',
    borderRadius: 8,
    padding: 12,
    fontFamily: 'Consolas, "Courier New", monospace',
    fontSize: 13,
  },
  logLine: {
    lineHeight: 1.7,
    color: '#d4d4d4',
  },
  logPlaceholder: {
    color: '#666',
  },
};

export default ProgressPanel;
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/ProgressPanel.tsx
git commit -m "feat: ProgressPanel component with log viewer"
```

---

### Task 13: 前端 — RadarPanel 组件

**Files:**
- Create: `frontend/src/components/RadarPanel.tsx`

- [ ] **Step 1: 创建雷达图组件**

```tsx
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { Radar } from 'react-chartjs-2';
import type { RadarData } from '../types';

ChartJS.register(
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend
);

interface Props {
  data: RadarData[];
}

const COLORS = [
  'rgba(26, 115, 232, 0.7)',
  'rgba(244, 67, 54, 0.7)',
  'rgba(46, 160, 67, 0.7)',
  'rgba(255, 152, 0, 0.7)',
];

function RadarPanel({ data }: Props) {
  if (!data || data.length === 0) {
    return (
      <div style={styles.empty}>
        <p>雷达图将在爬取完成后显示</p>
        <p style={styles.hint}>添加用户并开始爬取，完成后自动生成能力对比雷达图</p>
      </div>
    );
  }

  const chartData = {
    labels: data[0].labels,
    datasets: data.map((d, i) => ({
      label: d.handle,
      data: d.values,
      backgroundColor: COLORS[i % COLORS.length].replace('0.7', '0.15'),
      borderColor: COLORS[i % COLORS.length],
      borderWidth: 2,
      pointBackgroundColor: COLORS[i % COLORS.length],
      pointRadius: 3,
    })),
  };

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    scales: {
      r: {
        beginAtZero: true,
        ticks: {
          stepSize: Math.max(1, Math.ceil(
            Math.max(...data.flatMap(d => d.values), 1) / 6
          )),
          font: { size: 11 },
        },
        pointLabels: {
          font: { size: 13 },
        },
      },
    },
    plugins: {
      legend: {
        position: 'bottom' as const,
      },
    },
  };

  return (
    <div style={styles.wrapper}>
      <Radar data={chartData} options={options} />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    maxWidth: 600,
    margin: '0 auto',
  },
  empty: {
    textAlign: 'center',
    padding: 60,
    color: '#999',
    fontSize: 15,
  },
  hint: {
    fontSize: 13,
    color: '#bbb',
    marginTop: 8,
  },
};

export default RadarPanel;
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/RadarPanel.tsx
git commit -m "feat: RadarPanel component with Chart.js radar chart"
```

---

### Task 14: 前端 — StatsPanel 组件

**Files:**
- Create: `frontend/src/components/StatsPanel.tsx`

- [ ] **Step 1: 创建统计表格组件**

```tsx
import type { UserStatsResponse } from '../types';

interface Props {
  results: UserStatsResponse[];
}

function StatsPanel({ results }: Props) {
  if (!results || results.length === 0) {
    return (
      <div style={styles.empty}>
        <p>统计数据将在爬取完成后显示</p>
      </div>
    );
  }

  return (
    <div style={styles.wrapper}>
      <table style={styles.table}>
        <thead>
          <tr>
            <th>用户</th>
            <th>平台</th>
            <th>Rating</th>
            <th>总提交</th>
            <th>AC 数</th>
            <th>通过率</th>
            <th>连续 AC</th>
          </tr>
        </thead>
        <tbody>
          {results.map(r => {
            if (r.failed) {
              return (
                <tr key={r.handle} style={styles.failedRow}>
                  <td>{r.handle}</td>
                  <td>{r.platform}</td>
                  <td colSpan={5} style={{ color: '#f44336' }}>
                    失败: {r.error}
                  </td>
                </tr>
              );
            }
            const s = r.stats!;
            const p = r.profile;
            return (
              <tr key={r.handle}>
                <td style={styles.handle}>{r.handle}</td>
                <td>{r.platform}</td>
                <td>{p?.rating ?? '-'}</td>
                <td>{s.totalSubmissions}</td>
                <td style={{ color: '#2ea043', fontWeight: 600 }}>
                  {s.acceptedCount}
                </td>
                <td>{s.acceptanceRate.toFixed(1)}%</td>
                <td>{s.maxStreak} 天</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: {
    overflowX: 'auto',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: 14,
  },
  empty: {
    textAlign: 'center',
    padding: 60,
    color: '#999',
    fontSize: 15,
  },
  handle: {
    fontWeight: 600,
    color: '#1a73e8',
  },
  failedRow: {
    background: '#fff5f5',
  },
};

export default StatsPanel;
```

表格需要加 th/td 的边框样式，放在 `index.css` 里：

`frontend/src/index.css` 追加：

```css
th {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 2px solid #e8e8e8;
  font-weight: 600;
  color: #555;
  font-size: 13px;
}

td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

tr:hover td {
  background: #fafafa;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/StatsPanel.tsx frontend/src/index.css
git commit -m "feat: StatsPanel component with results table"
```

---

### Task 15: 端到端验证

- [ ] **Step 1: 启动后端**

```bash
cd pachong && mvn spring-boot:run
```

Expected: 后端在 localhost:8080 启动，无报错

- [ ] **Step 2: 启动前端**

```bash
cd frontend && npm run dev
```

Expected: 前端在 localhost:5173 启动

- [ ] **Step 3: 浏览器测试完整流程**

1. 打开 http://localhost:5173
2. 输入用户名 `tourist`，平台选择 Codeforces，点添加
3. 再输入 `jiangly`，点添加
4. 点「开始爬取」
5. 观察进度条和日志滚动
6. 完成后切换到雷达图 Tab，确认雷达图显示
7. 切换到统计 Tab，确认表格数据正确

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: final adjustments after E2E testing"
```
