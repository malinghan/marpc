package com.malinghan.marpc.core;

import lombok.Data;

@Data
public class RpcRequest {
    private String service;
    private String method;
    /** 方法签名：methodName@paramCount_type1_type2 */
    private String methodSign;
    private Object[] args;
}
