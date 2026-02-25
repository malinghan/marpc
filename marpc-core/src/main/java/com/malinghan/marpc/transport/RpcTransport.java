package com.malinghan.marpc.transport;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;

public interface RpcTransport {
    RpcResponse send(String instance, RpcRequest request);
}
