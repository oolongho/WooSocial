package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 玩家数据模型类
 * 存储玩家的基础信息和设置
 */
public class PlayerData {
    
    private final UUID uuid;
    private String lastName;
    private long firstJoinTime;
    private long lastOnlineTime;
    private boolean notifyOnline;
    private boolean allowTeleport;
    private String settings;
    
    /**
     * 构造函数
     * @param uuid 玩家UUID
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.notifyOnline = true;
        this.allowTeleport = true;
        this.settings = "{}";
    }
    
    /**
     * 完整构造函数
     */
    public PlayerData(UUID uuid, String lastName, long firstJoinTime, long lastOnlineTime,
                      boolean notifyOnline, boolean allowTeleport, String settings) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.firstJoinTime = firstJoinTime;
        this.lastOnlineTime = lastOnlineTime;
        this.notifyOnline = notifyOnline;
        this.allowTeleport = allowTeleport;
        this.settings = settings;
    }
    
    // Getters
    public UUID getUuid() {
        return uuid;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public long getFirstJoinTime() {
        return firstJoinTime;
    }
    
    public long getLastOnlineTime() {
        return lastOnlineTime;
    }
    
    public boolean isNotifyOnline() {
        return notifyOnline;
    }
    
    public boolean isAllowTeleport() {
        return allowTeleport;
    }
    
    public String getSettings() {
        return settings;
    }
    
    // Setters
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setFirstJoinTime(long firstJoinTime) {
        this.firstJoinTime = firstJoinTime;
    }
    
    public void setLastOnlineTime(long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }
    
    public void setNotifyOnline(boolean notifyOnline) {
        this.notifyOnline = notifyOnline;
    }
    
    public void setAllowTeleport(boolean allowTeleport) {
        this.allowTeleport = allowTeleport;
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", lastName='" + lastName + '\'' +
                ", firstJoinTime=" + firstJoinTime +
                ", lastOnlineTime=" + lastOnlineTime +
                ", notifyOnline=" + notifyOnline +
                ", allowTeleport=" + allowTeleport +
                '}';
    }
}
