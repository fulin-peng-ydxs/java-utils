package basic.copy;

import basic.clazz.ClassUtils;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对象拷贝工具类，提供浅拷贝和深拷贝功能
 *
 * @author pengshuaifeng
 * @since 2023/12/24
 */
public class ObjectCopyUtils {

    private ObjectCopyUtils() {
        // Utility class should not be instantiated
    }

    /** 字段缓存，提高反射性能 */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 对象拷贝（默认不包含父类属性且不覆盖目标对象非空属性）
     * 
     * @param source 源对象
     * @param target 目标对象
     * @param <T> 目标对象类型
     * @return 拷贝后的目标对象
     * @throws IllegalArgumentException 如果源对象或目标对象为null
     * @author pengfulin
     * @since 2023/2/10 10:57
     */
    public static <T> T copy(Object source, T target) {
        return copy(source, target, false, false);
    }

    /**
     * 对象拷贝（默认不覆盖目标对象非空属性）
     * 
     * @param source 源对象
     * @param target 目标对象
     * @param includeSuper 是否包含父类属性
     * @param <T> 目标对象类型
     * @return 拷贝后的目标对象
     * @throws IllegalArgumentException 如果源对象或目标对象为null
     */
    public static <T> T copy(Object source, T target, boolean includeSuper) {
        return copy(source, target, includeSuper, false);
    }

    /**
     * 对象拷贝
     * 
     * @param source 源对象
     * @param target 目标对象
     * @param includeSuper 是否包含父类属性
     * @param overwrite 是否覆盖目标对象非空属性
     * @param <T> 目标对象类型
     * @return 拷贝后的目标对象
     * @throws IllegalArgumentException 如果源对象或目标对象为null
     * @author pengfulin
     * @since 2023/2/10 10:57
     */
    public static <T> T copy(Object source, T target, boolean includeSuper, boolean overwrite) {
        validateParameters(source, target);

        try {
            // 获取源对象和目标对象的属性
            List<Field> sourceFields = getCachedFields(source.getClass(), includeSuper);
            List<Field> targetFields = getCachedFields(target.getClass(), includeSuper);

            // 源对象属性映射
            Map<String, Field> sourceFieldMap = sourceFields.stream()
                    .collect(Collectors.toMap(
                        Field::getName,
                        Function.identity(),
                        (existing, replacement) -> existing // 保留第一个字段
                    ));

            // 复制属性
            copyFields(source, target, targetFields, sourceFieldMap, overwrite);

            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象拷贝失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建对象的深拷贝
     * 
     * @param source 源对象
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 深拷贝后的对象
     * @throws IllegalArgumentException 如果参数无效
     */
    public static <T> T deepCopy(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            throw new IllegalArgumentException("源对象和目标类不能为null");
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            return copy(source, target, true, true);
        } catch (Exception e) {
            throw new RuntimeException("创建深拷贝失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建对象的浅拷贝
     * 
     * @param source 源对象
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 浅拷贝后的对象
     * @throws IllegalArgumentException 如果参数无效
     */
    public static <T> T shallowCopy(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            throw new IllegalArgumentException("源对象和目标类不能为null");
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            return copy(source, target, false, true);
        } catch (Exception e) {
            throw new RuntimeException("创建浅拷贝失败: " + e.getMessage(), e);
        }
    }

    /**
     * 复制集合中的对象
     * 
     * @param sources 源对象集合
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 复制后的对象集合
     * @throws IllegalArgumentException 如果参数无效
     */
    public static <T> List<T> copyList(Collection<?> sources, Class<T> targetClass) {
        if (sources == null || targetClass == null) {
            throw new IllegalArgumentException("源集合和目标类不能为null");
        }

        return sources.stream()
                .map(source -> shallowCopy(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 验证参数
     */
    private static void validateParameters(Object source, Object target) {
        if (source == null) {
            throw new IllegalArgumentException("源对象不能为null");
        }
        if (target == null) {
            throw new IllegalArgumentException("目标对象不能为null");
        }
    }

    /**
     * 获取缓存的字段列表
     */
    private static List<Field> getCachedFields(Class<?> clazz, boolean includeSuper) {
        return FIELD_CACHE.computeIfAbsent(
            clazz,
            k -> Collections.unmodifiableList(ClassUtils.getFields(k, includeSuper, null))
        );
    }

    /**
     * 复制字段值
     */
    private static void copyFields(Object source, Object target, List<Field> targetFields,
            Map<String, Field> sourceFieldMap, boolean overwrite) throws IllegalAccessException {
        for (Field targetField : targetFields) {
            Field sourceField = sourceFieldMap.get(targetField.getName());
            if (canCopyField(sourceField, targetField)) {
                copyFieldValue(source, target, sourceField, targetField, overwrite);
            }
        }
    }

    /**
     * 检查字段是否可以复制
     */
    private static boolean canCopyField(Field sourceField, Field targetField) {
        return sourceField != null && 
               ClassUtils.typeEquals(sourceField.getType(), targetField.getType());
    }

    /**
     * 复制字段值
     */
    private static void copyFieldValue(Object source, Object target, Field sourceField,
            Field targetField, boolean overwrite) throws IllegalAccessException {
        sourceField.setAccessible(true);
        targetField.setAccessible(true);

        Object targetValue = targetField.get(target);
        if (targetValue == null || overwrite) {
            Object sourceValue = sourceField.get(source);
            targetField.set(target, sourceValue);
        }
    }
}
