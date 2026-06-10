# Training Management System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the existing CP Crawler into a student training management system with persistent storage, student CRUD, detail pages with rating history, contest results, and radar charts.

**Architecture:** Add H2 database + JPA entities for student/rating/contest persistence. New StudentController + StudentService for CRUD. Enhanced CrawlService auto-saves rating snapshots and contest records. Frontend adds react-router with student list and detail pages.

**Tech Stack:** Spring Boot 3.3.5, Java 23, H2, Spring Data JPA, React 19, Vite 8, TypeScript, Chart.js, react-chartjs-2, react-router-dom

---

### Task 1: Add H2 + JPA dependencies

**Files:**
- Modify: `E:/program/java/pachong/pom.xml`

- [ ] **Step 1: Add spring-boot-starter-data-jpa and h2 to pom.xml**

Add inside `<dependencies>`, after the existing `spring-boot-starter-web`:

```xml
<!-- Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: Configure H2 in application.properties**

Append to `E:/program/java/pachong/src/main/resources/application.properties`:

```properties
# H2 Database
spring.datasource.url=jdbc:h2:file:./data/h2/training;AUTO_SERVER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- [ ] **Step 3: Verify build**

Run: `cd E:/program/java/pachong && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: Create JPA Entities

**Files:**
- Create: `E:/program/java/pachong/src/main/java/com/pachong/entity/Student.java`
- Create: `E:/program/java/pachong/src/main/java/com/pachong/entity/RatingHistory.java`
- Create: `E:/program/java/pachong/src/main/java/com/pachong/entity/ContestRecord.java`

- [ ] **Step 1: Create Student entity**

```java
package com.pachong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"handle", "platform"})
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String handle;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Student() {}

    public Student(String name, String handle, String platform) {
        this.name = name;
        this.handle = handle;
        this.platform = platform;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Create RatingHistory entity**

```java
package com.pachong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rating_history")
public class RatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public RatingHistory() {}

    public RatingHistory(Student student, Integer rating, LocalDateTime recordedAt) {
        this.student = student;
        this.rating = rating;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
```

- [ ] **Step 3: Create ContestRecord entity**

```java
package com.pachong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest_record")
public class ContestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "contest_name", length = 500)
    private String contestName;

    @Column(name = "contest_id", length = 100)
    private String contestId;

    @Column(name = "contest_rank")
    private Integer rank;

    @Column(name = "old_rating")
    private Integer oldRating;

    @Column(name = "new_rating")
    private Integer newRating;

    @Column(name = "contest_date")
    private LocalDateTime contestDate;

    public ContestRecord() {}

    public ContestRecord(Student student, String contestName, String contestId,
                         Integer rank, Integer oldRating, Integer newRating,
                         LocalDateTime contestDate) {
        this.student = student;
        this.contestName = contestName;
        this.contestId = contestId;
        this.rank = rank;
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.contestDate = contestDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getContestName() { return contestName; }
    public void setContestName(String contestName) { this.contestName = contestName; }
    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public Integer getOldRating() { return oldRating; }
    public void setOldRating(Integer oldRating) { this.oldRating = oldRating; }
    public Integer getNewRating() { return newRating; }
    public void setNewRating(Integer newRating) { this.newRating = newRating; }
    public LocalDateTime getContestDate() { return contestDate; }
    public void setContestDate(LocalDateTime contestDate) { this.contestDate = contestDate; }
}
```

- [ ] **Step 4: Verify compile**

Run: `cd E:/program/java/pachong && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: Create JPA Repositories

**Files:**
- Create: `E:/program/java/pachong/src/main/java/com/pachong/repository/StudentRepository.java`
- Create: `E:/program/java/pachong/src/main/java/com/pachong/repository/RatingHistoryRepository.java`
- Create: `E:/program/java/pachong/src/main/java/com/pachong/repository/ContestRecordRepository.java`

- [ ] **Step 1: Create StudentRepository**

```java
package com.pachong.repository;

import com.pachong.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByHandleAndPlatform(String handle, String platform);
    boolean existsByHandleAndPlatform(String handle, String platform);
}
```

- [ ] **Step 2: Create RatingHistoryRepository**

```java
package com.pachong.repository;

