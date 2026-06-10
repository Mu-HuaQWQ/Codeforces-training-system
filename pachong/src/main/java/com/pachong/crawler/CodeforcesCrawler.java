package com.pachong.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pachong.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Codeforces爬虫 — 使用官方API
 */
public class CodeforcesCrawler extends AbstractCrawler {
    private static final String API_BASE = "https://codeforces.com/api/";
    private final ObjectMapper mapper;

    public CodeforcesCrawler() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public Platform getPlatform() {
        return Platform.CODEFORCES;
    }

    @Override
    public UserProfile fetchUserProfile(String handle) throws CrawlerException {
        String url = API_BASE + "user.info?handles=" + encode(handle);
        log.info("Fetching CF user info for {}", handle);

        String json;
        try {
            json = httpGetWithRetry(url);
        } catch (CrawlerException e) {
            throw e; // rethrow network/HTTP errors directly
        }

        log.debug("CF API response (first 200 chars): {}", json.substring(0, Math.min(200, json.length())));

        try {
            JsonNode root = mapper.readTree(json);
            checkStatus(root);

            JsonNode user = root.path("result").get(0);
            UserProfile profile = new UserProfile();
            profile.setHandle(handle);
            profile.setPlatform(Platform.CODEFORCES);
            profile.setRating(user.has("rating") ? user.get("rating").asInt() : 0);
            profile.setMaxRating(user.has("maxRating") ? user.get("maxRating").asInt() : 0);
            profile.setRank(user.has("rank") ? user.get("rank").asText() : "unrated");
            profile.setContribution(user.has("contribution") ? user.get("contribution").asInt() : 0);
            profile.setFriendCount(user.has("friendOfCount") ? user.get("friendOfCount").asInt() : 0);
            if (user.has("registrationTimeSeconds")) {
                profile.setRegistrationTimeSeconds(user.get("registrationTimeSeconds").asLong());
            }

            log.info("CF user {}: rating={}, rank={}", handle, profile.getRating(), profile.getRank());
            return profile;
        } catch (IOException e) {
            log.error("CF API response for {} ({} chars): {}",
                handle, json.length(),
                json.substring(0, Math.min(500, json.length())));
            throw new CrawlerException(
                "Failed to parse CF user info for " + handle + ": " + e.getMessage(),
                Platform.CODEFORCES, e);
        }
    }

    @Override
    public List<Submission> fetchSubmissions(String handle) throws CrawlerException {
        String url = API_BASE + "user.status?handle=" + encode(handle);
        log.info("Fetching CF submissions for {}", handle);

        String json = httpGetWithRetry(url);
        try {
            JsonNode root = mapper.readTree(json);
            checkStatus(root);

            List<Submission> submissions = new ArrayList<>();
            JsonNode results = root.path("result");

            for (JsonNode item : results) {
                Submission sub = new Submission();
                sub.setPlatform(Platform.CODEFORCES);
                sub.setHandle(handle);
                sub.setId(item.get("id").asLong());

                // 题目信息
                JsonNode problem = item.path("problem");
                String contestId = problem.has("contestId")
                    ? String.valueOf(problem.get("contestId").asInt()) : "";
                String index = problem.has("index") ? problem.get("index").asText() : "";
                sub.setProblemId(contestId + index);
                sub.setProblemName(problem.has("name") ? problem.get("name").asText() : "");
                sub.setContestName(contestId.isEmpty() ? null : "CF Round #" + contestId);
                sub.setRating(problem.has("rating") ? problem.get("rating").asInt() : null);
                sub.setProblemUrl("https://codeforces.com/problemset/problem/"
                    + contestId + "/" + index);

                // 标签
                JsonNode tagsNode = problem.path("tags");
                String[] tags = new String[tagsNode.size()];
                for (int i = 0; i < tagsNode.size(); i++) {
                    tags[i] = tagsNode.get(i).asText();
                }
                sub.setTags(tags);

                // 判题结果
                String verdict = item.has("verdict") ? item.get("verdict").asText() : "UNKNOWN";
                sub.setVerdict(Verdict.fromCodeforces(verdict));

                // 时间和语言
                sub.setSubmissionTime(item.has("creationTimeSeconds")
                    ? item.get("creationTimeSeconds").asLong() : 0L);
                sub.setProgrammingLanguage(item.has("programmingLanguage")
                    ? item.get("programmingLanguage").asText() : "");
                sub.setExecutionTimeMs(item.has("timeConsumedMillis")
                    ? item.get("timeConsumedMillis").asInt() : 0);
                sub.setMemoryUsedKB(item.has("memoryConsumedBytes")
                    ? item.get("memoryConsumedBytes").asInt() / 1024 : 0);

                submissions.add(sub);
            }

            log.info("CF {}: fetched {} submissions", handle, submissions.size());
            return submissions;
        } catch (IOException e) {
            throw new CrawlerException("Failed to parse CF submissions for " + handle,
                Platform.CODEFORCES, e);
        }
    }

    /**
     * 获取 Rating 变化历史（从 CF user.rating API）
     * 每次 rated 比赛后 CF 会记录 rating 变化
     */
    public List<CfrRatingChange> fetchRatingHistory(String handle) throws CrawlerException {
        String url = API_BASE + "user.rating?handle=" + encode(handle);
        log.info("Fetching CF rating history for {}", handle);

        String json = httpGetWithRetry(url);
        try {
            JsonNode root = mapper.readTree(json);
            checkStatus(root);

            List<CfrRatingChange> changes = new ArrayList<>();
            JsonNode results = root.path("result");
            for (JsonNode item : results) {
                changes.add(new CfrRatingChange(
                    item.get("contestId").asInt(),
                    item.get("contestName").asText(),
                    item.get("rank").asInt(),
                    item.get("oldRating").asInt(),
                    item.get("newRating").asInt(),
                    item.get("ratingUpdateTimeSeconds").asLong()
                ));
            }
            log.info("CF {}: fetched {} rating changes", handle, changes.size());
            return changes;
        } catch (IOException e) {
            throw new CrawlerException("Failed to parse CF rating history for " + handle,
                Platform.CODEFORCES, e);
        }
    }

    public record CfrRatingChange(int contestId, String contestName, int rank,
                                   int oldRating, int newRating, long timestamp) {}

    private void checkStatus(JsonNode root) throws CrawlerException {
        String status = root.path("status").asText("");
        if (!"OK".equals(status)) {
            String comment = root.path("comment").asText("Unknown error");
            throw new CrawlerException("CF API error: " + comment, Platform.CODEFORCES);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
