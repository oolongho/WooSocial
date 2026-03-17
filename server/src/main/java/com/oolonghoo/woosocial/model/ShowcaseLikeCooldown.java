package com.oolonghoo.woosocial.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShowcaseLikeCooldown {
    
    private UUID playerUuid;
    private long lastLikeTime;
    private int dailyCount;
    private long dailyResetTime;
    private Map<UUID, Integer> playerDailyLikes;
    
    public ShowcaseLikeCooldown(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.lastLikeTime = 0;
        this.dailyCount = 0;
        this.dailyResetTime = System.currentTimeMillis();
        this.playerDailyLikes = new HashMap<>();
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public long getLastLikeTime() {
        return lastLikeTime;
    }
    
    public void setLastLikeTime(long lastLikeTime) {
        this.lastLikeTime = lastLikeTime;
    }
    
    public int getDailyCount() {
        return dailyCount;
    }
    
    public void setDailyCount(int dailyCount) {
        this.dailyCount = dailyCount;
    }
    
    public void incrementDailyCount() {
        this.dailyCount++;
    }
    
    public long getDailyResetTime() {
        return dailyResetTime;
    }
    
    public void setDailyResetTime(long dailyResetTime) {
        this.dailyResetTime = dailyResetTime;
    }
    
    public Map<UUID, Integer> getPlayerDailyLikes() {
        return playerDailyLikes;
    }
    
    public void setPlayerDailyLikes(Map<UUID, Integer> playerDailyLikes) {
        this.playerDailyLikes = playerDailyLikes;
    }
    
    public int getPlayerDailyLikeCount(UUID targetUuid) {
        return playerDailyLikes.getOrDefault(targetUuid, 0);
    }
    
    public void incrementPlayerDailyLike(UUID targetUuid) {
        playerDailyLikes.put(targetUuid, getPlayerDailyLikeCount(targetUuid) + 1);
    }
    
    public void resetDailyData() {
        this.dailyCount = 0;
        this.dailyResetTime = System.currentTimeMillis();
        this.playerDailyLikes.clear();
    }
    
    public boolean shouldResetDaily() {
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000L;
        return currentTime - dailyResetTime >= oneDayInMillis;
    }
}
