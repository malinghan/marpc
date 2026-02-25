package com.malinghan.marpc.registry;

import java.util.List;

/**
 * 注册中心抽象接口，支持服务注册、注销、发现和订阅。
 */
public interface RegistryCenter {

    /** 启动注册中心连接 */
    void start();

    /** 关闭注册中心连接 */
    void stop();

    /** Provider 注册服务实例 */
    void register(String service, String instance);

    /** Provider 注销服务实例 */
    void unregister(String service, String instance);

    /** Consumer 获取服务的所有实例列表 */
    List<String> fetchAll(String service);

    /** Consumer 订阅服务变更，变更时回调 listener */
    void subscribe(String service, ChangeListener listener);

    @FunctionalInterface
    interface ChangeListener {
        void onChange(List<String> newInstances);
    }
}
