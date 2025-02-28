package json.jackson.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import lombok.Getter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON工具类，提供JSON序列化和反序列化功能
 *
 * @author fulin-peng
 * @since 2023/8/3
 */
public class JsonUtils {

    private JsonUtils() {
        // Utility class should not be instantiated
    }

    /** 默认ObjectMapper实例 */
    private static final ObjectMapper DEFAULT_MAPPER = createDefaultMapper();

    /** JavaType缓存，提高性能 */
    private static final Map<Class<?>, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    /** 默认错误消息 */
    private static final String SERIALIZE_ERROR = "JSON序列化失败: ";
    private static final String DESERIALIZE_ERROR = "JSON反序列化失败: ";

    /**
     * 将对象序列化为JSON字符串
     * 
     * @param object 待序列化的对象
     * @param prettyPrinter 是否美化输出
     * @return JSON字符串
     * @throws RuntimeException 如果序列化失败
     * @author fulin-peng
     * @since 2023/8/3 11:47
     */
    public static String getString(Object object, boolean prettyPrinter) {
        if (object == null) {
            return null;
        }

        try {
            ObjectWriter writer = DEFAULT_MAPPER.writer();
            if (prettyPrinter) {
                writer = writer.with(SerializationFeature.INDENT_OUTPUT);
            }
            return writer.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(SERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将对象序列化为JSON字符串（不美化输出）
     * 
     * @param object 待序列化的对象
     * @return JSON字符串
     */
    public static String getString(Object object) {
        return getString(object, false);
    }

    /**
     * 将JSON字符串反序列化为对象
     * 
     * @param str JSON字符串
     * @param valueType 目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> T getObject(String str, Class<T> valueType) {
        if (str == null || valueType == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(str, valueType);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将对象转换为另一个类型的对象
     * 
     * @param object 源对象
     * @param valueType 目标类型
     * @return 转换后的对象
     */
    public static <T> T getObject(Object object, Class<T> valueType) {
        if (object == null || valueType == null) {
            return null;
        }
        return getObject(getString(object), valueType);
    }

    /**
     * 将JSON字符串反序列化为Map
     * 
     * @param str JSON字符串
     * @param type Map类型引用
     * @return Map对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <K, V> Map<K, V> getMap(String str, TypeReference<Map<K, V>> type) {
        if (str == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(str, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串反序列化为List
     * 
     * @param str JSON字符串
     * @param type List类型引用
     * @return List对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> List<T> getList(String str, TypeReference<List<T>> type) {
        if (str == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(str, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将输入流反序列化为对象
     * 
     * @param stream 输入流
     * @param valueType 目标类型
     * @return 反序列化后的对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> T getObject(InputStream stream, Class<T> valueType) {
        if (stream == null || valueType == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(stream, valueType);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将输入流反序列化为Map
     * 
     * @param stream 输入流
     * @param type Map类型引用
     * @return Map对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <K, V> Map<K, V> getMap(InputStream stream, TypeReference<Map<K, V>> type) {
        if (stream == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(stream, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 将输入流反序列化为List
     * 
     * @param stream 输入流
     * @param type List类型引用
     * @return List对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> List<T> getList(InputStream stream, TypeReference<List<T>> type) {
        if (stream == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(stream, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 使用JavaType进行反序列化
     * 
     * @param str JSON字符串
     * @param type JavaType类型
     * @return 反序列化后的对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> T getObjectParams(String str, JavaType type) {
        if (str == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(str, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 使用JavaType进行输入流反序列化
     * 
     * @param stream 输入流
     * @param type JavaType类型
     * @return 反序列化后的对象
     * @throws RuntimeException 如果反序列化失败
     */
    public static <T> T getObjectParams(InputStream stream, JavaType type) {
        if (stream == null || type == null) {
            return null;
        }

        try {
            return DEFAULT_MAPPER.readValue(stream, type);
        } catch (IOException e) {
            throw new RuntimeException(DESERIALIZE_ERROR + e.getMessage(), e);
        }
    }

    /**
     * 获取参数化的JavaType
     * 
     * @param parametrized 参数化类型
     * @param parameterClasses 参数类型数组
     * @return JavaType实例
     */
    public static JavaType getParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return DEFAULT_MAPPER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
    }

    /**
     * 获取默认的ObjectMapper实例
     */
    public static ObjectMapper getDefaultMapper() {
        return DEFAULT_MAPPER;
    }

    /**
     * 创建默认的ObjectMapper实例
     */
    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置序列化特性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        
        return mapper;
    }
}
