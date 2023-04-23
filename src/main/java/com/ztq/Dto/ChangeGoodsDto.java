package com.ztq.Dto;

import com.ztq.entity.ChangeGoods;
import lombok.Data;

import java.io.Serializable;
@Data
public class ChangeGoodsDto extends ChangeGoods implements Serializable {
    Integer isManager;
}
