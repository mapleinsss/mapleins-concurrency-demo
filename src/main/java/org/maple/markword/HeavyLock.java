package org.maple.markword;

import org.openjdk.jol.info.ClassLayout;

/**
 * @author mapleins
 * 通过 jol 打印出重量级锁
 */
public class HeavyLock {

    private final static User USER = new User();

    private static void printf() {
        System.out.println(ClassLayout.parseInstance(USER).toPrintable());
    }

    /*
      0     4                     (object header)                           8a 29 0a 20 (10001010 00101001 00001010 00100000) (537536906)
      4     4                     (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4                     (object header)                           43 c1 00 f8 (01000011 11000001 00000000 11111000) (-134168253)

      可以看到最后三位变成了 010，USER 升级为重量级锁
     */
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                while (!Thread.interrupted()) {
                    synchronized (USER) {
                        printf();
                    }
                }
            }).start();
        }
        Thread.sleep(Integer.MAX_VALUE);
    }
}
