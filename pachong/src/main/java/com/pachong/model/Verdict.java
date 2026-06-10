package com.pachong.model;

/**
 * 判题结果枚举
 */
public enum Verdict {
    ACCEPTED("Accepted", "AC"),
    WRONG_ANSWER("Wrong Answer", "WA"),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded", "TLE"),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded", "MLE"),
    RUNTIME_ERROR("Runtime Error", "RE"),
    COMPILATION_ERROR("Compilation Error", "CE"),
    SKIPPED("Skipped", "SKIP"),
    HACKED("Hacked", "HACKED"),
    PRESENTATION_ERROR("Presentation Error", "PE"),
    UNKNOWN("Unknown", "??");

    private final String fullName;
    private final String shortName;

    Verdict(String fullName, String shortName) {
        this.fullName = fullName;
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * 从Codeforces API的verdict字符串解析
     */
    public static Verdict fromCodeforces(String cfVerdict) {
        if (cfVerdict == null) return UNKNOWN;
        return switch (cfVerdict.toLowerCase()) {
            case "ok" -> ACCEPTED;
            case "wrong_answer", "wrong answer" -> WRONG_ANSWER;
            case "time_limit_exceeded", "time limit exceeded" -> TIME_LIMIT_EXCEEDED;
            case "memory_limit_exceeded", "memory limit exceeded" -> MEMORY_LIMIT_EXCEEDED;
            case "runtime_error", "runtime error" -> RUNTIME_ERROR;
            case "compilation_error", "compilation error" -> COMPILATION_ERROR;
            case "skipped" -> SKIPPED;
            case "hacked" -> HACKED;
            case "presentation_error", "presentation error" -> PRESENTATION_ERROR;
            default -> UNKNOWN;
        };
    }

    /**
     * 从Luogu的status值解析
     */
    public static Verdict fromLuogu(int status) {
        return switch (status) {
            case 12 -> ACCEPTED;         // Accepted
            case 0, 8 -> WRONG_ANSWER;   // WA / Unaccepted
            case 14 -> TIME_LIMIT_EXCEEDED;
            case 13 -> MEMORY_LIMIT_EXCEEDED;
            case 11, 16, 17, 20 -> RUNTIME_ERROR;
            case 1 -> COMPILATION_ERROR;
            default -> UNKNOWN;
        };
    }
}
