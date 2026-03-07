package com.oolonghoo.woosocial.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置加载器基类
 * 提供配置文件的加载、保存、重载和缓存功能
 * 
 * @author oolongho
 */
public class ConfigLoader {
    
    protected final JavaPlugin plugin;
    protected final String fileName;
    protected final File configFile;
    protected FileConfiguration config;
    protected final Map<String, Object> cache = new HashMap<>();
    
    public ConfigLoader(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.configFile = new File(plugin.getDataFolder(), fileName);
    }
    
    /**
     * 初始化配置文件
     * 如果配置文件不存在，会从jar包中复制默认配置
     */
    public void initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        boolean isCustomFile = !isBundledResource(fileName);
        
        if (!configFile.exists()) {
            if (isCustomFile) {
                plugin.getLogger().warning("配置文件未找到: " + fileName);
                try {
                    configFile.getParentFile().mkdirs();
                    configFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("创建配置文件失败: " + fileName);
                }
            } else {
                plugin.saveResource(fileName, false);
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        if (!isCustomFile) {
            InputStream defaultStream = plugin.getResource(fileName);
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
                config.options().copyDefaults(true);
                save();
            }
        }
        
        loadValues();
    }
    
    /**
     * 检查资源是否存在于jar包中
     * 
     * @param name 资源名称
     * @return 是否存在
     */
    protected boolean isBundledResource(String name) {
        return plugin.getResource(name) != null;
    }
    
    /**
     * 加载配置值到缓存
     * 子类应重写此方法以加载特定配置
     */
    protected void loadValues() {
        cache.clear();
    }
    
    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件失败: " + fileName);
            e.printStackTrace();
        }
    }
    
    /**
     * 重载配置文件
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 重新加载默认配置
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }
        
        loadValues();
    }
    
    /**
     * 从缓存获取配置值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @param type 值类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    protected <T> T getCached(String path, T defaultValue, Class<T> type) {
        if (cache.containsKey(path)) {
            return (T) cache.get(path);
        }
        T value = config.getObject(path, type, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的字符串
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 字符串值
     */
    protected String getCachedString(String path, String defaultValue) {
        if (cache.containsKey(path)) {
            return (String) cache.get(path);
        }
        String value = config.getString(path, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的整数
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 整数值
     */
    protected int getCachedInt(String path, int defaultValue) {
        if (cache.containsKey(path)) {
            return (Integer) cache.get(path);
        }
        int value = config.getInt(path, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的长整数
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 长整数值
     */
    protected long getCachedLong(String path, long defaultValue) {
        if (cache.containsKey(path)) {
            return (Long) cache.get(path);
        }
        long value = config.getLong(path, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的布尔值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 布尔值
     */
    protected boolean getCachedBoolean(String path, boolean defaultValue) {
        if (cache.containsKey(path)) {
            return (Boolean) cache.get(path);
        }
        boolean value = config.getBoolean(path, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的双精度浮点数
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    protected double getCachedDouble(String path, double defaultValue) {
        if (cache.containsKey(path)) {
            return (Double) cache.get(path);
        }
        double value = config.getDouble(path, defaultValue);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取缓存的字符串列表
     * 
     * @param path 配置路径
     * @return 字符串列表
     */
    @SuppressWarnings("unchecked")
    protected List<String> getCachedStringList(String path) {
        if (cache.containsKey(path)) {
            return (List<String>) cache.get(path);
        }
        List<String> value = config.getStringList(path);
        cache.put(path, value);
        return value;
    }
    
    /**
     * 获取FileConfiguration对象
     * 
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取配置文件名
     * 
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * 获取配置文件对象
     * 
     * @return File对象
     */
    public File getConfigFile() {
        return configFile;
    }
}
