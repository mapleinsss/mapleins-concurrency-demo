package org.maple.juc.aqs;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 自旋锁简易实现
 * - 第一个是锁饥饿问题。在锁竞争激烈的情况下，可能存在一个线程一直被其他线程”插队“而一直获取不到锁的情况。
 * - 第二是性能问题。在实际的多处理上运行的自旋锁在锁竞争激烈时性能较差。
 */
public class SpinLock {

    private final AtomicReference<Thread> owner = new AtomicReference<>();

    public void lock() {
        Thread currentThread = Thread.currentThread();
        // 如果锁未被占用，则设置当前线程为锁的拥有者
        while (!owner.compareAndSet(null, currentThread)) {
        }
    }

    public void unlock() {
        Thread currentThread = Thread.currentThread();
        // 只有锁的拥有者才能释放锁
        owner.compareAndSet(currentThread, null);
    }

}
