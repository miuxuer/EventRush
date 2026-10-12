package com.miuxuer.eventrush.config;

import com.miuxuer.eventrush.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * <p>业务代码里抛出的运行时异常统一在这里兜住，转成 {@link Result} 返回给前端，
 * 避免把堆栈信息暴露出去，也省得每个 Controller 都写 try-catch。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 兜底处理运行时异常。
     *
     * @param e 捕获到的异常
     * @return 统一的失败响应，具体原因只记日志不返回给前端
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }
}
