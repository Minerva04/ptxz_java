package com.ztq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ztq.entity.GoodsOrder;

public interface GoodsOrderService extends IService<GoodsOrder> {
    void autoConfirm(Integer id);

}
