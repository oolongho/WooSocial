package com.oolonghoo.woosocial.module.friend;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.command.FriendCommand;
import com.oolonghoo.woosocial.listener.GUIListener;
import com.oolonghoo.woosocial.listener.PlayerJoinListener;
import com.oolonghoo.woosocial.listener.PlayerQuitListener;
import com.oolonghoo.woosocial.module.Module;

/**
 * 好友模块
 * 提供好友系统功能，包括好友添加、删除、列表查询等
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class FriendModule extends Module {
    
    private FriendDataManager dataManager;
    private FriendCommand friendCommand;
    private PlayerJoinListener joinListener;
    private PlayerQuitListener quitListener;
    private GUIListener guiListener;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public FriendModule(WooSocial plugin) {
        super(plugin, "friend");
    }
    
    @Override
    public void onEnable() {
        dataManager = new FriendDataManager(plugin);
        dataManager.initialize();
        
        friendCommand = new FriendCommand(plugin, dataManager);
        try {
            plugin.getCommand("friend").setExecutor(friendCommand);
            plugin.getCommand("friend").setTabCompleter(friendCommand);
        } catch (Exception e) {
            logWarning("命令处理器注册失败: " + e.getMessage());
        }
        
        joinListener = new PlayerJoinListener(plugin, dataManager);
        quitListener = new PlayerQuitListener(plugin, dataManager);
        guiListener = new GUIListener();
        
        plugin.getServer().getPluginManager().registerEvents(joinListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(quitListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            dataManager.onPlayerJoin(player);
        });
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
            dataManager.shutdown();
        }
        
        if (joinListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(joinListener);
        }
        if (quitListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(quitListener);
        }
    }
    
    @Override
    public void onReload() {
        saveAll();
        
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
    
    /**
     * 获取数据管理器
     * 
     * @return 数据管理器
     */
    public FriendDataManager getDataManager() {
        return dataManager;
    }
}
