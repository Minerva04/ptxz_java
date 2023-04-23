package com.ztq.Dto;

import com.ztq.entity.User;
import lombok.Data;

@Data
public class UserDto extends User {
    private String code;

    private String newPassword;

    private String confirmPassword;
}
