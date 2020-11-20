package com.vow.gulimall.member.exception;

public class MobileExistException extends RuntimeException {
    public MobileExistException() {
        super("手机号已存在");
    }
}
