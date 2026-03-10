package com.oolonghoo.woosocial.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息管理器
 * 负责加载和管理语言文件，支持颜色代码和占位符替换
 * 
 * @author oolongho
 */
public class MessageManager {
    
    private final JavaPlugin plugin;
    private File langFile;
    private FileConfiguration langConfig;
    private Component prefixComponent;
    private String prefixString;
    private String currentLanguage;
    
    /**
     * Legacy颜色代码序列化器
     * 支持 & 颜色代码格式（如 &a, &c 等）
     */
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化消息管理器
     * 从配置文件中读取语言设置并加载对应的语言文件
     */
    public void initialize() {
        String language = plugin.getConfig().getString("settings.language", "zh-CN");
        this.currentLanguage = language;
        loadLanguageFile(language);
        updatePrefix();
    }
    
    /**
     * 更新前缀组件
     */
    private void updatePrefix() {
        String prefixText = langConfig.getString("prefix", "&8[&6WooSocial&8]&r ");
        this.prefixComponent = SERIALIZER.deserialize(prefixText);
        this.prefixString = SERIALIZER.serialize(prefixComponent);
    }
    
    /**
     * 加载语言文件
     * 
     * @param language 语言代码（如 zh-CN, en-US）
     */
    private void loadLanguageFile(String language) {
        langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            InputStream defaultStream = plugin.getResource("lang/" + language + ".yml");
            if (defaultStream != null) {
                plugin.saveResource("lang/" + language + ".yml", false);
            } else {
                // 如果请求的语言文件不存在，尝试加载默认语言
                plugin.getLogger().warning("语言文件 " + language + " 不存在，使用默认语言 zh-CN");
                InputStream fallbackStream = plugin.getResource("lang/zh-CN.yml");
                if (fallbackStream != null) {
                    plugin.saveResource("lang/zh-CN.yml", true);
                    langFile = new File(plugin.getDataFolder(), "lang/zh-CN.yml");
                    this.currentLanguage = "zh-CN";
                } else {
                    try {
                        langFile.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().severe("创建语言文件失败: " + language);
                    }
                }
            }
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 设置默认配置（从jar包中读取）
        InputStream defaultStream = plugin.getResource("lang/zh-CN.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);
        }
    }
    
    /**
     * 重载语言文件
     */
    public void reload() {
        String language = plugin.getConfig().getString("settings.language", "zh-CN");
        this.currentLanguage = language;
        loadLanguageFile(language);
        updatePrefix();
    }
    
    /**
     * 获取当前语言
     * 
     * @return 当前语言代码
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * 获取消息字符串（已转换颜色代码）
     * 
     * @param key 消息键
     * @param args 占位符参数（键值对形式）
     * @return 格式化后的消息字符串
     */
    public String get(String key, Object... args) {
        String message = langConfig.getString(key, key);
        message = formatPlaceholders(message, args);
        return SERIALIZER.serialize(SERIALIZER.deserialize(message));
    }
    
    /**
     * 获取消息组件（用于 Adventure 发送）
     * 
     * @param key 消息键
     * @param args 占位符参数（键值对形式）
     * @return Component组件
     */
    public Component getComponent(String key, Object... args) {
        String message = langConfig.getString(key, key);
        message = formatPlaceholders(message, args);
        return SERIALIZER.deserialize(message);
    }
    
    /**
     * 获取带前缀的消息字符串
     * 
     * @param key 消息键
     * @param args 占位符参数
     * @return 带前缀的消息字符串
     */
    public String getWithPrefix(String key, Object... args) {
        return prefixString + get(key, args);
    }
    
    /**
     * 获取带前缀的消息组件
     * 
     * @param key 消息键
     * @param args 占位符参数
     * @return 带前缀的Component组件
     */
    public Component getWithPrefixComponent(String key, Object... args) {
        return prefixComponent.append(getComponent(key, args));
    }
    
