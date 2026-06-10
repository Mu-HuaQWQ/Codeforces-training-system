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
