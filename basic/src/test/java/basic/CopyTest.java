package basic;


import basic.copy.ObjectCopyUtils;
import basic.entity.User;
import basic.entity.UserNew;
import org.junit.Test;

/**
 * 拷贝测试
 *
 * @author pengshuaifeng
 * 2023/12/24
 */
public class CopyTest {


    @Test
    public void test(){
        User user = new User();
        user.setAddress("湖南");
        user.setAge(77);
        user.setName("彭帅疯");
        UserNew targetNew = new UserNew();
        ObjectCopyUtils.copy(user, targetNew, false);
        ObjectCopyUtils.copy(user, targetNew, false,true);
        System.out.println(targetNew);
    }
}
