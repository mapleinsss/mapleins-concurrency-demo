package org.maple.juc.aqs;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class AtomicReferenceFieldUpdaterTest {

    static class Person {
        volatile String name = "老刘";
    }

    public static void main(String[] args) {
        AtomicReferenceFieldUpdater<Person, String> updater =
                AtomicReferenceFieldUpdater.newUpdater(Person.class, String.class, "name");
        Person person = new Person();
        updater.compareAndSet(person, person.name, "老王");
        System.out.println(person.name);
    }
}
