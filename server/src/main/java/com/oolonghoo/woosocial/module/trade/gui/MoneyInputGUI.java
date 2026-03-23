package com.oolonghoo.woosocial.module.trade.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.TradeEconomyManager;
import com.oolonghoo.woosocial.module.trade.model.TradeOffer;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.module.trade.model.TradeState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * 金币输入界面
 * 使用聊天栏输入金额
 */
public class MoneyInputGUI implements InventoryHolder, Listener {
    
    private final WooSocial plugin;
    private final Player player;
    private final TradeSession session;
    private final TradeGUI tradeGUI;
    private final TradeEconomyManager economyManager;
    private final MessageManager messageManager;
    
    private Inventory inventory;
    private boolean completed = false;
    
    public MoneyInputGUI(WooSocial plugin, Player player, TradeSession session, 
                         TradeGUI tradeGUI, TradeEconomyManager economyManager) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.tradeGUI = tradeGUI;
        this.economyManager = economyManager;
        this.messageManager = plugin.getMessageManager();
        
        createInventory();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void createInventory() {
        inventory = Bukkit.createInventory(this, 9, Component.text("§8输入金币数量"));
        
        ItemStack hintItem = new ItemStack(Material.PAPER);
        ItemMeta hintMeta = hintItem.getItemMeta();
        hintMeta.displayName(Component.text("§e请在聊天栏输入金额"));
        hintItem.setItemMeta(hintMeta);
        inventory.setItem(4, hintItem);
        
        ItemStack cancelItem = new ItemStack(Material.RED_DYE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("§c取消"));
        cancelItem.setItemMeta(cancelMeta);
        inventory.setItem(8, cancelItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (completed) return;
        
        event.setCancelled(true);
        
        String message = event.getMessage();
        
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
            Bukkit.getScheduler().runTask(plugin, this::cancel);
            return;
        }
        
        try {
            double amount = Double.parseDouble(message);
            
            if (amount < 0) {
                messageManager.send(player, "trade.invalid-amount");
                return;
            }
            
            if (economyManager != null && economyManager.hasVault() && 
                !economyManager.hasMoney(player, amount)) {
                messageManager.send(player, "trade.not-enough-money");
                return;
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> complete(amount));
            
        } catch (NumberFormatException e) {
            messageManager.send(player, "trade.invalid-amount");
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;
        
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        if (slot == 8) {
            cancel();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;
        
        if (!completed) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (tradeGUI.getSession().getState() != TradeState.CANCELLED && 
                    tradeGUI.getSession().getState() != TradeState.COMPLETED) {
                    player.openInventory(tradeGUI.getInventory());
                    tradeGUI.refresh();
                }
            }, 1L);
        }
        
        HandlerList.unregisterAll(this);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            completed = true;
            HandlerList.unregisterAll(this);
        }
    }
    
    private void complete(double amount) {
        completed = true;
        
        TradeOffer offer = session.getOffer(player.getUniqueId());
        offer.setMoney(amount);
        
        messageManager.send(player, "trade.money-set", "amount", String.format("%.2f", amount));
        
        player.closeInventory();
    }
    
    private void cancel() {
        completed = true;
        player.closeInventory();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
