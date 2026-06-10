package com.pachong.crawler;

import com.pachong.model.Platform;
import com.pachong.model.Submission;
import com.pachong.model.UserProfile;

import java.util.List;

/**
 * 爬虫策略接口 — 策略模式的核心
 * 不同平台实现此接口，提供各自的爬取逻辑
 */
public interface CrawlerStrategy {

    /**
     * 获取平台标识
     */
    Platform getPlatform();

    /**
     * 爬取用户基本信息
     *
     * @param handle CF的handle或Luogu的uid
     * @return 用户信息
     * @throws CrawlerException 网络异常、解析异常或用户不存在
     */
    UserProfile fetchUserProfile(String handle) throws CrawlerException;

    /**
     * 爬取用户的所有提交记录
     *
     * @param handle CF的handle或Luogu的uid
     * @return 提交记录列表，按时间倒序
     * @throws CrawlerException 网络异常或解析异常
     */
    List<Submission> fetchSubmissions(String handle) throws CrawlerException;
}
