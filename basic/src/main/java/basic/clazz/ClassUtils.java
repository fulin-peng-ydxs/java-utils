package basic.clazz;

import basic.string.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 类工具类，提供反射、类型操作、注解处理等功能
 *
 * @author pengshuaifeng
 * @since 2023/12/18
 */
public class ClassUtils {

    private ClassUtils() {
        // Utility class should not be instantiated
    }

    /** 缓存配置 */
    private static final int DEFAULT_CACHE_SIZE = 256;
    private static final int DEFAULT_COLLECTION_SIZE = 16;

    /** 字段缓存，提高反射性能 */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = 
        new ConcurrentHashMap<>(DEFAULT_CACHE_SIZE);

    /** 方法缓存，提高反射性能 */
    private static final Map<Class<?>, Map<MethodKey, Method>> METHOD_CACHE = 
        new ConcurrentHashMap<>(DEFAULT_CACHE_SIZE);

    /** 构造函数缓存 */
    private static final Map<Class<?>, Map<ConstructorKey, Constructor<?>>> CONSTRUCTOR_CACHE = 
        new ConcurrentHashMap<>(DEFAULT_CACHE_SIZE);

    /** 基本类型包装类映射 */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>(8);

    static {
        PRIMITIVE_WRAPPER_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(char.class, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(double.class, Double.class);
        PRIMITIVE_WRAPPER_MAP.put(float.class, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(int.class, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(long.class, Long.class);
        PRIMITIVE_WRAPPER_MAP.put(short.class, Short.class);
    }

    /**
     * 创建实例（使用无参构造函数）
     * @param clazz 类对象
     * @return 新实例
     * @throws RuntimeException 如果创建实例失败
     */
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("类对象不能为null");
        }
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建实例失败: " + clazz.getName(), e);
        }
    }

    /**
     * 创建实例（使用带参构造函数）
     * @param clazz 类对象
     * @param parameterTypes 参数类型数组
     * @param args 构造函数参数
     * @return 新实例
     * @throws RuntimeException 如果创建实例失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz, Class<?>[] parameterTypes, Object... args) {
        if (clazz == null) {
            throw new IllegalArgumentException("类对象不能为null");
        }
        try {
            Constructor<?> constructor = getCachedConstructor(clazz, 
                new ConstructorKey(parameterTypes));
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("创建实例失败: " + clazz.getName(), e);
        }
    }

    /**
     * 获取类的泛型类型
     * @param source 类对象
     * @return 泛型类型列表，如果没有泛型则返回空列表
     * @author pengfulin
     * @since 2022/9/7 15:17
     */
    public static List<Class<?>> getParamTypes(Class<?> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        Type genericSuperclass = source.getGenericSuperclass();
        return getActualTypeArguments(genericSuperclass);
    }

    /**
     * 获取参数的泛型类型
     * @param parameter 参数对象
     * @return 泛型类型列表，如果没有泛型则返回空列表
     * @author fulin-peng
     * @since 2024/9/4 11:02
     */
    public static List<Class<?>> getParamTypes(Parameter parameter) {
        if (parameter == null) {
            return Collections.emptyList();
        }
        Type genericType = parameter.getParameterizedType();
        return getActualTypeArguments(genericType);
    }

    /**
     * 获取Type的实际泛型类型
     * @param type 类型对象
     * @return 泛型类型列表，如果没有泛型则返回空列表
     * @author fulin-peng
     * @since 2024/9/4 11:08
     */
    public static List<Class<?>> getActualTypeArguments(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return Collections.emptyList();
        }

