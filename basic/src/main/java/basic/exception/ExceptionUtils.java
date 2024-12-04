package basic.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * 异常工具类
 *
 * @author fulin-peng
 * 2024-12-04  18:04
 */
public class ExceptionUtils {

    /**
     * 获取根异常：第一个异常
     * 2024/12/4 下午6:07 
     * @author fulin-peng
     */
    public static Throwable getRootCause(Throwable throwable) {
        List<Throwable> list = getThrowableList(throwable);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * 获取异常链
     * 2024/12/4 下午6:06
     * @author fulin-peng
     * @return List有序集合：异常从最后一个到第一个
     */
    public static List<Throwable> getThrowableList(Throwable throwable) {
        ArrayList<Throwable> list;
        for(list = new ArrayList<>(); throwable != null && !list.contains(throwable); throwable = throwable.getCause()) {
            list.add(throwable);
        }
        return list;
    }
}
