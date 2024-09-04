package basic.collection;

import java.util.*;

/**
 * 集合工具类
 *
 * @author peng_fu_lin
 * 2022-09-07 15:55
 */
public class CollectionUtils {


    /**是否为空
     * 2022/9/29 0029-14:00
     * @author pengfulin
    */
    public static boolean isEmpty(Collection<?> collection){
        return collection==null||collection.isEmpty();
    }

    public static <T> Collection<T> isEmptyOrDefault(Collection<T> collection){
        return isEmpty(collection)?Collections.emptyList():collection;
    }

    public static <T> Collection<T> isNullOrDefault(Collection<T> collection){
        return collection==null?new LinkedList<>() :collection;
    }

    public static boolean isNotEmpty(Collection<?> collection){
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?,?> collection){
        return collection==null||collection.isEmpty();
    }

    public static boolean isNotEmpty(Map<?,?> collection){
        return !isEmpty(collection);
    }

    public static <K,V> Map<K,V> isNullOrDefault(Map<K,V> collection){
        return collection==null?new HashMap<>():collection;
    }
}