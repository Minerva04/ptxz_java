package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ztq.Dto.ChangeGoodsDto;

import com.ztq.entity.*;
import com.ztq.service.ChangeGoodsService;
import com.ztq.service.ChangeOrderService;
import com.ztq.utils.CheckOtherLogin;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RequestMapping("/changegoods")
@RestController
public class ChangeGoodsController {
    @Autowired
    ChangeGoodsService changeGoodsService;
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    CheckOtherLogin checkOtherLogin;
    @Autowired
    ChangeOrderService changeOrderService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody ChangeGoods changeGoods, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        changeGoods.setUserId(user.getId());
        changeGoods.setTime(LocalDateTime.now());
        changeGoodsService.save(changeGoods);
        redisTemplate.opsForValue().set("changeGoods_" + changeGoods.getId(), changeGoods);
        return Result.success("发布成功");
    }

    @GetMapping
    public Result<Page> getChangeGoodsList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<ChangeGoods> queryWrapper = new LambdaQueryWrapper<>();
        Page<ChangeGoods> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(queryInfo.getName() != null, ChangeGoods::getName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getArea() != null, ChangeGoods::getArea, queryInfo.getArea());
        queryWrapper.eq(queryInfo.getBuilding() != null, ChangeGoods::getBuilding, queryInfo.getBuilding());
        queryWrapper.eq(queryInfo.getFloor() != null, ChangeGoods::getFloor, queryInfo.getFloor());
        queryWrapper.eq(queryInfo.getRoom() != null, ChangeGoods::getRoom, queryInfo.getRoom());
        queryWrapper.eq(ChangeGoods::getStatus, 0);
        queryWrapper.eq(ChangeGoods::getIsDelete,0);
        changeGoodsService.page(page, queryWrapper);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<ChangeGoodsDto> getOne(HttpServletRequest request, @PathVariable Integer id) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods = null;
        goods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);
        User user =(User) redisTemplate.opsForValue().get("user_" + token);
        if (goods != null) {
            if (goods.getIsDelete() == 1) {
                return Result.error("该商品已被删除");
            }
            ChangeGoodsDto changeGoodsDto=new ChangeGoodsDto();
            BeanUtils.copyProperties(goods,changeGoodsDto);
            if (user.getIsManager()==1){
                changeGoodsDto.setIsManager(1);
            }
            return Result.success(changeGoodsDto);
        }
        LambdaQueryWrapper<ChangeGoods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeGoods::getId, id);
        goods = changeGoodsService.getOne(queryWrapper);
        redisTemplate.opsForValue().set("changeGoods_" + id, goods);
        ChangeGoodsDto changeGoodsDto=new ChangeGoodsDto();
        BeanUtils.copyProperties(goods,changeGoodsDto);
        if (user.getIsManager()==1){
            changeGoodsDto.setIsManager(1);
        }
        return Result.success(changeGoodsDto);
    }

    @GetMapping("/mychange")
    public Result<Page> getMyChange(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        LambdaQueryWrapper<ChangeGoods> queryWrapper = new LambdaQueryWrapper<>();
        Page<ChangeGoods> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(queryInfo.getName() != null, ChangeGoods::getName, queryInfo.getName());
        queryWrapper.eq(ChangeGoods::getUserId, user.getId());
        queryWrapper.eq(ChangeGoods::getStatus, 0);
        queryWrapper.eq(ChangeGoods::getIsDelete,0);
        changeGoodsService.page(page, queryWrapper);
        return Result.success(page);
    }

    @GetMapping("/sendchange/{sendid}/{acid}")
    public Result<String> sendChange(@PathVariable Integer sendid, @PathVariable Integer acid, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods sendGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + sendid);
        ChangeGoods acGoods = (ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + acid);
        if (acGoods.getIsDelete() == 1) {
            return Result.error("该商品已被删除");
        }
        if(sendGoods.getStatus()!=0||acGoods.getStatus()!=0){
            return Result.error("你的物品或对方的物品正处于交换状态");
        }
        ChangeOrder changeOrder = (ChangeOrder) redisTemplate.opsForValue().get("changeOrder_" + sendid + acid);
        if (changeOrder != null) {
            if (changeOrder.getLastStatus() != 0) {
                return Result.error("错误");
            }
            //被拒绝后重新发送申请
            if (changeOrder.getStatus() == 2) {
                changeOrder.setStatus(0);
                redisTemplate.opsForValue().set("changeOrder_" + sendid + acid, changeOrder);
                changeOrderService.updateById(changeOrder);
                return Result.success("交换请求已发送 等待对方回应");
            }
            return Result.error("请勿重发发送请求");
        }
        //查询是否发送6个以上请求
        LambdaQueryWrapper<ChangeOrder>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeOrder::getLaunchGoodsId,sendid);
        queryWrapper.eq(ChangeOrder::getStatus,0);
        List<ChangeOrder> list = changeOrderService.list(queryWrapper);
        if (list.size()>5){
            return Result.error("一件物品最多只能与六件物品同时交换");
        }

        ChangeOrder order = (ChangeOrder) redisTemplate.opsForValue().get("changeOrder_" + acid + sendid);
        if (order != null) {
            //当前发送交换请求的商品已经向自己的商品发送过请求 自动同意 并将两件商品的发送请求订单删除减小数据库的垃圾数据

            LambdaQueryWrapper<ChangeOrder> queryWrapper2=new LambdaQueryWrapper<>();
            queryWrapper2.eq(ChangeOrder::getLaunchGoodsId,acid);
            queryWrapper2.ne(ChangeOrder::getAcceptGoodsId,sendid);
            changeOrderService.remove(queryWrapper2);
            Set<String> keys2 = redisTemplate.keys("changeOrder_"+acid+"*");
            for (String key : keys2) {
                if (!key.equals("changeOrder_"+acid+sendid)){
                    redisTemplate.delete(key);
                }
            }

            Set<String> keys3 = redisTemplate.keys("changeOrder_*"+acid);
            for (String key : keys3) {
                ChangeOrder sendOrder =(ChangeOrder) redisTemplate.opsForValue().get(key);
                sendOrder.setStatus(2);
                changeOrderService.updateById(sendOrder);
            }

            //清除接收物品的其他请求
            LambdaQueryWrapper<ChangeOrder>queryWrapper1=new LambdaQueryWrapper<>();
            queryWrapper1.eq(ChangeOrder::getLaunchGoodsId,sendid);
            changeOrderService.remove(queryWrapper1);
            //清除redis缓存
            Set<String> keys = redisTemplate.keys("changeOrder_"+sendid+"*");
            for (String key : keys) {
                redisTemplate.delete(key);
            }
            //将向当前商品发送的请求自动拒绝
            Set<String> keys1 = redisTemplate.keys("changeOrder_*"+sendid);
            for (String key : keys1) {
                ChangeOrder sendOrder =(ChangeOrder) redisTemplate.opsForValue().get(key);
                sendOrder.setStatus(2);
                changeOrderService.updateById(sendOrder);
            }


            order.setStatus(1);
            order.setLastStatus(1);
            order.setTime(LocalDateTime.now());
            redisTemplate.opsForValue().set("changeOrder_" + acid + sendid, order);
            changeOrderService.updateById(order);
            sendGoods.setStatus(1);
            acGoods.setStatus(1);
            changeGoodsService.updateById(sendGoods);
            changeGoodsService.updateById(acGoods);
            redisTemplate.opsForValue().set("changeGoods_"+sendid,sendGoods);
            redisTemplate.opsForValue().set("changeGoods_"+acid,acGoods);
            return Result.success("当前物品已向你发过交换请求 已自动同意");
        }
        order = new ChangeOrder();
        order.setTime(LocalDateTime.now());
        order.setAcceptGoodsId(acid);
        order.setLaunchGoodsId(sendid);
        order.setAcceptGoodsName(acGoods.getName());
        order.setLaunchGoodsName(sendGoods.getName());
        order.setAcceptUserId(acGoods.getUserId());
        order.setLaunchUserId(sendGoods.getUserId());
        order.setAcImg(acGoods.getImgUrl());
        order.setSendImg(sendGoods.getImgUrl());
        changeOrderService.save(order);
        redisTemplate.opsForValue().set("changeOrder_" + sendid + acid, order);
        return Result.success("交换请求已发送 等待对方回应");
    }
    @GetMapping("/mychangegoods")
    public Result<Page> getMyChangeGoods(HttpServletRequest request,QueryInfo queryInfo){
        String token = request.getHeader("Authorization");
        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        Page<ChangeGoods> page=new Page(queryInfo.getPageNum(),queryInfo.getPageSize());
        User user=(User) redisTemplate.opsForValue().get("user_"+token);
        Integer id=user.getId();
        LambdaQueryWrapper<ChangeGoods> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), ChangeGoods::getName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus() != null, ChangeGoods::getStatus, queryInfo.getStatus());
        queryWrapper.eq( ChangeGoods::getUserId, id);
        queryWrapper.eq(ChangeGoods::getIsDelete,0);
        changeGoodsService.page(page,queryWrapper);
        return Result.success(page);
    }
    @PutMapping("/change/{id}")
    public Result<String> changeGoods(@PathVariable Integer id,HttpServletRequest request,@RequestBody ChangeGoods goods){
        String token = request.getHeader("Authorization");
        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods1 =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);
        if (goods1==null){
            LambdaQueryWrapper<ChangeGoods> queryWrapper=new LambdaQueryWrapper<>();
            queryWrapper.eq(ChangeGoods::getId,id);
            goods1= changeGoodsService.getOne(queryWrapper);
        }
        if (goods1.getIsDelete() == 1) {
            return Result.error("该商品已被删除");
        }
        if (goods1.getStatus()!=0){
            return Result.error("错误");
        }
        if (goods1.getIsDelete()==1){
            return Result.error("错误");
        }
        goods.setTime(LocalDateTime.now());
        goods.setId(goods1.getId());
        changeGoodsService.updateById(goods);
        redisTemplate.opsForValue().set("changeGoods_"+goods.getId(),goods);
        return Result.success("修改成功");
    }

    @GetMapping("/pushchange/{id}")
    public Result<Page> myPushChange(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request,@PathVariable Integer id) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);
        if (goods.getStatus()!=0){
            return Result.error("错误");
        }
        LambdaQueryWrapper<ChangeOrder> queryWrapper = new LambdaQueryWrapper<>();
        Page<ChangeOrder> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.eq(ChangeOrder::getLaunchGoodsId,id);
        queryWrapper.like(queryInfo.getName() != null, ChangeOrder::getAcceptGoodsName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus()!=null,ChangeOrder::getStatus, queryInfo.getStatus());
        changeOrderService.page(page, queryWrapper);
        return Result.success(page);
    }

    @GetMapping("/acceptchange/{id}")
    public Result<Page> myAcceptChange(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request,@PathVariable Integer id) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);
        if (goods.getStatus()!=0){
            return Result.error("错误");
        }
        LambdaQueryWrapper<ChangeOrder> queryWrapper = new LambdaQueryWrapper<>();
        Page<ChangeOrder> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.eq(ChangeOrder::getAcceptGoodsId,id);
        queryWrapper.like(queryInfo.getName() != null, ChangeOrder::getAcceptGoodsName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus()!=null,ChangeOrder::getStatus, queryInfo.getStatus());
        changeOrderService.page(page, queryWrapper);
        return Result.success(page);
    }
    @PutMapping("/refuse/{id}")
    public Result<String> refuse(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //同意之后会将当前商品发送的请求订单给删除 所以此处要判断当前拒绝的商品有没有接受请求 即判断当前订单是否存在
        LambdaQueryWrapper<ChangeOrder> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeOrder::getId,id);
        ChangeOrder order = changeOrderService.getOne(queryWrapper);
        if (order==null){
            return Result.error("当前物品已接受其他物品的交换请求");
        }
        if (order.getStatus()!=0){
            return Result.error("错误");
        }
        order.setStatus(2);
        Integer acId=order.getAcceptGoodsId();
        Integer sendId=order.getLaunchGoodsId();
        ChangeGoods acceptGoods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + acId);
        ChangeGoods sendGoods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + sendId);
        if (acceptGoods.getIsDelete()==1||sendGoods.getIsDelete()==1){
            return Result.error("物品已被删除");
        }
        redisTemplate.opsForValue().set("changeOrder_"+sendId+acId,order);
        changeOrderService.updateById(order);
        return Result.success("已拒绝");
    }
    @PutMapping("/agree/{id}")
    @Transactional
    public Result<String> agree(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //同意之后会将当前商品发送的请求订单给删除 并将向当前商品发送的请求自动拒绝
        LambdaQueryWrapper<ChangeOrder> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ChangeOrder::getId,id);
        ChangeOrder order = changeOrderService.getOne(queryWrapper);
        if (order==null){
            return Result.error("当前物品已接受其他物品的交换请求");
        }
        if (order.getStatus()!=0){
            return Result.error("错误");
        }
        //将当前商品发送的请求订单给删除
        Integer acceptGoodsId = order.getAcceptGoodsId();
        Integer sendGoodsId=order.getLaunchGoodsId();
       /* LambdaQueryWrapper<ChangeGoods>queryWrapper3=new LambdaQueryWrapper<>();
        queryWrapper3.eq(ChangeGoods::getId, sendGoodsId);
        ChangeGoods sendGoods = changeGoodsService.getOne(queryWrapper3);

        LambdaQueryWrapper<ChangeGoods>queryWrapper4=new LambdaQueryWrapper<>();
        queryWrapper4.eq(ChangeGoods::getId, acceptGoodsId);
        ChangeGoods acceptGoods = changeGoodsService.getOne(queryWrapper4);*/
        ChangeGoods acceptGoods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + acceptGoodsId);
        ChangeGoods sendGoods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + sendGoodsId);
        if (acceptGoods.getIsDelete()==1||sendGoods.getIsDelete()==1){
            return Result.error("物品已被删除");
        }
        LambdaQueryWrapper<ChangeOrder>queryWrapper1=new LambdaQueryWrapper<>();
        queryWrapper1.eq(ChangeOrder::getLaunchGoodsId,acceptGoodsId);
        changeOrderService.remove(queryWrapper1);
        //清除redis缓存
        Set<String> keys = redisTemplate.keys("changeOrder_"+acceptGoodsId+"*");
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        //将向当前商品发送的请求自动拒绝
        Set<String> keys1 = redisTemplate.keys("changeOrder_*"+acceptGoodsId);
        for (String key : keys1) {
            ChangeOrder sendOrder =(ChangeOrder) redisTemplate.opsForValue().get(key);
            sendOrder.setStatus(2);
            changeOrderService.updateById(sendOrder);
        }

        //将发送商品的发送其他请求删除 并将其他向他发送的请求拒绝
        LambdaQueryWrapper<ChangeOrder> queryWrapper2=new LambdaQueryWrapper<>();
        queryWrapper2.eq(ChangeOrder::getLaunchGoodsId,sendGoodsId);
        queryWrapper2.ne(ChangeOrder::getAcceptGoodsId,acceptGoodsId);
        changeOrderService.remove(queryWrapper2);
        Set<String> keys2 = redisTemplate.keys("changeOrder_"+sendGoodsId+"*");
        for (String key : keys2) {
            if (!key.equals("changeOrder_"+sendGoodsId+acceptGoodsId)){
                redisTemplate.delete(key);
            }
        }
        Set<String> keys3 = redisTemplate.keys("changeOrder_*"+sendGoodsId);
        for (String key : keys3) {
            ChangeOrder sendOrder =(ChangeOrder) redisTemplate.opsForValue().get(key);
            sendOrder.setStatus(2);
            changeOrderService.updateById(sendOrder);
        }
        //同意 设置商品status
        order.setStatus(1);
        order.setLastStatus(1);

        sendGoods.setStatus(1);
        changeGoodsService.updateById(sendGoods);

        acceptGoods.setStatus(1);
        changeGoodsService.updateById(acceptGoods);
        redisTemplate.opsForValue().set("changeGoods_"+sendGoodsId,sendGoods);
        redisTemplate.opsForValue().set("changeGoods_"+acceptGoodsId,acceptGoods);
        redisTemplate.opsForValue().set("changeOrder_"+sendGoodsId+acceptGoodsId,order);
        changeOrderService.updateById(order);
        return Result.success("已同意");
    }
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);

        if (goods.getIsDelete()==1){
            return Result.error("当前物品已删除");
        }
        if (goods.getStatus()==0){
            //将当前商品发送的请求删除 并拒绝请求
            LambdaQueryWrapper<ChangeOrder>queryWrapper1=new LambdaQueryWrapper<>();
            queryWrapper1.eq(ChangeOrder::getLaunchGoodsId,id);
            changeOrderService.remove(queryWrapper1);
            //清除redis缓存
            Set<String> keys = redisTemplate.keys("changeOrder_"+id+"*");
            for (String key : keys) {
                redisTemplate.delete(key);
            }
            //将向当前商品发送的请求自动拒绝
            Set<String> keys1 = redisTemplate.keys("changeOrder_*"+id);
            for (String key : keys1) {
                ChangeOrder sendOrder =(ChangeOrder) redisTemplate.opsForValue().get(key);
                sendOrder.setStatus(2);
                changeOrderService.updateById(sendOrder);
            }
            goods.setIsDelete(1);
            changeGoodsService.updateById(goods);
            redisTemplate.opsForValue().set("changeGoods_"+id,goods);

        } else if(goods.getStatus()==2){
            //设置isdelete=1
            goods.setIsDelete(1);
            changeGoodsService.updateById(goods);
            redisTemplate.opsForValue().set("changeGoods_"+id,goods);
        }else {
            return Result.error("错误");
        }
        return Result.success("删除成功");
    }


    //将原商品isdelete设为1 并重新赋值一个商品并存入数据库 更新redis
    @PutMapping("/reset/{id}")
    public Result<String> reset(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        ChangeGoods goods =(ChangeGoods) redisTemplate.opsForValue().get("changeGoods_" + id);
        goods.setIsDelete(1);
        redisTemplate.opsForValue().set("changeGoods_" + id,goods);
        changeGoodsService.updateById(goods);
        ChangeGoods changeGoods=new ChangeGoods();
        BeanUtils.copyProperties(goods,changeGoods,"id");
        changeGoods.setIsDelete(0);
        changeGoods.setTime(LocalDateTime.now());
        changeGoods.setStatus(0);
        changeGoodsService.save(changeGoods);
        redisTemplate.opsForValue().set("changeGoods_"+changeGoods.getId(),changeGoods);
        return Result.success("重新上架成功");
    }

}
