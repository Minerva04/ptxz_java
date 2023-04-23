package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.User;
import com.ztq.mapper.UserMapper;
import com.ztq.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
