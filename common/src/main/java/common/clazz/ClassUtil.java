package common.clazz;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * class工具类
 * author: pengshuaifeng
 * 2023/12/18
 */
public class ClassUtil {

    /**获取类的泛型
     * 2022/9/7 0007-15:17
     * @author pengfulin
     * @param source 类对象
     * @return 返回类型集合
     */
    public static List<Class<?>> getParamTypes(Class<?> source){
        List<Class<?>> paramTypes=null;
        Type genericSuperclass = source.getGenericSuperclass();
        if(genericSuperclass instanceof ParameterizedType){
            paramTypes=new LinkedList<>();
            for (Type typeArgument : ((ParameterizedType) genericSuperclass).getActualTypeArguments()) {
                paramTypes.add((Class<?>)typeArgument);
            }
        }
        return paramTypes;
    }


    /**
     * 获取字段值
     * 2023/11/9 0009 12:05
     * @author fulin-peng
     */
    public static  <T> T getFieldValue(String fieldName,Object value,Class<T> filedType){
        try {
            Field field = value.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(value);
        } catch (Exception e) {
            throw new RuntimeException("获取字段异常",e);
        }
    }

    public static  <T> T getFieldValue(Field field, Object value, Class<T> filedType){
        return getFieldValue(field.getName(), value, filedType);
    }

    /**
     * 设置字段值
     * 2023/12/7 0007 16:20
     * @author fulin-peng
     */
    public static void setFieldValue(String fieldName,Object fieldValue,Object value){
        try {
            Field field = value.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(value,fieldValue);
        } catch (Exception e) {
            throw new RuntimeException("设置字段异常",e);
        }
    }

    /**
     * 获取字段指定注解
     * 2023/11/30 0030 14:40
     * @author fulin-peng
     */
    public static <A extends Annotation> A getFieldAnnotation(Field field, Class<A> annotationType){
        return field.getAnnotation(annotationType);
    }
}
