package com.oolonghoo.woosocial.module.trade.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.model.TradeOffer;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * 金币输入界面
 * 使用铁砧GUI输入金额
 */
public class MoneyInputGUI implements InventoryHolder, Listener {
    
    private final WooSocial plugin;
    private final Player player;
    private final TradeSession session;
    private final TradeGUI tradeGUI;
    private final MessageManager messageManager;
    
    private Inventory inventory;
    private AnvilInventory anvilInventory;
    private boolean completed = false;
    
    public MoneyInputGUI(WooSocial plugin, Player player, TradeSession session, TradeGUI tradeGUI) {
        this.plugin = plugin;
        this.player = player;
        this.session = session;
        this.tradeGUI = tradeGUI;
        this.messageManager = plugin.getMessageManager();
        
        createInventory();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void createInventory() {
        inventory = Bukkit.createInventory(this, 9, Component.text("§8输入金币数量"));
        
        ItemStack inputItem = new ItemStack(Material.PAPER);
        ItemMeta meta = inputItem.getItemMeta();
        meta.displayName(Component.text("§f输入金额"));
        inputItem.setItemMeta(meta);
        
        inventory.setItem(0, inputItem);
        
        ItemStack confirmItem = new ItemStack(Material.LIME_DYE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("§a确认"));
        confirmItem.setItemMeta(confirmMeta);
        inventory.setItem(8, confirmItem);
        
        ItemStack cancelItem = new ItemStack(Material.RED_DYE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("§c取消"));
        cancelItem.setItemMeta(cancelMeta);
        inventory.setItem(7, cancelItem);
    }
    
    public void open() {
        player.openInventory(inventory);
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
            complete();
        } else if (slot == 7) {
            cancel();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;
        
        if (!completed) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (tradeManager().isInTrade(player.getUniqueId())) {
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
            HandlerList.unregisterAll(this);
        }
    }
    
    private void complete() {
        completed = true;
        
        ItemStack inputItem = inventory.getItem(0);
        if (inputItem == null || !inputItem.hasItemMeta()) {
            messageManager.send(player, "trade.invalid-amount");
            cancel();
            return;
        }
        
        String input = inputItem.getItemMeta().getDisplayName();
        input = input.replace("§f", "").replace("§e", "").trim();
        
        try {
            double amount = Double.parseDouble(input);
            
            if (amount < 0) {
                messageManager.send(player, "trade.invalid-amount");
                cancel();
                return;
            }
            
            if (plugin.getVaultHook() != null && !plugin.getVaultHook().has(player, amount)) {
                messageManager.send(player, "trade.not-enough-money");
                cancel();
                return;
            }
            
            TradeOffer offer = session.getOffer(player.getUniqueId());
            offer.setMoney(amount);
            
            messageManager.send(player, "trade.money-set", "amount", String.format("%.2f", amount));
            
            player.closeInventory();
            
        } catch (NumberFormatException e) {
            messageManager.send(player, "trade.invalid-amount");
            cancel();
        }
    }
    
    private void cancel() {
        completed = true;
        player.closeInventory();
    }
    
    private TradeManager tradeManager() {
        return plugin.getModuleManager().getModule("trade", com.oolonghoo.woosocial.module.trade.TradeModule.class).getTradeManager();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
