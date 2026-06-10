package com.pachong.analysis;

import com.pachong.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 提交记录分析器 — 使用Stream API进行数据聚合分析
 */
public class SubmissionAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(SubmissionAnalyzer.class);

    /**
     * 分析某个用户的所有提交记录，生成统计数据
     */
    public UserStatistics analyze(String handle, Platform platform,
                                   List<Submission> submissions) {
        UserStatistics stats = new UserStatistics(handle, platform);

        if (submissions == null || submissions.isEmpty()) {
            log.warn("No submissions to analyze for {}", handle);
            return stats;
        }

        stats.setTotalSubmissions(submissions.size());

        // 1. AC数量统计
        long acCount = submissions.stream()
            .filter(Submission::isAccepted)
            .count();
        stats.setAcceptedCount((int) acCount);

        // 2. 只分析AC的提交，用于标签统计
        List<Submission> accepted = submissions.stream()
            .filter(Submission::isAccepted)
            .toList();

        // 3. 按标签统计AC数量
        Map<String, Integer> tagAcMap = computeTagAcceptedCount(accepted);
        stats.setTagAcceptedCount(tagAcMap);

        // 4. 按标签统计总提交数
        Map<String, Integer> tagTotalMap = computeTagTotalCount(submissions);
        stats.setTagTotalCount(tagTotalMap);

        // 5. 难度分布统计
        Map<String, Integer> diffMap = computeDifficultyCount(accepted);
        stats.setDifficultyCount(diffMap);

        // 6. 计算通过率
        stats.computeRates();

        // 7. 生成雷达图数据
        List<RadarData> radarDataList = generateRadarData(handle, tagAcMap);
        stats.setRadarDataList(radarDataList);

        // 8. 计算最长连续AC天数
        int streak = computeMaxStreak(accepted);
        stats.setMaxStreak(streak);

        log.info("{}: {} total, {} AC, {}% rate, {} streak days",
            handle, stats.getTotalSubmissions(), stats.getAcceptedCount(),
            String.format("%.1f", stats.getAcceptanceRate()), streak);

        return stats;
    }

    /**
     * 按标签统计AC数量（只统计AC的提交）
     */
    public Map<String, Integer> computeTagAcceptedCount(List<Submission> accepted) {
        Map<String, Integer> tagCount = new LinkedHashMap<>();

        // 初始化雷达图核心维度为0
        for (ProblemTag pt : ProblemTag.radarDimensions()) {
            tagCount.put(pt.getDisplayName(), 0);
        }

        // 统计每个标签的AC数
        for (Submission sub : accepted) {
            String[] tags = sub.getTags();
            if (tags == null) continue;

            for (String rawTag : tags) {
                ProblemTag pt = ProblemTag.fromTagString(rawTag);
                if (pt == ProblemTag.OTHER) continue;
                String displayName = pt.getDisplayName();
                tagCount.merge(displayName, 1, Integer::sum);
            }
        }

        return tagCount;
    }

    /**
     * 按标签统计总提交数
     */
    Map<String, Integer> computeTagTotalCount(List<Submission> submissions) {
        Map<String, Integer> tagCount = new LinkedHashMap<>();

        for (ProblemTag pt : ProblemTag.radarDimensions()) {
            tagCount.put(pt.getDisplayName(), 0);
        }

        for (Submission sub : submissions) {
            String[] tags = sub.getTags();
            if (tags == null) continue;

            for (String rawTag : tags) {
                ProblemTag pt = ProblemTag.fromTagString(rawTag);
                if (pt == ProblemTag.OTHER) continue;
                String displayName = pt.getDisplayName();
                tagCount.merge(displayName, 1, Integer::sum);
            }
        }

        return tagCount;
    }

    /**
     * 按题目难度统计AC数量
     */
    Map<String, Integer> computeDifficultyCount(List<Submission> accepted) {
        Map<String, Integer> diffCount = new LinkedHashMap<>();

        for (Submission sub : accepted) {
            ProblemDifficulty diff;
            if (sub.getPlatform() == Platform.LUOGU && sub.getDifficulty() != null) {
                diff = ProblemDifficulty.fromLuoguDifficulty(sub.getDifficulty());
            } else {
                diff = ProblemDifficulty.fromRating(sub.getRating());
            }
            diffCount.merge(diff.getDisplayName(), 1, Integer::sum);
        }

        return diffCount;
    }

    /**
     * 从标签统计生成雷达图数据
     */
    public List<RadarData> generateRadarData(String handle, Map<String, Integer> tagAcMap) {
        // 使用核心维度，确保顺序一致
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (ProblemTag pt : ProblemTag.radarDimensions()) {
            labels.add(pt.getDisplayName());
            values.add((double) tagAcMap.getOrDefault(pt.getDisplayName(), 0));
        }

        RadarData data = new RadarData(handle, labels, values);
        return List.of(data);
    }

    /**
     * 生成多用户对比雷达图数据
     */
    public List<RadarData> generateComparisonRadarData(
            Map<String, Map<String, Integer>> userTagAcMap) {

        List<RadarData> dataList = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> entry : userTagAcMap.entrySet()) {
            String handle = entry.getKey();
            List<RadarData> userRadar = generateRadarData(handle, entry.getValue());
            dataList.addAll(userRadar);
        }

        return dataList;
    }

    /**
     * 计算最长连续AC天数
     */
    int computeMaxStreak(List<Submission> accepted) {
        if (accepted.isEmpty()) return 0;

        // 按时间排序
        List<Submission> sorted = accepted.stream()
            .sorted(Comparator.comparingLong(Submission::getSubmissionTime))
            .toList();

        int maxStreak = 0;
        int currentStreak = 1;
        long prevDay = sorted.get(0).getSubmissionTime() / 86400;

        for (int i = 1; i < sorted.size(); i++) {
            long currentDay = sorted.get(i).getSubmissionTime() / 86400;
            if (currentDay == prevDay) {
                continue; // 同一天，不计入连续天数
            } else if (currentDay == prevDay + 1) {
                currentStreak++;
            } else {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
            prevDay = currentDay;
        }
        maxStreak = Math.max(maxStreak, currentStreak);

        return maxStreak;
    }
}
