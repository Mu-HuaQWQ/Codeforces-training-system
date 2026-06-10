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
        List<Submission> submissions = List.of();

        if (!results.isEmpty()) {
            CrawlTask.CrawlResult r = results.get(0);
            if (r.isSuccess()) {
                profile = r.getProfile();
                submissions = r.getSubmissions();
                stats = analyzer.analyze(r.getHandle(), r.getPlatform(), submissions);

                // Save rating + contest data from CF/Luogu rating history API
                saveRatingAndContests(s, profile, submissions, platform);

                // Generate radar data
                List<Submission> acOnly = submissions.stream()
                    .filter(Submission::isAccepted).toList();
                Map<String, Integer> tagCount = analyzer.computeTagAcceptedCount(acOnly);
                radarData = analyzer.generateRadarData(r.getHandle(), tagCount);
            }
        }

        // Load history from DB
        List<RatingHistoryDto> ratingHistory = ratingHistoryRepo
            .findByStudentIdOrderByRecordedAtAsc(s.getId()).stream()
            .map(rh -> new RatingHistoryDto(rh.getId(), rh.getRating(), rh.getRecordedAt()))
            .toList();

        List<ContestRecordDto> contests = contestRecordRepo
            .findByStudentIdOrderByContestDateDesc(s.getId()).stream()
            .filter(cr -> cr.getOldRating() != null && cr.getNewRating() != null
                && !cr.getOldRating().equals(cr.getNewRating()))
            .map(cr -> new ContestRecordDto(
                cr.getId(), cr.getContestName(), cr.getRank(),
                cr.getOldRating(), cr.getNewRating(),
                cr.getNewRating() - cr.getOldRating(),
                cr.getSolvedCount(), cr.getContestDate()))
            .toList();

        return new StudentDetailDto(
            s.getId(), s.getName(), s.getHandle(), s.getPlatform(),
            profile, stats, radarData, ratingHistory, contests, s.getCreatedAt()
        );
    }

    private void saveRatingAndContests(Student student, UserProfile profile,
                                        List<Submission> submissions, Platform platform) {
        if (platform == Platform.CODEFORCES) {
            saveCFRatingAndContests(student, profile, submissions);
        } else {
            saveLuoguContests(student, submissions, profile);
        }
    }

    private void saveCFRatingAndContests(Student student, UserProfile profile,
                                          List<Submission> submissions) {
        try {
            var crawler = new com.pachong.crawler.CodeforcesCrawler();
            List<com.pachong.crawler.CodeforcesCrawler.CfrRatingChange> ratingChanges =
                crawler.fetchRatingHistory(student.getHandle());

            // Save rating history from CF data
            for (var rc : ratingChanges) {
                LocalDateTime time = LocalDateTime.ofEpochSecond(
                    rc.timestamp(), 0, java.time.ZoneOffset.ofHours(8));
                ratingHistoryRepo.save(new RatingHistory(student, rc.newRating(), time));
            }

            // Save contest records with solved counts
            // Build a map: contestId -> list of AC'd problem indices
            Map<Integer, Set<String>> contestAcMap = new java.util.HashMap<>();
            for (Submission sub : submissions) {
                if (!sub.isAccepted()) continue;
                String pid = sub.getProblemId(); // e.g. "2229H"
                // Extract contestId from problemId (leading digits)
                String digits = pid.replaceAll("^(\\d+).*", "$1");
                if (digits.isEmpty()) continue;
                try {
                    int cid = Integer.parseInt(digits);
                    String index = pid.substring(digits.length());
                    contestAcMap.computeIfAbsent(cid, k -> new java.util.HashSet<>()).add(index);
                } catch (NumberFormatException ignored) {}
            }

            for (var rc : ratingChanges) {
                String contestIdStr = String.valueOf(rc.contestId());
                // Skip if already saved
                if (contestRecordRepo.existsByStudentIdAndContestName(student.getId(), rc.contestName())) {
                    continue;
                }
                Set<String> acSet = contestAcMap.getOrDefault(rc.contestId(), Set.of());
                LocalDateTime time = LocalDateTime.ofEpochSecond(
                    rc.timestamp(), 0, java.time.ZoneOffset.ofHours(8));
                contestRecordRepo.save(new ContestRecord(
                    student, rc.contestName(), contestIdStr,
                    rc.rank(), rc.oldRating(), rc.newRating(),
                    acSet.size(), time));
            }
        } catch (Exception e) {
            // Fallback: save current rating snapshot
            if (profile != null && profile.getRating() != null) {
                RatingHistory last = ratingHistoryRepo.findTopByStudentIdOrderByRecordedAtDesc(student.getId());
                if (last == null || !last.getRating().equals(profile.getRating())) {
                    ratingHistoryRepo.save(new RatingHistory(student, profile.getRating(), LocalDateTime.now()));
                }
            }
            // Save basic contest records from submissions
            saveLuoguContests(student, submissions, profile);
        }
    }

    private void saveLuoguContests(Student student, List<Submission> submissions, UserProfile profile) {
        Set<String> seen = new java.util.HashSet<>();
        for (Submission sub : submissions) {
            String name = sub.getContestName();
            if (name == null || name.isBlank()) continue;
            if (!seen.add(name)) continue;
            if (contestRecordRepo.existsByStudentIdAndContestName(student.getId(), name)) continue;

            contestRecordRepo.save(new ContestRecord(
                student, name, null, null,
                profile != null ? profile.getRating() : null,
                profile != null ? profile.getRating() : null,
                null,
                sub.getSubmissionTime() > 0
                    ? LocalDateTime.ofEpochSecond(sub.getSubmissionTime(), 0,
                        java.time.ZoneOffset.ofHours(8))
                    : LocalDateTime.now()));
        }

        // Save current rating if no history exists
        if (profile != null && profile.getRating() != null) {
            RatingHistory last = ratingHistoryRepo.findTopByStudentIdOrderByRecordedAtDesc(student.getId());
            if (last == null || !last.getRating().equals(profile.getRating())) {
                ratingHistoryRepo.save(new RatingHistory(student, profile.getRating(), LocalDateTime.now()));
            }
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
        Integer oldRating, Integer newRating, Integer ratingChange,
        Integer solvedCount, LocalDateTime contestDate
    ) {}
}
