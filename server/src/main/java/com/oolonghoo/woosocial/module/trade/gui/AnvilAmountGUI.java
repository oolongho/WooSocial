package com.oolonghoo.woosocial.module.trade.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

/**
 * Anvil GUI 金额输入
 * 提供更好的金额输入体验
 */
public class AnvilAmountGUI implements Listener {
    
    private final Player player;
    private final Consumer<Long> callback;
    private final MessageManager messageManager;
    
    private final Inventory inventory;
    private long maxAmount = Long.MAX_VALUE;
    private boolean completed = false;
    
    private AnvilAmountGUI(Player player, String title, Consumer<Long> callback, MessageManager messageManager) {
        this.player = player;
        this.callback = callback;
        this.messageManager = messageManager;
        
        this.inventory = Bukkit.createInventory(null, 3, Component.text(title));
        
        setupInventory();
        
        player.openInventory(inventory);
    }
    
    public static AnvilAmountGUI create(WooSocial plugin, Player player, String title, Consumer<Long> callback) {
        AnvilAmountGUI gui = new AnvilAmountGUI(player, title, callback, plugin.getMessageManager());
        Bukkit.getPluginManager().registerEvents(gui, plugin);
        return gui;
    }
    
    private void setupInventory() {
        AnvilInventory anvil = (AnvilInventory) inventory;
        
        // 第一个槽位放置提示物品
        ItemStack hintItem = new ItemStack(Material.PAPER);
        ItemMeta meta = hintItem.getItemMeta();
        meta.displayName(Component.text("§e在此输入金额"));
        hintItem.setItemMeta(meta);
        anvil.setItem(0, hintItem);
        
        // 第二个槽位放置结果物品
        ItemStack resultItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta resultMeta = resultItem.getItemMeta();
        resultMeta.displayName(Component.text("§a点击确认"));
        resultItem.setItemMeta(resultMeta);
        anvil.setItem(2, resultItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().equals(player)) return;
        if (!event.getInventory().equals(inventory)) return;
        
        event.setCancelled(true);
        
        // 点击结果槽位
        if (event.getSlot() == 2) {
            AnvilInventory anvil = (AnvilInventory) inventory;
            ItemStack result = anvil.getItem(2);
            
            if (result != null && result.getType() == Material.GOLD_INGOT) {
                // 获取输入的数字（通过物品名称或经验值）
                long amount = getEnteredAmount();
                
                if (amount > 0 && amount <= maxAmount) {
                    completed = true;
                    callback.accept(amount);
                    close();
                } else {
                    messageManager.send(player, "trade.invalid-amount");
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            close();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!event.getInventory().equals(inventory)) return;
        
        // 如果未完成，取消回调
        if (!completed) {
            close();
        }
    }
    
    /**
     * 获取输入的金额
     * 通过铁砧的物品名称解析
     */
    private long getEnteredAmount() {
        AnvilInventory anvil = (AnvilInventory) inventory;
        ItemStack item = anvil.getItem(0);
        
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            try {
                // 尝试解析名称中的数字
                String numberStr = displayName.replaceAll("[^0-9]", "");
                if (!numberStr.isEmpty()) {
                    return Long.parseLong(numberStr);
                }
            } catch (NumberFormatException e) {
                // 解析失败，返回 0
            }
        }
        
        return 0;
    }
    
    /**
     * 设置最大金额
     */
    public AnvilAmountGUI setMaxAmount(long maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }
    
    /**
     * 关闭界面
     */
    public void close() {
        HandlerList.unregisterAll(this);
        player.closeInventory();
    }
    
    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return completed;
    }
}
