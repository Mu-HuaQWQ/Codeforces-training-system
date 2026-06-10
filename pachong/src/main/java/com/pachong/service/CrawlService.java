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
