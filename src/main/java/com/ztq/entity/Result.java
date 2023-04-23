package com.ztq.entity;

import lombok.Data;

import java.io.Serializable;

@Data

public class Result<T> implements Serializable {
    private  Integer code;

    private  String msg;

    private  T data;

    public static <T> Result<T> success(T object){
       Result<T> result=new Result<>();
       result.code=1;
       result.data=object;
       return result;
    }

    public static Result loginError(String msg){
        Result result=new Result();
        result.code=2;
        result.msg=msg;
        return result;
    }

    public static Result error(String msg){
        Result result=new Result();
        result.code=0;
        result.msg=msg;
        return result;
    }

}
