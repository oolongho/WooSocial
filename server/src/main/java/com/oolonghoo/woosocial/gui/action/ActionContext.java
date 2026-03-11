package com.oolonghoo.woosocial.gui.action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionContext {
    
    private final Map<String, Object> data;
    private String currentGuiName;
    private UUID friendUuid;
    private UUID targetUuid;
    private String friendName;
    private int currentPage;
    private int totalPages;
    
    public ActionContext() {
        this.data = new HashMap<>();
    }
    
    public static ActionContext create() {
        return new ActionContext();
    }
    
    public ActionContext guiName(String name) {
        this.currentGuiName = name;
        return this;
    }
    
    public ActionContext friendUuid(UUID uuid) {
        this.friendUuid = uuid;
        return this;
    }
    
    public ActionContext targetUuid(UUID uuid) {
        this.targetUuid = uuid;
        return this;
    }
    
    public ActionContext friendName(String name) {
        this.friendName = name;
        return this;
    }
    
    public ActionContext page(int current, int total) {
        this.currentPage = current;
        this.totalPages = total;
        return this;
    }
    
    public ActionContext set(String key, Object value) {
        data.put(key, value);
        return this;
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public String getCurrentGuiName() {
        return currentGuiName;
    }
    
    public UUID getFriendUuid() {
        return friendUuid;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public String getFriendName() {
        return friendName;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
}
