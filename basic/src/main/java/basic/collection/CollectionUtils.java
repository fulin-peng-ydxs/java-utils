package basic.collection;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 集合工具类，提供集合操作、验证、转换等功能
 *
 * @author peng_fu_lin
 * @since 2022-09-07 15:55
 */
public class CollectionUtils {

    private CollectionUtils() {
        // Utility class should not be instantiated
    }

    /** 常量 */
    private static final int DEFAULT_COLLECTION_SIZE = 16;
    private static final int DEFAULT_MAP_SIZE = 16;
    private static final int DEFAULT_CHUNK_SIZE = 100;
    private static final String NULL_COLLECTION = "集合不能为null";
    private static final String NULL_PREDICATE = "条件不能为null";
    private static final String NULL_FUNCTION = "函数不能为null";
    /**
     * 统计集合中元素出现的次数
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 元素出现次数的Map
     */
    public static <T> Map<T, Long> frequency(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                Function.identity(),
                Collectors.counting()
            ));
    }

    /**
     * 查找集合中第一个满足条件的元素
     * @param collection 源集合
     * @param predicate 查找条件
     * @param <T> 集合元素类型
     * @return Optional包装的元素
     */
    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<? super T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return Optional.empty();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .filter(predicate)
            .findFirst();
    }

    /**
     * 查找集合中最后一个满足条件的元素
     * @param collection 源集合
     * @param predicate 查找条件
     * @param <T> 集合元素类型
     * @return Optional包装的元素
     */
    public static <T> Optional<T> findLast(Collection<T> collection, Predicate<? super T> predicate) {
        if (isEmpty(collection) || predicate == null) {
            return Optional.empty();
        }
        List<T> list = collection.stream()
            .filter(Objects::nonNull)
            .filter(predicate)
            .collect(Collectors.toList());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    /**
     * 去除集合中的重复元素（使用equals比较）
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 去重后的List
     */
    public static <T> List<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 去除集合中的重复元素（使用自定义比较器）
     * @param collection 源集合
     * @param comparator 比较器
     * @param <T> 集合元素类型
     * @return 去重后的List
     */
    public static <T> List<T> distinct(Collection<T> collection, BiPredicate<? super T, ? super T> comparator) {
        if (isEmpty(collection) || comparator == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>();
        for (T item : collection) {
            if (item != null && result.stream().noneMatch(existing -> comparator.test(item, existing))) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 比较两个集合是否相等（忽略顺序）
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T> 集合元素类型
     * @return true如果两个集合包含相同的元素
     */
    public static <T> boolean isEqualCollection(Collection<T> coll1, Collection<T> coll2) {
        if (coll1 == coll2) {
            return true;
        }
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return isEmpty(coll1) && isEmpty(coll2);
        }
        return frequency(coll1).equals(frequency(coll2));
    }

    /**
     * 比较两个集合是否相等（考虑顺序）
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T> 集合元素类型
     * @return true如果两个集合包含相同的元素且顺序相同
     */
    public static <T> boolean isEqualCollectionOrdered(Collection<T> coll1, Collection<T> coll2) {
        if (coll1 == coll2) {
            return true;
        }
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return isEmpty(coll1) && isEmpty(coll2);
        }
        if (coll1.size() != coll2.size()) {
            return false;
        }
        Iterator<T> it1 = coll1.iterator();
        Iterator<T> it2 = coll2.iterator();
        while (it1.hasNext()) {
            T obj1 = it1.next();
            T obj2 = it2.next();
            if (!Objects.equals(obj1, obj2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算集合的笛卡尔积
     * @param collections 集合列表
     * @param <T> 集合元素类型
     * @return 笛卡尔积结果
     */
    @SafeVarargs
    public static <T> List<List<T>> cartesianProduct(Collection<T>... collections) {
        if (collections == null || collections.length == 0) {
            return new ArrayList<>();
        }
        
        List<List<T>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        
        for (Collection<T> collection : collections) {
            if (isEmpty(collection)) {
                continue;
            }
            List<List<T>> current = new ArrayList<>();
            for (List<T> product : result) {
                for (T item : collection) {
                    List<T> newProduct = new ArrayList<>(product);
                    newProduct.add(item);
                    current.add(newProduct);
                }
            }
            result = current;
        }
        return result;
    }

    /**
     * 将集合转换为不可修改的集合
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 不可修改的集合
     */
    public static <T> Collection<T> unmodifiable(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(collection);
    }

    /**
     * 将集合转换为同步集合
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 同步集合
     */
    public static <T> Collection<T> synchronizedCollection(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Collections.synchronizedCollection(new ArrayList<>());
        }
        return Collections.synchronizedCollection(collection);
    }

    /**
     * 检查集合是否为空
     * @param collection 待检查的集合
     * @return true如果为null或空，false如果非空
     * @author pengfulin
     * @since 2022/9/29 14:00
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 检查集合是否非空
     * @param collection 待检查的集合
     * @return true如果非空，false如果为null或空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 如果集合为空则返回空集合，否则返回原集合
     * @param collection 待检查的集合
     * @param <T> 集合元素类型
     * @return 非null的不可修改集合
     */
    public static <T> Collection<T> getEmptyIfNull(Collection<T> collection) {
        return isEmpty(collection) ? Collections.emptyList() : Collections.unmodifiableCollection(collection);
    }

    /**
     * 如果集合为null则返回新的ArrayList，否则返回原集合
     * @param collection 待检查的集合
     * @param <T> 集合元素类型
     * @return 非null的可修改集合
     */
    public static <T> List<T> getNewIfNull(Collection<T> collection) {
        return new ArrayList<>(collection != null ? collection : new ArrayList<>(DEFAULT_COLLECTION_SIZE));
    }

    /**
     * 检查Map是否为空
     * @param map 待检查的Map
     * @return true如果为null或空，false如果非空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 检查Map是否非空
     * @param map 待检查的Map
     * @return true如果非空，false如果为null或空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 如果Map为null则返回新的HashMap，否则返回原Map的副本
     * @param map 待检查的Map
     * @param <K> Map键类型
     * @param <V> Map值类型
     * @return 非null的可修改Map
     */
    public static <K, V> Map<K, V> getNewIfNull(Map<K, V> map) {
        return map == null ? new HashMap<>(DEFAULT_MAP_SIZE) : new HashMap<>(map);
    }

    /**
     * 如果Map为空则返回空Map，否则返回原Map
     * @param map 待检查的Map
     * @param <K> Map键类型
     * @param <V> Map值类型
     * @return 非null的不可修改Map
     */
    public static <K, V> Map<K, V> getEmptyIfNull(Map<K, V> map) {
        return isEmpty(map) ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    /**
     * 安全地添加元素到集合
     * @param collection 目标集合
     * @param element 待添加的元素
     * @param <T> 集合元素类型
     * @return true如果添加成功，false如果集合为null或添加失败
     */
    public static <T> boolean safeAdd(Collection<T> collection, T element) {
        return collection != null && collection.add(element);
    }

    /**
     * 安全地添加所有元素到集合
     * @param collection 目标集合
     * @param elements 待添加的元素集合
     * @param <T> 集合元素类型
     * @return true如果添加成功，false如果任一集合为null或添加失败
     */
    public static <T> boolean safeAddAll(Collection<T> collection, Collection<? extends T> elements) {
        return collection != null && elements != null && collection.addAll(elements);
    }

    /**
     * 安全地从集合中移除元素
     * @param collection 目标集合
     * @param element 待移除的元素
     * @param <T> 集合元素类型
     * @return true如果移除成功，false如果集合为null或移除失败
     */
    public static <T> boolean safeRemove(Collection<T> collection, Object element) {
        return collection != null && collection.remove(element);
    }

    /**
     * 安全地从集合中移除所有元素
     * @param collection 目标集合
     * @param elements 待移除的元素集合
     * @param <T> 集合元素类型
     * @return true如果移除成功，false如果任一集合为null或移除失败
     */
    public static <T> boolean safeRemoveAll(Collection<T> collection, Collection<?> elements) {
        return collection != null && elements != null && collection.removeAll(elements);
    }

    /**
     * 获取集合的第一个元素
     * @param collection 目标集合
     * @param <T> 集合元素类型
     * @return 第一个元素，如果集合为空则返回null
     */
    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            return ((List<T>) collection).get(0);
        }
        return collection.iterator().next();
    }

    /**
     * 获取集合的最后一个元素
     * @param collection 目标集合
     * @param <T> 集合元素类型
     * @return 最后一个元素，如果集合为空则返回null
     */
    public static <T> T getLast(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return list.get(list.size() - 1);
        }
        // 对于非List集合，避免使用stream以提高性能
        Iterator<T> iterator = collection.iterator();
        T last = null;
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }

    /**
     * 将数组转换为List
     * @param array 源数组
     * @param <T> 数组元素类型
     * @return 包含数组元素的List，如果数组为null则返回空List
     */
    @SafeVarargs
    public static <T> List<T> toList(T... array) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }
        ArrayList<T> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }

    /**
     * 将集合转换为新的ArrayList
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 包含集合元素的新ArrayList，如果集合为null则返回空List
     */
    public static <T> ArrayList<T> toArrayList(Collection<T> collection) {
        if (collection == null) {
            return new ArrayList<>();
        }
        return collection instanceof ArrayList ? 
            new ArrayList<>(collection) : 
            collection.stream().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 将集合按指定大小分块
     * @param collection 源集合
     * @param chunkSize 分块大小
     * @param <T> 集合元素类型
     * @return 分块后的List集合
     */
    public static <T> List<List<T>> chunk(Collection<T> collection, int chunkSize) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        
        int size = Math.max(1, chunkSize);
        List<T> list = toArrayList(collection);
        int numChunks = (list.size() + size - 1) / size;
        List<List<T>> chunks = new ArrayList<>(numChunks);
        
        for (int i = 0; i < numChunks; i++) {
            int start = i * size;
            int end = Math.min(start + size, list.size());
            chunks.add(list.subList(start, end));
        }
        return chunks;
    }

    /**
     * 将集合按默认大小分块
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 分块后的List集合
     */
    public static <T> List<List<T>> chunk(Collection<T> collection) {
        return chunk(collection, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 将集合按条件分组
     * @param collection 源集合
     * @param classifier 分组函数
     * @param <T> 集合元素类型
     * @param <K> 分组键类型
     * @return 分组后的Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, 
            Function<? super T, ? extends K> classifier) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(classifier));
    }

    /**
     * 将集合转换为Map
     * @param collection 源集合
     * @param keyMapper 键映射函数
     * @param valueMapper 值映射函数
     * @param <T> 集合元素类型
     * @param <K> Map键类型
     * @param <V> Map值类型
     * @return 转换后的Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(keyMapper, valueMapper, (v1, v2) -> v2));
    }

    /**
     * 对集合元素进行转换
     * @param collection 源集合
     * @param mapper 转换函数
     * @param <T> 源类型
     * @param <R> 目标类型
     * @return 转换后的List
     */
    public static <T, R> List<R> map(Collection<T> collection, 
            Function<? super T, ? extends R> mapper) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .map(mapper)
            .collect(Collectors.toList());
    }

    /**
     * 对集合进行过滤
     * @param collection 源集合
     * @param predicate 过滤条件
     * @param <T> 集合元素类型
     * @return 过滤后的List
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * 将集合转换为新的HashSet
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 包含集合元素的新HashSet，如果集合为null则返回空Set
     */
    public static <T> HashSet<T> toHashSet(Collection<T> collection) {
        return collection == null ? new HashSet<>() : new HashSet<>(collection);
    }

    /**
     * 过滤集合中的null元素
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 不包含null元素的新List
     */
    public static <T> List<T> filterNull(Collection<T> collection) {
        return filter(collection, Objects::nonNull);
    }

    /**
     * 对集合元素进行排序
     * @param collection 源集合
     * @param comparator 比较器
     * @param <T> 集合元素类型
     * @return 排序后的新List
     */
    public static <T> List<T> sort(Collection<T> collection, Comparator<? super T> comparator) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        List<T> list = toArrayList(collection);
        list.sort(comparator);
        return list;
    }

    /**
     * 获取集合中的随机元素
     * @param collection 源集合
     * @param count 获取数量
     * @param <T> 集合元素类型
     * @return 随机元素List
     */
    public static <T> List<T> random(Collection<T> collection, int count) {
        if (isEmpty(collection) || count <= 0) {
            return new ArrayList<>();
        }
        List<T> list = toArrayList(collection);
        Collections.shuffle(list);
        return list.subList(0, Math.min(count, list.size()));
    }

    /**
     * 获取集合中的一个随机元素
     * @param collection 源集合
     * @param <T> 集合元素类型
     * @return 随机元素，如果集合为空则返回null
     */
    public static <T> T random(Collection<T> collection) {
        List<T> randomList = random(collection, 1);
        return randomList.isEmpty() ? null : randomList.get(0);
    }

    /**
     * 获取两个集合的交集
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T> 集合元素类型
     * @return 包含交集元素的新List
     */
    public static <T> List<T> intersection(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1) || isEmpty(coll2)) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>(coll1);
        list.retainAll(coll2);
        return list;
    }

    /**
     * 获取两个集合的并集
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T> 集合元素类型
     * @return 包含并集元素的新List
     */
    public static <T> List<T> union(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1)) {
            return isEmpty(coll2) ? new ArrayList<>() : new ArrayList<>(coll2);
        }
        if (isEmpty(coll2)) {
            return new ArrayList<>(coll1);
        }
        Set<T> set = new HashSet<>(coll1);
        set.addAll(coll2);
        return new ArrayList<>(set);
    }

    /**
     * 获取两个集合的差集（第一个集合中存在但第二个集合中不存在的元素）
     * @param coll1 第一个集合
     * @param coll2 第二个集合
     * @param <T> 集合元素类型
     * @return 包含差集元素的新List
     */
    public static <T> List<T> subtract(Collection<T> coll1, Collection<T> coll2) {
        if (isEmpty(coll1)) {
            return new ArrayList<>();
        }
        if (isEmpty(coll2)) {
            return new ArrayList<>(coll1);
        }
        List<T> list = new ArrayList<>(coll1);
        list.removeAll(coll2);
        return list;
    }

    /**
     * 将集合转换为数组
     * @param collection 源集合
     * @param array 目标数组
     * @param <T> 集合元素类型
     * @return 包含集合元素的数组
     */
    public static <T> T[] toArray(Collection<T> collection, T[] array) {
        if (isEmpty(collection)) {
            return array;
        }
        return collection.toArray(array);
    }

    /**
     * 检查集合是否包含指定元素
     * @param collection 目标集合
     * @param element 待检查的元素
     * @return true如果包含，false如果不包含或集合为null
     */
    public static boolean safeContains(Collection<?> collection, Object element) {
        return collection != null && collection.contains(element);
    }

    /**
     * 检查集合是否包含所有指定元素
     * @param collection 目标集合
     * @param elements 待检查的元素集合
     * @return true如果包含所有元素，false如果不包含或任一集合为null
     */
    public static boolean safeContainsAll(Collection<?> collection, Collection<?> elements) {
        return collection != null && elements != null && collection.containsAll(elements);
    }
}
