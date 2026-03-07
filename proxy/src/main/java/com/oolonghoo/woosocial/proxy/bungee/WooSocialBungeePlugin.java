package com.oolonghoo.woosocial.proxy.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class WooSocialBungeePlugin extends Plugin {
    
    public static final String SYNC_CHANNEL = "woosocial:sync";
    
    private static WooSocialBungeePlugin instance;
    private Configuration config;
    private SyncMessageHandler messageHandler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        loadConfig();
        
        getProxy().registerChannel(SYNC_CHANNEL);
        
        messageHandler = new SyncMessageHandler(getProxy(), getLogger());
        
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(getProxy(), getLogger(), messageHandler));
        
        getLogger().info("[WooSocialProxy] BungeeCord 代理端插件已启用");
    }
    
    @Override
    public void onDisable() {
        getProxy().unregisterChannel(SYNC_CHANNEL);
        getLogger().info("[WooSocialProxy] BungeeCord 代理端插件已关闭");
    }
    
    private void loadConfig() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                }
            }
            
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().warning("[WooSocialProxy] 加载配置失败: " + e.getMessage());
        }
    }
    
    public static WooSocialBungeePlugin getInstance() {
        return instance;
    }
    
    public Configuration getConfig() {
        return config;
    }
}
