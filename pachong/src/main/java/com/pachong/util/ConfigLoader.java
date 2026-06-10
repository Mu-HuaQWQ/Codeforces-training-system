package com.pachong.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置加载器 — 从config.properties读取配置
 */
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.properties";

    private static volatile ConfigLoader instance;
    private final Properties props;

    private ConfigLoader() {
        props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
                log.info("Loaded config from {}", CONFIG_FILE);
            } else {
                log.warn("config.properties not found, using defaults");
            }
        } catch (IOException e) {
            log.error("Failed to load config.properties", e);
        }
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        return val != null ? Boolean.parseBoolean(val.trim()) : defaultValue;
    }

    // === 便捷方法 ===

    public int getConsumerCorePoolSize() {
        return getInt("consumer.core.pool.size", 4);
    }

    public int getConsumerMaxPoolSize() {
        return getInt("consumer.max.pool.size", 8);
    }

    public int getTaskQueueCapacity() {
        return getInt("task.queue.capacity", 500);
    }

    public int getKeepAliveSeconds() {
        return getInt("consumer.keep.alive.seconds", 60);
    }

    public double getCFRateLimitPerSecond() {
        return getDouble("ratelimit.codeforces.per.second", 3.0);
    }

    public double getLuoguRateLimitPerSecond() {
        return getDouble("ratelimit.luogu.per.second", 2.0);
    }

    public int getMaxRetryAttempts() {
        return getInt("retry.max.attempts", 3);
    }

    public long getRetryBackoffBaseMs() {
        return getInt("retry.backoff.base.ms", 1000);
    }

    public String getDataDir() {
        return get("data.dir", "data");
    }
}
