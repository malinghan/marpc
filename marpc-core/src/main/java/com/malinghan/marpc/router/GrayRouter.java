package com.malinghan.marpc.router;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灰度路由：按比例将流量路由到灰度节点。
 *
 * <p>通过 {@link #markGray(String)} 标记灰度实例，
 * 配置 grayRatio（0-100）控制灰度流量比例。
 */
@Slf4j
public class GrayRouter implements Router {

    private final int grayRatio;
    private final Random random = new Random();
    private final Map<String, Boolean> grayInstances = new ConcurrentHashMap<>();

    /**
     * @param grayRatio 灰度流量比例（0-100），0 表示全部正常流量，100 表示全部灰度流量
     */
    public GrayRouter(int grayRatio) {
        if (grayRatio < 0 || grayRatio > 100) {
            throw new IllegalArgumentException("grayRatio 必须在 0-100 之间");
        }
        this.grayRatio = grayRatio;
    }

    /** 标记实例为灰度节点 */
    public GrayRouter markGray(String instance) {
        grayInstances.put(instance, true);
        log.info("[GrayRouter] 标记灰度实例: {}", instance);
        return this;
    }

    /** 取消灰度标记 */
    public void unmarkGray(String instance) {
        grayInstances.remove(instance);
        log.info("[GrayRouter] 取消灰度标记: {}", instance);
    }

    /** 清空所有灰度标记 */
    public void clearGray() {
        grayInstances.clear();
    }

    @Override
    public List<String> route(List<String> instances) {
        if (instances == null || instances.isEmpty()) {
            return instances;
        }

        List<String> grayList = new ArrayList<>();
        List<String> normalList = new ArrayList<>();

        for (String instance : instances) {
            if (grayInstances.containsKey(instance)) {
                grayList.add(instance);
            } else {
                normalList.add(instance);
            }
        }

        // 无灰度实例，返回全部正常实例
        if (grayList.isEmpty()) {
            return normalList;
        }

        // 无正常实例，返回全部灰度实例
        if (normalList.isEmpty()) {
            return grayList;
        }

        // 按比例路由
        int dice = random.nextInt(100);
        if (dice < grayRatio) {
            log.debug("[GrayRouter] 命中灰度流量（{}%），返回灰度实例: {}", grayRatio, grayList);
            return grayList;
        } else {
            log.debug("[GrayRouter] 命中正常流量，返回正常实例: {}", normalList);
            return normalList;
        }
    }

    @Override
    public int order() {
        return 10;
    }
}
