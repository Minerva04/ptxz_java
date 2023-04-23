package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class QueryInfo implements Serializable {
    private Integer pageSize;
    private Integer pageNum;
    private String name;
    private Integer area;
    private Integer  building;
    private Integer  floor;
    private Integer room;
    private Integer status;
}
