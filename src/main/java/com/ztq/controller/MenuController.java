package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.Dto.MenuDto;
import com.ztq.entity.Menu;
import com.ztq.entity.Result;
import com.ztq.entity.User;
import com.ztq.service.MenuService;
import com.ztq.service.UserService;
import com.ztq.utils.CheckOtherLogin;
import com.ztq.utils.impl.CheckOtherLoginImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/menu")
@Slf4j
public class MenuController {

    @Autowired
    MenuService menuService;
    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    CheckOtherLogin checkOtherLogin;

    @GetMapping
    public Result getMenu(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        if (user == null) {
            LambdaQueryWrapper<User> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(User::getToken, token);
            user = userService.getOne(queryWrapper2);
        }

        if (user.getToken() != null) {
            //管理员菜单
            if (user.getIsManager() == 1) {
                //管理员
                List<MenuDto> res = null;
                res = (List<MenuDto>) redisTemplate.opsForValue().get("manager_menu");
                if(res!=null){
                    return Result.success(res);
                }
                LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Menu::getIsManager, 1);
                queryWrapper.eq(Menu::getParent, 0);
                List<Menu> one = menuService.list(queryWrapper);
                //先查一级菜单 再查其关联的子菜单再把子菜单存入一级菜单的child中
                res = one.stream().map((item) -> {
                    MenuDto menuDto = new MenuDto();
                    LambdaQueryWrapper<Menu> queryWrapper1 = new LambdaQueryWrapper<>();
                    queryWrapper1.eq(Menu::getParent, item.getId());
                    List<Menu> child = menuService.list(queryWrapper1);
                    BeanUtils.copyProperties(item, menuDto);
                    menuDto.setChild(child);
                    return menuDto;
                }).collect(Collectors.toList());
                redisTemplate.opsForValue().set("manager_menu",res);
                return Result.success(res);
            } else {
                //用户菜单
                List<MenuDto> res = null;
                res = (List<MenuDto>) redisTemplate.opsForValue().get("user_menu");
                if(res!=null){
                    return Result.success(res);
                }
                LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Menu::getIsManager, 0);
                queryWrapper.eq(Menu::getParent, 0);
                List<Menu> one = menuService.list(queryWrapper);
                //先查一级菜单 再查其关联的子菜单再把子菜单存入一级菜单的child中
                res = one.stream().map((item) -> {
                    MenuDto menuDto = new MenuDto();
                    LambdaQueryWrapper<Menu> queryWrapper1 = new LambdaQueryWrapper<>();
                    queryWrapper1.eq(Menu::getParent, item.getId());
                    List<Menu> child = menuService.list(queryWrapper1);
                    BeanUtils.copyProperties(item, menuDto);
                    menuDto.setChild(child);
                    return menuDto;
                }).collect(Collectors.toList());
                redisTemplate.opsForValue().set("user_menu",res);
                return Result.success(res);
            }
        }
        return Result.error("错误");
    }
}
