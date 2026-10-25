package com.forum.util;
/** 分页参数收敛，避免 size=100000 直接把库/内存打爆。 */
public final class Paging {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;
    private Paging() {}
    public static int page(int page) {
        return Math.max(page, 1);
    }
    public static int size(int size) {
        if (size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
    /** 转义 MySQL LIKE 通配符，防止用户输入的 % 和 _ 变成通配符。 */
    public static String escapeLike(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
