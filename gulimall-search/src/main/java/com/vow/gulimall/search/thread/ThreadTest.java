package com.vow.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main thread starting...");
        /*CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
        }, executorService);*/
        /**
         * 方法成功完成后的感知
         */
        /*CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 0;
            System.out.println("运行结果：" + i);
            return i;
        }, executorService).whenComplete((result, exception) -> {
            //虽然能得到返回信息，但是没法修改返回数据。
            System.out.println("异步任务成功完成了，结果是：" + result + ",异常是：" + exception);
        }).exceptionally(throwable -> {
            // 可以感知异常，同时返回默认值。
            return 10;
        });*/

        /**
         * 方法执行完成后的处理
         */
        /*CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 4;
            System.out.println("运行结果：" + i);
            return i;
        }, executorService).handle((result, exception) -> {
            if (result != null) {
                return result * 2;
            }
            if (exception != null) {
                return 0;
            }
            return 0;
        });*/
        /**
         * 线程串行化
         *  1、thenRunAsync：不能获取到上一步的执行结果，无返回值。
         *      .thenRunAsync(() -> {
         *             System.out.println("任务二启动了。。。");
         *         }, executorService);
         *  2、thenAcceptAsync能接收上一步的执行结果，但是无返回值。
         *      .thenAcceptAsync(res -> {
         *             System.out.println("线程3执行了。。。" + res);
         *         }, executorService);
         *  3、thenApplyAsync：能接收上一步的执行结果，而且有返回值。
         *      .thenApplyAsync(res -> {
         *             System.out.println("任务二启动了" + res);
         *             return "hello" + res;
         *         }, executorService)
         */
        /*CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 4;
            System.out.println("运行结果：" + i);
            return i;
        }, executorService).thenApplyAsync(res -> {
            System.out.println("任务二启动了" + res);
            return "hello" + res;
        }, executorService);*/

        /**
         * 两个都完成
         */
        /*CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1线程：" + Thread.currentThread().getId());
            int i = 10 / 4;
            System.out.println("任务1运行结果：" + i);
            return i;
        }, executorService);

        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("任务2线程：" + Thread.currentThread().getId());
            System.out.println("任务2运行结束");
            return "hello";
        }, executorService);*/

        /*future01.runAfterBothAsync(future02, () -> {
            System.out.println("任务3线程开始。。");
        }, executorService);*/

        /*future01.thenAcceptBothAsync(future02, (res1, res2) -> {
            System.out.println("任务3线程开始。。。。之前任务1和任务2的结果分别是：" + res1 + "," + res2 + "。");
        }, executorService);*/

        /*CompletableFuture<String> stringCompletableFuture = future01.thenCombineAsync(future02, (res1, res2) -> {
            System.out.println("任务3线程开始。。。");
            return res1 + ":" + res2 + " world!";
        }, executorService);*/

        /**
         * 两个任务，只要有一个完成，我们就执行任务3
         * runAfterEitherAsync：不感知结果，自己也无返回值
         * acceptEitherAsync：感知结果，无返回值
         * applyToEitherAsync：感知结果，并且有返回值
         */
        /*future01.runAfterEitherAsync(future02, () -> {
            System.out.println("任务3线程开始执行。。。");
        }, executorService);*/

        /*future01.acceptEitherAsync(future02, (res) -> {
            System.out.println("任务3线程开始执行。。。" + res);
        }, executorService);*/

        /*CompletableFuture<String> stringCompletableFuture = future01.applyToEitherAsync(future01, (res) -> {
            System.out.println("任务3线程开始执行。。。");
            return res.toString() + "hello";
        }, executorService);*/

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        }, executorService);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性");
            return "黑色+256G";
        }, executorService);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("查询商品的介绍");
            return "hello.jpg";
        }, executorService);

        /*CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureDesc, futureAttr);
        allOf.get();*/    // 等待所有结果执行完成

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureDesc, futureAttr);
        anyOf.get();    //只要有一个成功


        System.out.println("main thread ended..." + anyOf.get());

    }

    public void thread(String[] args) throws ExecutionException, InterruptedException {
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


