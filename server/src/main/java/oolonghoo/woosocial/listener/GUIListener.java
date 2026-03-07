package com.oolonghoo.woosocial.listener;

import com.oolonghoo.woosocial.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {
    
    public static final int LEFT_CLICK = 0;
    public static final int RIGHT_CLICK = 1;
    public static final int SHIFT_CLICK = 2;
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        InventoryHolder holder = inventory.getHolder();
        
        if (!(holder instanceof BaseGUI)) {
            return;
        }
        
        event.setCancelled(true);
        
        BaseGUI gui = (BaseGUI) holder;
        int slot = event.getRawSlot();
        
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        int clickType = LEFT_CLICK;
        if (event.isRightClick()) {
            clickType = RIGHT_CLICK;
        } else if (event.isShiftClick()) {
            clickType = SHIFT_CLICK;
        }
        
        try {
            gui.handleClick(slot, player, clickType);
        } catch (Exception e) {
            player.sendMessage("§cGUI操作发生错误，请联系管理员");
            e.printStackTrace();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (holder instanceof BaseGUI) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        
        if (holder instanceof BaseGUI) {
        }
    }
}
