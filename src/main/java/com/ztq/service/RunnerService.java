package com.ztq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ztq.entity.Runner;
import com.ztq.entity.User;
import org.springframework.data.redis.core.RedisTemplate;

public interface RunnerService extends IService<Runner> {
    void updateMoney(Runner runner, User publish, UserService userService, RunnerService runnerService, RedisTemplate redisTemplate, String token);
}
