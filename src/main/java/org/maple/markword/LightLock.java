package org.maple.markword;

import org.openjdk.jol.info.ClassLayout;

/**
 * @author mapleins
 * 通过 jol 打印出轻量级锁
 */
public class LightLock {

    static final User user = new User();
    public static void main(String[] args) {
        synchronized (user) {
            int i = 0;
            System.out.println(ClassLayout.parseInstance(user).toPrintable());
        }
    }

    /*
    org.maple.markword.User object internals:
         OFFSET  SIZE                TYPE DESCRIPTION                               VALUE
              0     4                     (object header)                           88 f1 bd 02 (10001000 11110001 10111101 00000010) (46002568)
              4     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
              8     4                     (object header)                           43 c1 00 f8 (01000011 11000001 00000000 11111000) (-134168253)
             12     1             boolean User.sex                                  false
             13     3                     (alignment/padding gap)
             16     4    java.lang.String User.name                                 null
             20     4   java.lang.Integer User.age                                  null
        Instance size: 24 bytes
        Space losses: 3 bytes internal + 0 bytes external = 3 bytes total

        观察最后三位的值为 000，所以当前 synchronized 锁住的 user 升级为轻量级锁
     */

}
