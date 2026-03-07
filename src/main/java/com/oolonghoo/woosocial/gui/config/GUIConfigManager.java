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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class GUIConfigManager {
    
    private final WooSocial plugin;
    private final Map<String, GUIConfig> configs;
    private final File guiFolder;
    
    private static final Set<String> GUI_NAMES = Set.of(
            "social_main",
            "friend_list",
            "friend_detail",
            "friend_requests",
            "social_settings",
            "blocked_list"
    );
    
    public GUIConfigManager(WooSocial plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.guiFolder = new File(plugin.getDataFolder(), "gui");
    }
    
    public void initialize() {
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }
        
        for (String guiName : GUI_NAMES) {
            loadConfig(guiName);
        }
    }
    
    public void loadConfig(String name) {
        File externalFile = new File(guiFolder, name + ".yml");
        
        YamlConfiguration config;
        
        if (externalFile.exists()) {
            try {
                config = YamlConfiguration.loadConfiguration(externalFile);
                plugin.getLogger().info("[GUI] 加载外部配置: " + name + ".yml");
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
            } else {
                plugin.getLogger().warning("[GUI] 配置解析失败: " + name + ".yml, 使用代码默认值");
            }
        }
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
            plugin.getLogger().info("[GUI] 已生成默认配置: " + name + ".yml");
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
        
        return config;
    }
    
    public GUIConfig getConfig(String name) {
        return configs.get(name);
    }
    
    public boolean hasConfig(String name) {
        return configs.containsKey(name);
    }
    
    public void reload() {
        configs.clear();
        initialize();
    }
}
