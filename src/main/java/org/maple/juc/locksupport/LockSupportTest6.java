package org.maple.juc.locksupport;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.LockSupport;

/**
 * @author mapleins
 *
 * parkUntil(long deadline)：阻塞在一个具体的时间戳上，是过去的就直接返回。
 * parkNanos(long nanos)：阻塞多少纳秒。
 */
public class LockSupportTest6 {

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
        // 阻塞 5秒
        LockSupport.parkNanos(5000L * 1000000);
        System.out.println(LocalDateTime.now());

        // 具体某个时间戳暂停
//        LockSupport.parkUntil(LocalDateTime.now().plusSeconds(5).toInstant(ZoneOffset.of("+8")).toEpochMilli());
        LockSupport.parkUntil(0);

        System.out.println(LocalDateTime.now());
    }
}
