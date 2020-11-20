package com.vow.gulimall.thirdparty.controller;

import com.vow.common.utils.R;
import com.vow.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    SmsComponent smsComponent;

    /**
     * 提供给别的服务进行调用的
     * @param mobile
     * @param code
     * @return
     */
    @GetMapping("/sendCode")
    public R sendSms(@RequestParam("mobile") String mobile, @RequestParam("code") String code) {
        smsComponent.sendSms(mobile, code);
        return R.ok();
    }
}
