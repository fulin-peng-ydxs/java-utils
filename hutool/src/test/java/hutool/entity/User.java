package hutool.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * user
 *
 * @author pengshuaifeng
 * 2023/12/30
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    private String name;
    private String address;
    private int age;
}

