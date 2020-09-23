package com.vow.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vow.gulimall.product.entity.BrandEntity;
import com.vow.gulimall.product.service.BrandService;
import com.vow.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

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

//    /*@Autowired
//    OSSClient ossClient;
//
//    @Test
//    public void testUpload() throws FileNotFoundException {
//        *//*//*/ Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
//        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "LTAI4GE56bWqusoMfgdWqoZh";
//        String accessKeySecret = "Fxm8musi6w2nOoUfn6crArDIcbDscs";*//*
//
//        // 创建OSSClient实例。
//        //OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        // 上传文件流。
//        InputStream inputStream = new FileInputStream("C:\\Users\\vow\\Pictures\\Saved Pictures\\下载.jpg");
//        ossClient.putObject("gulimall-vow", "test1.jpg", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
//    }*/

}
