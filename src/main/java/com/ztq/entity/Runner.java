package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Runner implements Serializable {

    Integer id;
    String type;
    Float needMoney;
    Float reward;
    String description;
    Integer publishUserId;
    Integer acceptUserId;
    // status= 0 未接单 1 正在交易 2 接单者仲裁 3 发单者仲裁 4 已完成
    Integer status = 0;
    String contact;
    LocalDateTime time;
    Integer publishUserConfirm = 0;
    Integer acceptUserConfirm = 0;
    Integer isPublishDelete = 0;
    Integer isAcceptDelete=0;
}
