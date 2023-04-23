package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
public class RunnerOrder implements Serializable {

    Integer id;
    Integer publishUserId;
    Integer acceptUserId;
    Float needMoney;
    Float reward;
    //status=1 正在交易 2 接单者仲裁 3 发单者仲裁 4 已完成
    Integer status=1;
    LocalDateTime createTime;


}
