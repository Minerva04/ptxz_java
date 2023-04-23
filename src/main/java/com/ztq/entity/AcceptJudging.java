package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AcceptJudging implements Serializable {
    private Integer id;
    private Integer runnerId;
    private String imag;
    private String description;
    private LocalDateTime commitTime;
}
