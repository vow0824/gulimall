package com.vow.common.exception;

/**
 * @ControllerAdvice + @ExceptionHandler
 * 系统错误码
 * 错误码和错误信息定义类
 * 1.错误码定义规则为5为数字
 * 2.前两位表示业务场景，最后三位表示错误码。例如：10001。10：通用，001：系统未知异常。
 * 3.维护错误码后需要维护错误描述，将他们定义为枚举形式
 *     10：通用
 *         001：参数格式校验
 *         002：验证码获取频率太高，请稍后再试。
 *     11：商品
 *     12：订单
 *     13：购物车
 *     14：物流
 *     15: 用户
 *     ...
 */
public enum BizCodeEnum {
    UNKWON_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码获取频率太高，请稍后再试。"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USERNAME_EXISTED_EXCEPTION(150001, "用户名已存在"),
    MOBILE_EXISTED_EXCEPTION(150002, "手机号已存在"),
    LOGINACCT_PASSWORD_INVALID_EXCEPTION(150003, "账号或密码错误");

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
