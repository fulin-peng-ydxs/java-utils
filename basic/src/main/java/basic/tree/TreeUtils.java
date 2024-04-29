package basic.tree;

import basic.clazz.ClassUtils;
import java.util.*;

/**
 * 树形结构工具
 *
 * @author fulin-peng
 * 2023-12-06  17:40
 */
public class TreeUtils {

    /**
     * 集合转树
     * @param roots 根节点集合
     * @param sources 除根节点外的所有节点集合
     * @param idName id字段名称
     * @param pidName 父id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * 2023/12/6 0006 17:43
     * @author fulin-peng
     */
    public static <R,K> Collection<R> toListTree(Collection<R> roots,Collection<R> sources,String idName,String pidName,Class<K> idType,String childrenName){
        Map<K,Collection<R>> sourceMap = new HashMap<>();
        for (R source : sources) {
            K pidValue = ClassUtils.getFieldValue(pidName, source, idType);
            Collection<R> children = sourceMap.get(pidValue);
            if(children==null)
                children=new LinkedList<>();
            children.add(source);
            sourceMap.put(pidValue,children);
        }
        return mapConvertListTree(roots,sourceMap,idName,idType,childrenName);
    }
    /**
     * 对象转树
     * @param root 根节点对象
     * @param sources 除根节点外的所有节点集合
     * @param idName id字段名称
     * @param pidName 父id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * 2023/12/6 0006 17:43
     * @author fulin-peng
     */
    public static <R,K>  R toObjectTree(R root,Collection<R> sources,String idName,String pidName,Class<K> idType,String childrenName){
        Collection<R> singletonRoot = Collections.singletonList(root);
        Collection<R> roots = toListTree(singletonRoot, sources, idName, pidName, idType, childrenName);
        return roots.iterator().next();
    }

    /**
     * map转换list树
     * @param roots 根节点集合
     * @param sourceMap 除根节点的所有节点所拥有的子集映射
     * @param idName id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * 2023/12/7 0007 16:13
     * @author fulin-peng
     */
    public static <R,K>  Collection<R>  mapConvertListTree(Collection<R> roots,Map<K,Collection<R>> sourceMap,String idName,Class<K> idType,String childrenName){
        for (R root : roots) {
            K idValue = ClassUtils.getFieldValue(idName, root, idType);
            Collection<R> children = sourceMap.get(idValue);
            if(children!=null)
                mapConvertListTree(children,sourceMap,idName,idType,childrenName);
            ClassUtils.setFieldValue(childrenName,children,root);
        }
        return roots;
    }
}
