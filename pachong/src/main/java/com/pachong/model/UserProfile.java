package com.pachong.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 用户基本信息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {
    private String handle;
    private Platform platform;
    private Integer rating;
    private Integer maxRating;
    private String rank;
    private Integer contribution;
    private Integer friendCount;
    private LocalDateTime registrationTime;

    public UserProfile() {}

    public UserProfile(String handle, Platform platform) {
        this.handle = handle;
        this.platform = platform;
    }

    // === Getters and Setters ===

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(Integer maxRating) {
        this.maxRating = maxRating;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getContribution() {
        return contribution;
    }

    public void setContribution(Integer contribution) {
        this.contribution = contribution;
    }

    public Integer getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(Integer friendCount) {
        this.friendCount = friendCount;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(LocalDateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    /**
     * 从Unix时间戳设置注册时间
     */
    public void setRegistrationTimeSeconds(long seconds) {
        this.registrationTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    @Override
    public String toString() {
        return String.format("UserProfile{handle='%s', platform=%s, rating=%d, rank='%s'}",
            handle, platform, rating, rank);
    }
}
