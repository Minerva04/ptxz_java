package com.ztq.entity;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChangeGoods implements Serializable {
    private Integer id;
    private String name;
    private String hope;
    private String description;
    private String contact;

    private Integer userId;
    private Integer area;
    private Integer building;
    private Integer room;
    private Integer floor;
    private String imgUrl;
    private LocalDateTime time;
    private Integer isDelete=0;
    private Integer status=0;
}
