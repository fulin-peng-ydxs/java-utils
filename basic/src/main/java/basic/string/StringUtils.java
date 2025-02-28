package basic.string;

import basic.collection.CollectionUtils;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字符串工具类，提供字符串操作、验证、转换等功能
 *
 * @author peng_fu_lin
 * @since 2022-09-08 16:23
 */
public class StringUtils {

    private StringUtils() {
        // Utility class should not be instantiated
    }

    /** 常量 */
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String ELLIPSIS = "...";
    public static final String COMMA = ",";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String BACKSLASH = "\\";
    public static final String UNDERSCORE = "_";
    public static final String HYPHEN = "-";
    public static final String LEFT_BRACE = "{";
    public static final String RIGHT_BRACE = "}";
    public static final String COLON = ":";

    /** 缓存配置 */
    private static final int PATTERN_CACHE_SIZE = 256;
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>(PATTERN_CACHE_SIZE);
    private static final int DEFAULT_BUILDER_SIZE = 256;
    private static final int MAX_ABBREVIATE_LENGTH = 12;
    private static final int MAX_FORMAT_ARGS = 10;

    /** 常用正则表达式 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$");
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    /**
     * 字符清除类型枚举
     */
    public enum ClearCharType {
        /** 清除所有匹配字符 */
        ALL,
        /** 清除开头的匹配字符 */
        START,
        /** 清除结尾的匹配字符 */
        END,
        /** 只清除开头和结尾的匹配字符 */
        NO_MIDDLE
    }

    /**
     * 字符统计类型枚举
     */
    public enum TotalCharType {
        /** 统计所有匹配字符 */
        ALL,
        /** 统计开头的连续匹配字符 */
        START,
        /** 统计结尾的连续匹配字符 */
        END,
        /** 只统计开头和结尾的连续匹配字符 */
        NO_MIDDLE
    }

    /**
     * 检查字符串是否为空
     * @param value 待检查的字符串
     * @param isNotTrim 是否不去除空格
     * @return true如果为空，false如果不为空
     * @author pengfulin
     * @since 2022/9/20 13:48
     */
    public static boolean isEmpty(String value, boolean isNotTrim) {
        if (value == null) {
            return true;
        }
        return isNotTrim ? value.isEmpty() : value.trim().isEmpty();
    }

    /**
     * 检查字符串是否为空（不去除空格）
     * @param value 待检查的字符串
     * @return true如果为空，false如果不为空
     */
    public static boolean isEmpty(String value) {
        return isEmpty(value, true);
    }

