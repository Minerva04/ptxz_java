package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.ChangeOrder;
import com.ztq.mapper.ChangeOrderMapper;
import com.ztq.service.ChangeOrderService;
import org.springframework.stereotype.Service;

@Service
public class ChangeOrderServiceImpl extends ServiceImpl<ChangeOrderMapper, ChangeOrder> implements ChangeOrderService{
}
