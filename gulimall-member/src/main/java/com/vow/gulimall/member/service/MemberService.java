package com.vow.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.member.entity.MemberEntity;
import com.vow.gulimall.member.exception.MobileExistException;
import com.vow.gulimall.member.exception.UserNameExistException;
import com.vow.gulimall.member.vo.MemberLoginVo;
import com.vow.gulimall.member.vo.MemberRegistVo;

import java.util.Map;

/**
 * 会员
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 13:51:51
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo memberRegistVo);

    void checkUserNameUnique(String username) throws UserNameExistException;

    void checkMobileUnique(String mobile) throws MobileExistException;

    MemberEntity login(MemberLoginVo memberLoginVo);
}

