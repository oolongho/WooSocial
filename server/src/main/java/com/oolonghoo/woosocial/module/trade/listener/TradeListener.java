package com.oolonghoo.woosocial.module.trade.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.TradeRequestManager;
import com.oolonghoo.woosocial.module.trade.gui.TradeGUI;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.module.trade.model.TradeState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * 交易监听器
 * 处理交易GUI的交互事件
 */
public class TradeListener implements Listener {
    
    private final WooSocial plugin;
    private final TradeManager tradeManager;
    private final TradeRequestManager requestManager;
    
    public TradeListener(WooSocial plugin, TradeManager tradeManager, TradeRequestManager requestManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.requestManager = requestManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof TradeGUI) {
            TradeGUI tradeGUI = (TradeGUI) holder;
            tradeGUI.handleClick(event);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof TradeGUI) {
            TradeGUI tradeGUI = (TradeGUI) holder;
            TradeSession session = tradeGUI.getSession();
            
            if (session.getState() == TradeState.COUNTDOWN) {
                event.setCancelled(true);
                return;
            }
            
            for (int slot : event.getRawSlots()) {
                if (isOtherSlot(tradeGUI, slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof TradeGUI) {
            TradeGUI tradeGUI = (TradeGUI) holder;
            tradeGUI.handleClose(event);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        requestManager.clearPlayerRequests(player.getUniqueId());
        
        if (tradeManager.isInTrade(player.getUniqueId())) {
            tradeManager.cancelTrade(player.getUniqueId(), "玩家断开连接");
        }
    }
    
    private boolean isOtherSlot(TradeGUI gui, int slot) {
        for (int s : TradeGUI.getOtherSlots()) {
            if (s == slot) return true;
        }
        return false;
    }
}
