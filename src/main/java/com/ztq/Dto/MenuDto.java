package com.ztq.Dto;

import com.ztq.entity.Menu;
import lombok.Data;

import java.util.List;

@Data
public class MenuDto extends Menu {
    List<Menu> child;
}
