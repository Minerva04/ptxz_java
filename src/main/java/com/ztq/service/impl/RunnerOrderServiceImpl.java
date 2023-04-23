package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.RunnerOrder;
import com.ztq.mapper.RunnerOrderMapper;
import com.ztq.service.RunnerOrderService;
import org.springframework.stereotype.Service;

@Service
public class RunnerOrderServiceImpl extends ServiceImpl<RunnerOrderMapper, RunnerOrder>implements RunnerOrderService {
}
