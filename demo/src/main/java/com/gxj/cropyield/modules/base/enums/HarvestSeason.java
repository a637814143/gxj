package com.gxj.cropyield.modules.base.enums;

/**
 * 标识主要的农作物收获季型，用于按季节窗口聚合天气特征。
 */
public enum HarvestSeason {

    /**
     * 常规全年性作物，使用全年天气统计。
     */
    ANNUAL,

    /**
     * 夏收粮（如冬小麦、早熟油菜），跨年越冬，重点关注冬春到夏初天气。
     */
    SUMMER_GRAIN,

    /**
     * 秋收粮（如玉米、中晚稻），主要关注同年春播至秋收的天气。
     */
    AUTUMN_GRAIN
}
