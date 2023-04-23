package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.Menu;
import com.ztq.mapper.MenuMapper;
import com.ztq.service.MenuService;
import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
}
