package hutool.excel;


import basic.file.FileUtils;
import hutool.entity.User;
import hutool.excel.utils.ExcelUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
        ExcelUtils.exportNoHead(null,users, FileUtils.getFileOutputStream("demo.xls",null),
                false,null);
    }
}
