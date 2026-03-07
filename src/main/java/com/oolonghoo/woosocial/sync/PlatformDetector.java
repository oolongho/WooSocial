package com.oolonghoo.woosocial.sync;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class PlatformDetector {
    
    private final WooSocial plugin;
    private PlatformType detectedPlatform;
    private boolean proxyDetected = false;
    
    public PlatformDetector(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    public PlatformType detect() {
        if (detectedPlatform != null) {
            return detectedPlatform;
        }
        
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            detectedPlatform = PlatformType.BUNGEECORD;
            plugin.getLogger().info("[Sync] 检测到 BungeeCord 代理端环境");
            return detectedPlatform;
        } catch (ClassNotFoundException ignored) {
        }
        
        try {
            Class.forName("com.velocitypowered.api.proxy.ProxyServer");
            detectedPlatform = PlatformType.VELOCITY;
            plugin.getLogger().info("[Sync] 检测到 Velocity 代理端环境");
            return detectedPlatform;
        } catch (ClassNotFoundException ignored) {
        }
        
        try {
            Class.forName("org.bukkit.Bukkit");
            detectedPlatform = PlatformType.BUKKIT;
            
            if (isBehindProxy()) {
                proxyDetected = true;
                plugin.getLogger().info("[Sync] 检测到 Bukkit 服务端环境 (代理端模式)");
            } else {
                plugin.getLogger().info("[Sync] 检测到 Bukkit 服务端环境 (单机模式)");
            }
            return detectedPlatform;
        } catch (ClassNotFoundException ignored) {
        }
        
        detectedPlatform = PlatformType.UNKNOWN;
        plugin.getLogger().warning("[Sync] 无法检测运行环境");
        return detectedPlatform;
    }
    
    private boolean isBehindProxy() {
        if (Bukkit.getServer().getOnlineMode()) {
            return false;
        }
        
        if (Bukkit.getServer().spigot().getConfig().getBoolean("settings.bungeecord", false)) {
            return true;
        }
        
        return Bukkit.getServer().getOnlinePlayers().stream()
                .anyMatch(player -> player.getPendingConnection().getVersion() > 0);
    }
    
    public boolean isProxyDetected() {
        if (detectedPlatform == null) {
            detect();
        }
        return proxyDetected;
    }
    
    public boolean isBungeeCord() {
        return detect() == PlatformType.BUNGEECORD;
    }
    
    public boolean isVelocity() {
        return detect() == PlatformType.VELOCITY;
    }
    
    public boolean isBukkit() {
        return detect() == PlatformType.BUKKIT;
    }
    
    public boolean isUnknown() {
        return detect() == PlatformType.UNKNOWN;
    }
    
    public PlatformType getDetectedPlatform() {
        return detect();
    }
}
