package com.ztq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ztq.Dto.GoodsDto;
import com.ztq.Dto.RunnerDto;
import com.ztq.entity.*;
import com.ztq.service.AcceptJudgingService;
import com.ztq.service.PublisherJudgingService;
import com.ztq.service.RunnerService;
import com.ztq.service.UserService;
import com.ztq.utils.CheckOtherLogin;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/runner")
public class RunnerController {

    @Autowired
    RunnerService runnerService;
    @Autowired
    CheckOtherLogin checkOtherLogin;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserService userService;
    @Autowired
    PublisherJudgingService publisherJudgingService;
    @Autowired
    AcceptJudgingService acceptJudgingService;

    @GetMapping
    public Result<Page> getRunnerList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<Runner> queryWrapper = new LambdaQueryWrapper<>();
        Page<Runner> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.eq(Runner::getStatus, 0);
        queryWrapper.eq(Runner::getIsPublishDelete, 0);
        runnerService.page(page, queryWrapper);
        return Result.success(page);
    }

    @PostMapping("/add")
    public Result<String> addGoods(@RequestBody Runner runner, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user = (User) redisTemplate.opsForValue().get("user_" + token);

        //冻结用户对应资金
        Float need = runner.getNeedMoney();
        Float reward = runner.getReward();
        BigDecimal money = BigDecimal.valueOf(user.getMoney());
        BigDecimal freeze = BigDecimal.valueOf(user.getFreezeMoney());
        BigDecimal needDec = BigDecimal.valueOf(need);
        BigDecimal rewardDec = BigDecimal.valueOf(reward);
        BigDecimal result = needDec.add(rewardDec);
        int i = money.compareTo(result);
        if (i < 0) {
            //余额不足无法发布
            return Result.error("余额不足 无法发布");
        }
        user.setMoney(money.subtract(result).floatValue());
        user.setFreezeMoney(freeze.add(result).floatValue());
        userService.updateById(user);
        redisTemplate.opsForValue().set("user_" + token, user);
        runner.setTime(LocalDateTime.now());
        runner.setPublishUserId(user.getId());
        runnerService.save(runner);
        redisTemplate.opsForValue().set("runner_" + runner.getId(), runner);
        return Result.success("发布成功");
    }

    @GetMapping("/{id}")
    public Result<RunnerDto> getOne(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner = null;
        runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        User user =(User) redisTemplate.opsForValue().get("user_" + token);
        if (runner != null) {
            if (runner.getIsPublishDelete() == 1) {
                return Result.error("该订单已被删除");
            }
            RunnerDto runnerDto=new RunnerDto();
            BeanUtils.copyProperties(runner,runnerDto);
            if (user.getIsManager()==1){
                runnerDto.setIsManager(1);
            }
            return Result.success(runnerDto);
        }
        LambdaQueryWrapper<Runner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Runner::getId, id);
        runner = runnerService.getOne(queryWrapper);
        redisTemplate.opsForValue().set("runner_" + id, runner);
        RunnerDto runnerDto=new RunnerDto();
        BeanUtils.copyProperties(runner,runnerDto);
        if (user.getIsManager()==1){
            runnerDto.setIsManager(1);
        }
        return Result.success(runnerDto);
    }

    @PutMapping("/accept/{id}")
    public Result<String> accept(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner == null || runner.getStatus() != 0) {
            return Result.error("当前订单已不存在");
        }
        //先判断接受者有无相应资金 若有 则冻结对应资金 生成相应订单
        User ac = (User) redisTemplate.opsForValue().get("user_" + token);
        if (ac.getStatus() == 1) {
            return Result.error("账号已被封禁");
        }
        if (runner.getIsPublishDelete() == 1) {
            return Result.error("该订单已被删除");
        }

        BigDecimal reward = BigDecimal.valueOf(runner.getReward());
        BigDecimal acMoney = BigDecimal.valueOf(ac.getMoney());
        BigDecimal acFreeze = BigDecimal.valueOf(ac.getFreezeMoney());
        int i = acMoney.compareTo(reward);
        if (i<0){
            return Result.error("余额不足");
        }
        ac.setMoney(acMoney.subtract(reward).floatValue());
        ac.setFreezeMoney(acFreeze.add(reward).floatValue());
        runner.setStatus(1);
        runner.setAcceptUserId(ac.getId());
        runner.setTime(LocalDateTime.now());
        //生成订单
        userService.updateById(ac);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("user_" + token, ac);
        redisTemplate.opsForValue().set("runner_" + runner.getId(), runner);
        return Result.success("接单成功");
    }

    @GetMapping("/myrunner")
    public Result<Page> getMyRunnerList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user =(User) redisTemplate.opsForValue().get("user_" + token);
        LambdaQueryWrapper<Runner> queryWrapper = new LambdaQueryWrapper<>();
        Page<Runner> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), Runner::getType, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus() != null, Runner::getStatus, queryInfo.getStatus());
        queryWrapper.eq(Runner::getIsPublishDelete, 0);
        queryWrapper.eq(Runner::getPublishUserId,user.getId());
        runnerService.page(page, queryWrapper);
        return Result.success(page);

    }

    @PutMapping("/change/{id}")
    public Result<String> change(@PathVariable Integer id, HttpServletRequest request, @RequestBody Runner runner) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner1 = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner1.getIsPublishDelete() == 1) {
            return Result.error("该订单已被删除");
        }
        if (runner1.getStatus() != 0) {
            return Result.error("当前订单正在进行");
        }
        runner1.setDescription(runner.getDescription());
        runner1.setContact(runner.getContact());
        runner1.setType(runner.getType());
        runnerService.updateById(runner1);
        redisTemplate.opsForValue().set("runner_" + id, runner1);
        return Result.success("修改成功");
    }

    //该接口需要判断订单删除时状态  若订单未完成还需退回订单金额
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 1 || runner.getStatus() == 2 || runner.getStatus() == 3) {
            return Result.error("当前订单正在进行 无法删除");
        }
        //订单未完成还需退回订单金额
        User user = (User) redisTemplate.opsForValue().get("user_" + token);
        if (runner.getStatus() == 0) {
            BigDecimal needMoney = BigDecimal.valueOf(runner.getNeedMoney());
            BigDecimal reward = BigDecimal.valueOf(runner.getReward());
            BigDecimal result = needMoney.add(reward);
            BigDecimal money = BigDecimal.valueOf(user.getMoney());
            BigDecimal freeze = BigDecimal.valueOf(user.getFreezeMoney());
            money = money.add(result);
            freeze = freeze.subtract(result);
            user.setMoney(money.floatValue());
            user.setFreezeMoney(freeze.floatValue());
            userService.updateById(user);
            redisTemplate.opsForValue().set("user_" + token, user);
            runner.setIsPublishDelete(1);
            runnerService.updateById(runner);
            redisTemplate.opsForValue().set("runner_" + runner.getId(), runner);
        } else {
            //订单完成 只需删除即可
            runner.setIsPublishDelete(1);
            runnerService.updateById(runner);
            redisTemplate.opsForValue().set("runner_" + runner.getId(), runner);
        }

        return Result.success("删除成功");
    }

    @GetMapping("/publishconfirm/{id}")
    public Result<String> confirm(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //发单者确认 将订单完成 删除仲裁数据 将订单金额转移给接单者 更新redis缓存
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 0 || runner.getStatus() == 4) {
            return Result.error("当前订单未进行");
        }
        User publish = (User) redisTemplate.opsForValue().get("user_" + token);
        if (runner.getStatus() == 1) {
            //订单正常进行 更改订单状态 移交订单金
            runnerService.updateMoney(runner, publish, userService, runnerService, redisTemplate, token);
            return Result.success("订单确认完成");
        }
        //订单处于仲裁状态 清除仲裁数据和缓存 更新金额
        if (redisTemplate.opsForValue().get("publisherJudging_" + runner.getId()) != null) {
            redisTemplate.delete("publisherJudging_" + runner.getId());
            LambdaQueryWrapper<PublisherJudging> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PublisherJudging::getRunnerId, runner.getId());
            publisherJudgingService.remove(queryWrapper);
        }
        if (redisTemplate.opsForValue().get("acceptJudging_" + runner.getId()) != null) {
            redisTemplate.delete("acceptJudging_" + runner.getId());
            LambdaQueryWrapper<AcceptJudging> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AcceptJudging::getRunnerId, runner.getId());
            acceptJudgingService.remove(queryWrapper);
        }
        runnerService.updateMoney(runner, publish, userService, runnerService, redisTemplate, token);
        return Result.success("订单确认完成");
    }

    @GetMapping("/publishercancel/{id}")
    public Result<String> cancel(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //取消订单 修改订单状态
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 0 || runner.getStatus() == 4) {
            return Result.error("当前订单未进行");
        }
        if (runner.getStatus()==2){
            return Result.error("当前订单已被对方取消");
        }
        runner.setStatus(3);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        return Result.success("已取消 等待对方确认或仲裁");
    }
    @GetMapping("/publisherconfirmcancel/{id}")
    public Result<String> publisherConfirmCancel(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus()!=2){
            return Result.error("当前订单已完成");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,runner.getAcceptUserId());
        User ac = userService.getOne(queryWrapper);
        //确认取消 当前订单金额将退回原来账户 订单状态设置为已完成
        User publisher=(User) redisTemplate.opsForValue().get("user_"+token);
        BigDecimal reward = BigDecimal.valueOf(runner.getReward());
        BigDecimal needMoney = BigDecimal.valueOf(runner.getNeedMoney());
        BigDecimal publisherMoney = BigDecimal.valueOf(publisher.getMoney());
        BigDecimal publisherFreezeMoney = BigDecimal.valueOf(publisher.getFreezeMoney());
        BigDecimal acFreeze = BigDecimal.valueOf(ac.getFreezeMoney());
        BigDecimal acMoney = BigDecimal.valueOf(ac.getMoney());
        BigDecimal result = reward.add(needMoney);
        publisher.setMoney(publisherMoney.add(result).floatValue());
        publisher.setFreezeMoney(publisherFreezeMoney.subtract(result).floatValue());
        ac.setFreezeMoney(acFreeze.subtract(reward).floatValue());
        ac.setMoney(acMoney.add(reward).floatValue());
        userService.updateById(publisher);
        userService.updateById(ac);
        redisTemplate.opsForValue().set("user_"+token,publisher);
        if (redisTemplate.opsForValue().get("user_"+ac.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+ac.getToken(),ac);
        }
        runner.setStatus(4);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        //清除仲裁数据
        if (redisTemplate.opsForValue().get("publisherJudging_" + runner.getId()) != null) {
            redisTemplate.delete("publisherJudging_" + runner.getId());
            LambdaQueryWrapper<PublisherJudging> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(PublisherJudging::getRunnerId, runner.getId());
            publisherJudgingService.remove(queryWrapper1);
        }
        if (redisTemplate.opsForValue().get("acceptJudging_" + runner.getId()) != null) {
            redisTemplate.delete("acceptJudging_" + runner.getId());
            LambdaQueryWrapper<AcceptJudging> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(AcceptJudging::getRunnerId, runner.getId());
            acceptJudgingService.remove(queryWrapper1);
        }
        return Result.success("订单取消成功");
    }

    @GetMapping("/myaccept")
    public Result<Page> getMyAccept(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        User user =(User) redisTemplate.opsForValue().get("user_" + token);
        LambdaQueryWrapper<Runner> queryWrapper = new LambdaQueryWrapper<>();
        Page<Runner> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(queryInfo.getName() != null, Runner::getType, queryInfo.getName());
        queryWrapper.eq(queryInfo.getStatus() != null, Runner::getStatus, queryInfo.getStatus());
        queryWrapper.eq(Runner::getIsAcceptDelete, 0);
        queryWrapper.eq(Runner::getAcceptUserId,user.getId());
        runnerService.page(page, queryWrapper);
        return Result.success(page);

    }


    @GetMapping("/acceptcancel/{id}")
    public Result<String> acCancel(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        //取消订单 修改订单状态
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 0 || runner.getStatus() == 4) {
            return Result.error("当前订单未进行");
        }
        if (runner.getStatus()==3){
            return Result.error("当前订单已被对方取消");
        }
        runner.setStatus(2);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        return Result.success("已取消 等待对方确认或仲裁");
    }

    @GetMapping("/acceptconfirmcancel/{id}")
    public Result<String> acceptConfirmCancel(@PathVariable Integer id,HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus()!=3){
            return Result.error("当前订单已完成");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,runner.getPublishUserId());
        User publisher = userService.getOne(queryWrapper);
        //确认取消 当前订单金额将退回原来账户 订单状态设置为已完成
        User ac=(User) redisTemplate.opsForValue().get("user_"+token);
        BigDecimal reward = BigDecimal.valueOf(runner.getReward());
        BigDecimal needMoney = BigDecimal.valueOf(runner.getNeedMoney());
        BigDecimal publisherMoney = BigDecimal.valueOf(publisher.getMoney());
        BigDecimal publisherFreezeMoney = BigDecimal.valueOf(publisher.getFreezeMoney());
        BigDecimal acFreeze = BigDecimal.valueOf(ac.getFreezeMoney());
        BigDecimal acMoney = BigDecimal.valueOf(ac.getMoney());
        BigDecimal result = reward.add(needMoney);
        publisher.setMoney(publisherMoney.add(result).floatValue());
        publisher.setFreezeMoney(publisherFreezeMoney.subtract(result).floatValue());
        ac.setFreezeMoney(acFreeze.subtract(reward).floatValue());
        ac.setMoney(acMoney.add(reward).floatValue());
        userService.updateById(publisher);
        userService.updateById(ac);
        redisTemplate.opsForValue().set("user_"+token,ac);
        if (redisTemplate.opsForValue().get("user_"+publisher.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+publisher.getToken(),publisher);
        }
        runner.setStatus(4);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        //清除仲裁数据
        if (redisTemplate.opsForValue().get("publisherJudging_" + runner.getId()) != null) {
            redisTemplate.delete("publisherJudging_" + runner.getId());
            LambdaQueryWrapper<PublisherJudging> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(PublisherJudging::getRunnerId, runner.getId());
            publisherJudgingService.remove(queryWrapper1);
        }
        if (redisTemplate.opsForValue().get("acceptJudging_" + runner.getId()) != null) {
            redisTemplate.delete("acceptJudging_" + runner.getId());
            LambdaQueryWrapper<AcceptJudging> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(AcceptJudging::getRunnerId, runner.getId());
            acceptJudgingService.remove(queryWrapper1);
        }

        return Result.success("订单取消成功");
    }

    @DeleteMapping("/accept/{id}")
    public Result<String> acDelete(@PathVariable Integer id, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner = (Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 1 || runner.getStatus() == 2 || runner.getStatus() == 3 || runner.getStatus() == 0) {
            return Result.error("当前订单正在进行 无法删除");
        }
            runner.setIsAcceptDelete(1);
            runnerService.updateById(runner);
            redisTemplate.opsForValue().set("runner_" + runner.getId(), runner);

        return Result.success("删除成功");
    }


    @GetMapping("/judgingrunner")
    public Result<Page> getJudgingRunnerList(@ModelAttribute QueryInfo queryInfo, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        LambdaQueryWrapper<Runner> queryWrapper = new LambdaQueryWrapper<>();
        Page<Runner> page = new Page<>(queryInfo.getPageNum(), queryInfo.getPageSize());
        queryWrapper.like(StringUtils.isNotEmpty(queryInfo.getName()), Runner::getType, queryInfo.getName());
        queryWrapper.and(wrapper->wrapper.eq(Runner::getStatus,2).or().eq(Runner::getStatus,3));
        runnerService.page(page, queryWrapper);
        return Result.success(page);
    }
    @PutMapping("/returntopu/{id}")
    public Result<String> returnToPu(@RequestBody User user,HttpServletRequest request,@PathVariable Integer id){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }

        Runner runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 1 || runner.getStatus() == 4 || runner.getStatus() == 0) {
            return Result.error("当前订单并未申请仲裁");
        }
        if (user.getMoney()>runner.getReward()){
            return Result.error("扣除金额不能大于跑腿费");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,runner.getAcceptUserId());
        User ac = userService.getOne(queryWrapper);

        LambdaQueryWrapper<User>queryWrapper1=new LambdaQueryWrapper<>();
        queryWrapper1.eq(User::getId,runner.getPublishUserId());
        User pu = userService.getOne(queryWrapper1);

        BigDecimal result=new BigDecimal(Float.toString(runner.getNeedMoney())).add(new BigDecimal(Float.toString(runner.getReward())));
        BigDecimal reward=new BigDecimal(Float.toString(runner.getReward()));
        BigDecimal acFreeze=new BigDecimal(Float.toString(ac.getFreezeMoney()));
        BigDecimal acMoney=new BigDecimal(Float.toString(ac.getMoney()));
        BigDecimal puFreeze=new BigDecimal(Float.toString(pu.getFreezeMoney()));
        BigDecimal puMoney=new BigDecimal(Float.toString(pu.getMoney()));
        BigDecimal cutMoney=new BigDecimal(Float.toString(user.getMoney()));
        ac.setFreezeMoney(acFreeze.subtract(reward).floatValue());
        ac.setMoney(acMoney.add(reward.subtract(cutMoney)).floatValue());
        pu.setFreezeMoney(puFreeze.subtract(result).floatValue());
        pu.setMoney(puMoney.add(result).add(cutMoney).floatValue());

        userService.updateById(pu);
        userService.updateById(ac);
        if (redisTemplate.opsForValue().get("user_"+pu.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+pu.getToken(),pu);
        }
        if (redisTemplate.opsForValue().get("user_"+ac.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+ac.getToken(),ac);
        }
        runner.setStatus(4);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        //清除仲裁数据
        if (redisTemplate.opsForValue().get("publisherJudging_" + runner.getId()) != null) {
            redisTemplate.delete("publisherJudging_" + runner.getId());
            LambdaQueryWrapper<PublisherJudging> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(PublisherJudging::getRunnerId, runner.getId());
            publisherJudgingService.remove(queryWrapper2);
        }
        if (redisTemplate.opsForValue().get("acceptJudging_" + runner.getId()) != null) {
            redisTemplate.delete("acceptJudging_" + runner.getId());
            LambdaQueryWrapper<AcceptJudging> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(AcceptJudging::getRunnerId, runner.getId());
            acceptJudgingService.remove(queryWrapper2);
        }
        return Result.success("扣除成功");

    }

    @PutMapping("/returntoac/{id}")
    public Result<String> returnToAc(@RequestBody User user,HttpServletRequest request,@PathVariable Integer id){
        String token = request.getHeader("Authorization");
        if (checkOtherLogin.check(token) == 1) {
            return Result.loginError("账号在别处登录");
        } else if (checkOtherLogin.check(token) == 2) {
            return Result.loginError("登录信息过期");
        }
        Runner runner =(Runner) redisTemplate.opsForValue().get("runner_" + id);
        if (runner.getStatus() == 1 || runner.getStatus() == 4 || runner.getStatus() == 0) {
            return Result.error("当前订单并未申请仲裁");
        }
        BigDecimal result=new BigDecimal(Float.toString(runner.getNeedMoney())).add(new BigDecimal(Float.toString(runner.getReward())));
        BigDecimal cutMoney=new BigDecimal(Float.toString(user.getMoney()));
        int i = result.compareTo(cutMoney);
        if (i<0){
            return Result.error("返给接单者的钱不能超过订单总额");
        }
        LambdaQueryWrapper<User>queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId,runner.getAcceptUserId());
        User ac = userService.getOne(queryWrapper);

        LambdaQueryWrapper<User>queryWrapper1=new LambdaQueryWrapper<>();
        queryWrapper1.eq(User::getId,runner.getPublishUserId());
        User pu = userService.getOne(queryWrapper1);


        BigDecimal reward=new BigDecimal(Float.toString(runner.getReward()));
        BigDecimal acFreeze=new BigDecimal(Float.toString(ac.getFreezeMoney()));
        BigDecimal acMoney=new BigDecimal(Float.toString(ac.getMoney()));
        BigDecimal puFreeze=new BigDecimal(Float.toString(pu.getFreezeMoney()));
        BigDecimal puMoney=new BigDecimal(Float.toString(pu.getMoney()));

        ac.setFreezeMoney(acFreeze.subtract(reward).floatValue());
        ac.setMoney(acMoney.add(reward).add(cutMoney).floatValue());
        pu.setFreezeMoney(puFreeze.subtract(result).floatValue());
        pu.setMoney(puMoney.add(result.subtract(cutMoney)).floatValue());

        userService.updateById(pu);
        userService.updateById(ac);
        if (redisTemplate.opsForValue().get("user_"+pu.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+pu.getToken(),pu);
        }
        if (redisTemplate.opsForValue().get("user_"+ac.getToken())!=null){
            redisTemplate.opsForValue().set("user_"+ac.getToken(),ac);
        }
        runner.setStatus(4);
        runnerService.updateById(runner);
        redisTemplate.opsForValue().set("runner_"+id,runner);
        //清除仲裁数据
        if (redisTemplate.opsForValue().get("publisherJudging_" + runner.getId()) != null) {
            redisTemplate.delete("publisherJudging_" + runner.getId());
            LambdaQueryWrapper<PublisherJudging> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(PublisherJudging::getRunnerId, runner.getId());
            publisherJudgingService.remove(queryWrapper2);
        }
        if (redisTemplate.opsForValue().get("acceptJudging_" + runner.getId()) != null) {
            redisTemplate.delete("acceptJudging_" + runner.getId());
            LambdaQueryWrapper<AcceptJudging> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(AcceptJudging::getRunnerId, runner.getId());
            acceptJudgingService.remove(queryWrapper2);
        }
        return Result.success("扣除成功");

    }



}
