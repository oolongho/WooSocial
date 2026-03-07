package com.oolonghoo.woosocial.sync;

public enum SyncMode {
    AUTO,
    BUNGEE,
    VELOCITY,
    REDIS,
    MYSQL,
    DISABLED;
    
    public static SyncMode fromString(String str) {
        if (str == null) return AUTO;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
