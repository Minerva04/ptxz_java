package com.ztq.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.entity.GoodsOrder;
import com.ztq.service.GoodsOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

public class KeyExpiredListener extends KeyExpirationEventMessageListener {

    @Autowired
    GoodsOrderService goodsOrderService;
    @Autowired
    RedisTemplate redisTemplate;
    public KeyExpiredListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        //过期监听 将id分割 因为后续还会用到缓存 所以从数据库中需要查出 原来订单 存入缓存 判断状态 如果未收货 自动收货
        if (message.toString().startsWith("goodsOrder_")){
            String[] s = message.toString().split("_");
            Integer id = Integer.parseInt(s[1]);
            LambdaQueryWrapper<GoodsOrder>queryWrapper=new LambdaQueryWrapper<>();
            queryWrapper.eq(GoodsOrder::getId,id);
            GoodsOrder order = goodsOrderService.getOne(queryWrapper);
            redisTemplate.opsForValue().set(message.toString(),order);
            if (order.getStatus()==0){
                goodsOrderService.autoConfirm(id);
            }

        }
    }

}
