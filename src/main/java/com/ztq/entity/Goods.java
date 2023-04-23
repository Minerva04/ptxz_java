package com.ztq.entity;

import lombok.Data;


import java.io.Serializable;
import java.time.LocalDateTime;

@Data

public class Goods implements Serializable {
    private Integer id;
   private String name;
    private Float price;
   private Integer businessId;
   private Integer customId;
    private String description;
    private String contact;
   private Integer status=0;
   private Integer area;
   private Integer building;
   private Integer room;
    private Integer floor;
    private String imgUrl;
   private LocalDateTime time;
   private Integer isDelete=0;
}
