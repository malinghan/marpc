package com.malinghan.marpc.registry;

import java.util.List;

public interface RegistryCenter {

    void start();
    void stop();
    void register(String service, String instance);
    void unregister(String service, String instance);
    List<String> fetchAll(String service);
    void subscribe(String service, ChangeListener listener);

    @FunctionalInterface
    interface ChangeListener {
        void onChange(List<String> newInstances);
    }
}
