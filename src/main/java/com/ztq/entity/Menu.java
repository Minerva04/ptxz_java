package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Menu implements Serializable {
    private Integer id;
    private String name;
    private String path;
    private Integer parent;
    private Integer isManager=0;
}
