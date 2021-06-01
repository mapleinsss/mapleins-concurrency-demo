package org.maple.juc.locksupport;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 * 通过父线程 unpark 控制子线程的 permit
 */
public class LockSupportTest3 {

    public static void main(String[] args) throws InterruptedException {
        Thread child = new Thread(() -> {
            System.out.println("child thread start park");
            LockSupport.park();
            System.out.println("child thread unpark");
        });

        child.start();

        TimeUnit.SECONDS.sleep(1);

        System.out.println("main thread begin unpark child thread");
        LockSupport.unpark(child);
    }
}
