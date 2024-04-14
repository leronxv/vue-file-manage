package com.ler.fm.exception;

import org.springframework.http.HttpStatus;

/**
 * @author Leron
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = HttpStatus.OK.value();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
