package com.lingrui.usermanagement.exception;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException e
    ){

        //获取抛出异常时填写的提示
        String message = e.getReason();

        //防止异常没有填写提示内容
        if(message == null || message.isBlank()){
            message = "请求处理失败";
        }

        //构造统一的响应内容
        Map<String, String> result = new HashMap<>();
        result.put("message", message);

        //保留原异常的 HTTP 状态码，同时返回 JSON 内容
        return ResponseEntity
                .status(e.getStatusCode())
                .body(result);
    }

    //处理没有预料到的异常，例如数据库连接失败
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleSystemException(
            Exception e
    ){

        // 请求体缺失或 JSON 格式错误，返回400
        if (e instanceof HttpMessageNotReadableException) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "请求体缺失或JSON格式不正确"));
        }

        // Spring 已确定状态码的异常，保留其状态码和响应头
        if (e instanceof ErrorResponse error) {
            return ResponseEntity
                    .status(error.getStatusCode())
                    .headers(error.getHeaders())
                    .body(Map.of("message", "请求处理失败"));
        }

        //详细错误在后端日志，方便查看
        log.error("处理请求时发生异常" ,e);

        return ResponseEntity
                .status(500)
                .body(Map.of("message", "服务器内部异常，请稍后重试"));
    }
}
