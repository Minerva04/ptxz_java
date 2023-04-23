package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ztq.entity.*;
import com.ztq.service.ChangeGoodsService;
import com.ztq.service.ChangeOrderService;
import com.ztq.utils.CheckOtherLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/changeorder")
public class ChangeOrderController {

    @Autowired
    ChangeGoodsService changeGoodsService;
    @Autowired
    CheckOtherLogin checkOtherLogin;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    ChangeOrderService changeOrderService;

    //last 1 交换中 2交换完成 3 交换取消
    @GetMapping()
    public Result<Page> getOrderList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        Integer userId = user.getId();
        LambdaQueryWrapper<ChangeOrder> queryWrapper = new LambdaQueryWrapper<>();
        Page<ChangeOrder> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        if (queryInfo.getStatus()!=null){
            queryWrapper.eq(ChangeOrder::getLastStatus,queryInfo.getStatus());
        }else {
            queryWrapper.ne(ChangeOrder::getLastStatus,0);
        }

        queryWrapper.and(wrapper->wrapper.eq(ChangeOrder::getAcceptUserId, userId).or().eq(ChangeOrder::getLaunchUserId, userId));
        queryWrapper.and((wrapper->wrapper.like(queryInfo.getName() != null, ChangeOrder::getAcceptGoodsName, queryInfo.getName())
                .or().like(queryInfo.getName() != null, ChangeOrder::getLaunchGoodsName, queryInfo.getName())));

        changeOrderService.page(page, queryWrapper);
        return Result.success(page);
    }

    @GetMapping("/confirm/{id}")
    @Transactional
    public Result<String> confirm(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<ChangeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeOrder::getId, id);
        ChangeOrder order = changeOrderService.getOne(queryWrapper);
        if (order.getLastStatus() != 1) {
            return Result.error("当前订单已确认或取消");
        }
        //设置订单状态和商品状态
        Integer sendId = order.getLaunchGoodsId();
        Integer acId = order.getAcceptGoodsId();
        ChangeGoods sendGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + sendId);
        ChangeGoods acGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + acId);
        sendGoods.setStatus(2);
        acGoods.setStatus(2);
        order.setLastStatus(2);
        changeGoodsService.updateById(sendGoods);
        changeGoodsService.updateById(acGoods);
        changeOrderService.updateById(order);
        redisTemplate.opsForValue().set("changeGoods_" + sendId, sendGoods);
        redisTemplate.opsForValue().set("changeGoods_" + acId, acGoods);
        redisTemplate.opsForValue().set("changeOrder_" + sendId + acId, order);
        return Result.success("交换完成");
    }

    @GetMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<ChangeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeOrder::getId, id);
        ChangeOrder order = changeOrderService.getOne(queryWrapper);
        if (order.getLastStatus() != 1) {
            return Result.error("当前订单已确认或取消");
        }
        Integer sendId = order.getLaunchGoodsId();
        Integer acId = order.getAcceptGoodsId();
        ChangeGoods sendGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + sendId);
        ChangeGoods acGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + acId);
        sendGoods.setStatus(3);
        acGoods.setStatus(3);
        order.setLastStatus(3);
        changeGoodsService.updateById(sendGoods);
        changeGoodsService.updateById(acGoods);
        changeOrderService.updateById(order);
        redisTemplate.opsForValue().set("changeGoods_" + sendId, sendGoods);
        redisTemplate.opsForValue().set("changeGoods_" + acId, acGoods);
        redisTemplate.opsForValue().set("changeOrder_" + sendId + acId, order);
        return Result.success("交换已取消");
    }
}
