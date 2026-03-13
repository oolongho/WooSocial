package com.oolonghoo.woosocial.manager;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 * 负责加载和管理插件配置文件
 * 支持配置缓存机制，避免重复读取
 */
public class ConfigManager {
    
    private final WooSocial plugin;
    private FileConfiguration config;
    private File configFile;
    
    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();
    private final Map<String, Object> configCache = new HashMap<>();
    
    public ConfigManager(WooSocial plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
        
        loadModuleConfigs();
        loadCachedValues();
    }
    
    /**
     * 加载模块配置文件
     */
    private void loadModuleConfigs() {
        File settingsFolder = new File(plugin.getDataFolder(), "settings");
        if (!settingsFolder.exists()) {
            settingsFolder.mkdirs();
        }
        
        String[] moduleNames = {"friend", "mail", "relation", "teleport", "gifts", "relation_types", "memorial_items"};
        
        for (String moduleName : moduleNames) {
            String fileName = moduleName + ".yml";
            File moduleFile = new File(settingsFolder, fileName);
            
            if (!moduleFile.exists()) {
                String resourcePath = "settings/" + fileName;
                InputStream resourceStream = plugin.getResource(resourcePath);
                if (resourceStream != null) {
                    try {
                        plugin.getLogger().info("[Config] Saving default " + fileName + " to settings folder");
                        File outputFile = new File(settingsFolder, fileName);
                        java.nio.file.Files.copy(resourceStream.readAllBytes(), outputFile.toPath());
                        resourceStream.close();
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Config] Failed to save default " + fileName + ": " + e.getMessage());
                    }
                }
            }
            
            FileConfiguration moduleConfig = YamlConfiguration.loadConfiguration(moduleFile);
            
            InputStream defaultStream = plugin.getResource("settings/" + fileName);
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                moduleConfig.setDefaults(defaultConfig);
            }
            
