package com.oolonghoo.woosocial.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 传送设置数据模型
 * 存储玩家的传送相关配置和状态
 *
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportSettings {

    /**
     * 设置唯一标识符（数据库主键）
     */
    private long settingsId;

    /**
     * 玩家UUID
     */
    private UUID playerId;

    /**
     * 是否允许好友传送
     */
    private boolean allowFriendTeleport;

    /**
     * 是否允许陌生人传送请求
     */
    private boolean allowStrangerTeleport;

    /**
     * 是否需要确认传送
     */
    private boolean requireConfirmation;

    /**
     * 传送冷却时间（秒）
     */
    private int teleportCooldown;

    /**
     * 传送倒计时时间（秒）
     */
    private int teleportCountdown;

    /**
     * 是否在传送时保护玩家（无敌状态）
     */
    private boolean protectDuringTeleport;

    /**
     * 是否允许跨服传送
     */
    private boolean allowCrossServerTeleport;

    /**
     * 默认传送目标类型
     */
    private TeleportTargetType defaultTargetType;

    /**
     * 上次传送时间（用于冷却计算）
     */
    private long lastTeleportTime;
    
    /**
     * 针对单个好友的传送权限（null表示使用全局设置）
     */
    private Map<UUID, Boolean> friendTeleportPermissions = new HashMap<>();

    /**
     * 传送目标类型枚举
     */
    public enum TeleportTargetType {
        /**
         * 传送到好友位置
         */
        TO_FRIEND,
        /**
         * 好友传送到自己位置
         */
        FRIEND_TO_ME,
        /**
         * 传送到随机位置
         */
        RANDOM,
        /**
         * 传送到指定坐标
         */
        COORDINATES
    }

    /**
     * 默认传送冷却时间（秒）
     */
    public static final int DEFAULT_COOLDOWN = 60;

    /**
     * 默认传送倒计时（秒）
     */
    public static final int DEFAULT_COUNTDOWN = 5;

    /**
     * 无参构造函数（用于序列化）
     */
    public TeleportSettings() {
    }

    /**
     * 创建传送设置的构造函数
     *
     * @param playerId 玩家UUID
     */
    public TeleportSettings(UUID playerId) {
        this.playerId = playerId;
        this.allowFriendTeleport = true;
        this.allowStrangerTeleport = false;
        this.requireConfirmation = true;
        this.teleportCooldown = DEFAULT_COOLDOWN;
        this.teleportCountdown = DEFAULT_COUNTDOWN;
        this.protectDuringTeleport = true;
        this.allowCrossServerTeleport = true;
        this.defaultTargetType = TeleportTargetType.TO_FRIEND;
        this.lastTeleportTime = 0;
    }

    // ==================== Getter 和 Setter 方法 ====================

    public long getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(long settingsId) {
        this.settingsId = settingsId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public boolean isAllowFriendTeleport() {
        return allowFriendTeleport;
    }

    public void setAllowFriendTeleport(boolean allowFriendTeleport) {
        this.allowFriendTeleport = allowFriendTeleport;
    }

    public boolean isAllowStrangerTeleport() {
        return allowStrangerTeleport;
    }

    public void setAllowStrangerTeleport(boolean allowStrangerTeleport) {
        this.allowStrangerTeleport = allowStrangerTeleport;
    }

    public boolean isRequireConfirmation() {
        return requireConfirmation;
    }

    public void setRequireConfirmation(boolean requireConfirmation) {
        this.requireConfirmation = requireConfirmation;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }

    public void setTeleportCooldown(int teleportCooldown) {
        this.teleportCooldown = Math.max(0, teleportCooldown);
    }

    public int getTeleportCountdown() {
        return teleportCountdown;
    }

    public void setTeleportCountdown(int teleportCountdown) {
        this.teleportCountdown = Math.max(0, teleportCountdown);
    }

    public boolean isProtectDuringTeleport() {
        return protectDuringTeleport;
    }

    public void setProtectDuringTeleport(boolean protectDuringTeleport) {
        this.protectDuringTeleport = protectDuringTeleport;
    }

    public boolean isAllowCrossServerTeleport() {
        return allowCrossServerTeleport;
    }

    public void setAllowCrossServerTeleport(boolean allowCrossServerTeleport) {
        this.allowCrossServerTeleport = allowCrossServerTeleport;
    }

    public TeleportTargetType getDefaultTargetType() {
        return defaultTargetType;
    }

    public void setDefaultTargetType(TeleportTargetType defaultTargetType) {
        this.defaultTargetType = defaultTargetType;
    }

    public long getLastTeleportTime() {
        return lastTeleportTime;
    }

    public void setLastTeleportTime(long lastTeleportTime) {
        this.lastTeleportTime = lastTeleportTime;
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 检查是否可以传送（冷却时间检查）
     *
     * @return 是否可以传送
     */
    public boolean canTeleport() {
        if (teleportCooldown <= 0) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = teleportCooldown * 1000L;
        return currentTime - lastTeleportTime >= cooldownMillis;
    }

    /**
     * 获取剩余冷却时间（秒）
     *
     * @return 剩余冷却时间，如果无冷却返回0
     */
    public int getRemainingCooldown() {
        if (teleportCooldown <= 0 || lastTeleportTime == 0) {
            return 0;
        }
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = teleportCooldown * 1000L;
        long remaining = cooldownMillis - (currentTime - lastTeleportTime);
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    /**
     * 记录传送时间（更新冷却）
     */
    public void recordTeleport() {
        this.lastTeleportTime = System.currentTimeMillis();
    }

    /**
     * 重置冷却时间
     */
    public void resetCooldown() {
        this.lastTeleportTime = 0;
    }

    /**
     * 检查是否允许指定类型的传送
     *
     * @param isFriend 是否是好友
     * @return 是否允许传送
     */
    public boolean allowsTeleport(boolean isFriend) {
        if (isFriend) {
            return allowFriendTeleport;
        }
        return allowStrangerTeleport;
    }

    /**
     * 快速切换好友传送权限
     */
    public void toggleFriendTeleport() {
        this.allowFriendTeleport = !this.allowFriendTeleport;
    }

    /**
     * 快速切换陌生人传送权限
     */
    public void toggleStrangerTeleport() {
        this.allowStrangerTeleport = !this.allowStrangerTeleport;
    }

    /**
     * 快速切换传送确认要求
     */
    public void toggleRequireConfirmation() {
        this.requireConfirmation = !this.requireConfirmation;
    }

    /**
     * 快速切换传送保护
     */
    public void toggleProtection() {
        this.protectDuringTeleport = !this.protectDuringTeleport;
    }

    /**
     * 快速切换跨服传送
     */
    public void toggleCrossServerTeleport() {
        this.allowCrossServerTeleport = !this.allowCrossServerTeleport;
    }
    
    // ==================== 好友传送权限管理 ====================
    
    /**
     * 获取针对单个好友的传送权限
     * 
     * @param friendUuid 好友UUID
     * @return 权限设置，null表示使用全局设置
     */
    public Boolean getFriendTeleportPermission(UUID friendUuid) {
        return friendTeleportPermissions.get(friendUuid);
    }
    
    /**
     * 设置针对单个好友的传送权限
     * 
     * @param friendUuid 好友UUID
     * @param allow 是否允许
     */
    public void setFriendTeleportPermission(UUID friendUuid, boolean allow) {
        friendTeleportPermissions.put(friendUuid, allow);
    }
    
    /**
     * 移除针对单个好友的传送权限设置
     * 
     * @param friendUuid 好友UUID
     */
    public void removeFriendTeleportPermission(UUID friendUuid) {
        friendTeleportPermissions.remove(friendUuid);
    }
    
    /**
     * 检查是否允许指定好友传送
     * 
     * @param friendUuid 好友UUID
     * @return 是否允许
     */
    public boolean isAllowFriendTeleport(UUID friendUuid) {
        Boolean permission = friendTeleportPermissions.get(friendUuid);
        if (permission != null) {
            return permission;
        }
        return allowFriendTeleport;
    }
    
    /**
     * 获取所有好友传送权限设置
     * 
     * @return 权限映射
     */
    public Map<UUID, Boolean> getFriendTeleportPermissions() {
        return friendTeleportPermissions;
    }
    
    /**
     * 设置所有好友传送权限
     * 
     * @param permissions 权限映射
     */
    public void setFriendTeleportPermissions(Map<UUID, Boolean> permissions) {
        this.friendTeleportPermissions = permissions != null ? permissions : new HashMap<>();
    }

    @Override
    public String toString() {
        return "TeleportSettings{" +
                "playerId=" + playerId +
                ", allowFriendTeleport=" + allowFriendTeleport +
                ", allowStrangerTeleport=" + allowStrangerTeleport +
                ", teleportCooldown=" + teleportCooldown +
                ", teleportCountdown=" + teleportCountdown +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeleportSettings that = (TeleportSettings) o;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}
