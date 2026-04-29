package com.itheima;

import org.junit.jupiter.api.Test;

public class ThreadLocalTest {


    @Test
    public void testThreadLocalSetAndGet(){
        //提供一个threadLocal对象
        ThreadLocal threadLocal = new ThreadLocal();
        //开启两个线程
        new Thread(()->{
            threadLocal.set("萧炎");
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
        },"萧 线程").start();

        new Thread(()->{
            threadLocal.set("药尘");
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
            System.out.println(Thread.currentThread().getName() + ":" + threadLocal.get());
        },"药 线程").start();
    }
}
