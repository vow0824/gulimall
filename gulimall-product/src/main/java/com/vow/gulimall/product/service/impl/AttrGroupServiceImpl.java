package com.vow.gulimall.product.service.impl;

import com.vow.gulimall.product.entity.AttrEntity;
import com.vow.gulimall.product.service.AttrService;
import com.vow.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.vow.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.product.dao.AttrGroupDao;
import com.vow.gulimall.product.entity.AttrGroupEntity;
import com.vow.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), queryWrapper);
            return new PageUtils(page);
        } else {
            queryWrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), queryWrapper);
            return new PageUtils(page);
        }
    }

    /**
     * 根据分类id查出所有的分组以及这些组里的属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {

        //1、查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //2、查询所有属性
        List<AttrGroupWithAttrsVo> vos = attrGroupEntities.stream().map(attrGroupEntity -> {
            AttrGroupWithAttrsVo vo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(attrGroupEntity, vo);
            List<AttrEntity> relationAttrs = attrService.getRelationAttr(vo.getAttrGroupId());
            vo.setAttrs(relationAttrs);
            return vo;
        }).collect(Collectors.toList());

        return vos;
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {

        // 1、查出当前spu对应的所有分组信息以及当前分组下的所有对应的值
        List<SpuItemAttrGroupVo> vos = this.baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
        return vos;
    }

}