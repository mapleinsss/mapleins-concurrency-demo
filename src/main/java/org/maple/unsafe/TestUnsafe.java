package org.maple.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnsafe {

    private volatile long state = 0;

    static final Unsafe unsafe;

    static final long stateOffset;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            stateOffset = unsafe.objectFieldOffset(TestUnsafe.class.getDeclaredField("state"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

    }
}
