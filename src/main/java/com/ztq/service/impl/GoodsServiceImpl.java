package com.ztq.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ztq.entity.Goods;
import com.ztq.mapper.GoodsMapper;
import com.ztq.service.GoodsService;
import org.springframework.stereotype.Service;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
}
