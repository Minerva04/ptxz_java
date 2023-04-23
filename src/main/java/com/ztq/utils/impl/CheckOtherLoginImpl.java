package com.ztq.utils.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.entity.User;
import com.ztq.service.UserService;
import com.ztq.utils.CheckOtherLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CheckOtherLoginImpl implements CheckOtherLogin {
    //判断是否异地登录 首先检查token是否合法 每次登录都会更新redis中存储的用户token 所以只需检查token是否相同
    //1通过 2过期 3 异地登录
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserService userService;
    public  Integer check(String token){
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getToken,token);
        //token的合法判断通过数据库中的token进行判断 token是否过期通过redis进行判断
        User one = userService.getOne(queryWrapper);
        if(one==null){
            //token不合法 将当前非法token在redis里缓存的数据清除
            redisTemplate.delete("user_"+token);
            return 1;
        }
        if(redisTemplate.opsForValue().get("user_"+token)==null){
            redisTemplate.delete("user_"+token);
            return 2;
        }
        return 0;

    }
}
