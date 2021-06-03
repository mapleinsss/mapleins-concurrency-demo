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

## AQS 源码组成

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

```java
static final class Node {

    static final Node SHARED = new Node();

    static final Node EXCLUSIVE = null;

    static final int CANCELLED =  1;

    static final int SIGNAL    = -1;

    static final int CONDITION = -2;

    static final int PROPAGATE = -3;

    volatile int waitStatus;

    volatile Node prev;

    volatile Node next;

    volatile Thread thread;

    Node nextWaiter;

    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null)
            throw new NullPointerException();
        else
            return p;
    }

    Node() {    // Used to establish initial head or SHARED marker
    }

    Node(Thread thread, Node mode) {     // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }

    Node(Thread thread, int waitStatus) { // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```



## AQS 源码执行流程分析

AQS 的源码顶部注释提供了一个互斥锁的案例，我们从这个案例来分析 AQS 的执行流程，代码如下 ：

```java
public class Mutex implements Lock, Serializable {
    
    // Our internal helper class
    private static class Sync extends AbstractQueuedSynchronizer {
        // Reports whether in locked state
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // Acquires the lock if state is zero
        public boolean tryAcquire(int acquires) {
            assert acquires == 1; // Otherwise unused
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // Releases the lock by setting state to zero
        protected boolean tryRelease(int releases) {
            assert releases == 1; // Otherwise unused
            if (getState() == 0) throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // Provides a Condition
        Condition newCondition() {
            return new ConditionObject();
        }

        // Deserializes properly
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }

    }

    // The sync object does all the hard work. We just forward to it.
    private final Sync sync = new Sync();

    public void lock() {
        sync.acquire(1);
    }

    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    public void unlock() {
        sync.release(1);
    }

    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}

```

编写一个测试用例：

```java
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
```

### 获取锁流程
对于 A、B 两个线程，开始执行后通过 `lock.lock();` 尝试上锁：

```java
public void lock() {
    sync.acquire(1);
}
```

其中 `lock` 方法会调用 `sync.acquire(1);`，这个方法是 AQS 封装为 final 的方法。

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

#### tryAcquire

对于 `tryAcquire(int arg)` 这个尝试获取锁的方法，AQS 定义为模板方法，子类必须实现，否则抛出不支持操作异常，	标准的模板方法设计模式。

```java
protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
}
```

Mutex 的实现如下：

```java
public boolean tryAcquire(int acquires) {
    // 定义 state 的值为 1 表示上锁成功。
    assert acquires == 1; // Otherwise unused
    // 通过 CAS 加锁，第一个进来的线程会成功上锁
    if (compareAndSetState(0, 1)) {
        // 在 AbstractOwnableSynchronizer 中将该线程设置为独占线程
        setExclusiveOwnerThread(Thread.currentThread());
        // 由于返回 true，在 acquire() 中返回，即第一个线程加锁成功
        return true;
    }
    // 获取锁失败
    return false;
}
```

#### addWaiter

> Creates and enqueues node for current thread and given mode.

将当前线程封装成一个 Node，并且入队。源码如下：

```java
private Node addWaiter(Node mode) {
    // 将当前线程封装成一个 Node
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;

    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 当 tail 节点为空时，会执行入队方法。
    enq(node);
    return node;
}
```

先分析第一次入队，即 `enq(final Node node)`：

对于第一次循环，此时尾节点的指针为空（懒加载），所以必须初始化，会创建一个新的 Node，并且放置在 head 的内存地址，然后 tail 也指向该内存地址。这个新建的 Node，网上称为哨兵节点。

对于第二次循环，此时尾部节点已经存在了，所以走 else 的逻辑，将参数 node 的 prev 指针指向 tail，即哨兵节点。再通过 CAS 将尾节点指针指向当前节点，最后将哨兵节点的 next 指向传入的 node，形成双向列表后返回传入的节点。

