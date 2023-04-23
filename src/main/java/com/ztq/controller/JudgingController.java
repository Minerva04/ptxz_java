package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ztq.entity.*;
import com.ztq.service.AcceptJudgingService;
import com.ztq.service.BusinessJudgingService;
import com.ztq.service.CustomJudgingService;
import com.ztq.service.PublisherJudgingService;
import com.ztq.utils.CheckOtherLogin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/judging")
@Slf4j
public class JudgingController {

    @Autowired
    CustomJudgingService customJudgingService;
    @Autowired
    CheckOtherLogin checkOtherLogin;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    BusinessJudgingService businessJudgingService;
    @Autowired
    PublisherJudgingService publisherJudgingService;
    @Autowired
    AcceptJudgingService acceptJudgingService;

    @PutMapping("/custom/{id}")
    public Result<String> customJudging(@RequestBody CustomJudging customJudging, @PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order =(GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        //防止仲裁完成而另一方没刷新页面继续提交
        if(order.getStatus()!=1){
            return Result.error("错误");
        }
        customJudging.setCommitTime(LocalDateTime.now());
        customJudging.setGoodsId(order.getGoodsId());
        if (redisTemplate.opsForValue().get("customJudging_"+order.getId())==null){
            //第一次提交
            customJudgingService.save(customJudging);
            redisTemplate.opsForValue().set("customJudging_"+order.getId(),customJudging);
        }else {
            CustomJudging customJudging1=(CustomJudging) redisTemplate.opsForValue().get("customJudging_"+order.getId());
            customJudging1.setImag(customJudging.getImag());
            customJudging1.setDescription(customJudging.getDescription());
            customJudging1.setCommitTime(LocalDateTime.now());
            customJudgingService.updateById(customJudging1);
            redisTemplate.opsForValue().set("customJudging_"+order.getId(),customJudging1);
        }
        return Result.success("上传成功");
    }

    @GetMapping("/goodsjudgingcancel/{id}")
    @Transactional
    public Result<String> goodsCancelJudging(@PathVariable Integer id,HttpServletRequest request){
        //取消仲裁自动收货并删除缓存中的仲裁资料
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order =(GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        //防止仲裁完成而另一方没刷新页面继续提交
        if(order.getStatus()!=1){
            return Result.error("错误");
        }
        //清除顾客仲裁缓存和数据
        if (redisTemplate.opsForValue().get("customJudging_"+order.getId())!=null){
            redisTemplate.delete("customJudging_"+order.getId());
            LambdaQueryWrapper<CustomJudging> customJudgingLambdaQueryWrapper=new LambdaQueryWrapper<>();
            customJudgingLambdaQueryWrapper.eq(CustomJudging::getGoodsId,order.getGoodsId());
            customJudgingService.remove(customJudgingLambdaQueryWrapper);
        }
        //清除商家仲裁缓存和数据
        if (redisTemplate.opsForValue().get("businessJudging_"+order.getId())!=null){
            redisTemplate.delete("businessJudging_"+order.getId());
            LambdaQueryWrapper<BusinessJudging> businessJudgingLambdaQueryWrapper=new LambdaQueryWrapper<>();
            businessJudgingLambdaQueryWrapper.eq(BusinessJudging::getGoodsId,order.getGoodsId());
            businessJudgingService.remove(businessJudgingLambdaQueryWrapper);
        }
        return Result.success("取消成功 订单已完成");
    }

    @PutMapping("/bis/{id}")
    public Result<String> bisJudging(@RequestBody BusinessJudging businessJudging, @PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order =(GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        //防止仲裁完成而另一方没刷新页面继续提交
        if(order.getStatus()!=1){
            return Result.error("错误");
        }
        businessJudging.setCommitTime(LocalDateTime.now());
        businessJudging.setGoodsId(order.getGoodsId());
        if (redisTemplate.opsForValue().get("businessJudging_"+order.getId())==null){
            //第一次提交
            businessJudgingService.save(businessJudging);
            redisTemplate.opsForValue().set("businessJudging_"+order.getId(),businessJudging);
        }else {
            BusinessJudging businessJudging1=(BusinessJudging) redisTemplate.opsForValue().get("businessJudging_"+order.getId());
            businessJudging1.setImag(businessJudging.getImag());
            businessJudging1.setDescription(businessJudging.getDescription());
            businessJudging1.setCommitTime(LocalDateTime.now());
            businessJudgingService.updateById(businessJudging1);
            redisTemplate.opsForValue().set("businessJudging_"+order.getId(),businessJudging1);
        }
        return Result.success("上传成功");
    }
    @PutMapping("/publisher/{id}")
    public Result<String> publisherJudging(@RequestBody PublisherJudging publisherJudging, @PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner=(Runner) redisTemplate.opsForValue().get("runner_"+id);
        //防止仲裁完成而另一方没刷新页面继续提交
        if(runner.getStatus()==0||runner.getStatus()==1||runner.getStatus()==4){
            return Result.error("错误");
        }
        publisherJudging.setCommitTime(LocalDateTime.now());
        publisherJudging.setRunnerId(id);
        if (redisTemplate.opsForValue().get("publisherJudging_"+id)==null){
            //第一次提交
            publisherJudgingService.save(publisherJudging);
            redisTemplate.opsForValue().set("publisherJudging_"+id,publisherJudging);
        }else {
            PublisherJudging publisherJudging1=(PublisherJudging) redisTemplate.opsForValue().get("publisherJudging_"+id);
            publisherJudging1.setImag(publisherJudging.getImag());
            publisherJudging1.setDescription(publisherJudging.getDescription());
            publisherJudging1.setCommitTime(LocalDateTime.now());
            publisherJudgingService.updateById(publisherJudging1);
            redisTemplate.opsForValue().set("publisherJudging_"+id,publisherJudging);
        }
        return Result.success("上传成功");
    }
    @PutMapping("/accept/{id}")
    public Result<String> acceptJudging(@RequestBody AcceptJudging acceptJudging, @PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner=(Runner) redisTemplate.opsForValue().get("runner_"+id);
        //防止仲裁完成而另一方没刷新页面继续提交
        if(runner.getStatus()==0||runner.getStatus()==1||runner.getStatus()==4){
            return Result.error("错误");
        }
        acceptJudging.setCommitTime(LocalDateTime.now());
        acceptJudging.setRunnerId(id);
        if (redisTemplate.opsForValue().get("acceptJudging_"+id)==null){
            //第一次提交
            acceptJudgingService.save(acceptJudging);
            redisTemplate.opsForValue().set("acceptJudging_"+id,acceptJudging);
        }else {
            AcceptJudging acceptJudging1=(AcceptJudging) redisTemplate.opsForValue().get("acceptJudging_"+id);
            acceptJudging1.setImag(acceptJudging.getImag());
            acceptJudging1.setDescription(acceptJudging.getDescription());
            acceptJudging1.setCommitTime(LocalDateTime.now());
            acceptJudgingService.updateById(acceptJudging1);
            redisTemplate.opsForValue().set("acceptJudging_"+id,acceptJudging1);
        }
        return Result.success("上传成功");
    }
    @GetMapping("/business/{id}")
    public Result<BusinessJudging> getBusinessJudging(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order =(GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        if (order.getStatus()!=1){
            return Result.error("当前订单未申请仲裁");
        }

        BusinessJudging jud =(BusinessJudging) redisTemplate.opsForValue().get("businessJudging_" + id);
        return Result.success(jud);


    }
    @GetMapping("/custom/{id}")
    public Result<CustomJudging> getCustomJudging(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        GoodsOrder order =(GoodsOrder) redisTemplate.opsForValue().get("goodsOrder_" + id);
        if (order.getStatus()!=1){
            return Result.error("当前订单未申请仲裁");
        }

        CustomJudging jud =(CustomJudging) redisTemplate.opsForValue().get("customJudging_" + id);
        return Result.success(jud);
    }

    @GetMapping("/publisher/{id}")
    public Result<PublisherJudging> getPublisherJudging(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
       Runner  runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus()==1||runner.getStatus()==0||runner.getStatus()==4){
            return Result.error("当前订单未申请仲裁");
        }

        PublisherJudging jud =(PublisherJudging) redisTemplate.opsForValue().get("publisherJudging_" + id);
        return Result.success(jud);


    }
    @GetMapping("/accept/{id}")
    public Result<AcceptJudging> getAcceptJudging(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner  runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus()==1||runner.getStatus()==0||runner.getStatus()==4){
            return Result.error("当前订单未申请仲裁");
        }
        AcceptJudging jud =(AcceptJudging) redisTemplate.opsForValue().get("acceptJudging_" + id);
        return Result.success(jud);
    }

}
