package org.maple.juc.aqs;

import java.util.concurrent.locks.ReentrantLock;

public class AQSTest {
    public static void main(String[] args) {
        final ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();

        reentrantLock.unlock();
    }
}
