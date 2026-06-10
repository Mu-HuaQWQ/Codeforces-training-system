package com.pachong.storage;

import com.pachong.model.Platform;
import com.pachong.model.Submission;
import com.pachong.model.UserProfile;
import com.pachong.model.UserStatistics;
import com.pachong.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 数据仓库 — 管理数据文件的读写路径
 *
 * 文件组织:
 *   data/raw/{platform}/{handle}_submissions.json
 *   data/raw/{platform}/{handle}_profile.json
 *   data/stats/{handle}_statistics.json
 *   data/charts/{handle}_radar.png
 */
public class DataRepository {
    private static final Logger log = LoggerFactory.getLogger(DataRepository.class);

    private final Path dataDir;
    private final Path rawDir;
    private final Path statsDir;
    private final Path chartsDir;

    public DataRepository() {
        String dir = ConfigLoader.getInstance().getDataDir();
        this.dataDir = Paths.get(dir);
        this.rawDir = dataDir.resolve("raw");
        this.statsDir = dataDir.resolve("stats");
        this.chartsDir = dataDir.resolve("charts");

        // 确保目录存在
        try {
            java.nio.file.Files.createDirectories(rawDir);
            java.nio.file.Files.createDirectories(statsDir);
            java.nio.file.Files.createDirectories(chartsDir);
        } catch (IOException e) {
            log.error("Failed to create data directories", e);
        }
    }

    public DataRepository(String baseDir) {
        // 允许自定义路径（使用相同的初始化逻辑）
        this();
    }

    // === 提交记录 ===

    public void saveSubmissions(String handle, Platform platform, List<Submission> submissions) {
        if (submissions == null || submissions.isEmpty()) return;
        Path path = getSubmissionPath(handle, platform);
        try {
            JsonStorage.writeSubmissions(path, submissions);
            log.info("Saved {} submissions to {}", submissions.size(), path);
        } catch (IOException e) {
            log.error("Failed to save submissions for {}", handle, e);
        }
    }

    public List<Submission> loadSubmissions(String handle, Platform platform)
            throws IOException {
        Path path = getSubmissionPath(handle, platform);
        return JsonStorage.readSubmissions(path);
    }

    // === 用户信息 ===

    public void saveUserProfile(String handle, Platform platform, UserProfile profile) {
        Path path = getUserProfilePath(handle, platform);
        try {
            JsonStorage.writeUserProfile(path, profile);
            log.info("Saved profile to {}", path);
        } catch (IOException e) {
            log.error("Failed to save profile for {}", handle, e);
        }
    }

    public UserProfile loadUserProfile(String handle, Platform platform) throws IOException {
        Path path = getUserProfilePath(handle, platform);
        return JsonStorage.readUserProfile(path);
    }

    // === 统计数据 ===

    public void saveStatistics(String handle, UserStatistics stats) {
        Path path = getStatisticsPath(handle);
        try {
            JsonStorage.writeStatistics(path, stats);
            log.info("Saved statistics to {}", path);
        } catch (IOException e) {
            log.error("Failed to save statistics for {}", handle, e);
        }
    }

    public UserStatistics loadStatistics(String handle) throws IOException {
        Path path = getStatisticsPath(handle);
        return JsonStorage.readStatistics(path);
    }

    // === 路径工具方法 ===

    private Path getSubmissionPath(String handle, Platform platform) {
        return rawDir.resolve(platform.name().toLowerCase())
            .resolve(handle + "_submissions.json");
    }

    private Path getUserProfilePath(String handle, Platform platform) {
        return rawDir.resolve(platform.name().toLowerCase())
            .resolve(handle + "_profile.json");
    }

    public Path getStatisticsPath(String handle) {
        return statsDir.resolve(handle + "_statistics.json");
    }

    public Path getChartPath(String handle) {
        return chartsDir.resolve(handle + "_radar.png");
    }

    public Path getChartPath(String handle, String suffix) {
        return chartsDir.resolve(handle + "_radar" + suffix + ".png");
    }

    // === 通用路径 ===

    public Path getRawDir() { return rawDir; }
    public Path getStatsDir() { return statsDir; }
    public Path getChartsDir() { return chartsDir; }
}
