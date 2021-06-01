package org.maple.juc.locksupport;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 * 通过 interrupt 来解除 park，LockSupport 不会抛出打断异常
 */
public class LockSupportTest4 {

    public static void main(String[] args) throws InterruptedException {
        Thread child = new Thread(() -> {
            System.out.println("child thread start park");
            // 未标记为打断，则 park
            while (!Thread.currentThread().isInterrupted()) {
                // park 在该行，放行后会判断是否标记为打断状态，如果不是的话继续 park

                LockSupport.park();
            }
            // 注意：unpark 后，isInterrupted 仍然为 true，不会清除打断标记
//            System.out.println(Thread.currentThread().isInterrupted());
            System.out.println("child thread unpark");
        });

        child.start();

        TimeUnit.SECONDS.sleep(1);

        System.out.println("main thread begin unpark child thread");

        child.interrupt();
    }
}
