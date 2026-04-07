package com.ben.workflow.util;

import java.util.Map;

/**
 * 节点执行器通用配置参数提取工具类
 * <p>
 * 消除各 Executor 中重复的 getStr/getInt/getDouble/getStringValue/getIntValue 等方法。
 * 所有 NodeExecutor 实现统一使用本工具类。
 * 
 * @author 龙傲天 (refactoring)
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * 获取 String 类型参数
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    /**
     * 获取 int 类型参数
     */
    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 double 类型参数
     */
    public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 Object 类型参数
     */
    public static Object getObject(Map<String, Object> map, String key, Object defaultValue) {
        if (map == null) return defaultValue;
        Object v = map.get(key);
        return v != null ? v : defaultValue;
    }
}
