package com.miuxuer.eventrush.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一的接口返回结果封装，所有 Controller 的出参都用它包一层。
 * <p>
 * 通过 success 表示业务是否成功，失败时 errorMsg 给出原因；data 承载业务数据，
 * 分页场景的数据放在 data 中、总记录数放在 total 中。建议使用下面的静态工厂方法创建实例。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    /**
     * 是否成功，true：成功；false：失败
     */
    private Boolean success;

    /**
     * 失败时的错误提示信息
     */
    private String errorMsg;

    /**
     * 返回的业务数据
     */
    private Object data;

    /**
     * 总记录数，分页查询时使用
     */
    private Long total;

    /**
     * 构造一个不带任何数据的成功结果。
     *
     * @return 成功且 data 为 null 的 Result
     */
    public static Result ok(){
        return new Result(true, null, null, null);
    }

    /**
     * 构造一个携带业务数据的成功结果。
     *
     * @param data 需要返回给前端的数据
     * @return 携带数据的成功结果
     */
    public static Result ok(Object data){
        return new Result(true, null, data, null);
    }

    /**
     * 构造一个携带列表数据和总记录数的成功结果，用于分页查询。
     *
     * @param data  当前页的数据列表
     * @param total 满足条件的总记录数
     * @return 携带分页数据的成功结果
     */
    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total);
    }

    /**
     * 构造一个失败结果。
     *
     * @param errorMsg 失败原因，会直接提示给前端用户
     * @return 失败结果
     */
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null);
    }
}
