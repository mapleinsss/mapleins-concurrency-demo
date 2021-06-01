package org.maple.juc.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 *
 * park(Object blocker)
 */
public class LockSupportTest5 {

    static class A {}

    public static void main(String[] args) {
        System.out.println("begin park");
        /**
         * 指定 Object blocker，方便调试
         * 通过 jstack 查看如下
         * "main" #1 prio=5 os_prio=0 tid=0x000000000316e800 nid=0x3c78 waiting on condition [0x00000000035bf000]
         *    java.lang.Thread.State: WAITING (parking)
         *         at sun.misc.Unsafe.park(Native Method)
         *         可以很清楚的看出当前 WATTING 状态的线程在等待 A 的对象
         *         - parking to wait for  <0x000000076b795f80> (a org.maple.juc.locksupport.LockSupportTest5$A)
         *         at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
         *         at org.maple.juc.locksupport.LockSupportTest5.main(LockSupportTest5.java:25)
         *
         */
        LockSupport.park(new A());
        System.out.println("end park");
    }
}
