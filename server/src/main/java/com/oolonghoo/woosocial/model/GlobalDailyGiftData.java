package com.oolonghoo.woosocial.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlobalDailyGiftData {
    
    private int id;
    private final UUID playerUuid;
    private final String date;
    private Map<String, Integer> giftsSent;
    
    public GlobalDailyGiftData(UUID playerUuid, String date) {
        this.playerUuid = playerUuid;
        this.date = date;
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
    
    public String getDate() {
        return date;
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
        } catch (RuntimeException e) {
            return result;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "GlobalDailyGiftData{" +
                "id=" + id +
                ", playerUuid=" + playerUuid +
                ", date='" + date + '\'' +
                ", giftsSent=" + giftsSent +
                '}';
    }
}
