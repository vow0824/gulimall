package com.vow.gulimall.order.interceptor;

import com.vow.common.constant.AuthServerConstant;
import com.vow.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**
         * 库存服务远程调（feign）用订单服务的订单状态接口时，单独配置，否则会被下面代码拦截，要求登录
         */
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", request.getRequestURI());
        boolean match1 = antPathMatcher.match("/payed/**", request.getRequestURI());
        if (match || match1) {
            return true;
        }

        MemberResponseVo memberResponseVo = (MemberResponseVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (memberResponseVo != null) {
            loginUser.set(memberResponseVo);
            return true;
        } else {
            // 没登录，就去登录
            request.getSession().setAttribute("msg", "请先登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
