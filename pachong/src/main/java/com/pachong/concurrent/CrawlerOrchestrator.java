package com.pachong.concurrent;

import com.pachong.crawler.CodeforcesCrawler;
import com.pachong.crawler.CrawlerStrategy;
import com.pachong.crawler.LuoguCrawler;
import com.pachong.model.Platform;
import com.pachong.model.Submission;
import com.pachong.model.UserProfile;
import com.pachong.storage.DataRepository;
import com.pachong.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 爬虫编排器 — 组装生产者-消费者模式
 *
 * 使用方式:
 * <pre>
 *   CrawlerOrchestrator orch = new CrawlerOrchestrator();
 *   orch.addUser("tourist", Platform.CODEFORCES);
 *   orch.addUser("1000", Platform.LUOGU);
 *   List<CrawlTask.CrawlResult> results = orch.startCrawl();
 * </pre>
 */
public class CrawlerOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CrawlerOrchestrator.class);

    private final ConfigLoader config;
    private final RateLimiter rateLimiter;
    private final DataRepository repository;
    private final Map<Platform, CrawlerStrategy> strategies;

    // 任务队列（有界阻塞队列实现背压）
    private final BlockingQueue<CrawlTask> taskQueue;

    // 消费者线程池
    private ThreadPoolExecutor consumerPool;

    // 进度跟踪
    private final AtomicInteger completedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger submittedCount = new AtomicInteger();
    private final ConcurrentHashMap<String, CrawlTask.CrawlResult> resultMap = new ConcurrentHashMap<>();

    // 要爬取的用户列表
    private final List<CrawlTask> pendingTasks = new ArrayList<>();

    private volatile boolean shutdown = false;

    public CrawlerOrchestrator() {
        this.config = ConfigLoader.getInstance();
        this.rateLimiter = new RateLimiter();
        this.repository = new DataRepository();

        int queueCapacity = config.getTaskQueueCapacity();
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);

        // 注册限流器
        this.rateLimiter.register(Platform.CODEFORCES, config.getCFRateLimitPerSecond());
        this.rateLimiter.register(Platform.LUOGU, config.getLuoguRateLimitPerSecond());

        // 初始化策略
        this.strategies = new HashMap<>();
        this.strategies.put(Platform.CODEFORCES, new CodeforcesCrawler());
        this.strategies.put(Platform.LUOGU, new LuoguCrawler());
    }

    /**
     * 添加要爬取的用户
     */
    public void addUser(String handle, Platform platform) {
        CrawlerStrategy strategy = strategies.get(platform);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
        pendingTasks.add(new CrawlTask(handle, strategy, rateLimiter, true));
        log.info("Added user: {} ({})", handle, platform.getDisplayName());
    }

    /**
     * 批量添加用户
     */
    public void addUsers(List<String> handles, Platform platform) {
        for (String handle : handles) {
            addUser(handle, platform);
        }
    }

    /**
     * 启动爬取，等待所有任务完成
     *
     * @return 所有用户的爬取结果
     */
    public List<CrawlTask.CrawlResult> startCrawl() {
        if (pendingTasks.isEmpty()) {
            log.warn("No tasks to crawl");
            return Collections.emptyList();
        }

        int totalTasks = pendingTasks.size();
        log.info("Starting crawl for {} users", totalTasks);

        // 创建线程池
        int coreSize = config.getConsumerCorePoolSize();
        int maxSize = config.getConsumerMaxPoolSize();
        consumerPool = new ThreadPoolExecutor(
            coreSize, maxSize,
            config.getKeepAliveSeconds(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：由生产者线程执行
        );

        // 提交所有任务
        List<Future<CrawlTask.CrawlResult>> futures = new ArrayList<>();
        for (CrawlTask task : pendingTasks) {
            futures.add(consumerPool.submit(task));
            submittedCount.incrementAndGet();
        }

        // 收集结果
        List<CrawlTask.CrawlResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                CrawlTask.CrawlResult result = futures.get(i).get();
                results.add(result);

                if (result.isSuccess()) {
                    completedCount.incrementAndGet();
                    resultMap.put(result.getHandle(), result);

                    // 持久化数据
                    repository.saveSubmissions(result.getHandle(), result.getPlatform(),
                        result.getSubmissions());
                    if (result.getProfile() != null) {
                        repository.saveUserProfile(result.getHandle(), result.getPlatform(),
                            result.getProfile());
                    }

                    log.info("[{}/{}] {} ({}) — {} submissions",
                        completedCount.get() + failedCount.get(), totalTasks,
                        result.getHandle(), result.getPlatform().getDisplayName(),
                        result.getSubmissionCount());
                } else {
                    failedCount.incrementAndGet();
                    log.error("[{}/{}] {} ({}) FAILED: {}",
                        completedCount.get() + failedCount.get(), totalTasks,
                        result.getHandle(), result.getPlatform().getDisplayName(),
                        result.getErrorMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failedCount.incrementAndGet();
                log.warn("Interrupted while waiting for result");
            } catch (ExecutionException e) {
                failedCount.incrementAndGet();
                log.error("Task execution failed", e.getCause());
            }
        }

        // 关闭线程池
        shutdown();

        log.info("=== Crawl complete: {} success, {} failed out of {} ===",
            completedCount.get(), failedCount.get(), totalTasks);

        return results;
    }

    /**
     * 优雅关闭
     */
    public void shutdown() {
        shutdown = true;
        if (consumerPool != null && !consumerPool.isShutdown()) {
            consumerPool.shutdown();
            try {
                if (!consumerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    consumerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("CrawlerOrchestrator shut down");
    }

    // === 统计信息 ===

    public int getCompletedCount() { return completedCount.get(); }
    public int getFailedCount() { return failedCount.get(); }
    public int getTotalSubmitted() { return submittedCount.get(); }

    public double getProgress() {
        int total = submittedCount.get();
        return total > 0 ? (double) (completedCount.get() + failedCount.get()) / total : 0.0;
    }

    public Map<String, CrawlTask.CrawlResult> getResultMap() {
        return Collections.unmodifiableMap(resultMap);
    }

    public List<Submission> getAllSubmissions() {
        List<Submission> all = new ArrayList<>();
        for (CrawlTask.CrawlResult result : resultMap.values()) {
            if (result.getSubmissions() != null) {
                all.addAll(result.getSubmissions());
            }
        }
        return all;
    }

    public Map<String, List<Submission>> getSubmissionsByUser() {
        Map<String, List<Submission>> map = new HashMap<>();
        for (Map.Entry<String, CrawlTask.CrawlResult> entry : resultMap.entrySet()) {
            if (entry.getValue().getSubmissions() != null) {
                map.put(entry.getKey(), entry.getValue().getSubmissions());
            }
        }
        return map;
    }

    public List<UserProfile> getAllProfiles() {
        List<UserProfile> profiles = new ArrayList<>();
        for (CrawlTask.CrawlResult result : resultMap.values()) {
            if (result.getProfile() != null) {
                profiles.add(result.getProfile());
            }
        }
        return profiles;
    }

    public DataRepository getRepository() {
        return repository;
    }
}
