package com.malinghan.marpc.consumer;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarpcInvocationHandlerTest {

    interface HelloService {
        String hello(String name);
        int add(int a, int b);
    }

    private MarpcInvocationHandler handlerWithMockResponse(Object returnData) throws Exception {
        RpcResponse mockResp = RpcResponse.ok(returnData);
        String respJson = JSON.toJSONString(mockResp);

        ResponseBody body = ResponseBody.create(respJson, MediaType.get("application/json"));
        Response httpResp = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:8080/marpc").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(httpResp);

        OkHttpClient mockClient = mock(OkHttpClient.class);
        when(mockClient.newCall(any())).thenReturn(call);

        MarpcInvocationHandler handler = new MarpcInvocationHandler(HelloService.class, () -> "localhost:8080");
        Field clientField = MarpcInvocationHandler.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(handler, mockClient);

        return handler;
    }

    @Test
    void invoke_stringReturn_deserializedCorrectly() throws Throwable {
        MarpcInvocationHandler handler = handlerWithMockResponse("hello, world");
        HelloService proxy = (HelloService) Proxy.newProxyInstance(
                HelloService.class.getClassLoader(), new Class[]{HelloService.class}, handler);

        assertEquals("hello, world", proxy.hello("world"));
    }

    @Test
    void invoke_intReturn_deserializedCorrectly() throws Throwable {
        MarpcInvocationHandler handler = handlerWithMockResponse(7);
        HelloService proxy = (HelloService) Proxy.newProxyInstance(
                HelloService.class.getClassLoader(), new Class[]{HelloService.class}, handler);

        assertEquals(7, proxy.add(3, 4));
    }

    @Test
    void invoke_errorResponse_throwsRuntimeException() throws Exception {
        RpcResponse errResp = RpcResponse.error("service not found");
        ResponseBody body = ResponseBody.create(JSON.toJSONString(errResp), MediaType.get("application/json"));
        Response httpResp = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:8080/marpc").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body).build();

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(httpResp);
        OkHttpClient mockClient = mock(OkHttpClient.class);
        when(mockClient.newCall(any())).thenReturn(call);

        MarpcInvocationHandler handler = new MarpcInvocationHandler(HelloService.class, () -> "localhost:8080");
        Field clientField = MarpcInvocationHandler.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(handler, mockClient);

        HelloService proxy = (HelloService) Proxy.newProxyInstance(
                HelloService.class.getClassLoader(), new Class[]{HelloService.class}, handler);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> proxy.hello("world"));
        assertEquals("service not found", ex.getMessage());
    }

    @Test
    void invoke_requestContainsMethodSign() throws Exception {
        MarpcInvocationHandler handler = handlerWithMockResponse("hello, world");

        OkHttpClient mockClient = mock(OkHttpClient.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        RpcResponse mockResp = RpcResponse.ok("hello, world");
        ResponseBody body = ResponseBody.create(JSON.toJSONString(mockResp), MediaType.get("application/json"));
        Response httpResp = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:8080/marpc").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body).build();
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(httpResp);
        when(mockClient.newCall(requestCaptor.capture())).thenReturn(call);

        Field clientField = MarpcInvocationHandler.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(handler, mockClient);

        HelloService proxy = (HelloService) Proxy.newProxyInstance(
                HelloService.class.getClassLoader(), new Class[]{HelloService.class}, handler);
        proxy.hello("world");

        Request captured = requestCaptor.getValue();
        Buffer buffer = new Buffer();
        captured.body().writeTo(buffer);
        RpcRequest rpcReq = JSON.parseObject(buffer.readUtf8(), RpcRequest.class);

        assertEquals(HelloService.class.getCanonicalName(), rpcReq.getService());
        assertEquals("hello", rpcReq.getMethod());
        assertEquals("hello@1_java.lang.String", rpcReq.getMethodSign());
    }
}
