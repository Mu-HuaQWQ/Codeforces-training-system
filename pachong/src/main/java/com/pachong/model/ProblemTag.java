package com.pachong.model;

import java.util.Map;
import java.util.Set;

/**
 * 题目标签枚举 — 统一CF和Luogu的标签体系
 */
public enum ProblemTag {
    DP("动态规划", Set.of("dp", "dynamic programming")),
    GREEDY("贪心", Set.of("greedy", "greedy algorithms")),
    MATH("数学", Set.of("math", "number theory", "combinatorics", "matrices", "probabilities")),
    DATA_STRUCTURES("数据结构", Set.of("data structures", "ds", "segment tree", "fenwick tree", "binary indexed tree", "bitmask")),
    STRINGS("字符串", Set.of("strings", "string suffix structures", "hashing")),
    GRAPHS("图论", Set.of("graphs", "graph matchings", "shortest paths", "dfs and similar", "flows", "2-sat")),
    TREES("树", Set.of("trees", "divide and conquer", "dsu")),
    GEOMETRY("几何", Set.of("geometry", "computational geometry")),
    CONSTRUCTIVE("构造", Set.of("constructive algorithms")),
    BINARY_SEARCH("二分", Set.of("binary search", "ternary search", "divide and conquer")),
    BRUTE_FORCE("暴力枚举", Set.of("brute force", "enumeration", "meet-in-the-middle")),
    IMPLEMENTATION("模拟实现", Set.of("implementation")),
    SORTINGS("排序", Set.of("sortings")),
    TWO_POINTERS("双指针", Set.of("two pointers")),
    GAMES("博弈论", Set.of("games")),
    INTERACTIVE("交互题", Set.of("interactive")),
    OTHER("其他", Set.of("*special", "special problem", "expression parsing"));

    private final String displayName;
    private final Set<String> aliases;

    ProblemTag(String displayName, Set<String> aliases) {
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    /**
     * 从CF或Luogu的标签字符串映射到统一标签
     */
    public static ProblemTag fromTagString(String tag) {
        if (tag == null || tag.isEmpty()) return OTHER;
        String lower = tag.toLowerCase().trim();

        for (ProblemTag pt : values()) {
            for (String alias : pt.aliases) {
                if (lower.equals(alias) || lower.contains(alias)) {
                    return pt;
                }
            }
        }
        return OTHER;
    }

    /**
     * 获取雷达图使用的核心标签维度（排除OTHER）
     */
    public static ProblemTag[] radarDimensions() {
        return new ProblemTag[]{
            DP, GREEDY, MATH, DATA_STRUCTURES, STRINGS,
            GRAPHS, TREES, GEOMETRY, CONSTRUCTIVE, BINARY_SEARCH
        };
    }
}
