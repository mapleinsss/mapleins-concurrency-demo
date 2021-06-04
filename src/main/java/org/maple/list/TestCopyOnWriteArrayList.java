package org.maple.list;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCopyOnWriteArrayList {

    public static void main(String[] args) {
        Person[] sourPerson = new Person[3];
        for (int i = 0; i < 3; i++) {
            sourPerson[i] = new Person(i);
        }

        // 该构造方法基于 System.arraycopy，对于引用类型数组，引用类型是浅拷贝！
        CopyOnWriteArrayList<Person> cowList = new CopyOnWriteArrayList<>(sourPerson);
        for (Person person : cowList) {
            System.out.println(person);
        }
        for (Person person : sourPerson) {
            System.out.println(person);
        }
        System.out.println("====modify===");
//        cowList.get(0).setAge(5);
//        sourPerson[0].setAge(6);
        for (Person person : cowList) {
            System.out.println(person);
        }
        for (Person person : sourPerson) {
            System.out.println(person);
        }
    }

    static class Person{
        private int age;
        public Person(int age) {
            this.age = age;
        }
        public void setAge(int age) {
            this.age = age;
        }
        @Override
        public String toString() {
            return "Person{" +
                    "age=" + age +
                    '}';
        }
    }
}
