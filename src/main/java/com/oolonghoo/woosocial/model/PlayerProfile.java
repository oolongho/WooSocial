package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 玩家档案数据模型
 * 存储玩家的基本信息和社交设置
 *
 * @author oolongho
 * @since 1.0.0
 */
public class PlayerProfile {

    /**
     * 玩家唯一标识符
     */
    private UUID playerId;

    /**
     * 玩家名称
     */
    private String playerName;

    /**
     * 玩家显示名称（可能包含颜色代码）
     */
    private String displayName;

    /**
     * 首次加入时间（时间戳）
     */
    private long firstJoinTime;

    /**
     * 最后在线时间（时间戳）
     */
    private long lastOnlineTime;

    /**
     * 是否接受好友请求
     */
    private boolean acceptFriendRequests;

    /**
     * 是否接受传送请求
     */
    private boolean acceptTeleportRequests;

    /**
     * 是否在线
     */
    private boolean online;

    /**
     * 当前所在服务器名称（用于跨服同步）
     */
    private String currentServer;

    /**
     * 自定义状态消息
     */
    private String statusMessage;

    /**
     * 无参构造函数（用于序列化）
     */
    public PlayerProfile() {
    }

    /**
     * 创建新玩家档案的构造函数
     *
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     */
    public PlayerProfile(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.displayName = playerName;
        this.firstJoinTime = System.currentTimeMillis();
        this.lastOnlineTime = System.currentTimeMillis();
        this.acceptFriendRequests = true;
        this.acceptTeleportRequests = true;
        this.online = false;
        this.statusMessage = "";
    }

    // ==================== Getter 和 Setter 方法 ====================

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getFirstJoinTime() {
        return firstJoinTime;
    }

    public void setFirstJoinTime(long firstJoinTime) {
        this.firstJoinTime = firstJoinTime;
    }

    public long getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public boolean isAcceptFriendRequests() {
        return acceptFriendRequests;
    }

    public void setAcceptFriendRequests(boolean acceptFriendRequests) {
        this.acceptFriendRequests = acceptFriendRequests;
    }

    public boolean isAcceptTeleportRequests() {
        return acceptTeleportRequests;
    }

    public void setAcceptTeleportRequests(boolean acceptTeleportRequests) {
        this.acceptTeleportRequests = acceptTeleportRequests;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
        if (online) {
            this.lastOnlineTime = System.currentTimeMillis();
        }
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 更新最后在线时间
     */
    public void updateLastOnlineTime() {
        this.lastOnlineTime = System.currentTimeMillis();
    }

    /**
     * 检查玩家是否允许好友请求
     *
     * @return 是否允许
     */
    public boolean canReceiveFriendRequest() {
        return acceptFriendRequests && online;
    }

    /**
     * 检查玩家是否允许传送请求
     *
     * @return 是否允许
     */
    public boolean canReceiveTeleportRequest() {
        return acceptTeleportRequests && online;
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", online=" + online +
                ", currentServer='" + currentServer + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerProfile that = (PlayerProfile) o;
        return playerId != null && playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() {
        return playerId != null ? playerId.hashCode() : 0;
    }
}
