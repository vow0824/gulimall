package com.vow.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.vow.common.exception.NoStockException;
import com.vow.common.to.mq.OrderTo;
import com.vow.common.to.mq.StockDetailTo;
import com.vow.common.to.mq.StockLockedTo;
import com.vow.common.utils.R;
import com.vow.gulimall.ware.Feign.OrderFeignService;
import com.vow.gulimall.ware.Feign.ProductFeignService;
import com.vow.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.vow.gulimall.ware.entity.WareOrderTaskEntity;
import com.vow.gulimall.ware.service.WareOrderTaskDetailService;
import com.vow.gulimall.ware.service.WareOrderTaskService;
import com.vow.gulimall.ware.vo.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.ware.dao.WareSkuDao;
import com.vow.gulimall.ware.entity.WareSkuEntity;
import com.vow.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录新增
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStock(skuNum);
            skuEntity.setStockLocked(0);
            // 远程查询skuname,如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 第二种方法让事务不回滚
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }

            this.baseMapper.insert(skuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询当前sku的总库存量
            Long count = this.baseMapper.getSkuStock(skuId);

            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return skuHasStockVos;
    }

    /**
     * 为某个订单锁定库存
     * rollbackFor = NoStockException.class
     * 默认只要是运行时异常都会回滚
     * <p>
     * 库存解锁的场景
     * 1）、下订单成功，订单过期没有支付被系统自动取消、被用户手动取消，都要解锁库存
     * 2）、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的库存就要自动解锁
     *
     * @param wareSkuLockVo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {
        /**
         * 保存库存工作单
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);

        // 1、按照下单的收货地址，找到一个就近仓库。
        // 1、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> orderItemVos = wareSkuLockVo.getLocks();
        List<SkuWareHasStockVo> collect = orderItemVos.stream().map(item -> {
            SkuWareHasStockVo skuWareHasStockVo = new SkuWareHasStockVo();
            Long skuId = item.getSkuId();
            skuWareHasStockVo.setSkuId(skuId);
            skuWareHasStockVo.setNum(item.getCount());
            // 查询这个商品在哪里有库存
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            skuWareHasStockVo.setWareId(wareIds);
            return skuWareHasStockVo;
        }).collect(Collectors.toList());

        // 2、循环遍历仓库锁定商品库存
        for (SkuWareHasStockVo skuWareHasStockVo : collect) {
            Boolean skuStocked = false;
            Long skuId = skuWareHasStockVo.getSkuId();
            List<Long> wareIds = skuWareHasStockVo.getWareId();
            Integer num = skuWareHasStockVo.getNum();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            /**
             * 1、如果每一件商品都锁定成功，将当前商品锁定了几件的工作单记录发送给MQ
             * 2、如果锁定失败，前面保存的工作单信息都回滚了，发送出去的消息，即使要解锁记录，由于去数据库查不到id，所以就不用解锁
             */
            for (Long wareId : wareIds) {
                // 成功就返回1，否则就是0
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, num);
                if (count == 1) {
                    skuStocked = true;
                    //TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", skuWareHasStockVo.getNum(), wareOrderTaskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                    // 1、只发ID不行，防止回滚之后找不到数据
                    stockLockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                } else {
                    // 当前仓库锁失败，重试下一个仓库
                }
            }
            if (!skuStocked) {
                // 当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        // 3、全部锁定成功
        return true;
    }

    /**
     * 1、库存自动解锁
     * 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的库存就要自动解锁
     * 2、订单失败
     *  锁库存失败
     *
     *  只要库存的消息失败，一定要告诉服务器解锁失败。
     * @param stockLockedTo
     */
    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {
        StockDetailTo detail = stockLockedTo.getDetail();
        Long detailId = detail.getId();
        // 解锁
        /**
         * 1、查询数据库关于这个订单锁定的库存信息
         * 有：证明库存锁定成功，要不要解锁还要看订单情况
         *      1）、没有这个订单，必须解锁
         *      2）、有这个订单，不是解锁库存，要看订单状态
         *          1、订单已取消：解锁库存
         *          2、订单没取消：不能解锁
         * 没有：库存锁定失败了，库存回滚了，这种情况无需解锁
         */
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = wareOrderTaskDetailService.getById(detailId);
        if (wareOrderTaskDetailEntity != null) {
            // 解锁
            Long lockedId = stockLockedTo.getId();
            WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getById(lockedId);
            String orderSn = wareOrderTaskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                // 订单数据返回成功
                OrderVo orderVo = r.getData(new TypeReference<OrderVo>() {
                });
                if (orderVo == null || orderVo.getStatus() == 4) {
                    // 订单不存在或者订单已经被取消了，解锁库存
                    if (wareOrderTaskDetailEntity.getLockStatus() == 1) {
                        // 当前库存工作单详情是已锁定状态，才解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                // 消息拒绝以后重新放入队列里面，让别人继续消费
                throw new RuntimeException("远程服务调用失败");
            }
        }

    }

    /**
     * 防止订单服务卡顿，导致订单消息发送延迟，库存消息优先到期，查询订单状态为创建状态，未执行任何操作，并且消费了库存解锁的消息。
     * 导致卡顿的订单永远不能解锁库存
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        // 查询当前订单最新的库存解锁状态，防止重复解锁
        WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderTo.getOrderSn());
        // 按照工作单找到所有没有解锁的库存，进行解锁
        List<WareOrderTaskDetailEntity> taskDetailEntities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", wareOrderTaskEntity.getId()).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity taskDetailEntity : taskDetailEntities) {
            unLockStock(taskDetailEntity.getSkuId(), taskDetailEntity.getWareId(), taskDetailEntity.getSkuNum(), taskDetailEntity.getId());
        }

    }


    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 库存解锁
        this.baseMapper.unlockStock(skuId, wareId, num);
        // 更新库存工作单的状态
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
        wareOrderTaskDetailEntity.setId(taskDetailId);
        wareOrderTaskDetailEntity.setLockStatus(2); // 变为已解锁
        wareOrderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }


}