package org.maple.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestArrayCopy {

    public static void main(String[] args) {
        int[] array1 = new int[]{1, 2, 8, 7, 6};
        int[] array2 = new int[array1.length];
        /**
         * src – the source array.
         * srcPos – starting position in the source array.
         * dest – the destination array.
         * destPos – starting position in the destination data.
         * length – the number of array elements to be copied.
         */
        System.arraycopy(array1, 0, array2, 0, array1.length);

        System.out.println("array1 = " + Arrays.toString(array1));
        System.out.println("array2 = " + Arrays.toString(array2));
        System.out.println("=========================");

        array2[0] = 100;
        System.out.println("array1 = " + Arrays.toString(array1));
        // 由结果可以看出，当对复制数组的某个元素进行改变时，并不影响被复制数组对应元素，
        // 即对于基本数据类型来说 System.arraycopy() 方法是深拷贝。
        System.out.println("array2 = " + Arrays.toString(array2));
        System.out.println(array1 == array2); // false
        System.out.println("=========================");

        // 对于引用数据类型来说 System.arraycopy() 方法是浅拷贝。不会进行递归拷贝,对象只是引用拷贝
        // 所以对于数组是深拷贝，但是对于数组中的引用元素，用的是同一个指针
        Person[] sourPerson = new Person[3];
        for (int i = 0; i < 3; i++) {
            sourPerson[i] = new Person(i);
        }
        Person[] copyPerson = new Person[3];
        System.arraycopy(sourPerson, 0, copyPerson, 0, sourPerson.length);

        copyPerson[0].setAge(18);
        System.out.println(sourPerson == copyPerson); // false
        for (int i = 0; i < sourPerson.length; i++) {
            System.out.println(sourPerson[i]);
            System.out.println(copyPerson[i]);
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
