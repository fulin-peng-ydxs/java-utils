package basic.tree;

import basic.clazz.ClassUtils;
import java.util.*;

/**
 * 树形结构工具类，提供集合与树形结构之间的转换功能
 *
 * @author fulin-peng
 * @since 2023-12-06 17:40
 */
public class TreeUtils {

    private TreeUtils() {
        // Utility class should not be instantiated
    }

    /** 默认Map初始容量 */
    private static final int DEFAULT_MAP_CAPACITY = 16;

    /**
     * 将集合转换为树形结构
     * 
     * @param roots 根节点集合
     * @param sources 除根节点外的所有节点集合
     * @param idName id字段名称
     * @param pidName 父id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * @param <R> 节点类型
     * @param <K> ID类型
     * @return 树形结构的集合
     * @throws IllegalArgumentException 如果必要参数为null或空
     * @author fulin-peng
     * @since 2023/12/6 17:43
     */
    public static <R, K> Collection<R> toListTree(
            Collection<R> roots,
            Collection<R> sources,
            String idName,
            String pidName,
            Class<K> idType,
            String childrenName) {
        
        // 参数校验
        validateParameters(roots, idName, pidName, idType, childrenName);
        if (sources == null || sources.isEmpty()) {
            return roots;
        }

        // 构建父子关系映射
        Map<K, Collection<R>> sourceMap = new HashMap<>(DEFAULT_MAP_CAPACITY);
        for (R source : sources) {
            if (source == null) {
                continue;
            }
            K pidValue = ClassUtils.getFieldValue(pidName, source, idType);
            if (pidValue != null) {
                sourceMap.computeIfAbsent(pidValue, k -> new LinkedList<>()).add(source);
            }
        }

        return buildTreeStructure(roots, sourceMap, idName, idType, childrenName);
    }

    /**
     * 将对象转换为树形结构
     * 
     * @param root 根节点对象
     * @param sources 除根节点外的所有节点集合
     * @param idName id字段名称
     * @param pidName 父id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * @param <R> 节点类型
     * @param <K> ID类型
     * @return 树形结构的对象
     * @throws IllegalArgumentException 如果必要参数为null
     * @author fulin-peng
     * @since 2023/12/6 17:43
     */
    public static <R, K> R toObjectTree(
            R root,
            Collection<R> sources,
            String idName,
            String pidName,
            Class<K> idType,
            String childrenName) {
        
        if (root == null) {
            throw new IllegalArgumentException("根节点不能为null");
        }

        Collection<R> singletonRoot = Collections.singletonList(root);
        Collection<R> tree = toListTree(singletonRoot, sources, idName, pidName, idType, childrenName);
        return tree.iterator().next();
    }

    /**
     * 构建树形结构
     * 
     * @param nodes 当前层级的节点集合
     * @param sourceMap 节点的子集映射
     * @param idName id字段名称
     * @param idType id字段类型
     * @param childrenName 子集合属性名称
     * @param <R> 节点类型
     * @param <K> ID类型
     * @return 构建好的树形结构
     * @author fulin-peng
     * @since 2023/12/7 16:13
     */
    private static <R, K> Collection<R> buildTreeStructure(
            Collection<R> nodes,
            Map<K, Collection<R>> sourceMap,
            String idName,
            Class<K> idType,
            String childrenName) {
        
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        for (R node : nodes) {
            if (node == null) {
                continue;
            }

            K idValue = ClassUtils.getFieldValue(idName, node, idType);
            if (idValue != null) {
                Collection<R> children = sourceMap.get(idValue);
                if (children != null && !children.isEmpty()) {
                    // 递归构建子树
                    buildTreeStructure(children, sourceMap, idName, idType, childrenName);
                }
                // 设置子节点
                ClassUtils.setFieldValue(childrenName, children, node);
            }
        }
        return nodes;
    }

    /**
     * 校验必要参数
     */
    private static void validateParameters(
            Collection<?> roots,
            String idName,
            String pidName,
            Class<?> idType,
            String childrenName) {
        
        if (roots == null || roots.isEmpty()) {
            throw new IllegalArgumentException("根节点集合不能为空");
        }
        if (idName == null || idName.trim().isEmpty()) {
            throw new IllegalArgumentException("ID字段名称不能为空");
        }
        if (pidName == null || pidName.trim().isEmpty()) {
            throw new IllegalArgumentException("父ID字段名称不能为空");
        }
        if (idType == null) {
            throw new IllegalArgumentException("ID字段类型不能为null");
        }
        if (childrenName == null || childrenName.trim().isEmpty()) {
            throw new IllegalArgumentException("子集合属性名称不能为空");
        }
    }

    /**
     * 遍历树形结构
     * 
     * @param root 根节点
     * @param childrenName 子集合属性名称
     * @param consumer 节点处理函数
     * @param <T> 节点类型
     * @throws IllegalArgumentException 如果必要参数为null
     */
    public static <T> void traverseTree(T root, String childrenName, TreeNodeConsumer<T> consumer) {
        if (root == null) {
            throw new IllegalArgumentException("根节点不能为null");
        }
        if (childrenName == null || childrenName.trim().isEmpty()) {
            throw new IllegalArgumentException("子集合属性名称不能为空");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("节点处理函数不能为null");
        }

        // 处理当前节点
        consumer.accept(root);

        // 处理子节点
        @SuppressWarnings("unchecked")
        Collection<T> children = ClassUtils.getFieldValue(childrenName, root, Collection.class);
        if (children != null) {
            for (T child : children) {
                traverseTree(child, childrenName, consumer);
            }
        }
    }

    /**
     * 树节点处理接口
     * @param <T> 节点类型
     */
    @FunctionalInterface
    public interface TreeNodeConsumer<T> {
        /**
         * 处理树节点
         * @param node 节点对象
         */
        void accept(T node);
    }
}
