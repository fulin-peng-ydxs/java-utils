package basic.thread;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 线程变量工具类，提供线程安全的变量存储和访问功能
 * 注意：使用完ThreadLocal变量后，请务必调用remove()方法，避免内存泄漏
 *
 * @author pengshuaifeng
 * @since 2024/2/28
 */
public class ThreadLocalUtil {

    private ThreadLocalUtil() {
        // Utility class should not be instantiated
    }

    /** 存储线程变量的ThreadLocal */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** 类型缓存，用于提高类型转换性能 */
    private static final Map<String, Class<?>> TYPE_CACHE = new ConcurrentHashMap<>();

    /**
     * 设置线程变量
     * @param map 要存储的Map
     * @throws IllegalArgumentException 如果map为null
     */
    public static void set(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map不能为null");
        }
        Map<String, Object> threadMap = THREAD_LOCAL.get();
        threadMap.clear();
        threadMap.putAll(map);
    }

    /**
     * 设置线程变量
     * @param key 键
     * @param value 值
     * @throws IllegalArgumentException 如果key为null
     */
    public static void set(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key不能为null");
        }
        THREAD_LOCAL.get().put(key, value);
    }

    /**
     * 获取所有线程变量
     * @return 不可修改的Map副本
     */
    public static Map<String, Object> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(THREAD_LOCAL.get()));
    }

    /**
     * 获取指定类型的线程变量
     * @param key 键
     * @param type 值的类型
     * @return 类型转换后的值，如果不存在则返回null
     * @throws ClassCastException 如果类型转换失败
     */
    public static <T> T get(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }
        Object value = THREAD_LOCAL.get().get(key);
        if (value == null) {
            return null;
        }
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(
                "无法将值 '%s' 转换为类型 '%s'", value, type.getName()));
        }
    }

    /**
     * 获取指定类型的线程变量，如果不存在则返回默认值
     * @param key 键
     * @param type 值的类型
     * @param defaultValue 默认值
     * @return 值或默认值
     */
    public static <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取指定类型的线程变量，如果不存在则使用提供者生成
     * @param key 键
     * @param type 值的类型
     * @param supplier 值的提供者
     * @return 值或新生成的值
     * @throws IllegalArgumentException 如果supplier为null
     */
    public static <T> T getOrSupply(String key, Class<T> type, Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier不能为null");
        }
        T value = get(key, type);
        if (value == null) {
            value = supplier.get();
            if (value != null) {
                set(key, value);
            }
        }
        return value;
    }

    /**
     * 获取字符串类型的线程变量
     * @param key 键
     * @return 字符串值，如果不存在则返回null
     */
    public static String getString(String key) {
        return get(key, String.class);
    }

    /**
     * 获取Integer类型的线程变量
     * @param key 键
     * @return Integer值，如果不存在则返回null
     */
    public static Integer getInteger(String key) {
        return get(key, Integer.class);
    }

    /**
     * 获取Long类型的线程变量
     * @param key 键
     * @return Long值，如果不存在则返回null
     */
    public static Long getLong(String key) {
        return get(key, Long.class);
    }

    /**
     * 获取Boolean类型的线程变量
     * @param key 键
     * @return Boolean值，如果不存在则返回null
     */
    public static Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    /**
     * 获取Double类型的线程变量
     * @param key 键
     * @return Double值，如果不存在则返回null
     */
    public static Double getDouble(String key) {
        return get(key, Double.class);
    }

    /**
     * 检查是否存在指定键的线程变量
     * @param key 键
     * @return true如果存在，false如果不存在
     */
    public static boolean contains(String key) {
        return key != null && THREAD_LOCAL.get().containsKey(key);
    }

    /**
     * 移除指定键的线程变量
     * @param key 键
     * @return 被移除的值，如果不存在则返回null
     */
    public static Object remove(String key) {
        return key != null ? THREAD_LOCAL.get().remove(key) : null;
    }

    /**
     * 清除当前线程的所有线程变量
     * 注意：为防止内存泄漏，在不需要使用ThreadLocal变量时应该调用此方法
     */
    public static void clear() {
        Map<String, Object> map = THREAD_LOCAL.get();
        map.clear();
        THREAD_LOCAL.remove();
    }

    /**
     * 获取当前线程变量的大小
     * @return 线程变量的数量
     */
    public static int size() {
        return THREAD_LOCAL.get().size();
    }

    /**
     * 原子操作：如果不存在则设置值
     * @param key 键
     * @param value 值
     * @return true如果设置成功，false如果已存在
     */
    public static boolean setIfAbsent(String key, Object value) {
        if (key == null) {
            return false;
        }
        Map<String, Object> map = THREAD_LOCAL.get();
        if (!map.containsKey(key)) {
            map.put(key, value);
            return true;
        }
        return false;
    }

    /**
     * 原子操作：如果存在则替换值
     * @param key 键
     * @param oldValue 旧值
     * @param newValue 新值
     * @return true如果替换成功，false如果值不匹配或键不存在
     */
    public static boolean replace(String key, Object oldValue, Object newValue) {
        if (key == null) {
            return false;
        }
        Map<String, Object> map = THREAD_LOCAL.get();
        return map.containsKey(key) && 
               Objects.equals(map.get(key), oldValue) && 
               map.replace(key, oldValue, newValue);
    }

    /**
     * 执行操作并自动清理线程变量
     * @param runnable 要执行的操作
     * @throws IllegalArgumentException 如果runnable为null
     */
    public static void doWithClear(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable不能为null");
        }
        try {
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * 执行操作并返回结果，自动清理线程变量
     * @param supplier 要执行的操作
     * @return 操作的结果
     * @throws IllegalArgumentException 如果supplier为null
     */
    public static <T> T supplyWithClear(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier不能为null");
        }
        try {
            return supplier.get();
        } finally {
            clear();
        }
    }
}
