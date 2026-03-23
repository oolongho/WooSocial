package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.Module;
import com.oolonghoo.woosocial.module.trade.command.TradeCommand;
import com.oolonghoo.woosocial.module.trade.listener.TradeListener;
import com.oolonghoo.woosocial.module.trade.listener.TradeSecurityListener;
import org.bukkit.Bukkit;

/**
 * 交易模块
 * 提供玩家之间的物品和经济货币交易功能
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TradeModule extends Module {
    
    private TradeConfig tradeConfig;
    private TradeManager tradeManager;
    private TradeRequestManager requestManager;
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
        
        requestManager = new TradeRequestManager(plugin, tradeConfig);
        
        tradeCommand = new TradeCommand(plugin, tradeManager, requestManager, tradeConfig);
        try {
            plugin.getCommand("trade").setExecutor(tradeCommand);
            plugin.getCommand("trade").setTabCompleter(tradeCommand);
        } catch (Exception e) {
            logWarning("命令处理器注册失败: " + e.getMessage());
        }
        
        tradeListener = new TradeListener(plugin, tradeManager, requestManager);
        securityListener = new TradeSecurityListener(plugin, tradeManager, tradeConfig);
        
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
    
    public TradeConfig getTradeConfig() {
        return tradeConfig;
    }
    
    public TradeManager getTradeManager() {
        return tradeManager;
    }
    
    public TradeRequestManager getRequestManager() {
        return requestManager;
    }
}
