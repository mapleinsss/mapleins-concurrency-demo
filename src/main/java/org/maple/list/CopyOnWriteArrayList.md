# CopyOnWriteArrayList
并发包中的 List 只有这一个，它是一个线程安全的 ArrayList，对其进行修改都是在底层的一个复制数组（快照）上进行的，也就是进行了写时复制策略。

## 构造方法

注意，对于有参构造，都是使用 `Arrays.copyOf` 进行复制，而 `Arrays.copyOf` 的实现又是通过 `System.arraycopy` 实现。`System.arraycopy` 对于基本类型的复制是深拷贝，而对于引用类型，数组数组是深拷贝，但是对于数组中的引用元素，用的是同一个指针，所以修改引用类型的值，会影响到原数组！

```java
// 无参创建一个大小为 0 的 Object 数组
public CopyOnWriteArrayList() {
    setArray(new Object[0]);
}

// 使用了 Arrays.copyOf 复制了 toCopyIn
public CopyOnWriteArrayList(E[] toCopyIn) {
    setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
}

// 如果传入的集合就是 CopyOnWriteArrayList，那么直接把当前指针指过去
// 如果不是，那么使用 Arrays.copyOf 复制
public CopyOnWriteArrayList(Collection<? extends E> c) {
    Object[] elements;
    if (c.getClass() == CopyOnWriteArrayList.class)
        elements = ((CopyOnWriteArrayList<?>)c).getArray();
    else {
        elements = c.toArray();
        // c.toArray might (incorrectly) not return Object[] (see 6260652)
        if (elements.getClass() != Object[].class)
            elements = Arrays.copyOf(elements, elements.length, Object[].class);
    }
    setArray(elements);
}
```

## add

整个 add 是原子操作：使用了独占锁，保证了在添加时，只有一个线程能 add，保证了这个线程在添加元素时，其他线程不会对 array 修改。

```java
public boolean add(E e) {
    // 互斥锁
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        // 先复制 elements 到新数组，然后再添加元素
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        // 使用新数组替换原来的数组
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}

// 指定位置插入，如果不是在 len 处添加，会复制两次到 newElements 中
public void add(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: "+index+
                                                ", Size: "+len);
        Object[] newElements;
        int numMoved = len - index;
        if (numMoved == 0)
            newElements = Arrays.copyOf(elements, len + 1);
        else {
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1,
                             numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    } finally {
        lock.unlock();
    }
}
```

## get

调用 `get(int index)` 是一个两步走的操作：

1. 获取数组指针
2. 根据数组下标获取值

```java
public E get(int index) {
    return get(getArray(), index);
}

final Object[] getArray() {
    return array;
}

private E get(Object[] a, int index) {
    return (E) a[index];
}
```

这两步操作并没有加锁，所以我们来看下会不会存在并发问题：假设现在有两个线程 A、B：

- A 执行到 `getArray` 拿到了当前的 array[]。
- B 执行 `remove()` 了一个值（remove 方法后面讲，这里先知道 remove 也是复制了当前的数组，在新数组上操作）。删除的最后，会把新

