package com.pachong.model;

/**
 * 竞赛平台枚举
 */
public enum Platform {
    CODEFORCES("Codeforces", "codeforces.com"),
    LUOGU("Luogu", "luogu.com.cn");

    private final String displayName;
    private final String domain;

    Platform(String displayName, String domain) {
        this.displayName = displayName;
        this.domain = domain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