            moduleConfigs.put(moduleName, moduleConfig);
            plugin.getLogger().fine("[Config] Loaded module config: " + fileName);
        }
    }
    
    /**
     * 加载所有配置值到缓存
     */
    private void loadCachedValues() {
        configCache.clear();
        
        configCache.put("database.type", config.getString("database.type", "sqlite").toLowerCase());
        configCache.put("database.mysql.host", config.getString("database.mysql.host", "localhost"));
        configCache.put("database.mysql.port", config.getInt("database.mysql.port", 3306));
        configCache.put("database.mysql.database", config.getString("database.mysql.database", "minecraft"));
        configCache.put("database.mysql.user", config.getString("database.mysql.user", "root"));
        configCache.put("database.mysql.password", config.getString("database.mysql.password", ""));
        configCache.put("database.mysql.table-prefix", config.getString("database.mysql.table-prefix", "woosocial_"));
        configCache.put("database.sqlite.file", config.getString("database.sqlite.file", "data.db"));
        
        configCache.put("database.pool.maximum-pool-size", config.getInt("database.pool.maximum-pool-size", 10));
        configCache.put("database.pool.minimum-idle", config.getInt("database.pool.minimum-idle", 2));
        configCache.put("database.pool.connection-timeout", config.getLong("database.pool.connection-timeout", 30000L));
        configCache.put("database.pool.idle-timeout", config.getLong("database.pool.idle-timeout", 600000L));
        configCache.put("database.pool.max-lifetime", config.getLong("database.pool.max-lifetime", 1800000L));
        
        FileConfiguration friendConfig = getModuleConfig("friend");
        if (friendConfig != null) {
            configCache.put("friend.max-friends", friendConfig.getInt("max-friends", 50));
            configCache.put("friend.request-expire-time", friendConfig.getInt("request-expire-time", 300));
            configCache.put("friend.default-notify-online", friendConfig.getBoolean("default-notify-online", true));
        }
        
        FileConfiguration teleportConfig = getModuleConfig("teleport");
        if (teleportConfig != null) {
            configCache.put("teleport.countdown", teleportConfig.getInt("countdown", 3));
            configCache.put("teleport.cooldown", teleportConfig.getInt("cooldown", 60));
            configCache.put("teleport.cancel-on-move", teleportConfig.getBoolean("cancel.on-move", true));
            configCache.put("teleport.move-threshold", teleportConfig.getDouble("cancel.move-threshold", 1.0));
            configCache.put("teleport.cancel-on-damage", teleportConfig.getBoolean("cancel.on-damage", true));
            configCache.put("teleport.default-allow-teleport", teleportConfig.getBoolean("default-allow-teleport", true));
        }
        
        configCache.put("settings.debug", config.getBoolean("settings.debug", false));
        configCache.put("settings.language", config.getString("settings.language", "zh-CN"));
        configCache.put("settings.server-name", config.getString("settings.server-name", "server1"));
        configCache.put("settings.auto-save-interval", config.getInt("settings.auto-save-interval", 300));
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
        loadModuleConfigs();
        loadCachedValues();
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取模块配置文件
     * 
     * @param moduleName 模块名称 (friend, mail, relation, teleport, gifts, etc.)
     * @return 模块配置文件，如果不存在则返回 null
     */
    public FileConfiguration getModuleConfig(String moduleName) {
        return moduleConfigs.get(moduleName);
    }
    
    /**
     * 获取缓存的字符串配置
     */
    private String getCachedString(String key, String defaultValue) {
        Object value = configCache.get(key);
        return value != null ? (String) value : defaultValue;
    }
    
    /**
     * 获取缓存的整数配置
     */
    private int getCachedInt(String key, int defaultValue) {
        Object value = configCache.get(key);
        return value != null ? (Integer) value : defaultValue;
    }
    
    /**
     * 获取缓存的长整数配置
     */
    private long getCachedLong(String key, long defaultValue) {
        Object value = configCache.get(key);
        return value != null ? (Long) value : defaultValue;
    }
    
    /**
     * 获取缓存的布尔配置
     */
    private boolean getCachedBoolean(String key, boolean defaultValue) {
        Object value = configCache.get(key);
        return value != null ? (Boolean) value : defaultValue;
    }
    
    /**
     * 获取缓存的双精度配置
     */
    private double getCachedDouble(String key, double defaultValue) {
        Object value = configCache.get(key);
        return value != null ? (Double) value : defaultValue;
    }
    
    // ==================== 数据库配置 ====================
    
    /**
     * 获取数据库类型
     * @return "mysql" 或 "sqlite"
     */
    public String getDatabaseType() {
        return getCachedString("database.type", "sqlite");
    }
    
    /**
     * 获取MySQL主机地址
     */
    public String getMySQLHost() {
        return getCachedString("database.mysql.host", "localhost");
    }
    
    /**
     * 获取MySQL端口
     */
    public int getMySQLPort() {
        return getCachedInt("database.mysql.port", 3306);
    }
    
    /**
     * 获取MySQL数据库名
     */
    public String getMySQLDatabase() {
        return getCachedString("database.mysql.database", "minecraft");
    }
    
    /**
     * 获取MySQL用户名
     */
    public String getMySQLUser() {
        return getCachedString("database.mysql.user", "root");
    }
    
    /**
     * 获取MySQL密码
     */
    public String getMySQLPassword() {
        return getCachedString("database.mysql.password", "");
    }
    
    /**
     * 获取MySQL连接池大小（已废弃，使用 getPoolMaximumPoolSize）
     */
    @Deprecated
    public int getMySQLPoolSize() {
        return getCachedInt("database.pool.maximum-pool-size", 10);
    }
    
    /**
     * 获取连接池最大连接数
     */
    public int getPoolMaximumPoolSize() {
        return getCachedInt("database.pool.maximum-pool-size", 10);
    }
    
    /**
     * 获取连接池最小空闲连接数
     */
    public int getPoolMinimumIdle() {
        return getCachedInt("database.pool.minimum-idle", 2);
    }
    
    /**
     * 获取连接超时时间（毫秒）
     */
    public long getPoolConnectionTimeout() {
        return getCachedLong("database.pool.connection-timeout", 30000L);
    }
    
    /**
     * 获取空闲连接超时时间（毫秒）
     */
    public long getPoolIdleTimeout() {
        return getCachedLong("database.pool.idle-timeout", 600000L);
    }
    
    /**
     * 获取连接最大生命周期（毫秒）
     */
    public long getPoolMaxLifetime() {
        return getCachedLong("database.pool.max-lifetime", 1800000L);
    }
    
    /**
     * 获取数据库表前缀
     */
    public String getTablePrefix() {
        return getCachedString("database.mysql.table-prefix", "woosocial_");
    }
    
    /**
     * 获取SQLite数据库文件名
     */
    public String getSQLiteFile() {
        return getCachedString("database.sqlite.file", "data.db");
    }
    
    // ==================== 好友系统配置 ====================
    
    /**
     * 获取最大好友数量
     */
    public int getMaxFriends() {
        return getCachedInt("friend.max-friends", 50);
    }
    
    /**
     * 获取好友请求过期时间（秒）
     */
    public int getRequestExpireTime() {
        return getCachedInt("friend.request-expire-time", 300);
    }
    
    /**
     * 获取默认在线通知设置
     */
    public boolean isDefaultNotifyOnline() {
        return getCachedBoolean("friend.default-notify-online", true);
    }
    
    // ==================== 传送系统配置 ====================
    
    /**
     * 获取传送倒计时（秒）
     */
    public int getTeleportCountdown() {
        return getCachedInt("teleport.countdown", 3);
    }
    
    /**
     * 获取传送冷却时间（秒）
     */
    public int getTeleportCooldown() {
        return getCachedInt("teleport.cooldown", 60);
    }
    
    /**
     * 是否在移动时取消传送
     */
    public boolean isCancelOnMove() {
        return getCachedBoolean("teleport.cancel-on-move", true);
    }
    
    /**
     * 获取移动阈值
     */
    public double getMoveThreshold() {
        return getCachedDouble("teleport.move-threshold", 1.0);
    }
    
    /**
     * 是否在受伤时取消传送
     */
    public boolean isCancelOnDamage() {
        return getCachedBoolean("teleport.cancel-on-damage", true);
    }
    
    /**
     * 获取默认允许传送设置
     */
    public boolean isDefaultAllowTeleport() {
        return getCachedBoolean("teleport.default-allow-teleport", true);
    }
    
    // ==================== 其他配置 ====================
    
    /**
     * 是否启用调试模式
     */
    public boolean isDebugMode() {
        return getCachedBoolean("settings.debug", false);
    }
    
    /**
     * 获取语言设置
     */
    public String getLanguage() {
        return getCachedString("settings.language", "zh-CN");
    }
    
    /**
     * 获取服务器名称（用于跨服同步）
     */
    public String getServerName() {
        return getCachedString("settings.server-name", "server1");
    }
    
    /**
     * 获取自动保存间隔（秒）
     */
    public int getAutoSaveInterval() {
        return getCachedInt("settings.auto-save-interval", 300);
    }
}