    /**
     * 检查字符串是否不为空
     * @param value 待检查的字符串
     * @return true如果不为空，false如果为空
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * 检查对象是否为空字符串
     * @param value 待检查的对象
     * @return true如果为空字符串，false如果不为空字符串
     */
    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String && isEmpty(value.toString(), true);
    }

    /**
     * 格式化字符串（使用{}作为占位符）
     * @param template 模板字符串
     * @param args 参数列表
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + 50);
        int templateStart = 0;
        int argsIndex = 0;

        while (templateStart < template.length()) {
            int placeholderStart = template.indexOf(LEFT_BRACE, templateStart);
            if (placeholderStart < 0) {
                builder.append(template.substring(templateStart));
                break;
            }

            int placeholderEnd = template.indexOf(RIGHT_BRACE, placeholderStart);
            if (placeholderEnd < 0) {
                builder.append(template.substring(templateStart));
                break;
            }

            builder.append(template.substring(templateStart, placeholderStart));
            if (argsIndex < args.length) {
                builder.append(args[argsIndex++]);
            } else {
                builder.append(LEFT_BRACE).append(RIGHT_BRACE);
            }
            templateStart = placeholderEnd + 1;
        }

        return builder.toString();
    }

    /**
     * 使用命名参数格式化字符串
     * @param template 模板字符串
     * @param params 参数Map
     * @return 格式化后的字符串
     */
    public static String formatNamed(String template, Map<String, Object> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + 50);
        int templateStart = 0;

        while (templateStart < template.length()) {
            int placeholderStart = template.indexOf(LEFT_BRACE, templateStart);
            if (placeholderStart < 0) {
                builder.append(template.substring(templateStart));
                break;
            }

            int placeholderEnd = template.indexOf(RIGHT_BRACE, placeholderStart);
            if (placeholderEnd < 0) {
                builder.append(template.substring(templateStart));
                break;
            }

            builder.append(template.substring(templateStart, placeholderStart));
            String key = template.substring(placeholderStart + 1, placeholderEnd);
            Object value = params.get(key);
            builder.append(value != null ? value : LEFT_BRACE + key + RIGHT_BRACE);
            templateStart = placeholderEnd + 1;
        }

        return builder.toString();
    }

    /**
     * 转换字符串编码
     * @param value 源字符串
     * @param fromCharset 源编码
     * @param toCharset 目标编码
     * @return 转换后的字符串
     */
    public static String convertEncoding(String value, Charset fromCharset, Charset toCharset) {
        if (value == null || fromCharset == null || toCharset == null) {
            return value;
        }
        return new String(value.getBytes(fromCharset), toCharset);
    }

    /**
     * 比较两个字符串（忽略大小写和空白）
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return true如果两个字符串相等
     */
    public static boolean equalsIgnoreCaseAndWhitespace(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.trim().equalsIgnoreCase(str2.trim());
    }

    /**
     * 计算两个字符串的相似度（Levenshtein距离）
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 相似度（0-1之间的值，1表示完全相同）
     */
    public static double similarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        if (str1.equals(str2)) {
            return 1.0;
        }

        int[][] distance = new int[str1.length() + 1][str2.length() + 1];
        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                distance[i][j] = Math.min(
                    Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                    distance[i - 1][j - 1] + cost
                );
            }
        }

        int maxLength = Math.max(str1.length(), str2.length());
        return maxLength == 0 ? 1.0 : 
            1.0 - ((double) distance[str1.length()][str2.length()] / maxLength);
    }

    /**
     * 检查字符串是否包含模式字符串（支持通配符 * 和 ?）
     * @param value 待检查的字符串
     * @param pattern 模式字符串
     * @return true如果字符串匹配模式
     */
    public static boolean matchesWildcard(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return value.matches(regex);
    }

    /**
     * 将字符串转换为标题样式（每个单词首字母大写）
     * @param value 源字符串
     * @return 转换后的字符串
     */
    public static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '_') {
                capitalizeNext = true;
                builder.append(c);
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    /**
     * 将字符串转换为固定宽度，不足部分用指定字符填充
     * @param value 源字符串
     * @param width 目标宽度
     * @param padChar 填充字符
     * @param leftPad 是否左填充
     * @return 填充后的字符串
     */
    public static String pad(String value, int width, char padChar, boolean leftPad) {
        if (value == null) {
            return null;
        }
        if (value.length() >= width) {
            return value;
        }

        StringBuilder builder = new StringBuilder(width);
        int padLength = width - value.length();

        if (leftPad) {
            for (int i = 0; i < padLength; i++) {
                builder.append(padChar);
            }
            builder.append(value);
        } else {
            builder.append(value);
            for (int i = 0; i < padLength; i++) {
                builder.append(padChar);
            }
        }

        return builder.toString();
    }

    /**
     * 检查字符串是否为空白
     * @param value 待检查的字符串
     * @return true如果字符串为null或只包含空白字符
     */
    public static boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查字符串是否不为空白
     * @param value 待检查的字符串
     * @return true如果字符串不为null且包含非空白字符
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * 获取字符串的缩写形式
     * @param value 源字符串
     * @param maxLength 最大长度
     * @param appendEllipsis 是否添加省略号
     * @return 缩写后的字符串
     */
    public static String abbreviate(String value, int maxLength, boolean appendEllipsis) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return EMPTY;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        int actualMaxLength = appendEllipsis ? maxLength - ELLIPSIS.length() : maxLength;
        return value.substring(0, actualMaxLength) + (appendEllipsis ? ELLIPSIS : EMPTY);
    }

    /**
     * 获取字符串的缩写形式（默认添加省略号）
     * @param value 源字符串
     * @param maxLength 最大长度
     * @return 缩写后的字符串
     */
    public static String abbreviate(String value, int maxLength) {
        return abbreviate(value, maxLength, true);
    }

    /**
     * 重复字符串指定次数
     * @param value 源字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String value, int count) {
        if (value == null || count <= 0) {
            return EMPTY;
        }
        if (count == 1) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    /**
     * 反转字符串
     * @param value 源字符串
     * @return 反转后的字符串
     */
    public static String reverse(String value) {
        if (value == null) {
            return null;
        }
        return new StringBuilder(value).reverse().toString();
    }

    /**
     * 检查字符串是否为有效的电子邮件地址
     * @param value 待检查的字符串
     * @return true如果是有效的电子邮件地址
     */
    public static boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /**
     * 检查字符串是否为有效的URL
     * @param value 待检查的字符串
     * @return true如果是有效的URL
     */
    public static boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    /**
     * 检查字符串是否为有效的IPv4地址
     * @param value 待检查的字符串
     * @return true如果是有效的IPv4地址
     */
    public static boolean isIpv4(String value) {
        return value != null && IPV4_PATTERN.matcher(value).matches();
    }

    /**
     * 检查字符串是否包含中文字符
     * @param value 待检查的字符串
     * @return true如果包含中文字符
     */
    public static boolean containsChinese(String value) {
        return value != null && CHINESE_PATTERN.matcher(value).find();
    }

    /**
     * 标准化字符串（去除重音符号等）
     * @param value 源字符串
     * @return 标准化后的字符串
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}", "");
    }

    /**
     * 将字符串转换为驼峰命名
     * @param value 源字符串
     * @return 驼峰命名的字符串
     */
    public static String toCamelCase(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = false;
        
        for (char c : value.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else {
                builder.append(capitalizeNext ? Character.toUpperCase(c) : 
                    Character.toLowerCase(c));
                capitalizeNext = false;
            }
        }
        return builder.toString();
    }

    /**
     * 将字符串转换为下划线命名
     * @param value 源字符串
     * @return 下划线命名的字符串
     */
    public static String toSnakeCase(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length() + 4);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 将字符串首字母大写
     * @param value 源字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + 
            (value.length() > 1 ? value.substring(1) : "");
    }

    /**
     * 将字符串首字母小写
     * @param value 源字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + 
            (value.length() > 1 ? value.substring(1) : "");
    }

    /**
     * 将字符串按指定长度分割
     * @param value 源字符串
     * @param length 分割长度
     * @return 分割后的字符串列表
     */
    public static List<String> split(String value, int length) {
        if (value == null || length <= 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            result.add(value.substring(index, 
                Math.min(index + length, value.length())));
            index += length;
        }
        return result;
    }

    /**
     * 清除指定字符
     * @param value 待清除字符的字符串
     * @param charValue 待清除的字符
     * @param clearType 清除字符方式
     * @param clearCount 待清除字符的个数，如果小于1则清除所有匹配字符
     * @return 清除指定字符后的字符串，如果输入为null则返回null
     * @author pengfulin
     * @since 2022/9/9 15:47
     */
    public static String clearChar(String value, char charValue, ClearCharType clearType, int clearCount) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (clearType == null) {
            return value;
        }

        int actualClearCount = clearCount < 1 ? Integer.MAX_VALUE : clearCount;

        switch (clearType) {
            case ALL:
                return clearAllChars(value, charValue, actualClearCount);
            case START:
                return clearStartChars(value, charValue, actualClearCount);
            case END:
                return clearEndChars(value, charValue, actualClearCount);
            case NO_MIDDLE:
                String start = clearStartChars(value, charValue, actualClearCount);
                int remainingCount = actualClearCount - (value.length() - start.length());
                return remainingCount > 0 ? clearEndChars(start, charValue, remainingCount) : start;
            default:
                return value;
        }
    }

    private static String clearAllChars(String value, char charValue, int maxCount) {
        StringBuilder builder = new StringBuilder(value.length());
        int count = 0;
        // 使用charAt替代toCharArray()以减少内存分配
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == charValue && count < maxCount) {
                count++;
                continue;
            }
            builder.append(c);
        }
        return builder.length() == value.length() ? value : builder.toString();
    }

    private static String clearStartChars(String value, char charValue, int maxCount) {
        int startIndex = 0;
        while (startIndex < value.length() && value.charAt(startIndex) == charValue && startIndex < maxCount) {
            startIndex++;
        }
        return startIndex == 0 ? value : value.substring(startIndex);
    }

    private static String clearEndChars(String value, char charValue, int maxCount) {
        int endIndex = value.length();
        int count = 0;
        while (endIndex > 0 && value.charAt(endIndex - 1) == charValue && count < maxCount) {
            endIndex--;
            count++;
        }
        return endIndex == value.length() ? value : value.substring(0, endIndex);
    }

    /**
     * 清除首尾字符
     * @param value 待清除的字符串
     * @param charValue 待清除的字符
     * @param fromHead true从头部清除，false从尾部清除
     * @return 清除后的字符串
     * @author pengshuaifeng
     * @since 2023/9/20 23:20
     */
    public static String clearChar(String value, char charValue, boolean fromHead) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        if (fromHead && value.charAt(0) == charValue) {
            return value.substring(1);
        } else if (!fromHead && value.charAt(value.length() - 1) == charValue) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 清除多个字符
     * @param value 待清除的字符串
     * @param clearType 清除类型
     * @param chars 待清除的字符数组
     * @return 清除后的字符串
     * @author pengfulin
     * @since 2022/9/19 16:25
     */
    public static String clearChars(String value, ClearCharType clearType, char... chars) {
        if (value == null || chars == null || chars.length == 0) {
            return value;
        }
        
        String result = value;
        for (char c : chars) {
            result = clearChar(result, c, clearType, -1);
        }
        return result;
    }

    /**
     * 清除首尾空白符（空格、制表符、换行符）
     * @param value 待清除空白符的字符串
     * @return 清除后的字符串，如果输入为null则返回null
     * @author pengshuaifeng
     * @since 2023/9/9 00:50
     */
    public static String cleanSpan(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 统计指定字符出现次数
     * @param value 待统计的字符串
     * @param charValue 待统计的字符
     * @param totalType 统计类型
     * @return 指定字符的出现次数
     * @author pengfulin
     * @since 2022/9/19 16:28
     */
    public static int countChar(String value, char charValue, TotalCharType totalType) {
        if (value == null) {
            return 0;
        }

        switch (totalType) {
            case ALL:
                return countAllChars(value, charValue);
            case START:
                return countStartChars(value, charValue);
            case END:
                return countEndChars(value, charValue);
            case NO_MIDDLE:
                return countStartChars(value, charValue) + countEndChars(value, charValue);
            default:
                return 0;
        }
    }

    private static int countAllChars(String value, char charValue) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c == charValue) {
                count++;
            }
        }
        return count;
    }

    private static int countStartChars(String value, char charValue) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if (c != charValue) {
                break;
            }
            count++;
        }
        return count;
    }

    private static int countEndChars(String value, char charValue) {
        int count = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            if (value.charAt(i) != charValue) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 查找字符串位置
     * @param value 源字符串
     * @param target 目标字符串
     * @param fromStart 是否从开始位置查找
     * @param inclusive 是否包含目标字符串
     * @param forward 是否向前计算索引
     * @return 目标字符串的位置，如果未找到则返回-1
     */
    private static int findIndex(String value, String target, boolean fromStart,
            boolean inclusive, boolean forward) {
        if (value == null || target == null) {
            return -1;
        }
        
        int index = fromStart ? value.indexOf(target) : value.lastIndexOf(target);
        if (index < 0) {
            return index;
        }
        
        if (forward) {
            return inclusive ? index : index + target.length();
        } else {
            return inclusive ? index + target.length() - 1 : index;
        }
    }

    /**
     * 提取字符串
     * @param value 待提取的字符串
     * @param start 开始字符串
     * @param end 结束字符串
     * @param isContain 是否包含开始/结束字符串
     * @param isStartIndex 是否从前开始搜索
     * @return 提取的字符串，如果无法提取则返回null
     * @author pengfulin
     * @since 2022/9/26 16:00
     */
    public static String substring(String value, String start, String end, 
            boolean isContain, boolean isStartIndex) {
        if (value == null) {
            return null;
        }
        
        // 如果start和end都为空
        if (isEmpty(start) && isEmpty(end)) {
            return null;
        }

        // 如果只有start或end为空
        if (isEmpty(start) || isEmpty(end)) {
            return extractWithSingleBoundary(value, start, end, isContain, isStartIndex);
        }

        // 如果start和end都不为空
        return extractWithBothBoundaries(value, start, end, isContain, isStartIndex);
    }

    private static String extractWithSingleBoundary(String value, String start, String end,
            boolean isContain, boolean isStartIndex) {
        int index;
        if (isEmpty(start)) {
            index = findIndex(value, end, isStartIndex, isContain, false);
            return index < 0 ? null : value.substring(0, index);
        } else {
            index = findIndex(value, start, isStartIndex, isContain, true);
            return index < 0 ? null : value.substring(index);
        }
    }

    private static String extractWithBothBoundaries(String value, String start, String end,
            boolean isContain, boolean isStartIndex) {
        int startIndex = findIndex(value, start, isStartIndex, isContain, true);
        if (startIndex < 0) {
            return null;
        }
        
        String remainingValue = value.substring(startIndex);
        int endIndex = findIndex(remainingValue, end, isStartIndex, isContain, false);
        if (endIndex < 0) {
            return null;
        }
        
        return remainingValue.substring(0, endIndex);
    }

    /**
     * 提取字符串（默认包含边界）
     * @param value 待提取的字符串
     * @param start 开始字符串
     * @param end 结束字符串
     * @param isContain 是否包含开始/结束字符串
     * @return 提取的字符串
     */
    public static String substring(String value, String start, String end, boolean isContain) {
        return substring(value, start, end, isContain, true);
    }
}
