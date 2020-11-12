package com.vow.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**
         * 1、初始化线程的4中方式
         *     1）、继承Thread
         *          Thread01 thread01 = new Thread01();
         *         thread01.start();
         *     2）、实现Runnable接口
         *          Runnable01 runnable01 = new Runnable01();
         *         Thread thread = new Thread(runnable01);
         *         thread.start();
         *     3）、实现Callable接口 + FutureTask（可以拿到返回结果，可以处理异常）
         *          Callable01 callable01 = new Callable01();
         *         FutureTask<Integer> integerFutureTask = new FutureTask<>(callable01);
         *         Thread thread = new Thread(integerFutureTask);
         *         thread.start();
         *         // 阻塞等待整个线程执行完成，获取返回结果
         *         Integer integer = integerFutureTask.get();
         *     4）、线程池(ExecutorService)
         *          给线程池直接提交任务
         *          1、创建
         *              1）、Executors
         *              2）、
         */

        System.out.println("main thread starting...");
        // new Thread(() -> System.out.println("hello")).start();

        // 当前系统中只有一两个，每个异步任务直接提交给线程池，让它自己去执行
        /**
         * int corePoolSize,    // 核心线程数，线程池创建好就准备就绪的线程数量，就等待接收异步任务去执行。
         * int maximumPoolSize, // 最大线程数量，控制资源并发
         * long keepAliveTime,  // 存活时间，如果当前的线程数量大于核心线程数量（corePoolSize），释放空闲线程。只要空闲时间大于指定的存活时间（corePoolSize）。
         * TimeUnit unit,       // 时间单位
         * BlockingQueue<Runnable> workQueue,   // 阻塞队列。如果任务有很多，就会将目前的任务列表放在队列里面，只要有线程空闲，就会去队列里面去除新的任务执行。
         * ThreadFactory threadFactory, // 线程的创建工厂
         * RejectedExecutionHandler handler // 如果队列满了，按照我们指定的决绝策略拒绝执行任务。
         *
         * 运行流程：
         * 1、线程池创建，准备好core数量的核心线程，准备接收任务。
         * 2、新的任务进来，用core准备好的空闲线程执行。
         *     （1）、core满了，就会将再进来的任务放入阻塞队列中。空闲的core就会自己去阻塞队队列获取任务执行。
         *     （2）、阻塞队列满了，就直接开新线程执行，最大只能开到max指定的数量。
         *     （3）、max都实行好了。max-core数量空闲的线程会在keepAliveTime指定的时间后自动销毁，最终保持到core大小。
         *     （4）、如果线程数开到了max的数量，还有新任务进来，就会使用reject指定的拒绝策略进行处理。
         * 3、所有的线程都是由指定的factory创建的。
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 200, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        System.out.println("main thread ended...");
    }

    public static class Thread01 extends Thread{
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Runnable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
            return i;
        }
    }
}


