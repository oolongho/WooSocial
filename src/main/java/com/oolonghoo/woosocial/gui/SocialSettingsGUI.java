package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.teleport.TeleportDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SocialSettingsGUI extends BaseGUI {
    
    private final TeleportDataManager teleportDataManager;
    private final UUID viewerUUID;
    
    private static final int TELEPORT_TOGGLE_SLOT = 10;
    
    public SocialSettingsGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "social_settings");
        this.teleportDataManager = plugin.getModuleManager().getTeleportModule().getDataManager();
        this.viewerUUID = viewer.getUniqueId();
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        inventory.clear();
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        
        boolean allowTeleport = teleportDataManager.isAllowFriendTeleport(viewerUUID);
        
        ItemStack toggleItem = createTeleportToggleItem(allowTeleport);
        inventory.setItem(TELEPORT_TOGGLE_SLOT, toggleItem);
    }
    
    private ItemStack createTeleportToggleItem(boolean allowTeleport) {
        ItemStack item = new ItemStack(allowTeleport ? Material.LIME_DYE : Material.GRAY_DYE);
        var meta = item.getItemMeta();
        
        meta.displayName(messageManager.getComponent("gui.button-allow-teleport"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-allow-teleport"));
        lore.add(Component.empty());
        
        if (allowTeleport) {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("允许", NamedTextColor.GREEN)));
        } else {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("禁止", NamedTextColor.RED)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("点击切换", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    @Override
    public void refresh() {
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            new SocialMainGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == TELEPORT_TOGGLE_SLOT) {
            boolean newState = teleportDataManager.toggleFriendTeleport(viewerUUID);
            
            if (newState) {
                messageManager.send(player, "teleport.toggle-teleport-on");
            } else {
                messageManager.send(player, "teleport.toggle-teleport-off");
            }
            
            refresh();
            player.openInventory(inventory);
            return;
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
}