import com.pachong.entity.RatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {
    List<RatingHistory> findByStudentIdOrderByRecordedAtAsc(Long studentId);
    RatingHistory findTopByStudentIdOrderByRecordedAtDesc(Long studentId);
}
```

- [ ] **Step 3: Create ContestRecordRepository**

```java
package com.pachong.repository;

import com.pachong.entity.ContestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContestRecordRepository extends JpaRepository<ContestRecord, Long> {
    List<ContestRecord> findByStudentIdOrderByContestDateDesc(Long studentId);
}
```

- [ ] **Step 4: Verify compile**

Run: `cd E:/program/java/pachong && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 4: Create StudentService

**Files:**
- Create: `E:/program/java/pachong/src/main/java/com/pachong/service/StudentService.java`

- [ ] **Step 1: Create StudentService with DTOs and business logic**

```java
package com.pachong.service;

import com.pachong.analysis.SubmissionAnalyzer;
import com.pachong.concurrent.CrawlTask;
import com.pachong.concurrent.CrawlerOrchestrator;
import com.pachong.entity.*;
import com.pachong.model.*;
import com.pachong.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepo;
    private final RatingHistoryRepository ratingHistoryRepo;
    private final ContestRecordRepository contestRecordRepo;
    private final SubmissionAnalyzer analyzer = new SubmissionAnalyzer();

    public StudentService(StudentRepository studentRepo,
                          RatingHistoryRepository ratingHistoryRepo,
                          ContestRecordRepository contestRecordRepo) {
        this.studentRepo = studentRepo;
        this.ratingHistoryRepo = ratingHistoryRepo;
        this.contestRecordRepo = contestRecordRepo;
    }

    // === Student CRUD ===

    public List<StudentDto> getAllStudents() {
        List<StudentDto> result = new ArrayList<>();
        for (Student s : studentRepo.findAll()) {
            RatingHistory latest = ratingHistoryRepo.findTopByStudentIdOrderByRecordedAtDesc(s.getId());
            Integer currentRating = latest != null ? latest.getRating() : null;
            result.add(new StudentDto(
                s.getId(), s.getName(), s.getHandle(), s.getPlatform(),
                currentRating, 0, s.getCreatedAt()
            ));
        }
        return result;
    }

    public StudentDto addStudent(String name, String handle, String platform) {
        if (studentRepo.existsByHandleAndPlatform(handle, platform)) {
            throw new IllegalArgumentException("该用户已存在");
        }
        Student s = studentRepo.save(new Student(name, handle, platform));
        return new StudentDto(s.getId(), s.getName(), s.getHandle(), s.getPlatform(),
            null, 0, s.getCreatedAt());
    }

    public void deleteStudent(Long id) {
        studentRepo.deleteById(id);
    }

    // === Student Detail (with fresh crawl) ===

    @Transactional
    public StudentDetailDto getStudentDetail(Long id) {
        Student s = studentRepo.findById(id)
            .orElseThrow(() -> new NoSuchElementException("学生不存在: " + id));

        Platform platform = Platform.valueOf(s.getPlatform());

        // Crawl fresh data
        CrawlerOrchestrator orchestrator = new CrawlerOrchestrator();
        orchestrator.addUser(s.getHandle(), platform);
        List<CrawlTask.CrawlResult> results = orchestrator.startCrawl();

        UserProfile profile = null;
        UserStatistics stats = null;
        List<RadarData> radarData = List.of();

        if (!results.isEmpty()) {
            CrawlTask.CrawlResult r = results.get(0);
            if (r.isSuccess()) {
                profile = r.getProfile();
                stats = analyzer.analyze(r.getHandle(), r.getPlatform(), r.getSubmissions());

                // Save rating snapshot
                if (profile != null && profile.getRating() != null) {
                    ratingHistoryRepo.save(new RatingHistory(s, profile.getRating(), LocalDateTime.now()));
                }

                // Generate radar data
                List<Submission> acOnly = r.getSubmissions().stream()
                    .filter(Submission::isAccepted).toList();
                Map<String, Integer> tagCount = analyzer.computeTagAcceptedCount(acOnly);
                radarData = analyzer.generateRadarData(r.getHandle(), tagCount);

                // Save contest records (from submission contest names)
                saveContestRecords(s, r.getSubmissions(), profile);
            }
        }

        // Load history from DB
        List<RatingHistoryDto> ratingHistory = ratingHistoryRepo
            .findByStudentIdOrderByRecordedAtAsc(s.getId()).stream()
            .map(rh -> new RatingHistoryDto(rh.getId(), rh.getRating(), rh.getRecordedAt()))
            .toList();

        List<ContestRecordDto> contests = contestRecordRepo
            .findByStudentIdOrderByContestDateDesc(s.getId()).stream()
            .map(cr -> new ContestRecordDto(
                cr.getId(), cr.getContestName(), cr.getRank(),
                cr.getOldRating(), cr.getNewRating(), cr.getContestDate()))
            .toList();

        return new StudentDetailDto(
            s.getId(), s.getName(), s.getHandle(), s.getPlatform(),
            profile, stats, radarData, ratingHistory, contests, s.getCreatedAt()
        );
    }

    private void saveContestRecords(Student student, List<Submission> submissions, UserProfile profile) {
        Set<String> seenContests = new HashSet<>();
        for (Submission sub : submissions) {
            String contestName = sub.getContestName();
            if (contestName == null || contestName.isBlank()) continue;
            if (!seenContests.add(contestName)) continue;

            ContestRecord cr = new ContestRecord(
                student, contestName, null, null,
                profile != null ? profile.getRating() : null,
                profile != null ? profile.getRating() : null,
                sub.getSubmissionTime() > 0
                    ? LocalDateTime.ofEpochSecond(sub.getSubmissionTime(), 0,
                        java.time.ZoneOffset.ofHours(8))
                    : LocalDateTime.now()
            );
            contestRecordRepo.save(cr);
        }
    }

    // === Refresh all students ===

    public int refreshAllStudents() {
        List<Student> all = studentRepo.findAll();
        int count = 0;
        for (Student s : all) {
            try {
                getStudentDetail(s.getId());
                count++;
            } catch (Exception e) {
                // log and continue
            }
        }
        return count;
    }

    // === DTOs ===

    public record StudentDto(
        Long id, String name, String handle, String platform,
        Integer currentRating, int weeklyAcCount, LocalDateTime createdAt
    ) {}

    public record StudentDetailDto(
        Long id, String name, String handle, String platform,
        UserProfile profile, UserStatistics stats,
        List<RadarData> radarData,
        List<RatingHistoryDto> ratingHistory,
        List<ContestRecordDto> contests,
        LocalDateTime createdAt
    ) {}

    public record RatingHistoryDto(Long id, Integer rating, LocalDateTime recordedAt) {}

    public record ContestRecordDto(
        Long id, String contestName, Integer rank,
        Integer oldRating, Integer newRating, LocalDateTime contestDate
    ) {}
}
```

