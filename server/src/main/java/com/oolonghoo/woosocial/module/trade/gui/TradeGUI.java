package com.oolonghoo.woosocial.module.trade.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.BaseGUI;
import com.oolonghoo.woosocial.module.trade.TradeConfig;
import com.oolonghoo.woosocial.module.trade.TradeEconomyManager;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.model.TradeOffer;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.module.trade.model.TradeState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 交易界面
 * 左右对称布局，左侧自己的物品，右侧对方的物品
 */
public class TradeGUI implements InventoryHolder {
    
    private static final int GUI_SIZE = 54;
    private static final int[] MY_SLOTS = {0, 1, 2, 3, 4, 9, 10, 11, 12, 13, 18, 19, 20, 21, 22, 27, 28, 29, 30, 31};
    private static final int[] OTHER_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int READY_BUTTON_SLOT = 47;
    private static final int CANCEL_BUTTON_SLOT = 49;
    private static final int MONEY_BUTTON_SLOT = 45;
    
    private final WooSocial plugin;
    private final TradeManager tradeManager;
    private final TradeConfig config;
    private final TradeEconomyManager economyManager;
    private final MessageManager messageManager;
    
    private final Player player;
    private final UUID playerUuid;
    private final TradeSession session;
    
    private Inventory inventory;
    
    public TradeGUI(WooSocial plugin, TradeManager tradeManager, TradeConfig config, TradeEconomyManager economyManager, Player player, TradeSession session) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.config = config;
        this.economyManager = economyManager;
        this.messageManager = plugin.getMessageManager();
        this.player = player;
        this.playerUuid = player.getUniqueId();
        this.session = session;
        
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, Component.text("§8交易 - §e" + session.getOtherPlayerName(playerUuid)));
        
        initializeGUI();
    }
    
    private void initializeGUI() {
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, createDecorationItem());
        }
        
        for (int slot : MY_SLOTS) {
            inventory.setItem(slot, null);
        }
        for (int slot : OTHER_SLOTS) {
            inventory.setItem(slot, null);
        }
        
        updateReadyButton();
        updateCancelButton();
        updateMoneyButton();
    }
    
    private ItemStack createDecorationItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(""));
        item.setItemMeta(meta);
        return item;
    }
    
    public void updateReadyButton() {
        boolean isReady = session.isReady(playerUuid);
        ItemStack item = new ItemStack(isReady ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (isReady) {
            meta.displayName(Component.text("§a已准备"));
        } else {
            meta.displayName(Component.text("§e点击准备"));
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7修改报价会取消准备"));
        lore.add(Component.text(""));
        lore.add(Component.text("§b点击切换准备状态"));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(READY_BUTTON_SLOT, item);
    }
    
    public void updateCancelButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c取消交易"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7取消本次交易"));
        lore.add(Component.text("§c物品将返还"));
        lore.add(Component.text(""));
        lore.add(Component.text("§b点击取消"));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(CANCEL_BUTTON_SLOT, item);
    }
    
    public void updateMoneyButton() {
        TradeOffer offer = session.getOffer(playerUuid);
        double money = offer.getMoney();
        
        ItemStack item = new ItemStack(money > 0 ? Material.GOLD_INGOT : Material.AIR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6金币: §e" + String.format("%.2f", money)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7点击输入金币数量"));
        if (config.isVaultEnabled() && economyManager.hasVault()) {
            double balance = economyManager.getBalance(player);
            lore.add(Component.text("§7当前余额: §e" + String.format("%.2f", balance)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(MONEY_BUTTON_SLOT, item);
    }
    
    public void updateOtherItems() {
        TradeOffer otherOffer = session.getOtherOffer(playerUuid);
        int index = 0;
        for (int slot : OTHER_SLOTS) {
            if (index < otherOffer.getItems().size()) {
                ItemStack item = otherOffer.getItems().get(index);
                inventory.setItem(slot, item != null ? item.clone() : null);
            } else {
                inventory.setItem(slot, null);
            }
            index++;
        }
    }
    
    public void updateMyItems() {
        TradeOffer myOffer = session.getOffer(playerUuid);
        int index = 0;
        for (int slot : MY_SLOTS) {
            if (index < myOffer.getItems().size()) {
                ItemStack item = myOffer.getItems().get(index);
                inventory.setItem(slot, item != null ? item.clone() : null);
            } else {
                inventory.setItem(slot, null);
            }
            index++;
        }
    }
    
    public void refresh() {
        updateReadyButton();
        updateMoneyButton();
        updateMyItems();
        updateOtherItems();
    }
    
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        if (slot == READY_BUTTON_SLOT) {
            event.setCancelled(true);
            if (session.getState() == TradeState.PENDING) {
                tradeManager.toggleReady(playerUuid);
                refresh();
            }
            return;
        }
        
        if (slot == CANCEL_BUTTON_SLOT) {
            event.setCancelled(true);
            tradeManager.cancelTrade(playerUuid, "玩家取消");
            return;
        }
        
        if (slot == MONEY_BUTTON_SLOT) {
            event.setCancelled(true);
            openMoneyInput();
            return;
        }
        
        if (isMySlot(slot)) {
            if (session.getState() == TradeState.COUNTDOWN) {
                event.setCancelled(true);
                return;
            }
            return;
        }
        
        if (isOtherSlot(slot)) {
            event.setCancelled(true);
            return;
        }
        
        event.setCancelled(true);
    }
    
    public void handleClose(InventoryCloseEvent event) {
        if (session.getState() == TradeState.COUNTDOWN) {
            tradeManager.acknowledgeGuiSwitch(player);
        } else if (session.getState() == TradeState.PENDING) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (tradeManager.isInTrade(playerUuid) && !player.isOnline()) {
                    tradeManager.cancelTrade(playerUuid, "玩家关闭界面");
                }
            }, 1L);
        }
    }
    
    private void openMoneyInput() {
        player.closeInventory();
        messageManager.send(player, "trade.money-input-hint");
        
        new com.oolonghoo.woosocial.module.trade.gui.MoneyInputGUI(plugin, player, session, this).open();
    }
    
    private boolean isMySlot(int slot) {
        for (int s : MY_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    private boolean isOtherSlot(int slot) {
        for (int s : OTHER_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
    
    public static int[] getMySlots() {
        return MY_SLOTS;
    }
    
    public static int[] getOtherSlots() {
        return OTHER_SLOTS;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public TradeSession getSession() {
        return session;
    }
}
