package com.pachong.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pachong.model.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Luogu爬虫 — 解析网页和XHR接口
 *
 * Luogu使用Vue.js渲染页面，数据通常嵌入在HTML的&lt;script&gt;中
 * 使用 _contentOnly=1 参数可获取纯JSON数据
 */
public class LuoguCrawler extends AbstractCrawler {
    private static final String LUOGU_BASE = "https://www.luogu.com.cn";
    private static final String UID_REGEX = ".*<script>.*window\\._feInjection.*?</script>.*";

    private final ObjectMapper mapper;

    // 用于提取HTML中嵌入JSON的正则
    private static final Pattern JSON_IN_SCRIPT = Pattern.compile(
        "JSON\\.parse\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)",
        Pattern.DOTALL
    );
    // decodeURIComponent中的JSON
    private static final Pattern DECODED_JSON = Pattern.compile(
        "decodeURIComponent\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)",
        Pattern.DOTALL
    );

    public LuoguCrawler() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public Platform getPlatform() {
        return Platform.LUOGU;
    }

    @Override
    public UserProfile fetchUserProfile(String handle) throws CrawlerException {
        String url = LUOGU_BASE + "/user/" + handle + "?_contentOnly=1";
        log.info("Fetching Luogu user info for {}", handle);

        String response = httpGetWithRetry(url, Map.of(
            "Referer", LUOGU_BASE + "/user/" + handle,
            "X-Requested-With", "XMLHttpRequest"
        ));

        try {
            JsonNode root = mapper.readTree(response);
            JsonNode data = root.path("currentData").path("user");

            UserProfile profile = new UserProfile();
            profile.setHandle(handle);
            profile.setPlatform(Platform.LUOGU);

            if (!data.isMissingNode()) {
                profile.setRating(data.has("rating") ? data.get("rating").asInt() : 0);
                profile.setMaxRating(data.has("maxRating") ? data.get("maxRating").asInt() : 0);
                if (data.has("registerTime")) {
                    long seconds = data.get("registerTime").asLong();
                    profile.setRegistrationTime(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(seconds), ZoneId.systemDefault()));
                }
                // Luogu等级作为rank
                if (data.has("color")) {
                    profile.setRank(data.get("color").asText());
                }
                profile.setFriendCount(data.has("followingCount")
                    ? data.get("followingCount").asInt() : 0);
            }

            log.info("Luogu user {}: rating={}", handle, profile.getRating());
            return profile;
        } catch (IOException e) {
            throw new CrawlerException("Failed to parse Luogu user info for " + handle,
                Platform.LUOGU, e);
        }
    }

    @Override
    public List<Submission> fetchSubmissions(String handle) throws CrawlerException {
        log.info("Fetching Luogu submissions for {}", handle);
        List<Submission> allSubmissions = new ArrayList<>();

        // Luogu的提交记录分页接口
        int page = 1;
        int totalPages = 1;
        boolean hasData = false;

        while (page <= totalPages && page <= 100) { // 最多100页，防止无限循环
            String url = LUOGU_BASE + "/record/list?user=" + handle
                + "&_contentOnly=1&page=" + page;

            try {
                String response = httpGetWithRetry(url, Map.of(
                    "Referer", LUOGU_BASE + "/user/" + handle,
                    "X-Requested-With", "XMLHttpRequest"
                ));

                JsonNode root = mapper.readTree(response);
                JsonNode records = root.path("currentData").path("records");

                if (records.isMissingNode()) {
                    log.debug("No records node found on page {}", page);
                    break;
                }

                JsonNode result = records.path("result");
                if (result.isMissingNode() || result.isEmpty()) {
                    log.debug("No more results on page {}", page);
                    break;
                }

                // 总分页信息
                if (page == 1) {
                    totalPages = records.has("pages")
                        ? records.get("pages").asInt() : 1;
                    log.debug("Total pages: {}", totalPages);
                }

                hasData = true;

                for (JsonNode item : result) {
                    Submission sub = parseLuoguSubmission(item, handle);
                    if (sub != null) {
                        allSubmissions.add(sub);
                    }
                }

                log.debug("Luogu {} page {}/{}: {} submissions",
                    handle, page, totalPages, result.size());

                page++;

                // 分页之间的短暂延迟
                if (page <= totalPages) {
                    sleep(300);
                }

            } catch (CrawlerException e) {
                if (e.getStatusCode() == 404) {
                    log.warn("Page {} not found, stopping pagination", page);
                    break;
                }
                if (page > 1) {
                    log.warn("Error on page {}, using data from {} pages", page, page - 1);
                    break;
                }
                throw e;
            } catch (JsonProcessingException e) {
                if (hasData) {
                    log.warn("JSON parse error on page {}, using {} submissions from previous pages",
                        page, allSubmissions.size());
                    break;
                }
                throw new CrawlerException("Failed to parse Luogu records for " + handle,
                    Platform.LUOGU, e);
            }
        }

        log.info("Luogu {}: fetched {} submissions across {} pages",
            handle, allSubmissions.size(), page - 1);
        return allSubmissions;
    }

    private Submission parseLuoguSubmission(JsonNode item, String handle) {
        try {
            Submission sub = new Submission();
            sub.setPlatform(Platform.LUOGU);
            sub.setHandle(handle);
            sub.setId(item.has("id") ? item.get("id").asLong() : 0L);

            // 题目信息
            JsonNode problem = item.path("problem");
            if (!problem.isMissingNode()) {
                sub.setProblemId(problem.has("pid") ? problem.get("pid").asText() : "");
                sub.setProblemName(problem.has("title") ? problem.get("title").asText() : "");
                sub.setDifficulty(problem.has("difficulty")
                    ? problem.get("difficulty").asInt() : 0);
                sub.setProblemUrl(LUOGU_BASE + "/problem/" + sub.getProblemId());

                // Luogu标签
                if (problem.has("tags") && problem.get("tags").isArray()) {
                    JsonNode tagsNode = problem.get("tags");
                    String[] tags = new String[tagsNode.size()];
                    for (int i = 0; i < tagsNode.size(); i++) {
                        tags[i] = tagsNode.get(i).asText();
                    }
                    sub.setTags(tags);
                }
            }

            // 判题结果 — Luogu status: 12=AC
            int status = item.has("status") ? item.get("status").asInt() : -1;
            sub.setVerdict(Verdict.fromLuogu(status));

            // 提交时间
            if (item.has("submitTime")) {
                sub.setSubmissionTime(item.get("submitTime").asLong());
            }

            // 语言
            sub.setProgrammingLanguage(item.has("language")
                ? item.get("language").asText() : "");

            // 运行时间和内存
            sub.setExecutionTimeMs(item.has("time") ? item.get("time").asInt() : 0);
            sub.setMemoryUsedKB(item.has("memory") ? item.get("memory").asInt() : 0);

            return sub;
        } catch (Exception e) {
            log.warn("Error parsing Luogu submission: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 尝试从HTML页面中提取嵌入的JSON数据（备用方案）
     * Luogu使用Vue SSR，将数据作为JSON注入到HTML中
     */
    @Deprecated
    private String extractEmbeddedJson(String html) throws IOException {
        // 查找 <script id="lentille">...</script> 或 window._feInjection
        int idx = html.indexOf("window._feInjection");
        if (idx < 0) idx = html.indexOf("_feInjection");
        if (idx < 0) {
            throw new IOException("Cannot find embedded data in HTML");
        }

        // 查找包含的JSON
        int braceStart = html.indexOf('{', idx);
        if (braceStart < 0) {
            throw new IOException("Cannot find JSON start in HTML");
        }

        // 简单的括号匹配来提取完整JSON
        int depth = 0;
        int braceEnd = braceStart;
        for (int i = braceStart; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    braceEnd = i + 1;
                    break;
                }
            }
        }

        return html.substring(braceStart, braceEnd);
    }
}
