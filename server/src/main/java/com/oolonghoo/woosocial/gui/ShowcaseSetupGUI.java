package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.ShowcaseData;
import com.oolonghoo.woosocial.module.showcase.ShowcaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.UUID;

public class ShowcaseSetupGUI extends BaseGUI {
    
    private final ShowcaseManager showcaseManager;
    private final UUID viewerUUID;
    private ShowcaseData showcaseData;
    
    private static final int SAVE_SLOT = 4;
    private static final int CLEAR_SLOT = 8;
    
    private static final int HELMET_SLOT = 19;
    private static final int CHESTPLATE_SLOT = 28;
    private static final int LEGGINGS_SLOT = 37;
    private static final int BOOTS_SLOT = 46;
    
    private static final int[] SHOWCASE_SLOTS = {30, 31, 32, 33, 34, 39, 40, 41, 42, 43};
    
    public ShowcaseSetupGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "showcase_setup");
        this.showcaseManager = plugin.getModuleManager().getShowcaseModule().getShowcaseManager();
        this.viewerUUID = viewer.getUniqueId();
        
        this.showcaseData = showcaseManager.getShowcase(viewerUUID, viewer.getName());
        
        initInventory();
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("player_name", viewer.getName());
        setPlaceholder("likes", showcaseData.getLikes());
    }
    
    private void setupItems() {
        inventory.clear();
        fillBorder(54);
        fillRowWithGlass(1);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(SAVE_SLOT, createSaveButton());
        inventory.setItem(CLEAR_SLOT, createClearButton());
        
        setupEquipmentDisplay();
        setupShowcaseItems();
    }
    
    private void fillRowWithGlass(int row) {
        ItemStack glass = createGlassItem();
        int startSlot = row * 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(startSlot + i, glass);
        }
    }
    
    private ItemStack createGlassItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
    
    private void setupEquipmentDisplay() {
        PlayerInventory inv = viewer.getInventory();
        
        ItemStack helmet = inv.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            inventory.setItem(HELMET_SLOT, helmet.clone());
        } else {
            inventory.setItem(HELMET_SLOT, createEmptyEquipmentItem("头盔"));
        }
        
        ItemStack chestplate = inv.getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            inventory.setItem(CHESTPLATE_SLOT, chestplate.clone());
        } else {
            inventory.setItem(CHESTPLATE_SLOT, createEmptyEquipmentItem("胸甲"));
        }
        
        ItemStack leggings = inv.getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR) {
            inventory.setItem(LEGGINGS_SLOT, leggings.clone());
        } else {
            inventory.setItem(LEGGINGS_SLOT, createEmptyEquipmentItem("护腿"));
        }
        
        ItemStack boots = inv.getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            inventory.setItem(BOOTS_SLOT, boots.clone());
        } else {
            inventory.setItem(BOOTS_SLOT, createEmptyEquipmentItem("靴子"));
        }
    }
    
    private ItemStack createEmptyEquipmentItem(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }
    
    private void setupShowcaseItems() {
        List<ItemStack> items = showcaseData.getItems();
        
        for (int i = 0; i < SHOWCASE_SLOTS.length; i++) {
            if (i < items.size() && items.get(i) != null) {
                inventory.setItem(SHOWCASE_SLOTS[i], items.get(i).clone());
            } else {
                inventory.setItem(SHOWCASE_SLOTS[i], createEmptyShowcaseSlot(i));
            }
        }
    }
    
    private ItemStack createEmptyShowcaseSlot(int index) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("展示格子 " + (index + 1), NamedTextColor.YELLOW));
        var lore = List.of(
                Component.text("点击背包物品添加", NamedTextColor.GRAY),
                Component.text("点击此格子移除物品", NamedTextColor.GRAY)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSaveButton() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("保存设置", NamedTextColor.GREEN));
        var lore = List.of(
                Component.text("保存当前展示柜设置", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("点击保存", NamedTextColor.AQUA)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createClearButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("清空展示柜", NamedTextColor.RED));
        var lore = List.of(
                Component.text("清空所有展示物品", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Shift+点击确认", NamedTextColor.RED)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        this.showcaseData = showcaseManager.getShowcase(viewerUUID, viewer.getName());
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            goBack(player);
            return;
        }
        
        if (slot == SAVE_SLOT) {
            showcaseManager.saveShowcase(player);
            messageManager.send(player, "showcase.saved");
            player.closeInventory();
            return;
        }
        
        if (slot == CLEAR_SLOT) {
            if (clickType == 2) {
                showcaseManager.clearShowcase(player);
                messageManager.send(player, "showcase.cleared");
                refresh();
                player.openInventory(inventory);
            }
            return;
        }
        
        for (int showcaseSlot : SHOWCASE_SLOTS) {
            if (slot == showcaseSlot) {
                handleShowcaseSlotClick(player, showcaseSlot);
                return;
            }
        }
    }
    
    private void handleShowcaseSlotClick(Player player, int slot) {
        int index = getShowcaseIndex(slot);
        if (index == -1) return;
        
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            showcaseManager.addItem(player, cursor, index);
            messageManager.send(player, "showcase.item-added");
            refresh();
            player.openInventory(inventory);
        } else {
            ItemStack currentItem = showcaseData.getItem(index);
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                showcaseManager.removeItem(player, index);
                messageManager.send(player, "showcase.item-removed");
                refresh();
                player.openInventory(inventory);
            }
        }
    }
    
    private int getShowcaseIndex(int slot) {
        for (int i = 0; i < SHOWCASE_SLOTS.length; i++) {
            if (SHOWCASE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public boolean isInputSlot(int slot) {
        for (int showcaseSlot : SHOWCASE_SLOTS) {
            if (slot == showcaseSlot) {
                return true;
            }
        }
        return false;
    }
    
    public int[] getShowcaseSlots() {
        return SHOWCASE_SLOTS;
    }
}
