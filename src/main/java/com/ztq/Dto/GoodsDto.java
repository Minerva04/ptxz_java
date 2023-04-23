package com.ztq.Dto;

import com.ztq.entity.Goods;
import lombok.Data;

import java.io.Serializable;

@Data
public class GoodsDto extends Goods implements Serializable {
    private Integer isManager=0;

}
