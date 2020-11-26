package com.vow.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vow.common.utils.HttpUtils;
import com.vow.gulimall.member.dao.MemberLevelDao;
import com.vow.gulimall.member.entity.MemberLevelEntity;
import com.vow.gulimall.member.exception.MobileExistException;
import com.vow.gulimall.member.exception.UserNameExistException;
import com.vow.gulimall.member.vo.MemberLoginVo;
import com.vow.gulimall.member.vo.MemberRegistVo;
import com.vow.gulimall.member.vo.SocialUserVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.member.dao.MemberDao;
import com.vow.gulimall.member.entity.MemberEntity;
import com.vow.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo memberRegistVo) {
        MemberEntity memberEntity = new MemberEntity();
        // 1、设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        // 检查用户名和手机号是否唯一，为了让controller感知异常，使用异常机制
        checkMobileUnique(memberEntity.getMobile());
        checkUserNameUnique(memberEntity.getUsername());
        memberEntity.setMobile(memberRegistVo.getPhone());
        memberEntity.setUsername(memberRegistVo.getUserName());
        memberEntity.setNickname(memberRegistVo.getUserName());

        // 密码进行加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(memberRegistVo.getPassword());
        memberEntity.setPassword(encodedPassword);

        // 其他的默认信息

        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkUserNameUnique(String username) throws UserNameExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public void checkMobileUnique(String mobile) throws MobileExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", mobile));
        if (count > 0) {
            throw new MobileExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        String loginacct = memberLoginVo.getLoginacct();
        String password = memberLoginVo.getPassword();

        // 1、去数据库查询
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (memberEntity == null) {
            // 登录失败
            return null;
        } else {
            // 获取数据库的密码
            String passwordDb = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            // 密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (matches) {
                return memberEntity;
            } else {
                return null;
            }
        }
    }

    /**
     * 注册和登录合并逻辑
     * @param socialUserVo
     * @return
     */
    @Override
    public MemberEntity login(SocialUserVo socialUserVo) throws Exception {
        // 1、判断当前社交用户是否已经登录过系统
        String uid = socialUserVo.getUid();
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            // 这个用户已经注册了
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUserVo.getAccess_token());
            update.setExpiresIn(socialUserVo.getExpires_in());
            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUserVo.getAccess_token());
            memberEntity.setExpiresIn(socialUserVo.getExpires_in());

            return memberEntity;
        } else {
            // 该社交用户尚未登录过系统，注册新账号
            MemberEntity regist = new MemberEntity();
            try {
                // 查询当前社交用户的社交账号信息（昵称，性别）
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUserVo.getAccess_token());
                query.put("uid", socialUserVo.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String jsonString = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(jsonString);
                    // 昵称
                    String name = (String) jsonObject.get("name");
                    // 性别
                    String gender = (String) jsonObject.get("gender");
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);
                }
            } catch (Exception e) {}

            regist.setSocialUid(socialUserVo.getUid());
            regist.setAccessToken(socialUserVo.getAccess_token());
            regist.setExpiresIn(socialUserVo.getExpires_in());
            this.baseMapper.insert(regist);
            return regist;
        }

    }

}