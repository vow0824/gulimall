package com.vow.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.vow.common.constant.AuthServerConstant;
import com.vow.common.exception.BizCodeEnum;
import com.vow.common.utils.R;
import com.vow.common.vo.MemberResponseVo;
import com.vow.gulimall.auth.feign.MemberFeignService;
import com.vow.gulimall.auth.feign.ThirdPartyFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.vow.gulimall.auth.vo.UserLoginVo;
import com.vow.gulimall.auth.vo.UserRegistVo;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("mobile") String mobile) {
        // 1、接口防刷

        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile);
        if (!StringUtils.isEmpty(redisCode)) {
            long saveTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - saveTime < 600000) {
                // 60秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5);
        String saveCode = code + "_" + System.currentTimeMillis();
        // 2、验证码校验redis,存key-mobile，value-code  sms:code:12312312312 -> code
        // redis缓存验证码，防止同一个手机号在60s内再次发送验证码
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + mobile, saveCode, 10, TimeUnit.MINUTES);

        thirdPartyFeignService.sendSms(mobile, code);
        return R.ok();
    }

    /**
     * // TODO 重定向携带数据，利用session原理，将数据放在session中。只要跳到下一个页面，取出这个数据以后，session里面的数据就会删掉
     * // TODO 1、分布式下的session问题
     * RedirectAttributes redirectAttributes 模拟重定向携带数据
     * @param userRegistVo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo userRegistVo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);

            // 校验出错返回注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        // 真正注册，调用远程服务进行注册
        // 1、校验验证码
        String code = userRegistVo.getCode();
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            if (code.equals(redisCode.split("_")[0])) { // 验证码通过
                // 删除验证码。令牌机制
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
                R r = memberFeignService.regist(userRegistVo);
                if (r.getCode() == 0) {
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                //model.addAttribute("errors", errors);
                redirectAttributes.addFlashAttribute("errors", errors);
                // 校验出错返回注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            //model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            // 校验出错返回注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes, HttpSession session) {
        // 远程登录
        R r = memberFeignService.login(userLoginVo);
        if (r.getCode() == 0) {
            // 成功
            MemberResponseVo data = r.getData("data", new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        Object loginUser = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (loginUser == null) {
            // 没登录
            return "login";
        } else {
            return "redirect:http://gulimall.com";
        }
    }
}
