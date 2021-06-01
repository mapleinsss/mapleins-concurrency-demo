package org.maple.markword;

import org.openjdk.jol.info.ClassLayout;

/**
 * @author mapleins
 * jol 打印对象头
 */
public class Test {
    public static void main(String[] args) {
        User user = new User();
        System.out.println(ClassLayout.parseInstance(user).toPrintable());

        /*

        JDK8 默认是开启了指针压缩
        org.maple.markword.User object internals:
         OFFSET  SIZE                TYPE DESCRIPTION                               VALUE
              0     4                     (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
              4     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
              8     4                     (object header)                           43 c1 00 f8 (01000011 11000001 00000000 11111000) (-134168253)
             12     1             boolean User.sex                                  false
             13     3                     (alignment/padding gap)
             16     4    java.lang.String User.name                                 null
             20     4   java.lang.Integer User.age                                  null
        Instance size: 24 bytes
        Space losses: 3 bytes internal + 0 bytes external = 3 bytes total

         使用 -XX:-UseCompressedOops 关闭指针压缩
         org.maple.markword.User object internals:
         OFFSET  SIZE                TYPE DESCRIPTION                               VALUE
              0     4                     (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
              4     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
              8     4                     (object header)                           10 35 88 1c (00010000 00110101 10001000 00011100) (478688528)
             12     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
             16     1             boolean User.sex                                  false
             17     7                     (alignment/padding gap)
             24     8    java.lang.String User.name                                 null
             32     8   java.lang.Integer User.age                                  null
        Instance size: 40 bytes
        Space losses: 7 bytes internal + 0 bytes external = 7 bytes total

        其中：
            - OFFSET：偏移地址，单位字节；
            - SIZE：占用的内存大小，单位为字节；
            - TYPE DESCRIPTION：类型描述，其中 object header 为对象头；
            - VALUE：对应内存中当前存储的值；

        开启指针压缩，引用类型占 4 个字节，一个字节的 boolean 的基本类型需要 3 个字节来对齐
        关闭指针压缩，引用类型占 8 个字节，一个字节的 boolean 的基本类型也需要 7 个字节来对齐

        开启指针压缩，对象头占用 16 个字节，即 16 * 8 = 128 bit
        关闭指针压缩，对象头占用 12 个字节，即 12 * 8 = 96 bit

        源码中 markOop.hpp 通过最后三位的枚举来判断当前 MarkWord 的状态
        enum {  locked_value                = 0, // 0 00  轻量级锁
                unlocked_value              = 1, // 0 01  无锁
                monitor_value               = 2, // 0 10  重量级锁
                marked_value                = 3, // 0 11  gc标志
                biased_lock_pattern         = 5  // 1 01  偏向锁
        };


        |--------------------------------------------------------------------------------------------------------------|
        |                                              Object Header (128 bits)                                        |
        |--------------------------------------------------------------------------------------------------------------|
        |                        Mark Word (64 bits)                                    |      Klass Word (64 bits)    |
        |--------------------------------------------------------------------------------------------------------------|
        |  unused:25 | identity_hashcode:31 | unused:1 | age:4 | biased_lock:1 | lock:2 |     OOP to metadata object   |  无锁
        |----------------------------------------------------------------------|--------|------------------------------|
        |  thread:54 |         epoch:2      | unused:1 | age:4 | biased_lock:1 | lock:2 |     OOP to metadata object   |  偏向锁
        |----------------------------------------------------------------------|--------|------------------------------|
        |                     ptr_to_lock_record:62                            | lock:2 |     OOP to metadata object   |  轻量锁
        |----------------------------------------------------------------------|--------|------------------------------|
        |                     ptr_to_heavyweight_monitor:62                    | lock:2 |     OOP to metadata object   |  重量锁
        |----------------------------------------------------------------------|--------|------------------------------|
        |                                                                      | lock:2 |     OOP to metadata object   |    GC
        |--------------------------------------------------------------------------------------------------------------|


          0     4                     (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
          4     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)

          8     4                     (object header)                           10 35 88 1c (00010000 00110101 10001000 00011100) (478688528)
         12     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)

        对于第一行和第二行，为 64 bit Mark Word
        值采用小端存储，所以应该是倒着排：
        00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001
        后三位的值为 001，即无锁状态

        当我们调用 hashCode() 函数
            System.out.println(user.hashCode());
            System.out.println(ClassLayout.parseInstance(user).toPrintable());
        此时头部为：
         0     4                     (object header)                           01 82 44 a1 (00000001 10000010 01000100 10100001) (-1589345791)
         4     4                     (object header)                           74 00 00 00 (01110100 00000000 00000000 00000000) (116)
         8     4                     (object header)                           88 35 b0 1c (10001000 00110101 10110000 00011100) (481310088)
        12     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
        64 bit Mark Word 的值为：
        00000000 00000000 00000000 01110100 10100001 01000100 10000010 00000001
        它的锁的类型为 001，即无锁状态，下面为 Mark Word 具体的值：
        lock(2):01
        biased_lock(1):0
        age(4):0
        unused(1):
        identity_hashcode(31): 10000010 01000100 10100001 0111010
        unused(25):


        8~16 为元数据指针，即 Klass Word，未压缩占 64 bit，压缩只占 32 bit。

         */
    }
}
