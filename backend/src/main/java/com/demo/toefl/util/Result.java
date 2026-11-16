package com.demo.toefl.util;
import lombok.Data;

/**
 * 统一接口返回结果封装类
 * 所有controller接口全部返回Result，前端统一解析code、msg、data
 * @param <T> 泛型，兼容不同类型返回数据
 */
@Data
public class Result<T> {
    private Integer code;   // 状态码：200成功，500失败，401未登录
    private String msg;     // 提示信息
    private T data;         // 返回的数据体

    /**
     * 成功返回，携带数据
     * @param data 返回的数据
     * @return Result对象
     */
    public static <T> Result<T> success(T data){
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    /**
     * 成功返回，无返回数据
     */
    public static <T> Result<T> success(){
        return success(null);
    }

    /**
     * 失败返回
     * @param msg 失败提示文字
     * @return Result对象
     */
    public static <T> Result<T> fail(String msg){
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }
}
