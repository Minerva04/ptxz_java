package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {
    private Integer id;
    private String email;
    private String userName;
    private String head;
    private Float money;
    private Float freezeMoney;
    private Integer status=0;

    private Integer isManager;
    private String token;
    private String password;
}
