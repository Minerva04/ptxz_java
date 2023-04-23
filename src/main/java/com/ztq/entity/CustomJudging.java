package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomJudging implements Serializable {
    private Integer id;
    private Integer goodsId;
    private String imag;
    private String description;
    private LocalDateTime commitTime;

}
