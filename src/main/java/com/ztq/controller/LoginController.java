package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.Dto.UserDto;
import com.ztq.entity.Result;
import com.ztq.entity.User;
import com.ztq.service.UserService;
import com.ztq.utils.CodeUtils;
import com.ztq.utils.SendCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RequestMapping("/login")
@RestController
@Slf4j
public class LoginController {
    @Autowired
    UserService userService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    SendCode sendCode;

    //最后还需实现登录成功清除用户验证码缓存 开发阶段为了方便不做处理
    @PostMapping("/emaillogin")
    public Result<Object> login(@RequestBody UserDto userDto) {
        //获取原验证码
        Object code1 = redisTemplate.opsForValue().get(userDto.getEmail());
        if (userDto.getCode().equals(code1)) {
            //先判断是否存在 不存在则注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, userDto.getEmail());
            User one = userService.getOne(queryWrapper);
            String key=null;

            if (one != null) {
                //已注册成功登录
                //生成token
                String time=LocalDateTime.now()+"";
                String token1= one.getEmail()+one.getUserName()+time;
                String token = DigestUtils.md5DigestAsHex(token1.getBytes());

                //更新token
                one.setToken(token);
                /*if (one.getIsManager()==1){
                    token=token+"manager";
                    one.setToken(token);
                }*/
                //更新user
                userService.updateById(one);
                //将用户存入缓存
                 key="user_"+token;
                redisTemplate.opsForValue().set(key,one,2,TimeUnit.HOURS);

                return Result.success(one);
            } else {
                //未注册登录
                one=new User();
                one.setEmail(userDto.getEmail());
                Random random = new Random();
                int r = random.nextInt(100000);
                if (r<10000){
                    r+=10000;
                }
                String s=r+"";
                one.setUserName("新用户" + s);

                //生成token
                String time=LocalDateTime.now()+"";
                String token1= one.getEmail()+one.getUserName()+time;
                String token = DigestUtils.md5DigestAsHex(token1.getBytes());
                one.setToken(token);
                one.setPassword(userDto.getEmail());
                userService.save(one);
                key="user_"+token;
                redisTemplate.opsForValue().set(key,one,2,TimeUnit.HOURS);

                return Result.success(one);
            }
        }
        return Result.error("验证码或邮箱错误");
    }
    @PostMapping("/passwordlogin")
    public Result<Object> passwordLogin(@RequestBody UserDto userDto){
        String password = userDto.getPassword();
        String email = userDto.getEmail();
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPassword,password);
        queryWrapper.eq(User::getEmail,email);
        User one = userService.getOne(queryWrapper);
        if(one!=null){
            String time=LocalDateTime.now()+"";
            String token1= one.getEmail()+one.getUserName()+time;
            String token = DigestUtils.md5DigestAsHex(token1.getBytes());
            if (one.getIsManager()==1){
                token=token+"manager";
                one.setToken(token);
            }
            one.setToken(token);
            userService.updateById(one);
            redisTemplate.opsForValue().set("user_"+token,one,2,TimeUnit.HOURS);

            return Result.success(one);
        }
        return Result.error("账号或密码错误");
    }

    @GetMapping("/{to}")
    public Result<String> sendCode(@PathVariable String to){
        Integer integer = CodeUtils.GetCode(4);
        String code = integer.toString();
        sendCode.sendCode(to,code);
        redisTemplate.opsForValue().set(to,code,3, TimeUnit.MINUTES);
        return Result.success("发送成功");
    }

    @DeleteMapping()
    public Result<String> logout(HttpServletRequest request){
        String token = request.getHeader("Authorization");
        redisTemplate.delete("user_"+token);
        return Result.success("退出成功");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody UserDto userDto){
        String code =(String) redisTemplate.opsForValue().get(userDto.getEmail());

        if(!userDto.getCode().equals(code)){
            return Result.error("验证码错误");
        }
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getEmail,userDto.getEmail());
        User one = userService.getOne(lambdaQueryWrapper);
        if(one!=null){
            return Result.error("邮箱已经被注册");
        }
        one=new User();
        BeanUtils.copyProperties(userDto,one,"code");
        userService.save(one);

        redisTemplate.delete(userDto.getEmail());

        return Result.success("注册成功");
    }

}
