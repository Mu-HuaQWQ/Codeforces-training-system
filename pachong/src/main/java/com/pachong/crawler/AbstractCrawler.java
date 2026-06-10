package com.pachong.crawler;

import com.pachong.model.Platform;
import com.pachong.util.ConfigLoader;
import com.pachong.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 抽象爬虫基类 — 提供重试、日志等公共逻辑
 */
public abstract class AbstractCrawler implements CrawlerStrategy {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ConfigLoader config = ConfigLoader.getInstance();
    protected final int maxRetries;
    protected final long backoffBaseMs;
    protected final double backoffMultiplier;

    public AbstractCrawler() {
        this.maxRetries = config.getMaxRetryAttempts();
        this.backoffBaseMs = config.getRetryBackoffBaseMs();
        this.backoffMultiplier = config.getDouble("retry.backoff.multiplier", 2.0);
    }

    /**
     * 带重试的HTTP GET请求
     *
     * @param url 请求URL
     * @return 响应字符串
     * @throws CrawlerException 所有重试都失败后抛出
     */
    protected String httpGetWithRetry(String url) throws CrawlerException {
        return httpGetWithRetry(url, java.util.Map.of());
    }

    /**
     * 带重试的HTTP GET请求（带自定义headers）
     */
    protected String httpGetWithRetry(String url, java.util.Map<String, String> headers)
            throws CrawlerException {
        Exception lastException = null;
        int statusCode = 0;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    long delay = (long) (backoffBaseMs * Math.pow(backoffMultiplier, attempt - 1));
                    log.debug("Retry #{}, waiting {}ms for {}", attempt, delay, url);
                    Thread.sleep(delay);
                }
                return HttpUtils.get(url, headers);
            } catch (IOException e) {
                lastException = e;
                String msg = e.getMessage();
                if (msg != null) {
                    if (msg.contains("429")) statusCode = 429;
                    else if (msg.contains("502")) statusCode = 502;
                    else if (msg.contains("503")) statusCode = 503;
                    else if (msg.contains("403")) statusCode = 403;
                    else if (msg.contains("404")) statusCode = 404;
                }
                log.warn("Request failed (attempt {}/{}): {} - {}",
                    attempt + 1, maxRetries + 1, url, e.getMessage());

                // 404说明用户不存在，不重试
                if (statusCode == 404) {
                    throw new CrawlerException("User not found: " + url,
                        getPlatform(), e, attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CrawlerException("Interrupted while retrying", getPlatform(), e, attempt);
            }
        }

        throw new CrawlerException(
            String.format("Failed after %d attempts: %s", maxRetries + 1, url),
            getPlatform(), lastException, maxRetries);
    }

    /**
     * 安全休眠
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
