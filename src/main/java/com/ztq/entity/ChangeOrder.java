package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChangeOrder implements Serializable {
    Integer id              ;
   Integer launchGoodsId;
   Integer acceptGoodsId;
   Integer launchUserId ;
   Integer acceptUserId ;
   Integer status  =0         ;
   LocalDateTime time;
   Integer lastStatus   =0   ;
   String launchGoodsName;
   String acceptGoodsName;
   String acImg;
   String sendImg;
}
