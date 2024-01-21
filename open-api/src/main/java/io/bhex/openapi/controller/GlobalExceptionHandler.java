/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.controller
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.controller;

import com.google.common.base.Strings;
import io.bhex.broker.common.entity.ErrorRet;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.util.ErrorCodeConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.Iterator;
import java.util.StringJoiner;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String HEADER_ERROR_CODE = "error_code";
    @Resource
    private MessageSource messageSource;

    @ExceptionHandler({ServletRequestBindingException.class,
            HttpRequestMethodNotSupportedException.class,
            MissingServletRequestParameterException.class,
            TypeMismatchException.class})
    public ResponseEntity<String> handleIllegalRequestException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        String errorMessage = "";
        if (e instanceof HttpRequestMethodNotSupportedException) {
            log.warn("request method not support, 30002, url:{} msg:{}", request.getRequestURI(), e.getMessage());
        } else if (e instanceof MissingServletRequestParameterException) {
            errorMessage = String.format("Missing required parameter '%s'", ((MissingServletRequestParameterException) e).getParameterName());
            log.warn("request missing param name, 30002, url:{} msg:{}", request.getRequestURI(), e.getMessage());
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            log.warn("request arguments type mismatch, 30002, url:{} msg:{}", request.getRequestURI(), e.getMessage());
        } else if (e instanceof TypeMismatchException) {
            log.warn("request type mismatch, 30002, url:{} msg:{}", request.getRequestURI(), e.getMessage());
        } else {
            log.error(String.format("requestUri:[%s] 30002:", request.getRequestURI()), e);
        }
        return retResponseEntity(ApiErrorCode.BAD_REQUEST.getCode(), Strings.isNullOrEmpty(errorMessage) ?
                ApiErrorCode.BAD_REQUEST.getMsg() : errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BrokerException.class)
    public ResponseEntity<String> handleBrokerException(BrokerException e,
                                                        HttpServletRequest request, HttpServletResponse response) {
        ApiErrorCode apiErrorCode = ErrorCodeConvertor.convert(
                BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNEXPECTED_RESP);
        if (apiErrorCode == ApiErrorCode.UNEXPECTED_RESP) {
            log.error("catch a BrokerException, code:{} message: {}", e.getCode(), e.getErrorMessage());
        }
        return retResponseEntity(apiErrorCode.getCode(), apiErrorCode.getMsg(), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理OPENAPI返回的异常
     *
     * @param e
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler(OpenApiException.class)
    public ResponseEntity<String> handleOpenApiException(OpenApiException e,
                                                         HttpServletRequest request, HttpServletResponse response) {
        String errorMsg;
        if (e.getMessageArgs() == null || e.getMessageArgs().length <= 0) {
            errorMsg = e.getMessage();
        } else {
            errorMsg = String.format(e.getMessage(), e.getMessageArgs());
        }
        if (e.getCode() == ApiErrorCode.TOO_MANY_ORDERS.getCode() || e.getCode() == ApiErrorCode.TOO_MANY_REQUESTS.getCode()) {
            return retResponseEntity(e.getCode(), errorMsg, HttpStatus.TOO_MANY_REQUESTS);
        }
        return retResponseEntity(e.getCode(), errorMsg, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<String> handleOtherException(Exception e,
                                                       HttpServletRequest request, HttpServletResponse response) {
        if (e.getCause() instanceof BrokerException) {
            return handleBrokerException((BrokerException) e.getCause(), request, response);
        } else if (e.getCause() instanceof OpenApiException) {
            return handleOpenApiException((OpenApiException) e.getCause(), request, response);
        } else {
            log.error(String.format("requestUri:[%s] 30001:", request.getRequestURI()), e);
            return retResponseEntity(ApiErrorCode.DISCONNECTED.getCode(),
                    ApiErrorCode.DISCONNECTED.getMsg(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<String> retResponseEntity(Integer errorCode, String msg, HttpStatus httpStatus) {
        ErrorRet errorRet = new ErrorRet();
        errorRet.setCode(errorCode);
        errorRet.setMsg(msg);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.set(HEADER_ERROR_CODE, String.valueOf(errorCode));

        return new ResponseEntity<String>(JsonUtil.defaultGson().toJson(errorRet), headers, httpStatus);
    }

    /**
     * 处理校验异常
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request, HttpServletResponse response) {
        //取第一个
        ConstraintViolation violation = exception.getConstraintViolations().stream().findFirst().get();
        Path path = violation.getPropertyPath();
        StringJoiner sj = new StringJoiner(",");
        int index = 0;
        Iterator<Path.Node> it = path.iterator();
        //取字段名称
        while (it.hasNext()) {
            Path.Node node = it.next();
            if (++index > 1) {
                sj.add(node.getName());
            }
        }
        String errorMsg = String.format(ApiErrorCode.INVALID_PARAMETER.getMsg(), sj.toString());
        return retResponseEntity(ApiErrorCode.INVALID_PARAMETER.getCode(), errorMsg, HttpStatus.BAD_REQUEST);
    }

}
