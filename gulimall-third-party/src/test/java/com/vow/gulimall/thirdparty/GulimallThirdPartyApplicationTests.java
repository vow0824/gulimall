package com.vow.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.vow.gulimall.thirdparty.component.SmsComponent;
import com.vow.gulimall.thirdparty.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

    /*@Value("#{spring.cloud.alicloud.sms.appcode}")
    public String appcode;*/

    @Test
    public void testUpload() throws FileNotFoundException {
        /* Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
        */
        // 创建OSSClient实例。
        //OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传文件流。
        InputStream inputStream = new FileInputStream("C:\\Users\\vow\\Pictures\\Saved Pictures\\下载.jpg");
        ossClient.putObject("gulimall-vow", "test2.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
    }

    @Test
    public void sendSms() {
        System.out.println(smsComponent.getAppcode() + smsComponent.getHost() + smsComponent.getPath());
        smsComponent.sendSms("19805166510", "6789");
    }

}
