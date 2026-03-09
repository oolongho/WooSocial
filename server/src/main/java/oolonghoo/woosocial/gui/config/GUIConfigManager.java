package com.oolonghoo.woosocial.gui.config;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * GUI配置管理器
 * 负责加载、缓存和管理所有GUI配置
 * 支持热重载和动态注册
 * 
 * @author oolongho
 */
public class GUIConfigManager {
    
    private final WooSocial plugin;
    private final Map<String, GUIConfig> configs;
    private final Map<String, GUILayout> layouts;
    private final File guiFolder;
    private final Set<String> registeredGUIs;
    
    // 默认GUI列表
    private static final Set<String> DEFAULT_GUI_NAMES = Set.of(
            "social_main",
            "friend_list",
            "friend_detail",
            "friend_requests",
            "social_settings",
            "blocked_list",
            "mail_list",
            "mail_detail",
            "send_mail",
            "relation_list",
            "relation_detail",
            "relation_proposal",
            "gift_shop"
    );
    
    public GUIConfigManager(WooSocial plugin) {
        this.plugin = plugin;
        this.configs = new ConcurrentHashMap<>();
        this.layouts = new ConcurrentHashMap<>();
        this.guiFolder = new File(plugin.getDataFolder(), "gui");
        this.registeredGUIs = new HashSet<>(DEFAULT_GUI_NAMES);
    }
    
    public void initialize() {
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }
        
