package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.Dto.GoodsDto;
import com.ztq.entity.*;
import com.ztq.service.GoodsOrderService;
import com.ztq.service.GoodsService;
import com.ztq.service.UserService;
import com.ztq.utils.CheckOtherLogin;
import com.ztq.utils.impl.CheckOtherLoginImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/goods")
@Slf4j
public class GoodsController {
    @Autowired
    GoodsService goodsService;
    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    CheckOtherLogin checkOtherLogin;

    @Autowired
    GoodsOrderService goodsOrderService;

    @PostMapping("/add")
    public Result<String> addGoods(@RequestBody Goods goods, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        /*LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getToken, token);
        User user = userService.getOne(queryWrapper);
        Integer id = user.getId();*/
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);

        goods.setTime(LocalDateTime.now());
        goods.setBusinessId(user.getId());
        goodsService.save(goods);
        redisTemplate.opsForValue().set("goods_" + goods.getId(), goods);
        return Result.success("发布成功");
    }

    /* private Integer pageSize;
     private Integer pageNum;
     private Integer name;
     private Integer area;
     private Integer  building;
     private Integer  floor;
     private Integer room;*/
    @GetMapping
    public Result<Page> getGoodsList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        Page<Goods> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(queryInfo.getName() != null, Goods::getName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getArea() != null, Goods::getArea, queryInfo.getArea());
        queryWrapper.eq(queryInfo.getBuilding() != null, Goods::getBuilding, queryInfo.getBuilding());
        queryWrapper.eq(queryInfo.getFloor() != null, Goods::getFloor, queryInfo.getFloor());
        queryWrapper.eq(queryInfo.getRoom() != null, Goods::getRoom, queryInfo.getRoom());
        queryWrapper.eq(Goods::getStatus, 0);
        queryWrapper.eq(Goods::getIsDelete, 0);
        goodsService.page(page, queryWrapper);
        return Result.success(page);
    }


    @GetMapping("/{id}")
    public Result<GoodsDto> getOne(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        String key = "goods_" + id;
        Goods goods = null;
        User user =(User) redisTemplate.opsForValue().get("user_" + token);
        goods = (Goods) redisTemplate.opsForValue().get(key);
        if (goods != null) {
            if (goods.getIsDelete() == 1) {
                return Result.error("该商品已被删除");
            }
            GoodsDto goodsDto=new GoodsDto();
            BeanUtils.copyProperties(goods,goodsDto);
            if (user.getIsManager()==1){
               goodsDto.setIsManager(1);
            }
            return Result.success(goodsDto);
        }
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Goods::getId, id);
        goods = goodsService.getOne(queryWrapper);
        redisTemplate.opsForValue().set(key, goods);
        GoodsDto goodsDto=new GoodsDto();
        BeanUtils.copyProperties(goods,goodsDto);
        if (user.getIsManager()==1){
            goodsDto.setIsManager(1);
        }
        return Result.success(goodsDto);
    }

    /* Integer goods_id;
     LocalDateTime crete_time;
     Integer price;
     String imag;
     Integer business_id;
     Integer custom_id;*/
    @Transactional
    @PutMapping("/{id}")
    public Result<String> buy(@PathVariable Integer id, HttpServletRequest request) {
        //修改数据库 redis缓存 status 生成订单
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User cus = (User) redisTemplate.opsForValue().get("user_" + token);
        if (cus.getStatus() == 1) {
            return Result.error("账号已被封禁");
        }
        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + id);
        if (goods == null || goods.getStatus() != 0||goods.getIsDelete()==1) {
            return Result.error("当前商品不存在");
        }

        //先判断余额
        if (goods.getPrice() > cus.getMoney()) {
            return Result.error("余额不足 购买失败");
        } else {
            Float cusMoney = cus.getMoney();
            Float price = goods.getPrice();
            Float freezeMoney = cus.getFreezeMoney();
            BigDecimal freeze = new BigDecimal(Float.toString(freezeMoney));
            BigDecimal cusMoney2 = new BigDecimal(Float.toString(cusMoney));
            BigDecimal price2 = new BigDecimal(Float.toString(price));
            Float resultMoney = cusMoney2.subtract(price2).floatValue();
            Float resultFreezeMoney = freeze.add(price2).floatValue();
            cus.setMoney(resultMoney);
            cus.setFreezeMoney(resultFreezeMoney);
            userService.updateById(cus);

            redisTemplate.opsForValue().set("user_" + token, cus);
        }


        goods.setStatus(1);
        goods.setCustomId(cus.getId());
        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setGoodsId(goods.getId());
        goodsOrder.setCreteTime(LocalDateTime.now());
        goodsOrder.setPrice(goods.getPrice());
        goodsOrder.setImag(goods.getImgUrl());
        goodsOrder.setCustomId(goods.getCustomId());
        goodsOrder.setBusinessId(goods.getBusinessId());
        goodsOrder.setName(goods.getName());
        goodsOrderService.save(goodsOrder);


        redisTemplate.opsForValue().set("goodsOrder_" + goodsOrder.getId(), goodsOrder,3, TimeUnit.DAYS);
        goodsService.updateById(goods);
        redisTemplate.opsForValue().set("goods_" + id, goods);
        return Result.success("购买成功");
    }

    @GetMapping("/mygoods")
    public Result<Page> getMyGoods(HttpServletRequest request, QueryInfo queryInfo) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Page<Goods> page = new Page(queryInfo.getPageNum(), queryInfo.getPageSize());
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        Integer id = user.getId();
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), Goods::getName, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus() != null, Goods::getStatus, queryInfo.getStatus());
        queryWrapper.eq(Goods::getIsDelete, 0);
        queryWrapper.eq(Goods::getBusinessId, id);
        goodsService.page(page, queryWrapper);
        return Result.success(page);
    }

    //id bisinessid time
    @PutMapping("/changegoods/{id}")
    public Result<String> changeGoods(@PathVariable Integer id, HttpServletRequest request, @RequestBody Goods goods) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Goods goods1 = (Goods) redisTemplate.opsForValue().get("goods_" + id);
        if (goods1 == null) {
            LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Goods::getId, id);
            goods1 = goodsService.getOne(queryWrapper);
        }
        if (goods1.getIsDelete() == 1) {
            return Result.error("该商品已被删除");
        }

        if (goods1.getStatus() != 0) {
            return Result.error("错误");
        }
        goods.setTime(LocalDateTime.now());
        goods.setId(goods1.getId());
        goods.setBusinessId(goods1.getBusinessId());
        goodsService.updateById(goods);
        redisTemplate.opsForValue().set("goods_" + goods.getId(), goods);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }

        Goods goods = (Goods) redisTemplate.opsForValue().get("goods_" + id);
        if (goods == null) {
            LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Goods::getId, id);
            goods = goodsService.getOne(queryWrapper);
        }
        if (goods.getStatus() == 1 || goods.getStatus() == 2) {
            return Result.error("当前商品正在交易无法删除");
        }
        goods.setIsDelete(1);
        redisTemplate.opsForValue().set("goods_" + id, goods);
        goodsService.updateById(goods);
        return Result.success("删除成功");
    }


}
