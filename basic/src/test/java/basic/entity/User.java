package basic.entity;

import lombok.Data;

/**
 * 用户实体
 *
 * @author pengshuaifeng
 * 2023/12/24
 */
@Data
public class User {
    private String name;
    private String address;
    private int age;
}
