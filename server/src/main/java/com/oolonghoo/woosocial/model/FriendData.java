package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 好友关系数据模型类
 * 存储两个玩家之间的好友关系
 */
public class FriendData {
    
    private final UUID playerUuid;
    private final UUID friendUuid;
    private long addTime;
    private String friendName;
    private boolean favorite;
    private String nickname;
    private boolean receiveMessages;
    private boolean notifyOnline;
    
    public FriendData(UUID playerUuid, UUID friendUuid) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.addTime = System.currentTimeMillis();
        this.favorite = false;
        this.nickname = null;
        this.receiveMessages = true;
        this.notifyOnline = true;
    }
    
    public FriendData(UUID playerUuid, UUID friendUuid, long addTime, String friendName) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.addTime = addTime;
        this.friendName = friendName;
        this.favorite = false;
        this.nickname = null;
        this.receiveMessages = true;
        this.notifyOnline = true;
    }
    
    public FriendData(UUID playerUuid, UUID friendUuid, long addTime, String friendName, boolean favorite, String nickname) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.addTime = addTime;
        this.friendName = friendName;
        this.favorite = favorite;
        this.nickname = nickname;
        this.receiveMessages = true;
        this.notifyOnline = true;
    }
    
    public FriendData(UUID playerUuid, UUID friendUuid, long addTime, String friendName, 
                      boolean favorite, String nickname, boolean receiveMessages) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.addTime = addTime;
        this.friendName = friendName;
        this.favorite = favorite;
        this.nickname = nickname;
        this.receiveMessages = receiveMessages;
        this.notifyOnline = true;
    }
    
    public FriendData(UUID playerUuid, UUID friendUuid, long addTime, String friendName, 
                      boolean favorite, String nickname, boolean receiveMessages, boolean notifyOnline) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.addTime = addTime;
        this.friendName = friendName;
        this.favorite = favorite;
        this.nickname = nickname;
        this.receiveMessages = receiveMessages;
        this.notifyOnline = notifyOnline;
    }
    
    // Getters
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public UUID getFriendUuid() {
        return friendUuid;
    }
    
    public long getAddTime() {
        return addTime;
    }
    
    public String getFriendName() {
        return friendName;
    }
    
    public boolean isFavorite() {
        return favorite;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public String getDisplayName() {
        return nickname != null && !nickname.isEmpty() ? nickname : friendName;
    }
    
    public boolean isReceiveMessages() {
        return receiveMessages;
    }
    
    public boolean isNotifyOnline() {
        return notifyOnline;
    }
    
    // Setters
    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }
    
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }
    
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public void setReceiveMessages(boolean receiveMessages) {
        this.receiveMessages = receiveMessages;
    }
    
    public void setNotifyOnline(boolean notifyOnline) {
        this.notifyOnline = notifyOnline;
    }
    
    @Override
    public String toString() {
        return "FriendData{" +
                "playerUuid=" + playerUuid +
                ", friendUuid=" + friendUuid +
                ", addTime=" + addTime +
                ", friendName='" + friendName + '\'' +
                ", favorite=" + favorite +
                ", nickname='" + nickname + '\'' +
                ", receiveMessages=" + receiveMessages +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FriendData that = (FriendData) obj;
        return playerUuid.equals(that.playerUuid) && friendUuid.equals(that.friendUuid);
    }
    
    @Override
    public int hashCode() {
        return 31 * playerUuid.hashCode() + friendUuid.hashCode();
    }
}
