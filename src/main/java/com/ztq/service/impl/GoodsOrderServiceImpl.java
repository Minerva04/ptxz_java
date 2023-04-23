package com.ztq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.Goods;
import com.ztq.entity.GoodsOrder;
import com.ztq.entity.User;
import com.ztq.mapper.GoodsOrderMapper;
import com.ztq.service.GoodsOrderService;
import com.ztq.service.GoodsService;
import com.ztq.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GoodsOrderServiceImpl extends ServiceImpl<GoodsOrderMapper, GoodsOrder> implements GoodsOrderService {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserService userService;
    @Autowired
    GoodsService goodsService;

    @Override
    public void autoConfirm(Integer id) {
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + order.getGoodsId());
        LambdaQueryWrapper<User>queryWrapper1=new LambdaQueryWrapper<>();
        queryWrapper1.eq(User::getId,order.getCustomId());

        User cus = userService.getOne(queryWrapper1);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, order.getBusinessId());
        User bis = userService.getOne(queryWrapper);

        Float bisMoney = bis.getMoney();
        Float price = order.getPrice();
        Float freezeMoney = cus.getFreezeMoney();
        BigDecimal freeze = new BigDecimal(Float.toString(freezeMoney));
        BigDecimal bisMoney2 = new BigDecimal(Float.toString(bisMoney));
        BigDecimal price2 = new BigDecimal(Float.toString(price));
        Float resultMoney = bisMoney2.add(price2).floatValue();
        Float resultFreezeMoney = freeze.subtract(price2).floatValue();


        bis.setMoney(resultMoney);
        goods.setStatus(3);
        order.setStatus(2);
        cus.setFreezeMoney(resultFreezeMoney);

        this.updateById(order);
        goodsService.updateById(goods);
        userService.updateById(cus);
        userService.updateById(bis);
        redisTemplate.opsForValue().set("goods_" + goods.getId(), goods);
        redisTemplate.opsForValue().set("goodsOrder_" + order.getId(), order);
        if (redisTemplate.opsForValue().get("user_" + cus.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + cus.getToken(), cus);
        }
        //更新卖家的redis缓存 需先判断卖家是否登录 如果登录才需要更新缓存
        if (redisTemplate.opsForValue().get("user_" + bis.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + bis.getToken(), bis);
        }
    }
}
