package com.oolonghoo.woosocial.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyGiftData {
    
    private int id;
    private final UUID playerUuid;
    private final UUID targetUuid;
    private final String date;
    private int coinsSent;
    private Map<String, Integer> giftsSent;
    
    public DailyGiftData(UUID playerUuid, UUID targetUuid, String date) {
        this.playerUuid = playerUuid;
        this.targetUuid = targetUuid;
        this.date = date;
        this.coinsSent = 0;
        this.giftsSent = new HashMap<>();
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public String getDate() {
        return date;
    }
    
    public int getCoinsSent() {
        return coinsSent;
    }
    
    public void setCoinsSent(int coinsSent) {
        this.coinsSent = coinsSent;
    }
    
    public void addCoinsSent(int amount) {
        this.coinsSent += amount;
    }
    
    public Map<String, Integer> getGiftsSent() {
        return giftsSent;
    }
    
    public void setGiftsSent(Map<String, Integer> giftsSent) {
        this.giftsSent = giftsSent != null ? new HashMap<>(giftsSent) : new HashMap<>();
    }
    
    public int getGiftCount(String giftId) {
        return giftsSent.getOrDefault(giftId, 0);
    }
    
    public void addGiftSent(String giftId, int amount) {
        giftsSent.merge(giftId, amount, Integer::sum);
    }
    
    public String getGiftsJson() {
        if (giftsSent.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : giftsSent.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    public static Map<String, Integer> parseGiftsJson(String json) {
        Map<String, Integer> result = new HashMap<>();
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return result;
        }
        try {
            String content = json.substring(1, json.length() - 1);
            if (content.isEmpty()) {
                return result;
            }
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].replace("\"", "");
                    int value = Integer.parseInt(kv[1]);
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            return result;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "DailyGiftData{" +
                "id=" + id +
                ", playerUuid=" + playerUuid +
                ", targetUuid=" + targetUuid +
                ", date='" + date + '\'' +
                ", coinsSent=" + coinsSent +
                ", giftsSent=" + giftsSent +
                '}';
    }
}
