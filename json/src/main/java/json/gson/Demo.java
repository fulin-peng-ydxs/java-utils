package json.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * @author PengFuLin
 * @version 1.0
 * @description: 解析演示
 * @date 2021/11/14 16:42
 */
public class Demo {

    public static void main(String[] args) {
        //JSON解析器：gson
        Gson gson = new Gson();
        //对象转Json
        //1.非集合型
        Person person = new Person("测试",1,2);
        String jsonObject = gson.toJson(person);
        System.out.println(jsonObject);
        //2.集合型
        ArrayList<Person> peoples = new ArrayList<>();
        peoples.add(person);
        String jsonCollections = gson.toJson(peoples);
        System.out.println(jsonCollections);

        //JsonString转对象
        //1.非集合型
        person= gson.fromJson(jsonObject, Person.class);
        System.out.println(person);
        //2.集合型 : 需要用TypeToken指定集合中的元素类型
        List<Person> list = gson.fromJson(jsonCollections, new TypeToken<List<Person>>() {
        }.getType());
        System.out.println(list);
    }
}

class  Person{
    public Person(String name, int sex, int age) {
        this.name = name;
        this.sex = sex;
        this.age = age;
    }

    public String name;
    public int sex;
    public int age;

//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public int getSex() {
//        return sex;
//    }
//
//    public void setSex(int sex) {
//        this.sex = sex;
//    }
//
//    public int getAge() {
//        return age;
//    }
//
//    public void setAge(int age) {
//        this.age = age;
//    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", sex=" + sex +
                ", age=" + age +
                '}';
    }
}