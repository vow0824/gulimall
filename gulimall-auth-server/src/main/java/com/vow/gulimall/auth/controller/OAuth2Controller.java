package com.vow.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vow.common.constant.AuthServerConstant;
import com.vow.common.utils.HttpUtils;
import com.vow.common.utils.R;
import com.vow.gulimall.auth.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.vow.common.vo.MemberResponseVo;
import com.vow.gulimall.auth.vo.SocialUserVo;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Controller
@Slf4j
public class OAuth2Controller {

    @Value("${spring.cloud.weibo.app-key}")
    private String clientId;
    @Value("${spring.cloud.weibo.app-secret}")
    private String clientSecret;

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        System.out.println(clientId + "：" + clientSecret);
        Map<String, String> map = new HashMap<>();
        map.put("client_id", clientId);
        map.put("client_secret", clientSecret);
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);

        // 1、根据code换取accessToken
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<String, String>(), null, map);
        // 2、处理
        if (response.getStatusLine().getStatusCode() == 200) {
            // 获取到了accessToken
            String jsonString = EntityUtils.toString(response.getEntity());
            SocialUserVo socialUserVo = JSON.parseObject(jsonString, SocialUserVo.class);

            // 知道了是哪个社交用户
            // 1）、当前用户如果是第一次进入网站，自动注册进来（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定会员账号）
            // 登录或者注册这个社交用户
            R r = memberFeignService.oauthLogin(socialUserVo);
            if (r.getCode() == 0) {
                MemberResponseVo data = r.getData("data", new TypeReference<MemberResponseVo>() {
                });
                log.info("登录成功，用户：{}", data.toString());
                // TODO session作用域为当前域；解决子域共享问题
                // TODO 使用JSON的序列化方式来序列花对象数据到redis中
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);
                // 2、登录成功就跳回首页
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
