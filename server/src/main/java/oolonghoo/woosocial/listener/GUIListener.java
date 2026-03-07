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
import org.bukkit.inventory.ItemStack;

import java.util.Set;

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
        
        BaseGUI gui = (BaseGUI) holder;
        int slot = event.getRawSlot();
        
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        if (gui.isInputSlot(slot)) {
            return;
        }
        
        event.setCancelled(true);
        
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
            BaseGUI gui = (BaseGUI) holder;
            
            Set<Integer> slots = event.getRawSlots();
            for (int slot : slots) {
                if (slot >= 0 && slot < inventory.getSize() && !gui.isInputSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
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
            BaseGUI gui = (BaseGUI) holder;
            gui.onClose((Player) event.getPlayer());
        }
    }
}