        for (String guiName : registeredGUIs) {
            loadConfig(guiName);
        }
    }
    
    /**
     * 注册新的GUI配置
     * 
     * @param name GUI名称
     */
    public void registerGUI(String name) {
        if (!registeredGUIs.contains(name)) {
            registeredGUIs.add(name);
            loadConfig(name);
        }
    }
    
    /**
     * 加载指定GUI配置
     */
    public void loadConfig(String name) {
        File externalFile = new File(guiFolder, name + ".yml");
        
        YamlConfiguration config;
        
        if (externalFile.exists()) {
            try {
                config = YamlConfiguration.loadConfiguration(externalFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[GUI] 加载外部配置失败: " + name + ".yml, 使用默认配置", e);
                config = loadDefaultConfig(name);
            }
        } else {
            config = loadDefaultConfig(name);
            if (config != null) {
                saveDefaultConfig(name, config, externalFile);
            }
        }
        
        if (config != null) {
            GUIConfig guiConfig = parseConfig(name, config);
            if (guiConfig != null) {
                configs.put(name, guiConfig);
                layouts.put(name, new GUILayout(guiConfig));
            } else {
                plugin.getLogger().warning("[GUI] 配置解析失败: " + name + ".yml");
            }
        }
    }
    
    /**
     * 热重载单个GUI配置
     */
    public boolean reloadConfig(String name) {
        if (!registeredGUIs.contains(name)) {
            plugin.getLogger().warning("[GUI] 未注册的GUI: " + name);
            return false;
        }
        
        configs.remove(name);
        layouts.remove(name);
        loadConfig(name);
        
        return configs.containsKey(name);
    }
    
    private YamlConfiguration loadDefaultConfig(String name) {
        String resourcePath = "gui/" + name + ".yml";
        InputStream stream = plugin.getResource(resourcePath);
        
        if (stream == null) {
            plugin.getLogger().warning("[GUI] 未找到默认配置: " + resourcePath);
            return null;
        }
        
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[GUI] 加载默认配置失败: " + name, e);
            return null;
        }
    }
    
    private void saveDefaultConfig(String name, YamlConfiguration config, File targetFile) {
        try {
            config.save(targetFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[GUI] 保存默认配置失败: " + name, e);
        }
    }
    
    private GUIConfig parseConfig(String name, YamlConfiguration yaml) {
        try {
            GUIConfig config = new GUIConfig(name);
            
            if (yaml.contains("title")) {
                config.setTitle(yaml.getString("title"));
            }
            
            if (yaml.contains("layout")) {
                List<String> layout = yaml.getStringList("layout");
                config.setLayout(layout);
            }
            
            if (yaml.contains("icons")) {
                ConfigurationSection iconsSection = yaml.getConfigurationSection("icons");
                if (iconsSection != null) {
                    Map<String, IconConfig> icons = new HashMap<>();
                    for (String key : iconsSection.getKeys(false)) {
                        ConfigurationSection iconSection = iconsSection.getConfigurationSection(key);
                        if (iconSection != null) {
                            IconConfig iconConfig = parseIconConfig(iconSection);
                            icons.put(key, iconConfig);
                        }
                    }
                    config.setIcons(icons);
                }
            }
            
            return config;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GUI] 解析配置失败: " + name, e);
            return null;
        }
    }
    
    private IconConfig parseIconConfig(ConfigurationSection section) {
        IconConfig config = new IconConfig();
        
        if (section.contains("type")) {
            config.setType(section.getString("type"));
        }
        
        if (section.contains("material")) {
            String mat = section.getString("material");
            config.setMaterial(mat);
            
            if (mat != null && mat.contains("{model-data=")) {
                int start = mat.indexOf("{model-data=") + 12;
                int end = mat.indexOf("}", start);
                if (end > start) {
                    try {
                        config.setCustomModelData(Integer.parseInt(mat.substring(start, end)));
                        config.setMaterial(mat.substring(0, mat.indexOf("{")));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        
        if (section.contains("skull_owner")) {
            config.setSkullOwner(section.getString("skull_owner"));
        }
        
        if (section.contains("item_model")) {
            config.setItemModel(section.getString("item_model"));
        }
        
        if (section.contains("custom_model_data")) {
            config.setCustomModelData(section.getInt("custom_model_data"));
        }
        
        if (section.contains("name")) {
            config.setName(section.getString("name"));
        }
        
        if (section.contains("lore")) {
            List<String> loreList = section.getStringList("lore");
            config.setLore(loreList);
        }
        
        if (section.contains("amount")) {
            config.setAmount(section.getInt("amount"));
        }
        
        if (section.contains("glow")) {
            config.setGlow(section.getBoolean("glow"));
        }
        
        if (section.contains("actions")) {
            ConfigurationSection actionsSection = section.getConfigurationSection("actions");
            if (actionsSection != null) {
                Map<String, List<String>> parsedActions = new HashMap<>();
                for (String actionKey : actionsSection.getKeys(false)) {
                    List<String> actionList = actionsSection.getStringList(actionKey);
                    if (!actionList.isEmpty()) {
                        parsedActions.put(actionKey, actionList);
                    } else {
                        String singleAction = actionsSection.getString(actionKey);
                        if (singleAction != null && !singleAction.isEmpty()) {
                            List<String> singleList = new ArrayList<>();
                            singleList.add(singleAction);
                            parsedActions.put(actionKey, singleList);
                        }
                    }
                }
                config.setActions(parsedActions);
            }
        }
        
        // 解析条件显示配置
        if (section.contains("show_if")) {
            config.setShowConditions(section.getStringList("show_if"));
        }
        
        if (section.contains("hide_if")) {
            config.setHideConditions(section.getStringList("hide_if"));
        }
        
        // 解析优先级
        if (section.contains("priority")) {
            config.setPriority(section.getInt("priority"));
        }
        
        return config;
    }
    
    // ==================== 配置获取方法 ====================
    
    /**
     * 获取GUI配置
     */
    public GUIConfig getConfig(String name) {
        return configs.get(name);
    }
    
    /**
     * 获取GUI布局辅助对象
     */
    public GUILayout getLayout(String name) {
        return layouts.get(name);
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean hasConfig(String name) {
        return configs.containsKey(name);
    }
    
    /**
     * 获取所有已注册的GUI名称
     */
    public Set<String> getRegisteredGUIs() {
        return new HashSet<>(registeredGUIs);
    }
    
    /**
     * 获取所有已加载的GUI配置
     */
    public Map<String, GUIConfig> getAllConfigs() {
        return new HashMap<>(configs);
    }
    
    // ==================== 重载方法 ====================
    
    /**
     * 重载所有配置
     */
    public void reload() {
        configs.clear();
        layouts.clear();
        initialize();
    }
    
    /**
     * 重载所有配置并返回结果
     */
    public Map<String, Boolean> reloadAll() {
        Map<String, Boolean> results = new HashMap<>();
        
        for (String name : registeredGUIs) {
            results.put(name, reloadConfig(name));
        }
        
        return results;
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证所有配置
     */
    public Map<String, String> validateAll() {
        Map<String, String> errors = new HashMap<>();
        
        for (Map.Entry<String, GUIConfig> entry : configs.entrySet()) {
            String name = entry.getKey();
            GUIConfig config = entry.getValue();
            
            if (!config.validate()) {
                errors.put(name, "配置验证失败");
            }
            
            GUILayout layout = layouts.get(name);
            if (layout != null) {
                String layoutError = layout.validate();
                if (layoutError != null) {
                    errors.put(name, layoutError);
                }
            }
        }
        
        return errors;
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isConfigValid(String name) {
        GUIConfig config = configs.get(name);
        if (config == null) return false;
        
        if (!config.validate()) return false;
        
        GUILayout layout = layouts.get(name);
        if (layout == null) return false;
        
        return layout.validate() == null;
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取已加载配置数量
     */
    public int getLoadedCount() {
        return configs.size();
    }
    
    /**
     * 获取已注册GUI数量
     */
    public int getRegisteredCount() {
        return registeredGUIs.size();
    }
    
    /**
     * 打印配置状态（用于调试）
     */
    public void printStatus() {
        plugin.getLogger().info("=== GUI Config Manager Status ===");
        plugin.getLogger().info("Registered GUIs: " + registeredGUIs.size());
        plugin.getLogger().info("Loaded Configs: " + configs.size());
        
        for (String name : registeredGUIs) {
            GUIConfig config = configs.get(name);
            String status = config != null ? "loaded" : "not loaded";
            plugin.getLogger().info("  - " + name + ": " + status);
        }
        
        plugin.getLogger().info("=================================");
    }
}
