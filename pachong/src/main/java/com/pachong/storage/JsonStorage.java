package com.pachong.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pachong.model.Submission;
import com.pachong.model.UserProfile;
import com.pachong.model.UserStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * JSON存储工具 — Jackson封装
 */
public class JsonStorage {
    private static final Logger log = LoggerFactory.getLogger(JsonStorage.class);

    private static final ObjectMapper mapper = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.enable(SerializationFeature.INDENT_OUTPUT);
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * 将对象写入JSON文件
     */
    public static void writeJson(Path path, Object data) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writeValue(path.toFile(), data);
        log.debug("Written JSON to {}", path);
    }

    /**
     * 从JSON文件读取
     */
    public static <T> T readJson(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return mapper.readValue(path.toFile(), clazz);
    }

    /**
     * 从JSON文件读取泛型类型
     */
    public static <T> T readJson(Path path, TypeReference<T> typeRef) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return mapper.readValue(path.toFile(), typeRef);
    }

    // === 便捷方法 ===

    public static void writeSubmissions(Path path, List<Submission> submissions)
            throws IOException {
        writeJson(path, submissions);
    }

    public static List<Submission> readSubmissions(Path path) throws IOException {
        List<Submission> result = readJson(path,
            new TypeReference<List<Submission>>() {});
        return result != null ? result : Collections.emptyList();
    }

    public static void writeUserProfile(Path path, UserProfile profile) throws IOException {
        writeJson(path, profile);
    }

    public static UserProfile readUserProfile(Path path) throws IOException {
        return readJson(path, UserProfile.class);
    }

    public static void writeStatistics(Path path, UserStatistics stats) throws IOException {
        writeJson(path, stats);
    }

    public static UserStatistics readStatistics(Path path) throws IOException {
        return readJson(path, UserStatistics.class);
    }
}
