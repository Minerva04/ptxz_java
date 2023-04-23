package com.ztq.Dto;

import com.ztq.entity.Runner;
import lombok.Data;

import java.io.Serializable;
@Data
public class RunnerDto extends Runner implements Serializable {
    private Integer isManager;
}
