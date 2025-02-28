package basic.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 异常工具类，提供异常处理、分析、过滤等功能
 *
 * @author fulin-peng
 * @since 2024-12-04
 */
public class ExceptionUtils {

    private ExceptionUtils() {
        // Utility class should not be instantiated
    }

    /** 常量 */
    private static final int THROWABLE_LIST_INITIAL_CAPACITY = 8;
    private static final String WRAPPED_MARKER = " [wrapped] ";
    private static final String CAUSE_MARKER = "Caused by: ";
    private static final String SUPPRESSED_MARKER = "Suppressed: ";
    private static final int MAX_SUPPRESSED_EXCEPTIONS = 50;
    private static final String PACKAGE_SEPARATOR = ".";
    private static final String NEWLINE = System.lineSeparator();

    /** 错误消息 */
    private static final String NULL_EXCEPTION = "异常对象不能为null";
    private static final String NULL_TYPE = "异常类型不能为null";
    private static final String NULL_PREDICATE = "过滤条件不能为null";
    private static final String NULL_MESSAGE = "异常消息不能为null";
    private static final String NULL_WRAPPER = "包装异常不能为null";
    private static final String NULL_PATTERN = "正则表达式不能为null";
    /**
     * 过滤堆栈跟踪元素
     * @param throwable 异常对象
     * @param filter 过滤条件
     * @return 过滤后的异常对象
     * @throws IllegalArgumentException 如果参数为null
     */
    public static Throwable filterStackTrace(Throwable throwable, Predicate<StackTraceElement> filter) {
        if (throwable == null || filter == null) {
            throw new IllegalArgumentException("参数不能为null");
        }

        StackTraceElement[] trace = throwable.getStackTrace();
        List<StackTraceElement> filtered = Arrays.stream(trace)
            .filter(filter)
            .collect(Collectors.toList());
        throwable.setStackTrace(filtered.toArray(new StackTraceElement[0]));

        // 递归处理cause和suppressed异常
        Throwable cause = throwable.getCause();
        if (cause != null) {
            filterStackTrace(cause, filter);
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            filterStackTrace(suppressed, filter);
        }

        return throwable;
    }

    /**
     * 移除指定包名的堆栈跟踪
     * @param throwable 异常对象
     * @param packageName 要移除的包名
     * @return 过滤后的异常对象
     * @throws IllegalArgumentException 如果参数为null
     */
    public static Throwable removePackageFromStackTrace(Throwable throwable, String packageName) {
        if (packageName == null) {
            throw new IllegalArgumentException("包名不能为null");
        }
        return filterStackTrace(throwable, 
            element -> !element.getClassName().startsWith(packageName + PACKAGE_SEPARATOR));
    }

    /**
     * 比较两个异常是否相似（类型相同且消息相似）
     * @param first 第一个异常
     * @param second 第二个异常
     * @return true如果两个异常相似
     */
    public static boolean areSimilar(Throwable first, Throwable second) {
        if (first == null || second == null) {
            return first == second;
        }
        if (first.getClass() != second.getClass()) {
            return false;
        }
        String msg1 = getMessage(first);
        String msg2 = getMessage(second);
        if (msg1 == null || msg2 == null) {
            return msg1 == msg2;
        }
        // 移除数字和特定字符后比较
        return msg1.replaceAll("[\\d\\[\\]\\{\\}\\(\\)\\s]", "")
            .equalsIgnoreCase(msg2.replaceAll("[\\d\\[\\]\\{\\}\\(\\)\\s]", ""));
    }

    /**
     * 格式化异常信息
     * @param throwable 异常对象
     * @param format 格式字符串，支持以下占位符：
     *               %m - 异常消息
     *               %t - 异常类型
     *               %s - 堆栈跟踪
     *               %r - 根异常
     *               %n - 换行
     * @return 格式化后的异常信息
     * @throws IllegalArgumentException 如果参数为null
     */
    public static String format(Throwable throwable, String format) {
        if (throwable == null || format == null) {
            throw new IllegalArgumentException("参数不能为null");
        }

        return format.replace("%m", getMessage(throwable))
            .replace("%t", throwable.getClass().getName())
            .replace("%s", getStackTrace(throwable))
            .replace("%r", getMessage(getRootCause(throwable)))
            .replace("%n", NEWLINE);
    }

    /**
     * 根据正则表达式匹配异常消息
     * @param throwable 异常对象
     * @param pattern 正则表达式
     * @return true如果异常消息匹配正则表达式
     * @throws IllegalArgumentException 如果参数为null
     */
    public static boolean matchesMessage(Throwable throwable, String pattern) {
        if (throwable == null || pattern == null) {
            throw new IllegalArgumentException(NULL_PATTERN);
        }
        String message = getMessage(throwable);
        return message != null && Pattern.compile(pattern).matcher(message).find();
    }

    /**
     * 获取异常的唯一标识（类型 + 消息的哈希值）
     * @param throwable 异常对象
     * @return 异常的唯一标识
     */
    public static String getExceptionId(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = getMessage(throwable);
        return throwable.getClass().getName() + "#" + 
            (message != null ? Math.abs(message.hashCode()) : "0");
    }

