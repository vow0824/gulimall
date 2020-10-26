package com.vow.gulimall.product.web;

import com.vow.gulimall.product.entity.CategoryEntity;
import com.vow.gulimall.product.service.CategoryService;
import com.vow.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){
        // TODO 1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();

        model.addAttribute("categories", categoryEntities);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        // 1、获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redissonClient.getLock("my-lock");

        // 2、加锁
        // 1）、锁的自动续期，如果业务时间超长，运行期间自动给所续上30s。不用担心业务时间长，锁自动过期被删掉。
        // 2）、加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s以后自动删除。
        // lock.lock();    // 阻塞式等待，默认加的锁都是30s时间。
        lock.lock(10, TimeUnit.SECONDS);    // 10秒钟自动解锁，自动解锁的时间一定要大于业务执行的时间
        // 问题：lock.lock(10, TimeUnit.SECONDS); 在锁时间到了以后，不会自动续期。
        // 1、如果我们设置了锁自动解锁时间，就发送给redis执行脚本，进行占锁，默认事件就是我们设置的事件
        // 2、如果我没为指定自动解锁时间，就使用 30 * 1000ms（LockWatchDog【看门狗的默认时间】）。
        try {
            System.out.println("加锁成功，执行业务。。。。" + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (InterruptedException e) {

        } finally {
            // 3、解锁
            System.out.println("释放锁。。。。" + Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    // 保证一定能督导最新数据，修改期间，写锁是一个排他锁（互斥锁）。读锁是一个共享锁
    // 写锁没释放，读锁必须等待
    // 读 + 读：相当于无锁，并发读，只会在redis中记录好所有当前的读锁。他们都会同时加锁成功
    // 写 + 读：等待写锁释放
    // 写 + 写：阻塞方式
    // 读 + 写：有读锁，写也需要等待
    // 只要有写的存在，都必须等待。
    @ResponseBody
    @GetMapping("/write")
    public String writeValue() {

        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = lock.writeLock();

        String s = "";
        try {
            // 1、写数据加写锁，读数据加读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            stringRedisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }

        return s;
    }

    @ResponseBody
    @GetMapping("/read")
    public String readValue() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        RLock rLock = lock.readLock();
        rLock.lock();

        String s = "";
        try {
            s = stringRedisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }

        return s;
    }

    /**
     * 车库停车，3车位
     */
    @GetMapping("park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.acquire(); // 获取车位（获取信号量）
        return "ok";
    }

    @GetMapping("go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release(); // 释放一个车位（释放信号量）
        return "ok";
    }

    /**
     * 放假，锁门
     * 5个班全部走完，可以锁大门
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();   // 等待闭锁都完成
        return "放假了。。。。";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") String id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();   //计数减一

        return id + "班的人都走了。。。。";
    }
}
