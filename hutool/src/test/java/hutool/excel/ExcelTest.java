package hutool.excel;


import basic.file.FileUtils;
import hutool.entity.User;
import hutool.entity.UserRead;
import hutool.excel.utils.ExcelUtils;
import org.junit.Test;

import java.util.*;

/**
 * excel测试
 *
 * @author pengshuaifeng
 * 2023/12/30
 */
public class ExcelTest {


    @Test
    public void test(){
        List<User> users = Arrays.asList(new User("test1", "test1-1", 1),
                new User("test2", "test2-1", 12), new User("test3", "test3-1", 99));
        ExcelUtils.exportNoHead(null,users, FileUtils.getOutputStream("demo.xls",null),
                false,null);
    }

    @Test
    public void readCn() throws Exception {
        Map<String, String> targetFields = new HashMap<>();
        targetFields.put("name","用户名");
        targetFields.put("address","地址");
        targetFields.put("age","年龄");
        Collection<UserRead> users = ExcelUtils.read(ExcelTest.class.getResourceAsStream("/read_cn.xls"), UserRead.class, targetFields);
        System.out.println(users);
    }

    @Test
    public void readEn() throws Exception {
        Collection<UserRead> users = ExcelUtils.read(ExcelTest.class.getResourceAsStream("/read_en.xls"), UserRead.class);
        System.out.println(users);
    }
}
