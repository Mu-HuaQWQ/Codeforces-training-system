package com.pachong.concurrent;

import com.pachong.crawler.CrawlerException;
import com.pachong.crawler.CrawlerStrategy;
import com.pachong.model.Platform;
import com.pachong.model.Submission;
import com.pachong.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 爬取任务 — 封装单次爬取操作
 */
public class CrawlTask implements Callable<CrawlTask.CrawlResult> {

    private final String handle;
    private final CrawlerStrategy strategy;
    private final RateLimiter rateLimiter;
    private final boolean fetchProfile;

    public CrawlTask(String handle, CrawlerStrategy strategy, RateLimiter rateLimiter,
                     boolean fetchProfile) {
        this.handle = handle;
        this.strategy = strategy;
        this.rateLimiter = rateLimiter;
        this.fetchProfile = fetchProfile;
    }

    @Override
    public CrawlResult call() throws Exception {
        Platform platform = strategy.getPlatform();

        // 限流等待
        rateLimiter.acquire(platform);

        CrawlResult result = new CrawlResult(handle, platform);

        try {
            // 爬取用户信息
            if (fetchProfile) {
                result.profile = strategy.fetchUserProfile(handle);
            }

            // 爬取提交记录
            result.submissions = strategy.fetchSubmissions(handle);
            result.success = true;
        } catch (CrawlerException e) {
            result.success = false;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    public String getHandle() {
        return handle;
    }

    public Platform getPlatform() {
        return strategy.getPlatform();
    }

    // === 结果类 ===

    public static class CrawlResult {
        private final String handle;
        private final Platform platform;
        private boolean success;
        private UserProfile profile;
        private List<Submission> submissions;
        private String errorMessage;

        public CrawlResult(String handle, Platform platform) {
            this.handle = handle;
            this.platform = platform;
        }

        public String getHandle() { return handle; }
        public Platform getPlatform() { return platform; }
        public boolean isSuccess() { return success; }
        public UserProfile getProfile() { return profile; }
        public List<Submission> getSubmissions() { return submissions; }
        public String getErrorMessage() { return errorMessage; }

        public int getSubmissionCount() {
            return submissions != null ? submissions.size() : 0;
        }
    }
}
