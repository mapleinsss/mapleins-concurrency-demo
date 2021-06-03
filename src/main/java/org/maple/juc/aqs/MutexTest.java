package org.maple.juc.aqs;

import java.util.concurrent.TimeUnit;

public class MutexTest {

    static Mutex lock = new Mutex();
    static Integer num = 0;

    public static void main(String[] args) throws InterruptedException {


        Thread ta = new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread A get lock");
                num = 2;
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                lock.unlock();
                System.out.println("Thread A unlock");
            }
        }, "Thread A");


        Thread tb = new Thread(() -> {
            try {
                lock.lock();
                System.out.println("Thread B get lock");
                num = 5;

            } finally {
                lock.unlock();
                System.out.println("Thread B unlock");
            }
        }, "Thread B");

        ta.start();
        tb.start();

        ta.join();
        tb.join();

    }
}
