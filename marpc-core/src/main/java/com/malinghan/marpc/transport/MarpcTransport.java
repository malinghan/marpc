package com.malinghan.marpc.transport;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.provider.ProviderBootstrap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarpcTransport {

    private final ProviderBootstrap providerBootstrap;

    public MarpcTransport(ProviderBootstrap providerBootstrap) {
        this.providerBootstrap = providerBootstrap;
    }

    @PostMapping("/marpc")
    public RpcResponse invoke(@RequestBody RpcRequest request) {
        try {
            return providerBootstrap.invoke(request);
        } catch (MarpcBizException e) {
            return RpcResponse.error(e.getErrorCode() + ": " + e.getMessage());
        }
    }
}
