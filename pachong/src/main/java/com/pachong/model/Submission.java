package com.pachong.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 单条提交记录
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Submission {
    private Long id;
    private Platform platform;
    private String handle;
    private String problemId;          // 如 "4A", "CF_1A", "P1001"
    private String problemName;
    private String contestName;        // 比赛的名称，如 "Codeforces Round #100"
    private String[] tags;             // 来自CF的原始标签数组
    private Integer rating;            // 题目难度分
    private Verdict verdict;
    private Long submissionTime;       // epoch seconds
    private String programmingLanguage;
    private Integer executionTimeMs;   // 运行时间(ms)
    private Integer memoryUsedKB;      // 内存消耗(KB)
    private String problemUrl;         // 题目链接
    private Integer difficulty;        // Luogu难度(1-8) 或 CF的rating

    public Submission() {}

    // === Getters and Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getContestName() { return contestName; }
    public void setContestName(String contestName) { this.contestName = contestName; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }

    public Long getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(Long submissionTime) { this.submissionTime = submissionTime; }

    public String getProgrammingLanguage() { return programmingLanguage; }
    public void setProgrammingLanguage(String programmingLanguage) { this.programmingLanguage = programmingLanguage; }

    public Integer getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Integer executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public Integer getMemoryUsedKB() { return memoryUsedKB; }
    public void setMemoryUsedKB(Integer memoryUsedKB) { this.memoryUsedKB = memoryUsedKB; }

    public String getProblemUrl() { return problemUrl; }
    public void setProblemUrl(String problemUrl) { this.problemUrl = problemUrl; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    /**
     * 该提交是否为AC（Accepted）
     */
    public boolean isAccepted() {
        return verdict == Verdict.ACCEPTED;
    }

    @Override
    public String toString() {
        return String.format("Submission{platform=%s, handle='%s', problem='%s', verdict=%s, time=%d}",
            platform, handle, problemName, verdict, submissionTime);
    }
}
