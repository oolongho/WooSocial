package com.oolonghoo.woosocial.module.teleport;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.command.TeleportCommand;
import com.oolonghoo.woosocial.command.TeleportToggleCommand;
import com.oolonghoo.woosocial.listener.TeleportListener;
import com.oolonghoo.woosocial.module.Module;

/**
 * 传送模块
 * 提供好友传送功能，包括传送倒计时、冷却机制、权限控制等
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportModule extends Module {
    
    private TeleportDataManager dataManager;
    private TeleportManager teleportManager;
    private TeleportCommand teleportCommand;
    private TeleportToggleCommand toggleCommand;
    private TeleportListener teleportListener;
    
    public TeleportModule(WooSocial plugin) {
        super(plugin, "teleport");
    }
    
    @Override
    public void onEnable() {
        dataManager = new TeleportDataManager(plugin);
        dataManager.initialize();
        
        teleportManager = new TeleportManager(plugin, dataManager);
        teleportManager.initialize();
        
        teleportCommand = new TeleportCommand(plugin, teleportManager);
        toggleCommand = new TeleportToggleCommand(plugin, dataManager);
        
        try {
            plugin.getCommand("tpf").setExecutor(teleportCommand);
            plugin.getCommand("tpf").setTabCompleter(teleportCommand);
            
            plugin.getCommand("tpftoggle").setExecutor(toggleCommand);
            plugin.getCommand("tpftoggle").setTabCompleter(toggleCommand);
        } catch (Exception e) {
            logWarning("命令处理器注册失败: " + e.getMessage());
        }
        
        teleportListener = new TeleportListener(plugin, teleportManager);
        plugin.getServer().getPluginManager().registerEvents(teleportListener, plugin);
        
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            dataManager.onPlayerJoin(player);
        });
    }
    
    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAllTeleports();
            teleportManager.shutdown();
        }
        
        if (dataManager != null) {
            dataManager.saveAllData();
            dataManager.shutdown();
        }
        
        if (teleportListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(teleportListener);
        }
    }
    
    @Override
    public void onReload() {
        saveAll();
        
        if (teleportManager != null) {
            teleportManager.cancelAllTeleports();
        }
        
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            dataManager.onPlayerJoin(player);
        });
    }
    
    @Override
    public void saveAll() {
        if (dataManager != null) {
            dataManager.saveAllData();
        }
    }
    
    public TeleportDataManager getDataManager() {
        return dataManager;
    }
    
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
