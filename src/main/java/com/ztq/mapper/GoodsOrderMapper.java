package com.ztq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ztq.entity.Goods;
import com.ztq.entity.GoodsOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsOrderMapper extends BaseMapper<GoodsOrder > {

}
