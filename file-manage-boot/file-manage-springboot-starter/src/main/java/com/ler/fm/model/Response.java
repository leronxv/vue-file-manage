package com.ler.fm.model;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * @author Leron
 */
@Data
public class Response<T> {

    public int code;

    private T data;

    private String message;

    private boolean success;

    public Response() {
    }

    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setCode(HttpStatus.OK.value());
        response.setData(data);
        return response;
    }

    public static Response<Void> success() {
        Response<Void> response = new Response<>();
        response.setCode(HttpStatus.OK.value());
        return response;
    }

    public static <T> Response<T> error(String message) {
        Response<T> response = new Response<>();
        response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage(message);
        return response;
    }

    public boolean isSuccess() {
        return this.code == HttpStatus.OK.value();
    }
}
