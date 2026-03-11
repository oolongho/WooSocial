package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 传送冷却数据模型类
 * 存储玩家的传送冷却时间
 */
public class TeleportCooldown {
    
    private final UUID playerUuid;
    private long cooldownEndTime;
    private CooldownType cooldownType;
    
    /**
     * 冷却类型枚举
     */
    public enum CooldownType {
        TP_FRIEND,      // 好友传送冷却
        TP_REQUEST      // 传送请求冷却
    }
    
    /**
     * 构造函数
     * @param playerUuid 玩家UUID
     * @param cooldownSeconds 冷却时间（秒）
     */
    public TeleportCooldown(UUID playerUuid, int cooldownSeconds) {
        this.playerUuid = playerUuid;
        this.cooldownEndTime = System.currentTimeMillis() + cooldownSeconds * 1000L;
        this.cooldownType = CooldownType.TP_FRIEND;
    }
    
    /**
     * 完整构造函数
     */
    public TeleportCooldown(UUID playerUuid, long cooldownEndTime, CooldownType cooldownType) {
        this.playerUuid = playerUuid;
        this.cooldownEndTime = cooldownEndTime;
        this.cooldownType = cooldownType;
    }
    
    // Getters
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public long getCooldownEndTime() {
        return cooldownEndTime;
    }
    
    public CooldownType getCooldownType() {
        return cooldownType;
    }
    
    // Setters
    public void setCooldownEndTime(long cooldownEndTime) {
        this.cooldownEndTime = cooldownEndTime;
    }
    
    public void setCooldownType(CooldownType cooldownType) {
        this.cooldownType = cooldownType;
    }
    
    /**
     * 检查冷却是否结束
     * @return 冷却是否结束
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= cooldownEndTime;
    }
    
    /**
     * 获取剩余冷却时间（秒）
     * @return 剩余秒数，如果已过期返回0
     */
    public int getRemainingSeconds() {
        long remaining = cooldownEndTime - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }
    
    @Override
    public String toString() {
        return "TeleportCooldown{" +
                "playerUuid=" + playerUuid +
                ", cooldownEndTime=" + cooldownEndTime +
                ", cooldownType=" + cooldownType +
                '}';
    }
}
