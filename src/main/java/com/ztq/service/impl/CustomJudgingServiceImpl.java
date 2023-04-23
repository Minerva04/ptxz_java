package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.CustomJudging;
import com.ztq.mapper.CustomJudgingMapper;
import com.ztq.service.CustomJudgingService;
import org.springframework.stereotype.Service;

@Service
public class CustomJudgingServiceImpl extends ServiceImpl<CustomJudgingMapper,CustomJudging>implements CustomJudgingService {
}
