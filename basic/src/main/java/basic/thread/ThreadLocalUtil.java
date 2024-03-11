package basic.thread;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程变量工具类
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<Map<String,Object>> threadLocal = new ThreadLocal<>();

    /**
     * 设置值
     */
    public static void set(Map<String,Object> map){
        threadLocal.set(map);
    }

    /**
     * 设置值
     */
    public static void set(String key,Object value){
        Map<String, Object> map = threadLocal.get();
        if(map==null){
            map = new HashMap<>();
            threadLocal.set(map);
        }
        map.put(key,value);
    }

    /**
     * 获取值
     */
    public static Map<String,Object> get(){
        return threadLocal.get();
    }

    /**
     * 移除值
     */
    public static void remove(){
        threadLocal.remove();
    }
}
