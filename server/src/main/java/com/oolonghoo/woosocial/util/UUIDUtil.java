package com.oolonghoo.woosocial.util;

import java.util.UUID;

/**
 * UUID工具类
 * 提供UUID相关的实用方法
 */
public class UUIDUtil {
    
    /**
     * 将UUID转换为无连字符的字符串
     * @param uuid UUID对象
     * @return 无连字符的字符串
     */
    public static String toNoDashes(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
    
    /**
     * 将无连字符的字符串转换为UUID
     * @param noDashes 无连字符的字符串
     * @return UUID对象
     */
    public static UUID fromNoDashes(String noDashes) {
        if (noDashes.length() == 32) {
            String withDashes = noDashes.substring(0, 8) + "-" +
                    noDashes.substring(8, 12) + "-" +
                    noDashes.substring(12, 16) + "-" +
                    noDashes.substring(16, 20) + "-" +
                    noDashes.substring(20);
            return UUID.fromString(withDashes);
        }
        return UUID.fromString(noDashes);
    }
    
    /**
     * 验证字符串是否为有效的UUID
     * @param uuidString UUID字符串
     * @return 是否有效
     */
    public static boolean isValidUUID(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return false;
        }
        
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 安全地将字符串转换为UUID
     * @param uuidString UUID字符串
     * @return UUID对象，如果转换失败返回null
     */
    public static UUID safeFromString(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
