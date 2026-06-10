package com.pachong.crawler;

import com.pachong.model.Platform;

/**
 * 爬虫自定义异常
 */
public class CrawlerException extends Exception {

    private final Platform platform;
    private final int retryCount;
    private final int statusCode;

    public CrawlerException(String message, Platform platform) {
        super(message);
        this.platform = platform;
        this.retryCount = 0;
        this.statusCode = 0;
    }

    public CrawlerException(String message, Platform platform, Throwable cause) {
        super(message, cause);
        this.platform = platform;
        this.retryCount = 0;
        this.statusCode = 0;
    }

    public CrawlerException(String message, Platform platform, int retryCount, int statusCode) {
        super(message);
        this.platform = platform;
        this.retryCount = retryCount;
        this.statusCode = statusCode;
    }

    public CrawlerException(String message, Platform platform, Throwable cause, int retryCount) {
        super(message, cause);
        this.platform = platform;
        this.retryCount = retryCount;
        this.statusCode = 0;
    }

    public Platform getPlatform() { return platform; }
    public int getRetryCount() { return retryCount; }
    public int getStatusCode() { return statusCode; }

    @Override
    public String toString() {
        return String.format("CrawlerException{platform=%s, retry=%d, status=%d, msg='%s'}",
            platform, retryCount, statusCode, getMessage());
    }
}
