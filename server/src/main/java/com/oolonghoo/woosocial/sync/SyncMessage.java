package com.oolonghoo.woosocial.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncMessage {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final SyncMessageType type;
    private final String sourceServer;
    private final long timestamp;
    private final Map<String, Object> data;
    
    public SyncMessage(SyncMessageType type, String sourceServer) {
        this.type = type;
        this.sourceServer = sourceServer;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }
    
    public SyncMessage(SyncMessageType type, String sourceServer, long timestamp) {
        this.type = type;
        this.sourceServer = sourceServer;
        this.timestamp = timestamp;
        this.data = new HashMap<>();
    }
    
    public SyncMessageType getType() {
        return type;
    }
    
    public String getSourceServer() {
        return sourceServer;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public SyncMessage set(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    public UUID getUUID(String key) {
        Object value = data.get(key);
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public long getLong(String key) {
        Object value = data.get(key);
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("sourceServer", sourceServer);
        json.addProperty("timestamp", timestamp);
        json.add("data", GSON.toJsonTree(data));
        return GSON.toJson(json);
    }
    
    public static SyncMessage fromJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            SyncMessageType type = SyncMessageType.valueOf(obj.get("type").getAsString());
            String sourceServer = obj.get("sourceServer").getAsString();
            long timestamp = obj.get("timestamp").getAsLong();
            
            SyncMessage message = new SyncMessage(type, sourceServer, timestamp);
            JsonObject dataObj = obj.getAsJsonObject("data");
            for (String key : dataObj.keySet()) {
                message.set(key, GSON.fromJson(dataObj.get(key), Object.class));
            }
            return message;
        } catch (Exception e) {
            return null;
        }
    }
    
    public byte[] toBytes() {
        return toJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    public static SyncMessage fromBytes(byte[] bytes) {
        return fromJson(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
    }
    
    @Override
    public String toString() {
        return "SyncMessage{type=" + type + ", sourceServer='" + sourceServer + "', timestamp=" + timestamp + "}";
    }
}