        List<Class<?>> paramTypes = new ArrayList<>();
        for (Type typeArgument : ((ParameterizedType) type).getActualTypeArguments()) {
            if (typeArgument instanceof Class) {
                paramTypes.add((Class<?>) typeArgument);
            }
        }
        return paramTypes;
    }

    /**
     * 检查类是否是目标类的子类或实现了目标接口
     * @param clazz 待检查的类
     * @param targetClass 目标类或接口
     * @return true如果是子类或实现了接口，false如果不是
     * @throws IllegalArgumentException 如果参数为null
     * @author fulin-peng
     * @since 2024/8/30 09:20
     */
    public static boolean hasClass(Class<?> clazz, Class<?> targetClass) {
        if (clazz == null || targetClass == null) {
            throw new IllegalArgumentException("类参数不能为null");
        }
        return targetClass.isAssignableFrom(clazz);
    }

    /**
     * 检查类是否包含指定字段
     * @param fieldName 字段名
     * @param clazz 类对象
     * @return true如果包含字段，false如果不包含
     * @throws IllegalArgumentException 如果参数为null
     * @author fulin-peng
     * @since 2024/8/30 09:19
     */
    public static boolean hasField(String fieldName, Class<?> clazz) {
        if (fieldName == null || clazz == null) {
            throw new IllegalArgumentException("字段名和类对象不能为null");
        }
        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> field.getName().equals(fieldName));
    }

    /**
     * 获取多级字段值（支持点号分隔的字段路径）
     * @param fieldPath 字段路径，如"person.address.city"
     * @param target 目标对象
     * @return 字段值，如果任何中间字段为null则返回null
     * @throws RuntimeException 如果获取字段值失败
     * @author fulin-peng
     * @since 2023/11/9 12:05
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueWithMultistage(String fieldPath, Object target) {
        if (fieldPath == null || target == null) {
            return null;
        }

        try {
            Object current = target;
            if (fieldPath.contains(".")) {
                String[] fields = fieldPath.split("\\.");
                for (String field : fields) {
                    current = getFieldValue(field, current);
                    if (current == null) {
                        return null;
                    }
                }
                return (T) current;
            }
            return getFieldValue(fieldPath, current);
        } catch (Exception e) {
            throw new RuntimeException("获取多级字段值失败: " + fieldPath, e);
        }
    }

    /**
     * 获取字段值
     * @param fieldName 字段名
     * @param target 目标对象
     * @param fieldType 字段类型
     * @return 字段值
     * @throws RuntimeException 如果获取字段值失败
     */
    public static <T> T getFieldValue(String fieldName, Object target, Class<T> fieldType) {
        if (fieldName == null || target == null) {
            return null;
        }

        try {
            Field field = getCachedField(target.getClass(), fieldName);
            return getFieldValue(field, target, fieldType);
        } catch (Exception e) {
            throw new RuntimeException("获取字段值失败: " + fieldName, e);
        }
    }

    /**
     * 获取字段值
     * @param field 字段对象
     * @param target 目标对象
     * @param fieldType 字段类型
     * @return 字段值
     * @throws RuntimeException 如果获取字段值失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Field field, Object target, Class<T> fieldType) {
        if (field == null || target == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            Object value = field.get(target);
            if (fieldType != null && value != null) {
                if (!fieldType.isInstance(value)) {
                    throw new ClassCastException("字段值类型不匹配: 期望 " + fieldType.getName() + 
                            ", 实际 " + value.getClass().getName());
                }
            }
            return (T) value;
        } catch (Exception e) {
            throw new RuntimeException("获取字段值失败: " + field.getName(), e);
        }
    }

    /**
     * 获取字段值（不指定类型）
     */
    public static <T> T getFieldValue(Field field, Object target) {
        return getFieldValue(field, target, null);
    }

    /**
     * 获取字段值（不指定类型）
     */
    public static <T> T getFieldValue(String fieldName, Object target) {
        return getFieldValue(fieldName, target, null);
    }

    /**
     * 设置字段值
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @param target 目标对象
     * @throws RuntimeException 如果设置字段值失败
     */
    public static void setFieldValue(String fieldName, Object fieldValue, Object target) {
        if (fieldName == null || target == null) {
            return;
        }

        try {
            Field field = getCachedField(target.getClass(), fieldName);
            setFieldValue(field, fieldValue, target);
        } catch (Exception e) {
            throw new RuntimeException("设置字段值失败: " + fieldName, e);
        }
    }

    /**
     * 设置字段值
     * @param field 字段对象
     * @param fieldValue 字段值
     * @param target 目标对象
     * @throws RuntimeException 如果设置字段值失败
     */
    public static void setFieldValue(Field field, Object fieldValue, Object target) {
        if (field == null || target == null) {
            return;
        }

        try {
            field.setAccessible(true);
            field.set(target, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException("设置字段值失败: " + field.getName(), e);
        }
    }

    /**
     * 设置多级字段值
     * @param fieldPath 字段路径，如"person.address.city"
     * @param fieldValue 字段值
     * @param target 目标对象
     * @throws RuntimeException 如果设置字段值失败
     */
    public static void setFieldValueWithMultistage(String fieldPath, Object fieldValue, Object target) {
        if (fieldPath == null || target == null) {
            return;
        }

        try {
            if (fieldPath.contains(".")) {
                String[] fieldNames = fieldPath.split("\\.");
                Object current = target;
                Object parent = target;
                
                for (int i = 0; i < fieldNames.length; i++) {
                    current = getFieldValue(fieldNames[i], current);
                    if (current == null) {
                        break;
                    }
                    if (i == fieldNames.length - 1) {
                        setFieldValue(fieldNames[i], fieldValue, parent);
                    } else {
                        parent = current;
                    }
                }
            } else {
                setFieldValue(fieldPath, fieldValue, target);
            }
        } catch (Exception e) {
            throw new RuntimeException("设置多级字段值失败: " + fieldPath, e);
        }
    }

    /**
     * 获取方法对象
     * @param source 类对象
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组
     * @return 方法对象
     * @throws RuntimeException 如果获取方法失败
     */
    public static Method getMethod(Class<?> source, String methodName, Class<?>... parameterTypes) {
        if (source == null || methodName == null) {
            throw new IllegalArgumentException("类对象和方法名不能为null");
        }

        try {
            MethodKey key = new MethodKey(methodName, parameterTypes);
            return getCachedMethod(source, key);
        } catch (Exception e) {
            throw new RuntimeException("获取方法失败: " + methodName, e);
        }
    }

    /**
     * 获取方法对象
     * @param target 目标对象
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组
     * @return 方法对象
     */
    public static Method getMethod(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) {
            throw new IllegalArgumentException("目标对象不能为null");
        }
        return getMethod(target.getClass(), methodName, parameterTypes);
    }

    /**
     * 调用方法
     * @param target 目标对象（静态方法可为null）
     * @param method 方法对象
     * @param args 方法参数
     * @return 方法返回值
     * @throws RuntimeException 如果调用方法失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object target, Method method, Object... args) {
        if (method == null) {
            throw new IllegalArgumentException("方法对象不能为null");
        }

        try {
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("调用方法失败: " + method.getName(), e);
        }
    }

    /**
     * 获取枚举值
     * @param enumType 枚举类型
     * @param enumValue 枚举值字符串
     * @return 枚举对象
     * @throws IllegalArgumentException 如果参数无效或枚举值不存在
     */
    public static <T extends Enum<T>> T getEnum(Class<T> enumType, String enumValue) {
        if (enumType == null || enumValue == null) {
            throw new IllegalArgumentException("枚举类型和枚举值不能为null");
        }
        return Enum.valueOf(enumType, enumValue);
    }

    /**
     * 如果字段值为null或空则设置新值
     * @param field 字段对象
     * @param fieldValue 字段值
     * @param target 目标对象
     */
    public static void setFieldValueIfEmpty(Field field, Object fieldValue, Object target) {
        Object currentValue = getFieldValue(field, target);
        if (currentValue == null || (currentValue instanceof String && 
                StringUtils.isEmpty((String) currentValue))) {
            setFieldValue(field, fieldValue, target);
        }
    }

    /**
     * 获取类的所有字段
     * @param clazz 类对象
     * @param includeSuper 是否包含父类字段
     * @param fields 字段列表（可为null）
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> clazz, boolean includeSuper, List<Field> fields) {
        if (clazz == null || clazz == Object.class) {
            return fields == null ? Collections.emptyList() : fields;
        }

        if (fields == null) {
            fields = new ArrayList<>(DEFAULT_COLLECTION_SIZE);
        }

        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (includeSuper) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                getFields(superclass, true, fields);
            }
        }

        return fields;
    }

    /**
     * 获取类的所有字段（包括父类）
     */
    public static List<Field> getFieldsAll(Class<?> clazz) {
        return getFields(clazz, true, null);
    }

    /**
     * 获取字段对象
     * @param clazz 类对象
     * @param fieldName 字段名
     * @return 字段对象
     * @throws RuntimeException 如果获取字段失败
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            throw new IllegalArgumentException("类对象和字段名不能为null");
        }

        try {
            return getCachedField(clazz, fieldName);
        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + fieldName, e);
        }
    }

    /**
     * 获取字段对象（包括父类字段）
     */
    public static Field getFieldAll(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            throw new IllegalArgumentException("类对象和字段名不能为null");
        }

        try {
            Field field = getCachedField(clazz, fieldName);
            if (field != null) {
                return field;
            }

            return getFieldsAll(clazz).stream()
                    .filter(f -> f.getName().equals(fieldName))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchFieldException(fieldName));
        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + fieldName, e);
        }
    }

    /**
     * 检查类是否有指定注解
     * @param clazz 类对象
     * @param annotation 注解类型
     * @return true如果有注解，false如果没有
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz != null && annotation != null && clazz.getAnnotation(annotation) != null;
    }

    /**
     * 检查字段是否有指定注解
     * @param field 字段对象
     * @param annotation 注解类型
     * @return true如果有注解，false如果没有
     */
    public static boolean hasAnnotation(Field field, Class<? extends Annotation> annotation) {
        return field != null && annotation != null && field.getAnnotation(annotation) != null;
    }

    /**
     * 获取类的注解
     * @param clazz 类对象
     * @param annotation 注解类型
     * @return 注解对象，如果没有则返回null
     */
    public static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotation) {
        return clazz == null || annotation == null ? null : clazz.getAnnotation(annotation);
    }

    /**
     * 获取字段的注解
     * @param field 字段对象
     * @param annotation 注解类型
     * @return 注解对象，如果没有则返回null
     */
    public static <A extends Annotation> A getAnnotation(Field field, Class<A> annotation) {
        return field == null || annotation == null ? null : field.getAnnotation(annotation);
    }

    /**
     * 检查类型是否相同或是父子类关系
     * @param type1 类型1（子类）
     * @param type2 类型2（父类）
     * @return true如果类型相同或存在父子类关系，false如果不是
     */
    public static boolean typeEquals(Class<?> type1, Class<?> type2) {
        return type1 != null && type2 != null && 
               (type1 == type2 || type2.isAssignableFrom(type1));
    }

    /**
     * 获取所有方法（包括继承的方法）
     * @param clazz 类对象
     * @param predicate 过滤条件
     * @return 方法列表
     */
    public static List<Method> getMethods(Class<?> clazz, Predicate<Method> predicate) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(clazz.getMethods())
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有字段（包括继承的字段）
     * @param clazz 类对象
     * @param predicate 过滤条件
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> clazz, Predicate<Field> predicate) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(clazz.getFields())
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有构造函数
     * @param clazz 类对象
     * @return 构造函数列表
     */
    public static <T> List<Constructor<T>> getConstructors(Class<T> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Constructor<T>[] constructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
        return Arrays.asList(constructors);
    }

    /**
     * 判断类型是否为基本类型
     * @param type 类型
     * @return true如果是基本类型
     */
    public static boolean isPrimitive(Class<?> type) {
        return type != null && type.isPrimitive();
    }

    /**
     * 获取基本类型的包装类型
     * @param primitiveType 基本类型
     * @return 包装类型，如果不是基本类型则返回原类型
     */
    public static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == null || !primitiveType.isPrimitive()) {
            return primitiveType;
        }
        return PRIMITIVE_WRAPPER_MAP.get(primitiveType);
    }

    /**
     * 判断类型是否为包装类型
     * @param type 类型
     * @return true如果是包装类型
     */
    public static boolean isWrapperType(Class<?> type) {
        return type != null && PRIMITIVE_WRAPPER_MAP.containsValue(type);
    }

    /**
     * 获取类的所有接口
     * @param clazz 类对象
     * @return 接口列表
     */
    public static Set<Class<?>> getInterfaces(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptySet();
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        getAllInterfaces(clazz, interfaces);
        return interfaces;
    }

    private static void getAllInterfaces(Class<?> clazz, Set<Class<?>> interfaces) {
        while (clazz != null) {
            for (Class<?> i : clazz.getInterfaces()) {
                interfaces.add(i);
                getAllInterfaces(i, interfaces);
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 从缓存中获取字段对象
     */
    private static Field getCachedField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Map<String, Field> fields = FIELD_CACHE.computeIfAbsent(clazz, 
            k -> new ConcurrentHashMap<>());
        
        Field field = fields.get(fieldName);
        if (field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) { // 如果字段不存在则查找父类字段
                Class<?> superclass = clazz.getSuperclass();
                if (superclass!=Object.class) {
                    return getCachedField(superclass, fieldName);
                }else {
                    throw e; // 如果父类为Object则抛出异常
                }
            }
            fields.put(fieldName, field);
        }
        return field;
    }

    /**
     * 从缓存中获取方法对象
     */
    private static Method getCachedMethod(Class<?> clazz, MethodKey key) throws NoSuchMethodException {
        Map<MethodKey, Method> methods = METHOD_CACHE.computeIfAbsent(clazz, 
            k -> new ConcurrentHashMap<>());
        
        Method method = methods.get(key);
        if (method == null) {
            method = clazz.getDeclaredMethod(key.name, key.parameterTypes);
            methods.put(key, method);
        }
        return method;
    }

    /**
     * 从缓存中获取构造函数对象
     */
    private static Constructor<?> getCachedConstructor(Class<?> clazz, ConstructorKey key) 
            throws NoSuchMethodException {
        Map<ConstructorKey, Constructor<?>> constructors = CONSTRUCTOR_CACHE.computeIfAbsent(clazz,
            k -> new ConcurrentHashMap<>());
        
        Constructor<?> constructor = constructors.get(key);
        if (constructor == null) {
            constructor = clazz.getDeclaredConstructor(key.parameterTypes);
            constructors.put(key, constructor);
        }
        return constructor;
    }

    /**
     * 方法键，用于缓存Method对象
     */
    private static class MethodKey {
        private final String name;
        private final Class<?>[] parameterTypes;

        MethodKey(String name, Class<?>[] parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodKey methodKey = (MethodKey) o;
            return Objects.equals(name, methodKey.name) && 
                   Arrays.equals(parameterTypes, methodKey.parameterTypes);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(name) + Arrays.hashCode(parameterTypes);
        }
    }

    /**
     * 构造函数键，用于缓存Constructor对象
     */
    private static class ConstructorKey {
        private final Class<?>[] parameterTypes;

        ConstructorKey(Class<?>[] parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstructorKey that = (ConstructorKey) o;
            return Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(parameterTypes);
        }
    }
}
