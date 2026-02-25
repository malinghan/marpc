package com.malinghan.marpc.core;

import lombok.Data;

@Data
public class RpcResponse {
    private boolean status;
    private Object data;
    private String errorMessage;

    public static RpcResponse ok(Object data) {
        RpcResponse r = new RpcResponse();
        r.status = true;
        r.data = data;
        return r;
    }

    public static RpcResponse error(String message) {
        RpcResponse r = new RpcResponse();
        r.status = false;
        r.errorMessage = message;
        return r;
    }
}
