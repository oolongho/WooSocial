package com.oolonghoo.woosocial.manager;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 配置管理器
 * 负责加载和管理插件配置文件
 */
public class ConfigManager {
    
    private final WooSocial plugin;
    private FileConfiguration config;
    private File configFile;
    
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
        
        // 如果配置文件不存在，从资源中复制默认配置
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 合并默认值（确保新添加的配置项有默认值）
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
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
    
    // ==================== 数据库配置 ====================
    
    /**
     * 获取数据库类型
     * @return "mysql" 或 "sqlite"
     */
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite").toLowerCase();
    }
    
    /**
     * 获取MySQL主机地址
     */
    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    /**
     * 获取MySQL端口
     */
    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    /**
     * 获取MySQL数据库名
     */
    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "minecraft");
    }
    
    /**
     * 获取MySQL用户名
     */
    public String getMySQLUser() {
        return config.getString("database.mysql.user", "root");
    }
    
    /**
     * 获取MySQL密码
     */
    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }
    
    /**
     * 获取MySQL连接池大小（已废弃，使用 getPoolMaximumPoolSize）
     */
    @Deprecated
    public int getMySQLPoolSize() {
        return config.getInt("database.pool.maximum-pool-size", 10);
    }
    
    /**
     * 获取连接池最大连接数
     */
    public int getPoolMaximumPoolSize() {
        return config.getInt("database.pool.maximum-pool-size", 10);
    }
    
    /**
     * 获取连接池最小空闲连接数
     */
    public int getPoolMinimumIdle() {
        return config.getInt("database.pool.minimum-idle", 2);
    }
    
    /**
     * 获取连接超时时间（毫秒）
     */
    public long getPoolConnectionTimeout() {
        return config.getLong("database.pool.connection-timeout", 30000L);
    }
    
    /**
     * 获取空闲连接超时时间（毫秒）
     */
    public long getPoolIdleTimeout() {
        return config.getLong("database.pool.idle-timeout", 600000L);
    }
    
    /**
     * 获取连接最大生命周期（毫秒）
     */
    public long getPoolMaxLifetime() {
        return config.getLong("database.pool.max-lifetime", 1800000L);
    }
    
    /**
     * 获取数据库表前缀
     */
    public String getTablePrefix() {
        return config.getString("database.mysql.table-prefix", "woosocial_");
    }
    
    /**
     * 获取SQLite数据库文件名
     */
    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "data.db");
    }
    
    // ==================== 好友系统配置 ====================
    
    /**
     * 获取最大好友数量
     */
    public int getMaxFriends() {
        return config.getInt("friend.max-friends", 50);
    }
    
    /**
     * 获取好友请求过期时间（秒）
     */
    public int getRequestExpireTime() {
        return config.getInt("friend.request-expire-time", 300);
    }
    
    /**
     * 获取默认在线通知设置
     */
    public boolean isDefaultNotifyOnline() {
        return config.getBoolean("friend.default-notify-online", true);
    }
    
    // ==================== 传送系统配置 ====================
    
    /**
     * 获取传送倒计时（秒）
     */
    public int getTeleportCountdown() {
        return config.getInt("teleport.countdown", 3);
    }
    
    /**
     * 获取传送冷却时间（秒）
     */
    public int getTeleportCooldown() {
        return config.getInt("teleport.cooldown", 60);
    }
    
    /**
     * 是否在移动时取消传送
     */
    public boolean isCancelOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }
    
    /**
     * 获取移动阈值
     */
    public double getMoveThreshold() {
        return config.getDouble("teleport.move-threshold", 1.0);
    }
    
    /**
     * 是否在受伤时取消传送
     */
    public boolean isCancelOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }
    
    /**
     * 获取默认允许传送设置
     */
    public boolean isDefaultAllowTeleport() {
        return config.getBoolean("teleport.default-allow-teleport", true);
    }
    
    // ==================== 其他配置 ====================
    
    /**
     * 是否启用调试模式
     */
    public boolean isDebugMode() {
        return config.getBoolean("settings.debug", false);
    }
    
    /**
     * 获取语言设置
     */
    public String getLanguage() {
        return config.getString("settings.language", "zh-CN");
    }
    
    /**
     * 获取自动保存间隔（秒）
     */
    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 300);
    }
}
