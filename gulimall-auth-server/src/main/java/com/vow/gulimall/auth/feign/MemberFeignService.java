package com.vow.gulimall.auth.feign;

import com.vow.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.vow.gulimall.auth.vo.SocialUserVo;
import com.vow.gulimall.auth.vo.UserLoginVo;
import com.vow.gulimall.auth.vo.UserRegistVo;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo userRegistVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo userLoginVo);

    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUserVo socialUserVo) throws Exception;
}
