package basic.copy;


import basic.clazz.ClassUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对象拷贝
 * @author pengshuaifeng
 * 2023/12/24
 */
public class ObjectCopyUtils {

    /**拷贝
     * 2023/2/10 0010-10:57
     * @author pengfulin
     * @param source 拷贝源
     * @param newObject 目录对象 （默认不覆盖目录对象不为空的属性）
     */
    public static <T> T copy(Object source,T newObject,boolean containSuper){
        return copy(source,newObject,containSuper,false);
    }

    /**拷贝
     * 2023/2/10 0010-10:57
     * @author pengfulin
     * @param source 拷贝源
     * @param newObject 目录对象
     * @param isCover 是否覆盖目标对象中不为空的属性
     * @param containSuper 是否含有父类
     */
    public static <T> T copy(Object source,T newObject,boolean containSuper,boolean isCover){
        try {
            //获取源对象和目标对象的属性
            List<Field> sourceDeclaredFields = ClassUtils.getFields(source.getClass(),containSuper,null);
            List<Field> declaredFields = ClassUtils.getFields(newObject.getClass(),containSuper,null);
            //源对象属性集合封装
            Map<String, Field> sourceFieldMap =sourceDeclaredFields.stream().collect(
                    Collectors.toMap(
                            Field::getName, // 键函数，用于从每个对象中提取唯一标识符
                            Function.identity(), // 值函数，返回对象本身
                            (o1, o2) -> o1 // 合并函数，在发生冲突时，保留第一个对象
                    ));
            //遍历目标对象所有属性，进行属性拷贝 //TODO 目前仅支持同类型的属性拷贝
            for (Field newField : declaredFields) {
                newField.setAccessible(true);
                Field sourceField = sourceFieldMap.get(newField.getName());
                //源对象中存在此字段且类型同目录对象中的字段一致
                if(sourceField!=null&& ClassUtils.typeEquals(sourceField.getType(),newField.getType())){
                    sourceField.setAccessible(true);
                    //目标对象值为空或允许覆盖
                    if (newField.get(newObject)==null || isCover) {
                        newField.set(newObject,sourceField.get(source));
                    }
                }
            }
            return  newObject;
        } catch (Exception e) {
            throw new RuntimeException("对象拷贝异常",e);
        }
    }

}
