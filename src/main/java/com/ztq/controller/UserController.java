package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ztq.Dto.UserDto;
import com.ztq.entity.Goods;
import com.ztq.entity.QueryInfo;
import com.ztq.entity.Result;
import com.ztq.entity.User;
import com.ztq.service.UserService;
import com.ztq.utils.CheckOtherLogin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    CheckOtherLogin checkOtherLogin;
    @GetMapping()
    public Result<User> getOne(HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        User one=null;
        String key="user_"+token;
        one=(User) redisTemplate.opsForValue().get(key);
        if (one!=null){
            return Result.success(one);
        }
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getToken,token);
        one  = userService.getOne(queryWrapper);
        redisTemplate.opsForValue().set(key,one);
        return Result.success(one);
    }

    @PutMapping("/modifyinformation")
    public Result<String> modifyInformation(HttpServletRequest request,@RequestBody User user){
        String token = request.getHeader("Authorization");
        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        User user1 =(User) redisTemplate.opsForValue().get("user_" + token);

        if(StringUtils.isNotEmpty(user.getHead())&&StringUtils.isNotEmpty(user.getUserName())){
            BeanUtils.copyProperties(user1,user,"head","userName");
        }else if(StringUtils.isNotEmpty(user.getUserName())){
            BeanUtils.copyProperties(user1,user,"userName");

        }else {
            BeanUtils.copyProperties(user1,user,"head");
        }
        redisTemplate.opsForValue().set("user_"+token,user);
        userService.updateById(user);
        return Result.success("修改成功");
    }

    @PutMapping("/modifypassword")
    public Result<String> modifyPassword(HttpServletRequest request,@RequestBody UserDto userDto){
        String token = request.getHeader("Authorization");
        if( checkOtherLogin.check( token)==1){
            return Result.loginError("账号在别处登录");
        }else if (checkOtherLogin.check( token)==2){
            return Result.loginError("登录信息过期");
        }
        String code1 = (String) redisTemplate.opsForValue().get(userDto.getEmail());
        if(userDto.getCode().equals(code1)){
            User user =(User) redisTemplate.opsForValue().get("user_" + token);
            user.setPassword(userDto.getNewPassword());
            userService.updateById(user);
            redisTemplate.opsForValue().set("user_"+token,user);
            redisTemplate.delete(userDto.getEmail());
            return Result.success("修改成功");
        }
        return Result.error("验证码错误");
    }
    @GetMapping("/alluser")
    public Result<Page> allUser(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        Page<User> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), User::getEmail, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus() != null, User::getStatus, queryInfo.getStatus());
        queryWrapper.eq(User::getIsManager,0);
        userService.page(page, queryWrapper);
        return Result.success(page);
    }

    @PutMapping("/changemoney/{id}")
    @Transactional
    public Result<String> changeMoney(@RequestBody User user,HttpServletRequest request,@PathVariable Integer id){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,id);
        User user1 = userService.getOne(queryWrapper);
        user1.setMoney(user.getMoney());
        userService.updateById(user1);
        if (redisTemplate.opsForValue().get("user_"+user1.getToken())!=null){
            //当前用户登录 需更新缓存
            redisTemplate.opsForValue().set("user_" + user1.getToken(),user1);
        }
        return Result.success("修改成功");
    }
    @GetMapping("/banning/{id}")
    public Result<String> banning(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,id);
        User user1 = userService.getOne(queryWrapper);
        user1.setStatus(1);
        userService.updateById(user1);
        if (redisTemplate.opsForValue().get("user_"+user1.getToken())!=null){
            //当前用户登录 需更新缓存
            redisTemplate.opsForValue().set("user_" + user1.getToken(),user1);
        }
        return Result.success("修改成功");

    }
    @GetMapping("/cancelbanning/{id}")
    public Result<String> cancelBanning(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,id);
        User user1 = userService.getOne(queryWrapper);
        user1.setStatus(0);
        userService.updateById(user1);
        if (redisTemplate.opsForValue().get("user_"+user1.getToken())!=null){
            //当前用户登录 需更新缓存
            redisTemplate.opsForValue().set("user_" + user1.getToken(),user1);
        }
        return Result.success("修改成功");

    }

}