```java
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        // 尾节点为空，初始化
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            // 设置尾结点指针指向当前传入的节点
            // 和之前的尾结点形成双向链表
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}

// 头指针的内存地址
private static final long headOffset;
// 尾指针的内存地址
private static final long tailOffset;

private final boolean compareAndSetHead(Node update) {
    return unsafe.compareAndSwapObject(this, headOffset, null, update);
}

private final boolean compareAndSetTail(Node expect, Node update) {
    return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
}
```

#### acquireQueued

当 addWaiter 将当前节点入队后，会执行 acquireQueued 方法，返回值 interrupted 表示当前节点是否被标记为打断。

```java
final boolean acquireQueued(final Node node, int arg) {
    // 当出现异常，failed 会执行 cancelAcquire()
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            // p 为 node 的前节点
            final Node p = node.predecessor();
            // 前驱节点为 head 节点，并且再次尝试获取锁成功
            if (p == head && tryAcquire(arg)) {
                // 如果当前 Node 获取成功了
                // 将当前节点设置为头节点，并且清空 node 中的 thread 和 前指针
                setHead(node);
                // 将之前的头节点的 next 指针置空，方便 GC
                p.next = null; // help GC
                failed = false;
                // 返回 false
                return interrupted;
            }
            // 如果不是头节点，或者获取锁失败了
            // 先处理 ws，循环多次后，会将前置节点的 ws 置为 true
            // 然后将当前线程 LockSupport.park() 
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

private void setHead(Node node) {
    head = node;
    node.thread = null;
    node.prev = null;
}

// 获取失败后，将前置节点的 ws 设置为 SIGNAL，才返回 true，并且通过指针过滤一些 Cancelled 的节点，多次循环必返回 true
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    // 如果前置节点是 -1，那么返回 true，表示后记节点在等待唤醒。
    if (ws == Node.SIGNAL)
        return true;
    // ws 大于 0，前节点可能被 cancelled（CANCELLED(1)），所以需要跳过他们，并且更改指针
    // 将当前 node 节点的指针指向 ws <= 0 的节点上。
    // 并且 pred 也变成了 ws <= 0 的节点
    // 返回 false
    if (ws > 0) {
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        // ws = 0 或者 ws = PROPAGATE（-3）共享模式下，前继结点不仅会唤醒其后继结点，同时也可能会唤醒后继的后继结点。
        // 将前置节点的 ws 设置为 -1
        // 返回 false
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}

// waitStatus 的内存地址
private static final long waitStatusOffset;

private static final boolean compareAndSetWaitStatus(Node node,
                                                     int expect,
                                                     int update) {
    return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                    expect, update);
}

// 阻塞当前线程
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this);
    return Thread.interrupted();
}
```

### 释放锁流程

对于获取锁的线程，执行结束后通过 `lock.unlock();`  释放锁：

```java
public void unlock() {
    sync.release(1);
}

public final boolean release(int arg) {
    if (tryRelease(arg)) {
        // 拿到当前 AQS 的头节点，哨兵节点
        Node h = head;
        // 哨兵节点不为空，并且 ws ！= 0
        if (h != null && h.waitStatus != 0)
            // 唤醒后面的线程的线程
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

同样 `tryRelease()` 是模板方法，Mutex 实现如下：

```java
protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
}

// 将 AQS 独占线程置空，共享 state 设置为 0
protected boolean tryRelease(int releases) {
    assert releases == 1; // Otherwise unused
    if (getState() == 0) throw new IllegalMonitorStateException();
    setExclusiveOwnerThread(null);
    setState(0);
    return true;
}
```

释放后继的代码：

```java
private void unparkSuccessor(Node node) {
	// 这个是头节点
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```



https://www.cnblogs.com/waterystone/p/4920797.html











```java
public boolean offer(E e) {
    checkNotNull(e);
    final Node<E> newNode = new Node<E>(e);

    for (Node<E> t = tail, p = t;;) {
        Node<E> q = p.next;
        if (q == null) {

            if (p.casNext(null, newNode)) {
                if (p != t) // hop two nodes at a time
                    casTail(t, newNode);  // Failure is OK.
                return true;
            }
        }
        else if (p == q)
            p = (t != (t = tail)) ? t : head;
        else
            p = (p != t && t != (t = tail)) ? t : q;
    }
}
```



