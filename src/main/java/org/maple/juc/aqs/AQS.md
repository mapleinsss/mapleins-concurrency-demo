# AQS 详解



## TODO

AQS 是自旋还是阻塞？

AQS 是否是根据队列中前一个变量来自旋的？

## AQS 是什么

AQS（AbstractQueuedSynchronizer）：抽象的队列同步器，位于 `java.util.concurrent.locks` 包下，它是一个框架，在该类的头上的注释里这样描述 AQS：

> Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) that rely on first-in-first-out (FIFO) wait queues. 

AQS 提供了一个框架来实现阻塞锁和相关的同步器（比如信号量、事件等），它依赖于先进先出等待队列。

关于 AQS 的设计可以参考 Doug Lea 的 Paper 《The java.util.concurrent Synchronizer Framework》，中文翻译在网上可以搜到，例如并发编程网：http://ifeve.com/aqs/。

 由于 AQS 的设计初衷是要提供一个方便开发者在其基础上定制开发各类同步器，所以需要分析各类同步器的共通可复用特性， 将其抽取出来， 由 AQS 封装实现， 外部仅需调用。

经过分析， Doug Lea 提炼出了各类同步器共通的两类方法：

- `acquire` 操作

  当一个线程调用 acquire 操作后， 如果当前同步状态不允许该线程通过， 该线程会被阻塞。

- release 操作
  当一个线程调用 release 操作后，可以更改同步状态， 使得一个或者多个线程解除阻塞。

在上述两类操作基础上， 还需要附加支持如下特性：

- 非阻塞式的调用。
- 阻塞式调用情形下，允许设置超时。
- 允许通过中断的方式取消 acuqire 操作。

## AQS 源码分析

### State

> This class is designed to be a useful basis for most kinds of synchronizers that rely on a single atomic {@code int} value to represent state. Subclasses must define the protected methods that change this state, and which define what that state means in terms of this object being acquired or released.

源码中注释说，通过 state 的原子变量来定义对象（锁）是被获取还是释放。这个值是实现 AQS 框架的类自己定义的，比如：

- ReentrantLock：state 表示当前线程获取锁的可重入次数。
- ReentrantReadWriteLock：state 表示高 16 位表示获取读锁的次数，低 16 位表示写锁的可重入次数。 
- Semaphore：state 表示当前可用信号的个数。
- CountDownLatch：state 表示计数器当前的值。

下面是涉及 state 的源码：

```java
// 同步状态 state
private volatile int state;

protected final int getState() {
    return state;
}

protected final void setState(int newState) {
    state = newState;
}

// cas 改变 state 的值
protected final boolean compareAndSetState(int expect, int update) {
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}

// 初始化加载 unsafe 和 state 在内存的地址，方便做 CAS 操作
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long stateOffset;

static {
    try {
        stateOffset = unsafe.objectFieldOffset
            (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
        ...
    } catch (Exception ex) { throw new Error(ex); }
}
```

### Node

前面说了 AQS 是基于 FIFO 的等待队列，来实现线程的排队。源码中的队列结点是 Node 类来实现的，下面这段注释其实很重要：

> The wait queue is a variant of a "CLH" (Craig, Landin, and Hagersten) lock queue. CLH locks are normally used for spinlocks.  We instead use them for blocking synchronizers, but use the same basic tactic of holding some of the control information about a thread in the predecessor of its node.A "status" field in each node keeps track of whether a thread should block.  A node is signalled when its predecessor releases.  Each node of the queue otherwise serves as a specific-notification-style monitor holding a single waiting thread. The status field does NOT control whether threads are granted locks etc though.  A thread may try to acquire if it is first in the queue. But being first does not guarantee success; it only gives the right to contend.  So the currently released contender thread may need to rewait.

首先，AQS 的等待队列是基于 CLH 队列做了一个 variant（变种）。我们先来了解一下 CLH 锁主要解决了什么问题，关于 CLH 锁有空把它们的 Paper 翻译下。

对于一个线程等待锁一般有两种方式：

- 消极等待：线程让出 CPU 时间片，进入阻塞状态，在锁释放前，等待的线程不会获得 CPU 的调度。
- 积极等待：保持 CPU 控制权，只要线程获得到 CPU 时间片就会不停的检测锁状态并且尝试获取，这种尝尝称为自旋锁。

在对阻塞锁和自旋锁的比较时，经常看到这样的描述：

- 对于竞争不激烈的情况下，一般都会使用积极等待，即自旋锁来实现锁的功能，可以免除上下文的切换，提高性能，并且由于竞争不激烈，所以 CPU 空转导致的占用率也不高。
- 对于阻塞锁，阻塞的线程不会占用 CPU 时间， 不会导致 CPU 占用率过高，但进入时间以及恢复时间都要比自旋锁略慢，因为线程的状态需要切换。

这里阻塞锁不是我们分析的内容，我们具体看下自旋锁存在的问题，

1. CPU 会有通信开销：对于普通的自旋锁，在多个线程竞争的情况下，相当于要对同一块内存地址进行不停的获取期望值（获取锁）的操作。假设一个线程释放锁，对于一致内存访问架构（Uniform Memory Access，UMA），这个内存地址的变量会被释放锁的线程修改为可以获取锁的状态，为了保证这个值的可见性，会通过总线告知其他 CPU 缓存的这个变量失效（例如 MESI 协议），其他 CPU 又要从内存中读取这个变量。
2. 公平性：普通的自旋锁是抢占式的，所以会出现饥饿的情况。

CLH 解决上述两个问题的办法是通过一个单向链表来实现排它自旋锁。首先对于公平性的问题，采取队列来实现，进来的线程排在队尾，当获得锁的线程释放锁，队首的线程将获得锁，这样就解决了公平性的问题。而对于 CPU 通信开销，则是通过队列前一个节点保存的状态信息来决定当前线程的状态。所以每个 CPU 自旋时获取锁的状态取决于前一个节点，而不是所有 CPU 访问一个共享内存地址。

AQS 的 FIFO 队列



