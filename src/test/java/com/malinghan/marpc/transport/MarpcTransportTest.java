package com.malinghan.marpc.transport;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.provider.ProviderBootstrap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MarpcTransportTest {

    private MockMvc mockMvc;
    private ProviderBootstrap providerBootstrap;

    @BeforeEach
    void setUp() {
        providerBootstrap = mock(ProviderBootstrap.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MarpcTransport(providerBootstrap)).build();
    }

    @Test
    void invoke_successResponse() throws Exception {
        when(providerBootstrap.invoke(any())).thenReturn(RpcResponse.ok("hello, world"));

        RpcRequest req = new RpcRequest();
        req.setService("com.example.HelloService");
        req.setMethod("hello");
        req.setArgs(new Object[]{"world"});

        mockMvc.perform(post("/marpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").value("hello, world"));
    }

    @Test
    void invoke_errorResponse() throws Exception {
        when(providerBootstrap.invoke(any())).thenReturn(RpcResponse.error("service not found"));

        RpcRequest req = new RpcRequest();
        req.setService("com.example.NonExistent");
        req.setMethod("foo");
        req.setArgs(null);

        mockMvc.perform(post("/marpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.errorMessage").value("service not found"));
    }
}
