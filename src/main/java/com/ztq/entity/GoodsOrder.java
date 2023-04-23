package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GoodsOrder implements Serializable {
    Integer id;
    Integer goodsId;
    LocalDateTime creteTime;
    Float price;
    String imag;
    Integer status=0;
    Integer businessId;
    Integer customId;
    String name;
    Integer isCusdelete=0;
    Integer isBisdelete=0;
}
