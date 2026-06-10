package com.pachong.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具类 — OkHttp封装
 */
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    };

    private static final Random random = new Random();
    private static volatile OkHttpClient instance;

    private HttpUtils() {}

    public static OkHttpClient getClient() {
        if (instance == null) {
            synchronized (HttpUtils.class) {
                if (instance == null) {
                    instance = buildClient();
                }
            }
        }
        return instance;
    }

    private static OkHttpClient buildClient() {
        ConfigLoader config = ConfigLoader.getInstance();
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(config.getInt("http.connect.timeout.seconds", 10)))
            .readTimeout(Duration.ofSeconds(config.getInt("http.read.timeout.seconds", 30)))
            .writeTimeout(Duration.ofSeconds(config.getInt("http.write.timeout.seconds", 10)))
            .connectionPool(new ConnectionPool(
                50,
                5, TimeUnit.MINUTES
            ))
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(HttpUtils::userAgentInterceptor)
            .addInterceptor(HttpUtils::loggingInterceptor)
            .build();
    }

    /**
     * 随机User-Agent拦截器
     */
    private static Response userAgentInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
            .header("User-Agent", randomUserAgent())
            .header("Accept", "text/html,application/xhtml+xml,application/json,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .build();
        return chain.proceed(request);
    }

    /**
     * 日志拦截器
     */
    private static Response loggingInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        long start = System.currentTimeMillis();
        Response response = chain.proceed(request);
        long time = System.currentTimeMillis() - start;
        log.debug("{} {} → {} ({}ms)", request.method(), request.url(), response.code(), time);
        return response;
    }

    public static String randomUserAgent() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    /**
     * GET请求，返回字符串
     */
    public static String get(String url) throws IOException {
        return get(url, Map.of());
    }

    /**
     * GET请求，带自定义headers，返回字符串
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        headers.forEach(builder::addHeader);

        try (Response response = getClient().newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for " + url);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body for " + url);
            }
            return body.string();
        }
    }

    /**
     * GET请求，返回Response对象（调用者负责关闭）
     */
    public static Response getResponse(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        headers.forEach(builder::addHeader);
        return getClient().newCall(builder.build()).execute();
    }

    /**
     * POST请求，JSON body
     */
    public static String postJson(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json,
            MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for " + url);
            }
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.string() : "";
        }
    }
}
