package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ztq.entity.*;
import com.ztq.service.*;
import com.ztq.utils.CheckOtherLogin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping("/goodsorder")
@Slf4j
public class GoodsOrderController {
    @Autowired
    GoodsOrderService goodsOrderService;

    @Autowired
    CheckOtherLogin checkOtherLogin;
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    GoodsService goodsService;
    @Autowired
    UserService userService;
    @Autowired
    CustomJudgingService customJudgingService;
    @Autowired
    BusinessJudgingService businessJudgingService;

    @GetMapping
    public Result<Page> getGoodsOrderList(HttpServletRequest request, QueryInfo queryInfo) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        Page<GoodsOrder> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        LambdaQueryWrapper<GoodsOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), GoodsOrder::getName, queryInfo.getName());
        lambdaQueryWrapper.eq(GoodsOrder::getCustomId, user.getId());
        lambdaQueryWrapper.eq(GoodsOrder::getIsCusdelete, 0);
        lambdaQueryWrapper.eq(queryInfo.getStatus() != null, GoodsOrder::getStatus, queryInfo.getStatus());
        goodsOrderService.page(page, lambdaQueryWrapper);
        return Result.success(page);
    }

    @Transactional
    @GetMapping("/confirm/{id}")
    public Result<String> configOrder(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //修改订单和商品状态 将买家的冻结金额扣除 将卖家的余额增加 修改订单和商品缓存
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + order.getGoodsId());
        User cus = (User) redisTemplate.opsForValue().get("user_" + token);
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

        goodsOrderService.updateById(order);
        goodsService.updateById(goods);
        userService.updateById(cus);
        userService.updateById(bis);
        redisTemplate.opsForValue().set("goods_" + goods.getId(), goods);
        redisTemplate.opsForValue().set("goodsOrder_" + order.getId(), order);
        redisTemplate.opsForValue().set("user_" + token, cus);
        //更新卖家的redis缓存 需先判断卖家是否登录 如果登录才需要更新缓存
        if (redisTemplate.opsForValue().get("user_" + bis.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + bis.getToken(), bis);
        }
        return Result.success("收货成功");
    }

    @GetMapping("/judging/{id}")
    public Result<String> judging(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //改变商品和订单状态
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
      /*  if (order.getStatus()!=0){
            return Result.error("当前订单未处于交易状态");
        }*/
        Integer goodsId = order.getGoodsId();
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + goodsId);
        goods.setStatus(2);
        order.setStatus(1);
        goodsService.updateById(goods);
        goodsOrderService.updateById(order);
        redisTemplate.opsForValue().set("goodsOrder_" + id, order);
        redisTemplate.opsForValue().set("goods_" + goodsId, goods);
        return Result.success("申请成功，请提交仲裁资料并等待管理员处理");
    }

    @DeleteMapping("/cusdelete/{id}")
    public Result<String> cusDeleteOrder(HttpServletRequest request, @PathVariable Integer id) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        if (order.getStatus() != 2) {
            return Result.error("错误");
        }
        order.setIsCusdelete(1);
        goodsOrderService.updateById(order);
        redisTemplate.opsForValue().set("goodsOrder_" + id, order);
        return Result.success("删除成功");
    }

    @DeleteMapping("/bisdelete/{id}")
    public Result<String> bisDeleteOrder(HttpServletRequest request, @PathVariable Integer id) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        if (order.getStatus() != 2) {
            return Result.error("错误");
        }
        order.setIsBisdelete(1);
        goodsOrderService.updateById(order);
        redisTemplate.opsForValue().set("goodsOrder_" + id, order);
        return Result.success("删除成功");
    }

    @GetMapping("/bis")
    public Result<Page> getBuiGoodsOrderList(HttpServletRequest request, QueryInfo queryInfo) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        Page<GoodsOrder> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        LambdaQueryWrapper<GoodsOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), GoodsOrder::getName, queryInfo.getName());
        lambdaQueryWrapper.eq(GoodsOrder::getBusinessId, user.getId());
        lambdaQueryWrapper.eq(GoodsOrder::getIsBisdelete, 0);
        lambdaQueryWrapper.eq(queryInfo.getStatus() != null, GoodsOrder::getStatus, queryInfo.getStatus());
        goodsOrderService.page(page, lambdaQueryWrapper);
        return Result.success(page);
    }

    @PutMapping("/return/{id}")
    @Transactional
    public Result<String> returnMoney(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        if (order.getStatus() != 1) {
            return Result.error("错误");
        }
        Integer customId = order.getCustomId();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, customId);
        User cus = userService.getOne(queryWrapper);
        //商品和订单状态设置为已完成 用户冻结的相应资金退回
        Integer goodsId = order.getGoodsId();
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + goodsId);
        goods.setStatus(3);
        order.setStatus(2);
        BigDecimal price = new BigDecimal(Float.toString(order.getPrice()));
        BigDecimal freeze = new BigDecimal(Float.toString(cus.getFreezeMoney()));
        BigDecimal money = new BigDecimal(Float.toString(cus.getMoney()));
        cus.setMoney(money.add(price).floatValue());
        cus.setFreezeMoney(freeze.subtract(price).floatValue());
        userService.updateById(cus);
        goodsOrderService.updateById(order);
        goodsService.updateById(goods);
        //更新买家的redis缓存 需先判断卖家是否登录 如果登录才需要更新缓存
        if (redisTemplate.opsForValue().get("user_" + cus.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + cus.getToken(), cus);
        }
        redisTemplate.opsForValue().set("goodsOrder_" + id, order);
        redisTemplate.opsForValue().set("goods_" + goodsId, goods);

        //清除仲裁数据
        if (redisTemplate.opsForValue().get("customJudging_" + order.getId()) != null) {
            redisTemplate.delete("customJudging_" + order.getId());
            LambdaQueryWrapper<CustomJudging> customJudgingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            customJudgingLambdaQueryWrapper.eq(CustomJudging::getGoodsId, order.getGoodsId());
            customJudgingService.remove(customJudgingLambdaQueryWrapper);
        }
        //清除商家仲裁缓存和数据
        if (redisTemplate.opsForValue().get("businessJudging_" + order.getId()) != null) {
            redisTemplate.delete("businessJudging_" + order.getId());
            LambdaQueryWrapper<BusinessJudging> businessJudgingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            businessJudgingLambdaQueryWrapper.eq(BusinessJudging::getGoodsId, order.getGoodsId());
            businessJudgingService.remove(businessJudgingLambdaQueryWrapper);
        }
        return Result.success("退款成功");
    }

    @GetMapping("/judgingorder")
    public Result<Page> getJudgingGoods(HttpServletRequest request, QueryInfo queryInfo) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Page<GoodsOrder> page = new Page(queryInfo.getPageNum(), queryInfo.getPageSize());
        LambdaQueryWrapper<GoodsOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), GoodsOrder::getName, queryInfo.getName());
        queryWrapper.eq(GoodsOrder::getStatus, 1);
        goodsOrderService.page(page, queryWrapper);
        return Result.success(page);
    }

    @PutMapping("/returnfw/{id}")
    public Result<String> returnFw(@PathVariable Integer id, @RequestBody User user, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order = (GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);

        if (order.getStatus() != 1) {
            return Result.error("错误");
        }
        if (user.getMoney() > order.getPrice()) {
            return Result.error("退款金额不能超过商品总价值");
        }
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + order.getGoodsId());
        LambdaQueryWrapper<User> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(User::getId, order.getCustomId());
        User cus = userService.getOne(queryWrapper1);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, order.getBusinessId());
        User bis = userService.getOne(queryWrapper);

        BigDecimal returnMoney = new BigDecimal(Float.toString(user.getMoney())); /*BigDecimal.valueOf(user.getMoney());*/
        BigDecimal cusFreeze = new BigDecimal(Float.toString(cus.getFreezeMoney())); /*BigDecimal.valueOf(cus.getFreezeMoney());*/
        BigDecimal cusMoney = new BigDecimal(Float.toString(cus.getMoney()));
        BigDecimal bisMoney = new BigDecimal(Float.toString(bis.getMoney()));
        BigDecimal price = new BigDecimal(Float.toString(order.getPrice()));
        BigDecimal resFreeze = cusFreeze.subtract(price);

        BigDecimal cusResMoney = cusMoney.add(returnMoney);
        BigDecimal bisGetMoney = price.subtract(returnMoney);
        BigDecimal bisResMoney = bisGetMoney.add(bisMoney);

        bis.setMoney(bisResMoney.floatValue());
        goods.setStatus(3);
        order.setStatus(2);
        cus.setFreezeMoney(resFreeze.floatValue());
        cus.setMoney(cusResMoney.floatValue());
        goodsOrderService.updateById(order);
        goodsService.updateById(goods);
        userService.updateById(cus);
        userService.updateById(bis);
        redisTemplate.opsForValue().set("goods_" + goods.getId(), goods);
        redisTemplate.opsForValue().set("goodsOrder_" + order.getId(), order);
        //更新买家的redis缓存 需先判断卖家是否登录 如果登录才需要更新缓存
        if (redisTemplate.opsForValue().get("user_" + cus.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + cus.getToken(), cus);
        }
        //更新卖家的redis缓存 需先判断卖家是否登录 如果登录才需要更新缓存
        if (redisTemplate.opsForValue().get("user_" + bis.getToken()) != null) {
            redisTemplate.opsForValue().set("user_" + bis.getToken(), bis);
        }
        //清除仲裁数据
        if (redisTemplate.opsForValue().get("customJudging_" + order.getId()) != null) {
            redisTemplate.delete("customJudging_" + order.getId());
            LambdaQueryWrapper<CustomJudging> customJudgingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            customJudgingLambdaQueryWrapper.eq(CustomJudging::getGoodsId, order.getGoodsId());
            customJudgingService.remove(customJudgingLambdaQueryWrapper);
        }
        //清除商家仲裁缓存和数据
        if (redisTemplate.opsForValue().get("businessJudging_" + order.getId()) != null) {
            redisTemplate.delete("businessJudging_" + order.getId());
            LambdaQueryWrapper<BusinessJudging> businessJudgingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            businessJudgingLambdaQueryWrapper.eq(BusinessJudging::getGoodsId, order.getGoodsId());
            businessJudgingService.remove(businessJudgingLambdaQueryWrapper);
        }
        return Result.success("退款成功");

    }


}
