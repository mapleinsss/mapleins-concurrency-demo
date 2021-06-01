package org.maple.juc.locksupport;

import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 *
 * 通过 unpark 先获取 permit，再 park 该线程会直接返回
 */
public class LockSupportTest2 {

    public static void main(String[] args) {
        System.out.println("begin park");
        // 通过 unpark 获取 permit
        LockSupport.unpark(Thread.currentThread());
        // 现在当前线程有 permit，所以 park 会直接返回
        LockSupport.park();

        System.out.println("end park");
    }
}
