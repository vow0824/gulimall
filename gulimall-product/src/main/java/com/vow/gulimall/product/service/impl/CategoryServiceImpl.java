package com.vow.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vow.gulimall.product.service.CategoryBrandRelationService;
import com.vow.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.product.dao.CategoryDao;
import com.vow.gulimall.product.entity.CategoryEntity;
import com.vow.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    // private Map<String, Object> cache = new HashMap<>();

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        //2.组装成父子结构
        //2.1找到所有的以及分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((item) -> {
            return item.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildren(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //1、检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {

        List<Long> path = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, path);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联数据
     * @CacheEvict：失效模式
     * 1、同时进行多种缓存操作 @Caching
     * 2、指定删除某个分区下的所有缓存 @CacheEvict(value = "category", allEntries = true)
     * 3、存储同一类型的数据，都可以指定成同一个分区
     * @param category
     */
    // @CacheEvict(value = "category", key = "'getLevel1Categories'")
    /*@Caching(evict = {
            @CacheEvict(value = "category", key = "'getLevel1Categories'"),
            @CacheEvict(value = "category", key = "'getCatalogJson'")
    })*/
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        // 同时修改缓存中的数据
        // 删除缓存中的数据
    }

    /**
     * 1、每一个需要缓存的数据我们都来指定要放到哪个名字的缓存。【缓存的分区（按照业务类型分区）】
     * 2、@Cacheable({"category"})
     *  1）、当前方法执行的结果需要缓存，如果缓存中有，方法不用调用
     *  2）、如果缓存中没有，会调用方法，最后将方法的结果放入缓存
     * 3、默认行为
     *  1）、如果缓存中有，方法不会调用
     *  2）、key默认自动生成：缓存的名字::simpleKey[] （自主生成的key值）
     *  3）、缓存生成的value的值默认使用jdk序列化机制将序列化后的数据存到redis
     *  4）、默认时间是 -1，
     *
     * 自定义操作
     *  1）、指定生成缓存使用的key：key属性，接收一个SpEl
     *  2）、自定义缓存数据的存货时间：在配置文件中修改ttl
     *  3）、将数据保存为json格式
     * 4、spring-cache的不足
     *  1）、谈模式
     *      缓存穿透：查询一个null数据。解决方案：缓存空数据
     *      缓存击穿：大量并发进来同时查询一个正好过期的数据。解决方案：加锁？ 默认无加锁.sync = true（解决缓存击穿问题）
     *      缓存雪崩：大量的key同时过期。解决：加随机事件
     *  2）、写模式（缓存与数据库一致）
     *      1）、读写加锁
     *      2）、引入canal，感知到数据库的更新去更新缓存
     *      3）、读多写多，直接查询数据库
     * 总结：常规数据（读多写少，即时性，一致性要求不高的数据），完全可以使用spring-cache；写模式（只要缓存数据又过期时间就足够了）
     *       特殊数据，特殊设计
     * @return
     */
    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("查询一级分类。。。。");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    @Cacheable(value = {"category"}, key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        List<CategoryEntity> categoryEntityList = baseMapper.selectList(null);
        // 1、查出所有一级分类
        List<CategoryEntity> level1Categories = getParent_cid(categoryEntityList, 0L);
        // 2、封装数据
        Map<String, List<Catelog2Vo>> map = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1、每一个的一级分类，查到这个分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(categoryEntityList, v.getCatId());
            // 2、封装上面的数据
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 3、找二级分类的三级分类
                    List<CategoryEntity> level3Categories = getParent_cid(categoryEntityList, l2.getCatId());
                    if (level3Categories != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categories.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return map;
    }

    // TODO 产生堆外内存溢出：OutOfDirectMemoryError
    // 1、springboot2.0之后默认使用lettuce作为redis操作的客户端，他使用netty进行网络通信。
    // 2、lettuce的bug导致堆外内存溢出。-Xmx100m：netty如果没有指定堆外内存，默认使用-Xmx100m作为堆外内存。
    //    可以通过 -Dio.netty.maxDirectMemory进行设置
    // 解决方案：不能使用-Dio.netty.maxDirectMemory只去调整最大堆外内存。
    // 1、升级lettuce客户端。
    // 2、切换使用jedis
    // redisTemplate:
    // lettuce,jedis操作redis底层客户端。spring再次封装lettuce,jedis
    //@Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        // 给缓存中放json字符串，拿出的json字符串还要逆转为能用的对象类型，（序列化与反序列化）

        /**
         * 1、空结果缓存：解决缓存穿透问题
         * 2、设置过期时间（加随机值）：解决缓存雪崩问题
         * 3、加锁：解决缓存击穿问题
         */

        // 1、加入缓存逻辑，缓存中存的数据是JSON字符串
        // JSON跨语言、跨平台兼容。
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            // 2、缓存中没有
            System.out.println("缓存不命中，查询数据库。。。。");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedissonLock();

            return catalogJsonFromDB;
        }
        System.out.println("缓存命中，直接返回。。。。");
        // 转为我们指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });

        return result;
    }

    /**
     * 缓存里面的数据如何与数据库中的数据保持一致
     * 缓存数据一致性
     * 1）、双写模式
     * 2）、失效模式
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissonLock() {
        // 1、锁的名字。锁的粒度，越细越快
        // 锁的粒度，具体缓存的是某个数据，11号商品：product-11-lock，product-12-lock...
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }
        return dataFromDB;

    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        // 1、占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功。。。。");
            // 加锁成功，执行业务
            // 2、设置过期时间，必须和加锁是原子操作，否则可能会由于服务器异常导致死锁
            //stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            Map<String, List<Catelog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                // 获取值对比，对比成功删除，这两步也必须是原子操作，否则可能会删除别的线程的锁   lua脚本解锁
                /*String lockValue = stringRedisTemplate.opsForValue().get("lock");
                if (uuid.equals(lockValue)) {
                    // 释放锁
                    stringRedisTemplate.delete("lock");
                }*/
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 删除锁
                stringRedisTemplate.execute(new DefaultRedisScript<Integer>(script, Integer.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDB;
        } else {
            System.out.println("获取分布式锁失败，等待重试。。。。");
            // 加锁失败，重试 sync
            // 休眠100ms重试
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            return getCatalogJsonFromDBWithRedisLock(); // 自旋的方式
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        // 得到锁以后，我们应该再去缓存中确定一次，如果没有才需要继续查询
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            //缓存不为空，直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }

        System.out.println("查询了数据库。。。。");

        List<CategoryEntity> categoryEntityList = baseMapper.selectList(null);

        // 1、查出所有一级分类
        List<CategoryEntity> level1Categories = getParent_cid(categoryEntityList, 0L);

        // 2、封装数据
        Map<String, List<Catelog2Vo>> map = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1、每一个的一级分类，查到这个分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(categoryEntityList, v.getCatId());
            // 2、封装上面的数据
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 3、找二级分类的三级分类
                    List<CategoryEntity> level3Categories = getParent_cid(categoryEntityList, l2.getCatId());
                    if (level3Categories != null) {
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categories.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        // 3、将查到的数据放到缓存中
        String catalog = JSON.toJSONString(map);
        stringRedisTemplate.opsForValue().set("catalogJSON", catalog, 1, TimeUnit.DAYS);

        return map;
    }

    /**
     * 从数据库查询并封装数据
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {
        //本地缓存
        /*Map<String, List<Catelog2Vo>> catalogJson = (Map<String, List<Catelog2Vo>>) cache.get("catalogJson");
        if (catalogJson == null) {
            //调用业务
            //放入缓存
            cache.put("catalogJson", map);
        }
        return catalogJson;*/

        // 只要是同一把锁，就能锁住这个锁的所有线程
        // 1、synchronized (this)：springboot所有的组件在容器中都是单例的。
        // TODO 本地锁：synchronized,JUC(Lock)，在分布式情况下，想要锁住所有，必须使用分布式锁

        synchronized (this) {
            // 得到锁以后，我们应该再去缓存中确定一次，如果没有才需要继续查询
            return getDataFromDB();
        }


    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> categoryEntityList, Long parentCid) {
        List<CategoryEntity> collect = categoryEntityList.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
        return collect;
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        //1、收集当前节点id
        path.add(catelogId);
        CategoryEntity id = this.getById(catelogId);
        if (id.getParentCid() != 0) {
            findParentPath(id.getParentCid(), path);
        }
        return path;
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter((item) -> {
            return item.getParentCid() == root.getCatId();
        }).map((categoryEntity) -> {
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}