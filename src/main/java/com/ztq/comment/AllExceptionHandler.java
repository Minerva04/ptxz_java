package com.ztq.comment;

import com.ztq.entity.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AllExceptionHandler {

  /*  @ExceptionHandler(Exception.class)
    public Result<String> exceptionHandler(Exception e){
        return Result.error("系统繁忙请稍后再试");
    }*/
}
