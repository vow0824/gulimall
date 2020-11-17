package com.vow.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vow.gulimall.product.dao.AttrGroupDao;
import com.vow.gulimall.product.dao.SkuSaleAttrValueDao;
import com.vow.gulimall.product.entity.BrandEntity;
import com.vow.gulimall.product.service.BrandService;
import com.vow.gulimall.product.service.CategoryService;
import com.vow.gulimall.product.vo.SkuItemSaleAttrVo;
import com.vow.gulimall.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void testFindPath() {
        log.info("完整路径{}", Arrays.asList(categoryService.findCatelogPath(225L)));
    }

    @Test
    void contextLoads() {
        /*BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("huawei");
        brandService.save(brandEntity);
        System.out.println("saving success......");*/

        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", "1L"));
        list.forEach((item) -> {
            System.out.println(item);
        });
    }

    @Test
    public void testRedis() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 保存
        ops.set("hello", "world_" + UUID.randomUUID().toString());
        // 查询
        String hello = ops.get("hello");
        System.out.println(hello);
    }

    @Test
    public void testRedisson() {
        System.out.println(redissonClient);
    }

    @Test
    public void test() {
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(1L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public  void testAttr() {
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(1L);
        System.out.println(saleAttrsBySpuId);
    }

}

