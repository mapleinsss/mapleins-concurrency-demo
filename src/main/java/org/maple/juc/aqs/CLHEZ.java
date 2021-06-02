package org.maple.juc.aqs;

import java.util.concurrent.atomic.AtomicReference;

/**
 * https://juejin.cn/post/6942753984436404255
 *
 * 性能优异，获取和释放锁开销小。CLH 的锁状态不再是单一的原子变量，而是分散在每个节点的状态中，
 * 降低了自旋锁在竞争激烈时频繁同步的开销。在释放锁的开销也因为不需要使用 CAS 指令而降低了。
 *
 * 公平锁。先入队的线程会先得到锁。
 * 实现简单，易于理解。
 * 扩展性强。下面会提到 AQS 如何扩展 CLH 锁实现了 j.u.c 包下各类丰富的同步器。
 */
public class CLHEZ {

    private final ThreadLocal<Node> node = new ThreadLocal<>();
    private final AtomicReference<Node> tail = new AtomicReference<>();

    private static class Node {
        private volatile boolean locked;
    }

    public void lock(){
        Node node = this.node.get();
        node.locked = true;
        Node pre = this.tail.getAndSet(node);
        while (pre.locked){}
    }

    public void unlock(){
        Node node = this.node.get();
        node.locked = false;
        this.node.set(new Node());
    }
}
