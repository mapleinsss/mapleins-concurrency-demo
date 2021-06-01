package org.maple.juc.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 *
 * LockSupport 与线程关联，当中有个 permit 概念，也就是许可证，permit 决定线程是否能被阻塞
 */
public class LockSupportTest {

    public static void main(String[] args) {
        System.out.println("begin park");
        // 默认是没有 permit 的，所以被阻塞
        LockSupport.park();
        System.out.println("end park");
    }
}
