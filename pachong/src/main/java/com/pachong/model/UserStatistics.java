package com.pachong.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户统计数据
 */
public class UserStatistics {
    private String handle;
    private Platform platform;
    private int totalSubmissions;
    private int acceptedCount;
    private double acceptanceRate;
    private Map<String, Integer> tagAcceptedCount;   // 标签 → AC数量
    private Map<String, Integer> tagTotalCount;      // 标签 → 总提交数
    private Map<String, Integer> difficultyCount;     // 难度 → AC数量
    private List<RadarData> radarDataList;
    private int maxStreak;                            // 最长连续AC天数

    public UserStatistics() {
        this.tagAcceptedCount = new LinkedHashMap<>();
        this.tagTotalCount = new LinkedHashMap<>();
        this.difficultyCount = new LinkedHashMap<>();
    }

    public UserStatistics(String handle, Platform platform) {
        this();
        this.handle = handle;
        this.platform = platform;
    }

    // === Getters and Setters ===

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; }

    public double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(double acceptanceRate) { this.acceptanceRate = acceptanceRate; }

    public Map<String, Integer> getTagAcceptedCount() { return tagAcceptedCount; }
    public void setTagAcceptedCount(Map<String, Integer> tagAcceptedCount) { this.tagAcceptedCount = tagAcceptedCount; }

    public Map<String, Integer> getTagTotalCount() { return tagTotalCount; }
    public void setTagTotalCount(Map<String, Integer> tagTotalCount) { this.tagTotalCount = tagTotalCount; }

    public Map<String, Integer> getDifficultyCount() { return difficultyCount; }
    public void setDifficultyCount(Map<String, Integer> difficultyCount) { this.difficultyCount = difficultyCount; }

    public List<RadarData> getRadarDataList() { return radarDataList; }
    public void setRadarDataList(List<RadarData> radarDataList) { this.radarDataList = radarDataList; }

    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }

    /**
     * 计算通过率
     */
    public void computeRates() {
        this.acceptanceRate = totalSubmissions > 0
            ? (double) acceptedCount / totalSubmissions * 100.0 : 0.0;
    }

    @Override
    public String toString() {
        return String.format("UserStatistics{handle='%s', platform=%s, AC=%d/%d, rate=%.1f%%}",
            handle, platform, acceptedCount, totalSubmissions, acceptanceRate);
    }
}
