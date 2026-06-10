package com.pachong.model;

/**
 * 题目难度等级
 */
public enum ProblemDifficulty {
    BEGINNER("入门", 0, 1199),
    EASY("普及", 1200, 1599),
    MEDIUM("提高", 1600, 1999),
    HARD("省选", 2000, 2399),
    EXPERT("NOI", 2400, 2999),
    MASTER("CTSC", 3000, Integer.MAX_VALUE);

    private final String displayName;
    private final int minRating;
    private final int maxRating;

    ProblemDifficulty(String displayName, int minRating, int maxRating) {
        this.displayName = displayName;
        this.minRating = minRating;
        this.maxRating = maxRating;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据CF rating分数确定难度等级
     */
    public static ProblemDifficulty fromRating(Integer rating) {
        if (rating == null || rating < 0) return BEGINNER;
        for (ProblemDifficulty diff : values()) {
            if (rating >= diff.minRating && rating <= diff.maxRating) {
                return diff;
            }
        }
        return MASTER;
    }

    /**
     * 根据Luogu难度(1-8)确定等级
     */
    public static ProblemDifficulty fromLuoguDifficulty(int luoguDifficulty) {
        return switch (luoguDifficulty) {
            case 0, 1 -> BEGINNER;      // 暂无评定 / 入门
            case 2 -> EASY;             // 普及-
            case 3 -> EASY;             // 普及/提高-
            case 4 -> MEDIUM;           // 普及+/提高
            case 5 -> HARD;             // 提高+/省选-
            case 6 -> HARD;             // 省选/NOI-
            case 7 -> EXPERT;           // NOI/NOI+/CTSC
            case 8 -> MASTER;           // CTSC
            default -> BEGINNER;
        };
    }
}
