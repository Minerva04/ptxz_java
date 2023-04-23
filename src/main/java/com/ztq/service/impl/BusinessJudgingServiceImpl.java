package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.BusinessJudging;
import com.ztq.mapper.BusinessJudgingMapper;
import com.ztq.service.BusinessJudgingService;
import org.springframework.stereotype.Service;

@Service
public class BusinessJudgingServiceImpl extends ServiceImpl<BusinessJudgingMapper, BusinessJudging> implements BusinessJudgingService {
}
