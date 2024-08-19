package json.test;

import json.jackson.utils.JsonUtils;
import json.model.User;
import org.junit.Test;

/**
 * @author fulin-peng
 * 2024-08-19  10:17
 */
public class TestDemo {


    @Test
    public void testJson(){
        System.out.println(JsonUtils.getString(new User("xx", "1")));
    }

}