- [ ] **Step 2: Verify compile**

Run: `cd E:/program/java/pachong && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 5: Create StudentController

**Files:**
- Create: `E:/program/java/pachong/src/main/java/com/pachong/controller/StudentController.java`

- [ ] **Step 1: Create StudentController**

```java
package com.pachong.controller;

import com.pachong.service.StudentService;
import com.pachong.service.StudentService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    public List<StudentDto> listStudents() {
        return studentService.getAllStudents();
    }

    @PostMapping("/students")
    public ResponseEntity<StudentDto> addStudent(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String handle = body.get("handle");
        String platform = body.get("platform");
        if (name == null || handle == null || platform == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(studentService.addStudent(name, handle, platform));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(studentService.getStudentDetail(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/crawl/refresh-all")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        int count = studentService.refreshAllStudents();
        return ResponseEntity.ok(Map.of("refreshed", count));
    }
}
```

- [ ] **Step 2: Verify full build**

Run: `cd E:/program/java/pachong && mvn package -DskipTests -q`
Expected: BUILD SUCCESS

---

### Task 6: Backend startup verification

- [ ] **Step 1: Start backend and verify H2 tables auto-created + API responds**

Run: `cd E:/program/java/pachong && mvn spring-boot:run -q`

Wait for startup, then test:

```bash
# Test adding a student
curl -s -X POST http://localhost:8080/api/students \
  -H 'Content-Type: application/json' \
  -d '{"name":"测试","handle":"tourist","platform":"CODEFORCES"}'

# Test listing students
curl -s http://localhost:8080/api/students

# Test student detail (will trigger crawl)
curl -s http://localhost:8080/api/students/1
```

Expected: Student added with 200, list returns array, detail returns full data after crawling.

---

### Task 7: Install react-router-dom and update frontend types

**Files:**
- Modify: `E:/program/java/frontend/src/types/index.ts`

- [ ] **Step 1: Install react-router-dom**

```bash
cd E:/program/java/frontend && npm install react-router-dom
```

- [ ] **Step 2: Add new types to types/index.ts**

Append to `E:/program/java/frontend/src/types/index.ts`:

```typescript
export interface Student {
  id: number;
  name: string;
  handle: string;
  platform: string;
  currentRating: number | null;
  weeklyAcCount: number;
  createdAt: string;
}

export interface RatingRecord {
  id: number;
  rating: number;
  recordedAt: string;
}

export interface ContestRecord {
  id: number;
  contestName: string;
  rank: number | null;
  oldRating: number;
  newRating: number;
  contestDate: string;
}

export interface StudentDetail {
  id: number;
  name: string;
  handle: string;
  platform: string;
  profile: UserProfile | null;
  stats: UserStatistics | null;
  radarData: RadarData[];
  ratingHistory: RatingRecord[];
  contests: ContestRecord[];
  createdAt: string;
}
```

---

### Task 8: Add new API functions

**Files:**
- Modify: `E:/program/java/frontend/src/api/index.ts`

- [ ] **Step 1: Append API functions**

Append to `E:/program/java/frontend/src/api/index.ts`:

```typescript
import type { Student, StudentDetail, RatingRecord, ContestRecord } from '../types';

export function getStudents(): Promise<Student[]> {
  return request('/students');
}

export function addStudent(data: { name: string; handle: string; platform: string }): Promise<Student> {
  return request('/students', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function deleteStudent(id: number): Promise<void> {
  return request(`/students/${id}`, { method: 'DELETE' });
}

export function getStudentDetail(id: number): Promise<StudentDetail> {
  return request(`/students/${id}`);
}

export function refreshAll(): Promise<{ refreshed: number }> {
  return request('/crawl/refresh-all', { method: 'POST' });
}
```

---

### Task 9: Create StudentList page component

**Files:**
- Create: `E:/program/java/frontend/src/pages/StudentList.tsx`

- [ ] **Step 1: Create StudentList.tsx**

```tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Student } from '../types';
import { getStudents, deleteStudent, refreshAll } from '../api';

interface Props {
  onAddClick: () => void;
  refreshKey: number;
}

function StudentList({ onAddClick, refreshKey }: Props) {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getStudents().then(setStudents).catch(console.error);
  }, [refreshKey]);

  const handleRefreshAll = async () => {
    setLoading(true);
    try {
      await refreshAll();
      const list = await getStudents();
      setStudents(list);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`确定删除 ${name}？`)) return;
    await deleteStudent(id);
    setStudents(prev => prev.filter(s => s.id !== id));
  };

  return (
    <div style={styles.wrapper}>
      <div style={styles.toolbar}>
        <button style={styles.addBtn} onClick={onAddClick}>+ 添加学生</button>
        <button style={styles.refreshBtn} onClick={handleRefreshAll} disabled={loading}>
          {loading ? '更新中...' : '🔄 更新全部数据'}
        </button>
      </div>
      {students.length === 0 ? (
        <div style={styles.empty}>
          <p>暂无学生，点击「添加学生」开始</p>
        </div>
      ) : (
        <table style={styles.table}>
          <thead>
            <tr>
              <th>姓名</th>
              <th>用户名</th>
              <th>平台</th>
              <th>当前分数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {students.map(s => (
              <tr key={s.id}>
                <td style={styles.nameCell}>{s.name}</td>
                <td style={styles.mono}>{s.handle}</td>
                <td>
                  <span style={s.platform === 'CODEFORCES' ? styles.cfBadge : styles.lgBadge}>
                    {s.platform === 'CODEFORCES' ? 'CF' : 'LG'}
                  </span>
                </td>
                <td style={styles.rating}>{s.currentRating ?? '-'}</td>
                <td>
                  <button style={styles.detailBtn} onClick={() => navigate(`/${s.id}`)}>详情</button>
                  <button style={styles.delBtn} onClick={() => handleDelete(s.id, s.name)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { background: '#fff', borderRadius: 8, padding: 20, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' },
  toolbar: { display: 'flex', gap: 12, marginBottom: 16 },
  addBtn: { padding: '8px 20px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer', fontWeight: 500 },
  refreshBtn: { padding: '8px 20px', background: '#fff', color: '#1a73e8', border: '1px solid #1a73e8', borderRadius: 6, fontSize: 14, cursor: 'pointer' },
  empty: { textAlign: 'center', padding: 60, color: '#999', fontSize: 16 },
  table: { width: '100%', borderCollapse: 'collapse' },
  nameCell: { fontWeight: 600, color: '#1a73e8' },
  mono: { fontFamily: 'monospace', fontSize: 14 },
  rating: { fontWeight: 600, color: '#2ea043' },
  cfBadge: { padding: '2px 8px', borderRadius: 4, background: '#fff0e6', color: '#e67e22', fontSize: 12, fontWeight: 600 },
  lgBadge: { padding: '2px 8px', borderRadius: 4, background: '#e8f0fe', color: '#1a73e8', fontSize: 12, fontWeight: 600 },
  detailBtn: { padding: '4px 12px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 4, fontSize: 13, cursor: 'pointer', marginRight: 8 },
  delBtn: { padding: '4px 12px', background: '#fff', color: '#e74c3c', border: '1px solid #e74c3c', borderRadius: 4, fontSize: 13, cursor: 'pointer' },
};

export default StudentList;
```

---

### Task 10: Create AddStudentModal component

**Files:**
- Create: `E:/program/java/frontend/src/components/AddStudentModal.tsx`

- [ ] **Step 1: Create AddStudentModal.tsx**

```tsx
import { useState } from 'react';
import type { Platform } from '../types';

interface Props {
  onClose: () => void;
  onAdded: () => void;
}

function AddStudentModal({ onClose, onAdded }: Props) {
  const [name, setName] = useState('');
  const [handle, setHandle] = useState('');
  const [platform, setPlatform] = useState<Platform>('CODEFORCES');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async () => {
    const trimmedName = name.trim();
    const trimmedHandle = handle.trim();
    if (!trimmedName || !trimmedHandle) {
      setError('请填写所有字段');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const { addStudent } = await import('../api');
      await addStudent({ name: trimmedName, handle: trimmedHandle, platform });
      onAdded();
    } catch (e) {
      setError('添加失败，可能该用户已存在');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.modal} onClick={e => e.stopPropagation()}>
        <h3 style={styles.title}>添加学生</h3>
        {error && <div style={styles.error}>{error}</div>}
        <div style={styles.field}>
          <label style={styles.label}>学生姓名</label>
          <input style={styles.input} placeholder="如：张三" value={name}
            onChange={e => setName(e.target.value)} />
        </div>
        <div style={styles.field}>
          <label style={styles.label}>用户名</label>
          <input style={styles.input} placeholder="如：tourist" value={handle}
            onChange={e => setHandle(e.target.value)} />
        </div>
        <div style={styles.field}>
          <label style={styles.label}>平台</label>
          <select style={styles.select} value={platform}
            onChange={e => setPlatform(e.target.value as Platform)}>
            <option value="CODEFORCES">Codeforces</option>
            <option value="LUOGU">Luogu</option>
          </select>
        </div>
        <div style={styles.actions}>
          <button style={styles.cancelBtn} onClick={onClose}>取消</button>
          <button style={styles.submitBtn} onClick={submit} disabled={submitting}>
            {submitting ? '添加中...' : '确认添加'}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 },
  modal: { background: '#fff', borderRadius: 12, padding: 28, width: 400, boxShadow: '0 8px 30px rgba(0,0,0,0.15)' },
  title: { fontSize: 18, fontWeight: 600, marginBottom: 20, color: '#333' },
  error: { padding: '8px 12px', background: '#fff5f5', color: '#e74c3c', borderRadius: 6, marginBottom: 12, fontSize: 13 },
  field: { marginBottom: 16 },
  label: { display: 'block', fontSize: 13, fontWeight: 500, color: '#666', marginBottom: 6 },
  input: { width: '100%', padding: '8px 12px', border: '1px solid #d9d9d9', borderRadius: 6, fontSize: 14, outline: 'none', boxSizing: 'border-box' },
  select: { width: '100%', padding: '8px 12px', border: '1px solid #d9d9d9', borderRadius: 6, fontSize: 14, background: '#fff', boxSizing: 'border-box' },
  actions: { display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 24 },
  cancelBtn: { padding: '8px 20px', background: '#f5f5f5', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' },
  submitBtn: { padding: '8px 20px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer', fontWeight: 500 },
};

export default AddStudentModal;
```

---

### Task 11: Create RatingChart component

**Files:**
- Create: `E:/program/java/frontend/src/components/RatingChart.tsx`

- [ ] **Step 1: Create RatingChart.tsx using Chart.js Line**

```tsx
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  Tooltip, Legend, Filler,
} from 'chart.js';
import type { RatingRecord } from '../types';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

interface Props {
  data: RatingRecord[];
}

function RatingChart({ data }: Props) {
  if (!data || data.length === 0) {
    return <div style={styles.empty}>暂无 rating 历史数据</div>;
  }

  const chartData = {
    labels: data.map(r => {
      const d = new Date(r.recordedAt);
      return `${d.getMonth() + 1}/${d.getDate()}`;
    }),
    datasets: [{
      label: 'Rating',
      data: data.map(r => r.rating),
      fill: true,
      borderColor: '#1a73e8',
      backgroundColor: 'rgba(26, 115, 232, 0.1)',
      tension: 0.3,
      pointRadius: 3,
      pointBackgroundColor: '#1a73e8',
    }],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { min: Math.min(...data.map(r => r.rating)) - 100 },
    },
  };

  return (
    <div style={styles.wrapper}>
      <div style={{ height: 260 }}>
        <Line data={chartData} options={options} />
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { background: '#fff', borderRadius: 8, padding: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' },
  empty: { textAlign: 'center', padding: 40, color: '#999' },
};

export default RatingChart;
```

---

### Task 12: Create StudentDetail page

**Files:**
- Create: `E:/program/java/frontend/src/pages/StudentDetail.tsx`

- [ ] **Step 1: Create StudentDetail.tsx**

```tsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import type { StudentDetail as StudentDetailType } from '../types';
import { getStudentDetail } from '../api';
import RadarPanel from '../components/RadarPanel';
import StatsPanel from '../components/StatsPanel';
import RatingChart from '../components/RatingChart';

function StudentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<StudentDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState<'contests' | 'radar' | 'stats'>('contests');

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getStudentDetail(Number(id))
      .then(d => { setDetail(d); setLoading(false); })
      .catch(e => { setError('加载失败: ' + e); setLoading(false); });
  }, [id]);

  if (loading) return <div style={styles.center}>加载中...</div>;
  if (error) return <div style={{ ...styles.center, color: '#e74c3c' }}>{error}</div>;
  if (!detail) return <div style={styles.center}>无数据</div>;

  const tabs = [
    { key: 'contests' as const, label: '最近比赛' },
    { key: 'radar' as const, label: '雷达图' },
    { key: 'stats' as const, label: '统计' },
  ];

  return (
    <div style={styles.wrapper}>
      <button style={styles.backBtn} onClick={() => navigate('/')}>← 返回</button>

      {/* Header */}
      <div style={styles.header}>
        <h2 style={styles.name}>{detail.name}</h2>
        <span style={styles.handle}>{detail.handle}</span>
        <span style={detail.platform === 'CODEFORCES' ? styles.cfBadge : styles.lgBadge}>
          {detail.platform === 'CODEFORCES' ? 'Codeforces' : 'Luogu'}
        </span>
        {detail.profile && (
          <span style={styles.rating}>
            Rating: {detail.profile.rating ?? '-'} (max {detail.profile.maxRating ?? '-'})
          </span>
        )}
      </div>

      {/* Rating Chart */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>Rating 变化</h3>
        <RatingChart data={detail.ratingHistory} />
      </div>

      {/* Tabs */}
      <div style={styles.tabBar}>
        {tabs.map(t => (
          <button key={t.key}
            style={{ ...styles.tab, ...(tab === t.key ? styles.tabActive : {}) }}
            onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'contests' && (
        <div style={styles.section}>
          {detail.contests.length === 0 ? (
            <div style={styles.emptyText}>暂无比赛记录</div>
          ) : (
            <table style={styles.table}>
              <thead>
                <tr><th>比赛名称</th><th>日期</th></tr>
              </thead>
              <tbody>
                {detail.contests.map(c => (
                  <tr key={c.id}>
                    <td>{c.contestName}</td>
                    <td>{new Date(c.contestDate).toLocaleDateString('zh-CN')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {tab === 'radar' && (
        <div style={styles.section}>
          {detail.radarData.length > 0 ? (
            <RadarPanel data={detail.radarData} />
          ) : (
            <div style={styles.emptyText}>暂无雷达图数据</div>
          )}
        </div>
      )}

      {tab === 'stats' && detail.stats && (
        <div style={styles.section}>
          <StatsPanel results={[{
            handle: detail.handle, platform: detail.platform,
            profile: detail.profile, stats: detail.stats,
            failed: false, error: null,
          }]} />
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrapper: { maxWidth: 900, margin: '0 auto' },
  center: { textAlign: 'center', padding: 60 },
  backBtn: { padding: '6px 16px', background: '#f5f5f5', border: 'none', borderRadius: 6, cursor: 'pointer', marginBottom: 16, fontSize: 14 },
  header: { display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginBottom: 20 },
  name: { fontSize: 24, fontWeight: 700, color: '#333' },
  handle: { fontFamily: 'monospace', fontSize: 15, color: '#666' },
  rating: { fontWeight: 600, color: '#2ea043', fontSize: 15 },
  cfBadge: { padding: '2px 10px', borderRadius: 4, background: '#fff0e6', color: '#e67e22', fontSize: 13, fontWeight: 600 },
  lgBadge: { padding: '2px 10px', borderRadius: 4, background: '#e8f0fe', color: '#1a73e8', fontSize: 13, fontWeight: 600 },
  section: { background: '#fff', borderRadius: 8, padding: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: 16 },
  sectionTitle: { fontSize: 16, fontWeight: 600, marginBottom: 12, color: '#333' },
  tabBar: { display: 'flex', gap: 4, marginBottom: 16 },
  tab: { padding: '8px 20px', background: '#f5f5f5', border: 'none', borderRadius: '6px 6px 0 0', cursor: 'pointer', fontSize: 14 },
  tabActive: { background: '#fff', color: '#1a73e8', fontWeight: 600 },
  table: { width: '100%', borderCollapse: 'collapse' },
  emptyText: { textAlign: 'center', padding: 40, color: '#999' },
};

export default StudentDetail;
```

---

### Task 13: Rewrite App.tsx with routing

**Files:**
- Modify: `E:/program/java/frontend/src/App.tsx`

- [ ] **Step 1: Replace App.tsx with routing-based version**

```tsx
import { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import StudentList from './pages/StudentList';
import StudentDetail from './pages/StudentDetail';
import AddStudentModal from './components/AddStudentModal';
import './App.css';

function App() {
  const [showAddModal, setShowAddModal] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleAdded = () => {
    setShowAddModal(false);
    setRefreshKey(k => k + 1);
  };

  return (
    <BrowserRouter>
      <div className="app">
        <Header />
        <Routes>
          <Route path="/" element={
            <StudentList
              onAddClick={() => setShowAddModal(true)}
              refreshKey={refreshKey}
            />
          } />
          <Route path="/:id" element={<StudentDetail />} />
        </Routes>
        {showAddModal && (
          <AddStudentModal onClose={() => setShowAddModal(false)} onAdded={handleAdded} />
        )}
      </div>
    </BrowserRouter>
  );
}

export default App;
```

---

### Task 14: Update Header title

**Files:**
- Modify: `E:/program/java/frontend/src/components/Header.tsx`

- [ ] **Step 1: Change header content to show training management system**

Edit `Header.tsx` — replace the title and subtitle:

Old:
```tsx
<h1 style={styles.title}>CP Crawler</h1>
<p style={styles.subtitle}>Codeforces & Luogu 竞赛数据分析</p>
```

New:
```tsx
<h1 style={styles.title}>训练管理系统</h1>
<p style={styles.subtitle}>Codeforces & Luogu 学生训练追踪</p>
```

- [ ] **Step 2: Verify frontend compiles**

Run: `cd E:/program/java/frontend && npx tsc --noEmit 2>&1 | head -20`
Expected: No errors

---

### Task 15: End-to-end integration test

- [ ] **Step 1: Start both services**

```bash
# Terminal 1: Backend
cd E:/program/java/pachong && mvn spring-boot:run -q

# Terminal 2: Frontend
cd E:/program/java/frontend && npx vite --host
```

- [ ] **Step 2: Manual test flow**

1. Open `http://localhost:5173` — should see "训练管理系统" header and empty student list
2. Click "+ 添加学生" — modal appears
3. Enter name "测试", handle "tourist", platform "Codeforces"
4. Click "确认添加" — student appears in list
5. Click "详情" — navigates to detail page, triggers crawl
6. Verify rating chart, contest records, radar chart, stats tabs all work
7. Click "← 返回" — back to list
8. Click "删除" — confirm, student removed

---

### Task 16: Commit

- [ ] **Step 1: Commit all changes**

```bash
git add -A
git commit -m "feat: transform CP crawler into training management system

- Add H2 database with JPA entities (Student, RatingHistory, ContestRecord)
- Add Student CRUD API with auto-crawl on detail view
- Add rating history tracking and contest record saving
- Add refresh-all endpoint for batch student updates
- Rewrite frontend as training management system with routing
- Add StudentList, AddStudentModal, StudentDetail, RatingChart components
- Preserve existing crawler and radar chart functionality"
```
