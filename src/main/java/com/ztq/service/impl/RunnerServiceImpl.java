package com.ztq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.Runner;
import com.ztq.entity.User;
import com.ztq.mapper.RunnerMapper;
import com.ztq.service.RunnerService;
import com.ztq.service.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RunnerServiceImpl extends ServiceImpl<RunnerMapper, Runner>implements RunnerService {
    @Override
    public void updateMoney(Runner runner, User publish, UserService userService, RunnerService runnerService, RedisTemplate redisTemplate,String token) {
        BigDecimal result = BigDecimal.valueOf(runner.getNeedMoney()).add(BigDecimal.valueOf(runner.getReward()));
        BigDecimal freeze = BigDecimal.valueOf(publish.getFreezeMoney()).subtract(result);
        BigDecimal reward = BigDecimal.valueOf(runner.getReward());
        publish.setFreezeMoney(freeze.floatValue());
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,runner.getAcceptUserId());
        User accept = userService.getOne(queryWrapper);
        BigDecimal resultFreeze = BigDecimal.valueOf(accept.getFreezeMoney()).subtract(reward);
        BigDecimal resultMoney = BigDecimal.valueOf(accept.getMoney()).add(reward).add(result);
        String acToken = accept.getToken();
        //money+reward+result  freeze-reward
        accept.setFreezeMoney(resultFreeze.floatValue());
        accept.setMoney(resultMoney.floatValue());
        runner.setStatus(4);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+runner.getId(),runner);
        userService.updateById(publish);
        redisTemplate.opsForValue().set("user_"+token,publish);
        userService.updateById(accept);
        //判断接单者是否登录 如果登录则需更新redis缓存
        if (redisTemplate.opsForValue().get("user_"+acToken)!=null){
            redisTemplate.opsForValue().set("user_"+acToken,accept);
        }
    }
}
