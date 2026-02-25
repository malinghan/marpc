package com.malinghan.marpc.router;

import java.util.List;

/**
 * 路由接口：根据策略筛选实例列表。
 */
public interface Router {

    /**
     * 路由筛选：从候选实例中选择符合条件的实例。
     * @param instances 候选实例列表
     * @return 筛选后的实例列表
     */
    List<String> route(List<String> instances);

    /**
     * 路由优先级，数值越小越先执行。默认 0。
     */
    default int order() {
        return 0;
    }
}
