package com.oolonghoo.woosocial.sync;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.configuration.file.FileConfiguration;

public class SyncConfig {
    
    private final WooSocial plugin;
    private boolean enabled;
    private SyncMode mode;
    private String serverName;
    private boolean redisEnabled;
    private String redisUri;
    private String redisPassword;
    private int mysqlPollInterval;
    
    public SyncConfig(WooSocial plugin) {
        this.plugin = plugin;
        load();
    }
    
    public void load() {
        FileConfiguration config = plugin.getConfig();
        
        enabled = config.getBoolean("cross-server.enabled", false);
        mode = SyncMode.fromString(config.getString("cross-server.mode", "auto"));
        serverName = config.getString("cross-server.server-name", "server-1");
        
        redisEnabled = config.getBoolean("cross-server.redis.enabled", false);
        redisUri = config.getString("cross-server.redis.uri", "redis://localhost:6379");
        redisPassword = config.getString("cross-server.redis.password", "");
        
        mysqlPollInterval = config.getInt("cross-server.mysql-poll.interval", 5);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public SyncMode getMode() {
        return mode;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public boolean isRedisEnabled() {
        return redisEnabled;
    }
    
    public String getRedisUri() {
        return redisUri;
    }
    
    public String getRedisPassword() {
        return redisPassword;
    }
    
    public int getMysqlPollInterval() {
        return mysqlPollInterval;
    }
}
