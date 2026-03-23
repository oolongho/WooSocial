package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.Module;
import com.oolonghoo.woosocial.module.trade.command.TradeCommand;
import com.oolonghoo.woosocial.module.trade.listener.TradeListener;
import com.oolonghoo.woosocial.module.trade.listener.TradeSecurityListener;
import com.oolonghoo.woosocial.sync.SyncMessage;
import org.bukkit.Bukkit;

public class TradeModule extends Module {
    
    private TradeConfig tradeConfig;
    private TradeManager tradeManager;
    private TradeEconomyManager economyManager;
    private TradeRequestManager requestManager;
    private CrossServerTradeHandler crossServerHandler;
    private TradeCommand tradeCommand;
    private TradeListener tradeListener;
    private TradeSecurityListener securityListener;
    
    public TradeModule(WooSocial plugin) {
        super(plugin, "trade");
    }
    
    @Override
    public void onEnable() {
        tradeConfig = new TradeConfig(plugin);
        tradeConfig.load();
        
        tradeManager = new TradeManager(plugin, tradeConfig);
        
        economyManager = new TradeEconomyManager(plugin);
        
        requestManager = new TradeRequestManager(plugin, tradeConfig);
        
        crossServerHandler = new CrossServerTradeHandler(plugin, tradeManager, tradeConfig);
        
        tradeCommand = new TradeCommand(plugin, tradeManager, requestManager, tradeConfig, economyManager);
        try {
            plugin.getCommand("trade").setExecutor(tradeCommand);
            plugin.getCommand("trade").setTabCompleter(tradeCommand);
        } catch (Exception e) {
            logWarning("命令处理器注册失败: " + e.getMessage());
        }
        
        tradeListener = new TradeListener(tradeManager, requestManager);
        securityListener = new TradeSecurityListener(tradeManager, tradeConfig);
        
        plugin.getServer().getPluginManager().registerEvents(tradeListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(securityListener, plugin);
        
        log("交易模块已启用");
    }
    
    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAllTrades();
        }
        
        if (requestManager != null) {
            requestManager.clearAllRequests();
        }
        
        if (crossServerHandler != null) {
            crossServerHandler.cleanup();
        }
        
        if (tradeListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(tradeListener);
        }
        if (securityListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(securityListener);
        }
        
        log("交易模块已关闭");
    }
    
    @Override
    public void onReload() {
        if (tradeConfig != null) {
            tradeConfig.load();
        }
    }
    
    @Override
    public void saveAll() {
    }
    
    public void handleCrossServerMessage(SyncMessage message) {
        if (crossServerHandler != null) {
            crossServerHandler.handleSyncMessage(message);
        }
    }
    
    public TradeConfig getTradeConfig() {
        return tradeConfig;
    }
    
    public TradeManager getTradeManager() {
        return tradeManager;
    }
    
    public TradeRequestManager getRequestManager() {
        return requestManager;
    }
    
    public CrossServerTradeHandler getCrossServerHandler() {
        return crossServerHandler;
    }
}
