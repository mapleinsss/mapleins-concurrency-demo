package org.maple.juc.aqs;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;

public class NonReentrantLockTest {

    final static NonReentrantLock lock = new NonReentrantLock();
    final static Condition notFull = lock.newCondition();
    final static Condition notEmpty = lock.newCondition();
    final static Queue<String> queue = new LinkedBlockingQueue<>();
    final static int queueSize = 10;

    static Thread producer = new Thread(() -> {
        // 获取独占锁
        lock.lock();
        try {
            while (queue.size() == queueSize) {
                notEmpty.await();
                queue.add("ele");
                notFull.signalAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    });

    static Thread consumer = new Thread(() -> {
        // 获取独占锁
        lock.lock();
        try {
            while (0 == queue.size()) {
                notFull.await();
            }
            // 消费一个元素
            String ele = queue.poll();
            // 唤醒生产线程
            notEmpty.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    });

    public static void main(String[] args) {
        producer.start();
        consumer.start();
    }

}