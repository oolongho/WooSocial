package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.Module;
import com.oolonghoo.woosocial.module.relation.command.GiftCommand;
import com.oolonghoo.woosocial.module.relation.command.RelationCommand;
import com.oolonghoo.woosocial.module.relation.listener.RelationListener;
import org.bukkit.Bukkit;

public class RelationModule extends Module {
    
    private RelationDataManager dataManager;
    private RelationManager relationManager;
    private GiftManager giftManager;
    private IntimacyManager intimacyManager;
    private RelationCommand relationCommand;
    private GiftCommand giftCommand;
    private RelationListener relationListener;
    
    public RelationModule(WooSocial plugin) {
        super(plugin, "relation");
    }
    
    @Override
    public void onEnable() {
        dataManager = new RelationDataManager(plugin);
        dataManager.initialize();
        
        relationManager = new RelationManager(plugin, dataManager);
        relationManager.initialize();
        
        giftManager = new GiftManager(plugin, dataManager, relationManager);
        intimacyManager = new IntimacyManager(plugin, dataManager, relationManager);
        
        relationCommand = new RelationCommand(plugin, dataManager, relationManager, intimacyManager);
        try {
            plugin.getCommand("relation").setExecutor(relationCommand);
            plugin.getCommand("relation").setTabCompleter(relationCommand);
        } catch (Exception e) {
            logWarning("关系命令处理器注册失败: " + e.getMessage());
        }
        
        giftCommand = new GiftCommand(plugin, dataManager, relationManager, giftManager);
        try {
            plugin.getCommand("gift").setExecutor(giftCommand);
            plugin.getCommand("gift").setTabCompleter(giftCommand);
        } catch (Exception e) {
            logWarning("赠礼命令处理器注册失败: " + e.getMessage());
        }
        
        relationListener = new RelationListener(plugin, dataManager, relationManager);
        plugin.getServer().getPluginManager().registerEvents(relationListener, plugin);
        
        startDailyResetTask();
        
        log("关系模块已启用");
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        if (relationListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(relationListener);
        }
        
        log("关系模块已关闭");
    }
    
    @Override
    public void onReload() {
        if (relationManager != null) {
            relationManager.reload();
        }
        saveAll();
    }
    
    @Override
    public void saveAll() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
    }
    
    private void startDailyResetTask() {
        long ticksPerDay = 20L * 60 * 60 * 24;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            dataManager.getRelationDAO().cleanOldDailyGifts(7);
        }, ticksPerDay, ticksPerDay);
    }
    
    public RelationDataManager getDataManager() {
        return dataManager;
    }
    
    public RelationManager getRelationManager() {
        return relationManager;
    }
    
    public GiftManager getGiftManager() {
        return giftManager;
    }
    
    public IntimacyManager getIntimacyManager() {
        return intimacyManager;
    }
}
