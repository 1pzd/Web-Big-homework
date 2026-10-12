package com.iot.qa.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 轻量文本处理工具，用于问答引擎的中文相似度计算。
 *
 * <p>不引入分词组件，采用「字符二元组（bigram）」表示文本：中文没有天然空格分词，
 * 二元组能较好地保留局部语序信息，且实现简单、无需外部词典。</p>
 */
public final class TextUtils {

    /** 标点、空白、控制字符 */
    private static final Pattern NOISE = Pattern.compile("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFFEF]+");

    private TextUtils() {
    }

    /** 归一化：去标点空白、英文转小写 */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return NOISE.matcher(text.toLowerCase()).replaceAll("");
    }

    /** 生成字符二元组集合 */
    public static Set<String> bigrams(String text) {
        String normalized = normalize(text);
        Set<String> grams = new HashSet<>();
        if (normalized.isEmpty()) {
            return grams;
        }
        if (normalized.length() == 1) {
            grams.add(normalized);
            return grams;
        }
        for (int i = 0; i + 2 <= normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }

    /** 杰卡德相似度：交集大小 / 并集大小 */
    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0D;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    /** 文本相似度便捷方法 */
    public static double similarity(String textA, String textB) {
        return jaccard(bigrams(textA), bigrams(textB));
    }

    /**
     * 关键词命中率：命中数 / 期望命中数。
     *
     * @param question        用户问题（归一化后）
     * @param keywords        逗号分隔的关键词串
     * @param expectedHits    期望命中个数，命中达到该数量即视为满分
     */
    public static double keywordHitRate(String question, String keywords, int expectedHits) {
        if (keywords == null || keywords.isBlank()) {
            return 0D;
        }
        String normalizedQuestion = normalize(question);
        long hits = Arrays.stream(keywords.split("[,，、|\\s]+"))
                .map(TextUtils::normalize)
                .filter(k -> !k.isEmpty())
                .distinct()
                .filter(normalizedQuestion::contains)
                .count();
        if (hits == 0) {
            return 0D;
        }
        return Math.min(1D, (double) hits / expectedHits);
    }

    /** 截断文本，超出长度补省略号 */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
    }

    /** 转义 HTML 特殊字符，避免回答内容中的标签破坏页面结构 */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}