    /**
     * 获取异常的摘要信息（不包含堆栈）
     * @param throwable 异常对象
     * @return 异常的摘要信息
     */
    public static String getSummary(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName());
        String message = getMessage(throwable);
        if (message != null) {
            sb.append(": ").append(message);
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append(NEWLINE).append(CAUSE_MARKER).append(getSummary(cause));
        }
        return sb.toString();
    }

    /**
     * 获取异常链中的根异常（最初始的异常）
     * 
     * @param throwable 异常对象
     * @return 根异常，如果输入为null则返回null
     * @author fulin-peng
     * @since 2024/12/4
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * 获取所有被抑制的异常
     * 
     * @param throwable 异常对象
     * @return 被抑制的异常列表，如果没有则返回空列表
     */
    public static List<Throwable> getSuppressedExceptions(Throwable throwable) {
        if (throwable == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(throwable.getSuppressed());
    }

    /**
     * 添加被抑制的异常
     * 
     * @param throwable 主异常
     * @param suppressed 要添加的被抑制异常
     * @throws IllegalArgumentException 如果参数为null
     */
    public static void addSuppressed(Throwable throwable, Throwable suppressed) {
        if (throwable == null || suppressed == null) {
            throw new IllegalArgumentException("异常对象不能为null");
        }
        if (throwable != suppressed) {
            throwable.addSuppressed(suppressed);
        }
    }

    /**
     * 获取完整的异常链
     * 异常链按照从最新到最早的顺序排列，即：
     * - 第一个元素是传入的异常
     * - 最后一个元素是根异常
     *
     * @param throwable 异常对象
     * @return 异常链列表，如果输入为null则返回空列表
     * @author fulin-peng
     * @since 2024/12/4
     */
    public static List<Throwable> getThrowableList(Throwable throwable) {
        if (throwable == null) {
            return Collections.emptyList();
        }

        List<Throwable> list = new ArrayList<>(THROWABLE_LIST_INITIAL_CAPACITY);
        while (throwable != null && !list.contains(throwable)) {
            list.add(throwable);
            throwable = throwable.getCause();
        }
        return list;
    }

    /**
     * 获取异常的堆栈跟踪信息
     *
     * @param throwable 异常对象
     * @param includeSuppress 是否包含被抑制的异常
     * @param limit 堆栈深度限制，小于等于0表示不限制
     * @return 堆栈跟踪字符串，如果输入为null则返回null
     */
    public static String getStackTrace(Throwable throwable, boolean includeSuppress, int limit) {
        if (throwable == null) {
            return null;
        }
        
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            if (limit <= 0) {
                throwable.printStackTrace(pw);
            } else {
                pw.println(throwable);
                StackTraceElement[] trace = throwable.getStackTrace();
                int printLen = Math.min(limit, trace.length);
                for (int i = 0; i < printLen; i++) {
                    pw.println("\tat " + trace[i]);
                }
                if (printLen < trace.length) {
                    pw.println("\t... " + (trace.length - printLen) + " more");
                }
                
                if (includeSuppress) {
                    for (Throwable suppressed : throwable.getSuppressed()) {
                        pw.print(SUPPRESSED_MARKER);
                        getStackTrace(suppressed, true, limit);
                    }
                }
                
                Throwable cause = throwable.getCause();
                if (cause != null) {
                    pw.print(CAUSE_MARKER);
                    getStackTrace(cause, includeSuppress, limit);
                }
            }
            return sw.toString();
        } catch (Exception e) {
            return throwable.toString();
        }
    }

    /**
     * 获取异常的堆栈跟踪信息（默认包含被抑制的异常且不限制深度）
     *
     * @param throwable 异常对象
     * @return 堆栈跟踪字符串
     */
    public static String getStackTrace(Throwable throwable) {
        return getStackTrace(throwable, true, 0);
    }

    /**
     * 获取异常消息，如果异常消息为空则获取异常类名
     *
     * @param throwable 异常对象
     * @return 异常消息或类名，如果输入为null则返回null
     */
    public static String getMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getName();
    }

    /**
     * 获取完整的异常消息，包括所有嵌套异常和被抑制异常的消息
     *
     * @param throwable 异常对象
     * @param includeSuppress 是否包含被抑制的异常
     * @return 完整的异常消息链，如果输入为null则返回null
     */
    public static String getFullMessage(Throwable throwable, boolean includeSuppress) {
        if (throwable == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        buildFullMessage(throwable, sb, "", includeSuppress);
        return sb.toString();
    }

    /**
     * 递归构建完整异常消息
     */
    private static void buildFullMessage(Throwable throwable, StringBuilder sb, 
            String prefix, boolean includeSuppress) {
        if (prefix.length() > 0) {
            sb.append("\n").append(prefix);
        }
        sb.append(getMessage(throwable));

        if (includeSuppress) {
            for (Throwable suppressed : throwable.getSuppressed()) {
                buildFullMessage(suppressed, sb, SUPPRESSED_MARKER, true);
            }
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            buildFullMessage(cause, sb, CAUSE_MARKER, includeSuppress);
        }
    }

    /**
     * 获取完整的异常消息（默认包含被抑制的异常）
     *
     * @param throwable 异常对象
     * @return 完整的异常消息链
     */
    public static String getFullMessage(Throwable throwable) {
        return getFullMessage(throwable, true);
    }

    /**
     * 转换异常类型
     *
     * @param throwable 原始异常
     * @param mapper 异常转换函数
     * @return 转换后的异常
     * @throws IllegalArgumentException 如果参数为null
     */
    public static Throwable transform(Throwable throwable, 
            Function<Throwable, ? extends Throwable> mapper) {
        if (throwable == null || mapper == null) {
            throw new IllegalArgumentException("参数不能为null");
        }
        return mapper.apply(throwable);
    }

    /**
     * 过滤异常链
     *
     * @param throwable 异常对象
     * @param filter 过滤条件
     * @return 过滤后的异常列表
     * @throws IllegalArgumentException 如果过滤条件为null
     */
    public static List<Throwable> filterChain(Throwable throwable, Predicate<Throwable> filter) {
        if (filter == null) {
            throw new IllegalArgumentException(NULL_PREDICATE);
        }
        List<Throwable> chain = getThrowableList(throwable);
        return chain.stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    /**
     * 判断异常链中是否包含指定类型的异常
     *
     * @param throwable 异常对象
     * @param type 要检查的异常类型
     * @return true如果异常链中包含指定类型的异常
     * @throws IllegalArgumentException 如果type为null
     */
    public static boolean containsType(Throwable throwable, Class<? extends Throwable> type) {
        if (type == null) {
            throw new IllegalArgumentException(NULL_TYPE);
        }
        if (throwable == null) {
            return false;
        }

        return findFirstMatchingException(throwable, t -> type.isInstance(t)) != null;
    }

    /**
     * 在异常链中查找第一个匹配条件的异常
     *
     * @param throwable 异常对象
     * @param predicate 匹配条件
     * @return 第一个匹配的异常，如果没有找到则返回null
     * @throws IllegalArgumentException 如果predicate为null
     */
    public static Throwable findFirstMatchingException(Throwable throwable, 
            Predicate<Throwable> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException(NULL_PREDICATE);
        }
        if (throwable == null) {
            return null;
        }

        List<Throwable> chain = getThrowableList(throwable);
        for (Throwable t : chain) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 包装异常，保持原始异常作为cause
     *
     * @param throwable 原始异常
     * @param wrapper 包装异常类型
     * @return 包装后的异常
     * @throws IllegalArgumentException 如果参数无效
     */
    public static Throwable wrap(Throwable throwable, Class<? extends Throwable> wrapper) {
        if (throwable == null) {
            throw new IllegalArgumentException(NULL_EXCEPTION);
        }
        if (wrapper == null) {
            throw new IllegalArgumentException(NULL_WRAPPER);
        }

        try {
            return wrapper.getConstructor(String.class, Throwable.class)
                .newInstance(throwable.getMessage() + WRAPPED_MARKER + wrapper.getSimpleName(), 
                    throwable);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | 
                InvocationTargetException e) {
            return throwable;
        }
    }

    /**
     * 解包异常，如果是InvocationTargetException则获取其目标异常
     *
     * @param throwable 异常对象
     * @return 解包后的异常
     */
    public static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            return ((InvocationTargetException) throwable).getTargetException();
        }
        return throwable;
    }

    /**
     * 重新抛出异常，保持原始堆栈信息
     *
     * @param throwable 异常对象
     * @throws RuntimeException 包装后的运行时异常
     */
    public static void rethrow(Throwable throwable) {
        if (throwable == null) {
            throw new IllegalArgumentException(NULL_EXCEPTION);
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        throw new RuntimeException(throwable.getMessage(), throwable);
    }

    /**
     * 获取异常的简短描述（类名 + 消息）
     *
     * @param throwable 异常对象
     * @return 简短描述，如果输入为null则返回null
     */
    public static String getSimpleMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getClass().getSimpleName() + ": " + getMessage(throwable);
    }

    /**
     * 判断是否为检查型异常
     *
     * @param throwable 异常对象
     * @return true如果是检查型异常
     */
    public static boolean isCheckedException(Throwable throwable) {
        return throwable != null && !(throwable instanceof RuntimeException || 
            throwable instanceof Error);
    }

    /**
     * 判断异常是否由指定原因引起
     *
     * @param throwable 异常对象
     * @param causeType 原因异常类型
     * @return true如果异常由指定原因引起
     * @throws IllegalArgumentException 如果causeType为null
     */
    public static boolean isCausedBy(Throwable throwable, Class<? extends Throwable> causeType) {
        if (causeType == null) {
            throw new IllegalArgumentException(NULL_TYPE);
        }
        return throwable != null && causeType.isInstance(getRootCause(throwable));
    }
}
