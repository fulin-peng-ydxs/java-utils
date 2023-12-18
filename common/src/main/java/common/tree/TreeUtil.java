package common.tree;

import common.clazz.ClassUtil;
import java.util.*;

/**
 * 树形结构工具
 *
 * @author fulin-peng
 * 2023-12-06  17:40
 */
public class TreeUtil {

    /**
     * 集合转树
     * 2023/12/6 0006 17:43
     * @author fulin-peng
     */
    public static <R,K> List<R> toListTree(List<R> roots,List<R> sources,String idName,String pidName,Class<K> idType,String childrenName){
        Map<K,List<R>> sourceMap = new HashMap<>();
        for (R source : sources) {
            K pidValue = ClassUtil.getFieldValue(pidName, source, idType);
            List<R> children = sourceMap.get(pidValue);
            if(children==null)
                children=new LinkedList<>();
            children.add(source);
            sourceMap.put(pidValue,children);
        }
        return mapConvertListTree(roots,sourceMap,idName,pidName,idType,childrenName);
    }
    /**
     * 对象转树
     * 2023/12/6 0006 17:43
     * @author fulin-peng
     */
    public static <R,K>  R toObjectTree(R root,List<R> sources,String idName,String pidName,Class<K> idType,String childrenName){
        List<R> singletonRoot = Collections.singletonList(root);
        List<R> roots = toListTree(singletonRoot, sources, idName, pidName, idType, childrenName);
        return roots.get(0);
    }

    /**
     * map转换list树
     * 2023/12/7 0007 16:13
     * @author fulin-peng
     */
    public static <R,K>  List<R>  mapConvertListTree(List<R> roots,Map<K,List<R>> sourceMap,String idName,String pidName,Class<K> idType,String childrenName){
        for (R root : roots) {
            K idValue = ClassUtil.getFieldValue(idName, root, idType);
            List<R> children = sourceMap.get(idValue);
            if(children!=null)
                mapConvertListTree(children,sourceMap,idName,pidName,idType,childrenName);
            ClassUtil.setFieldValue(childrenName,children,root);
        }
        return roots;
    }
}