    /**
     * 发送消息给命令发送者（支持控制台和玩家）
     * 
     * @param sender 命令发送者
     * @param key 消息键
     * @param args 占位符参数
     */
    public void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(getWithPrefixComponent(key, args));
    }
    
    /**
     * 发送消息给玩家
     * 
     * @param player 玩家
     * @param key 消息键
     * @param args 占位符参数
     */
    public void send(Player player, String key, Object... args) {
        player.sendMessage(getWithPrefixComponent(key, args));
    }
    
    /**
     * 发送无前缀消息给命令发送者
     * 
     * @param sender 命令发送者
     * @param key 消息键
     * @param args 占位符参数
     */
    public void sendNoPrefix(CommandSender sender, String key, Object... args) {
        sender.sendMessage(getComponent(key, args));
    }
    
    /**
     * 发送无前缀消息给玩家
     * 
     * @param player 玩家
     * @param key 消息键
     * @param args 占位符参数
     */
    public void sendNoPrefix(Player player, String key, Object... args) {
        player.sendMessage(getComponent(key, args));
    }
    
    /**
     * 发送可点击消息给玩家
     * 使用 MiniMessage 的 click事件功能
     * 
     * @param player 玩家
     * @param key 消息键
     * @param args 占位符参数
     */
    public void sendClickable(Player player, String key, Object... args) {
        player.sendMessage(getWithPrefixComponent(key, args));
    }
    
    /**
     * 发送带可点击按钮的消息
     * 
     * @param player 玩家
     * @param messageKey 消息键
     * @param acceptCommand 同意命令
     * @param denyCommand 拒绝命令
     * @param args 占位符参数
     */
    public void sendWithClickableButtons(Player player, String messageKey, 
                                           String acceptCommand, String denyCommand, Object... args) {
        Component message = getWithPrefixComponent(messageKey, args);
        
        Component acceptButton = net.kyori.adventure.text.Component.text("[同意]", NamedTextColor.GREEN)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCmd(acceptCommand));
        Component denyButton = net.kyori.adventure.text.Component.text("[拒绝]", NamedTextColor.RED)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCmd(denyCommand));
        
        Component buttons = net.kyori.adventure.text.Component.text()
                .append(acceptButton)
                .append(net.kyori.adventure.text.Component.text("    ", NamedTextColor.WHITE))
                .append(denyButton);
        
        player.sendMessage(message);
        player.sendMessage(buttons);
    }
    
    /**
     * 获取前缀字符串
     * 
     * @return 前缀字符串
     */
    public String getPrefix() {
        return prefixString;
    }
    
    /**
     * 获取前缀组件
     * 
     * @return 前缀Component
     */
    public Component getPrefixComponent() {
        return prefixComponent;
    }
    
    /**
     * 格式化占位符
     * 占位符格式：%key%
     * 参数格式：键值对交替传入（如 "player", "Steve", "count", 10）
     * 
     * @param message 原始消息
     * @param args 占位符参数
     * @return 格式化后的消息
     */
    private String formatPlaceholders(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i] != null && args[i + 1] != null) {
                placeholders.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
            }
        }
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return message;
    }
    
    /**
     * 获取语言配置对象
     * 
     * @return FileConfiguration
     */
    public FileConfiguration getLangConfig() {
        return langConfig;
    }
    
    /**
     * 获取字符串列表（已转换颜色代码）
     * 
     * @param key 消息键
     * @return 字符串列表
     */
    public List<String> getList(String key) {
        List<String> list = langConfig.getStringList(key);
        if (list.isEmpty()) {
            list = new ArrayList<>();
            String single = langConfig.getString(key);
            if (single != null && !single.equals(key)) {
                list.add(single);
            } else {
                list.add("&cMissing: " + key);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            list.set(i, SERIALIZER.serialize(SERIALIZER.deserialize(list.get(i))));
        }
        return list;
    }
    
    /**
     * 获取组件列表
     * 
     * @param key 消息键
     * @return Component列表
     */
    public List<Component> getComponentList(String key) {
        List<String> strings = langConfig.getStringList(key);
        if (strings.isEmpty()) {
            strings = new ArrayList<>();
            String single = langConfig.getString(key);
            if (single != null && !single.equals(key)) {
                strings.add(single);
            } else {
                strings.add("&cMissing: " + key);
            }
        }
        List<Component> components = new ArrayList<>();
        for (String str : strings) {
            components.add(SERIALIZER.deserialize(str));
        }
        return components;
    }
    
    /**
     * 发送列表消息给命令发送者
     * 
     * @param sender 命令发送者
     * @param key 消息键
     * @param args 占位符参数
     */
    public void sendList(CommandSender sender, String key, Object... args) {
        List<Component> components = getComponentList(key);
        for (Component component : components) {
            String message = SERIALIZER.serialize(component);
            message = formatPlaceholders(message, args);
            sender.sendMessage(SERIALIZER.deserialize(message));
        }
    }
    
    /**
     * 检查消息键是否存在
     * 
     * @param key 消息键
     * @return 是否存在
     */
    public boolean hasMessage(String key) {
        return langConfig.contains(key);
    }
    
    /**
     * 获取原始消息（不进行颜色转换）
     * 
     * @param key 消息键
     * @return 原始消息字符串
     */
    public String getRaw(String key) {
        return langConfig.getString(key, key);
    }
    
    /**
     * 转换颜色代码
     * 将 & 颜色代码转换为实际颜色
     * 
     * @param message 包含颜色代码的消息
     * @return 转换后的消息字符串
     */
    public String parseColors(String message) {
        if (message == null) return "";
        return SERIALIZER.serialize(SERIALIZER.deserialize(message));
    }
}
