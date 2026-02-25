package com.malinghan.marpc.transport;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcNetworkException;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.NETWORK_ERROR;

public class OkHttpTransport implements RpcTransport {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;

    public OkHttpTransport(int timeoutMs) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public RpcResponse send(String instance, RpcRequest request) {
        try {
            String url = "http://" + instance + "/marpc";
            String body = JSON.toJSONString(request);
            Request httpRequest = new Request.Builder()
                    .url(url).post(RequestBody.create(body, JSON_TYPE)).build();
            try (Response resp = client.newCall(httpRequest).execute()) {
                String json = resp.body().string();
                return JSON.parseObject(json, RpcResponse.class);
            }
        } catch (Exception e) {
            throw new MarpcNetworkException(NETWORK_ERROR, "call failed: " + instance, e);
        }
    }
}
