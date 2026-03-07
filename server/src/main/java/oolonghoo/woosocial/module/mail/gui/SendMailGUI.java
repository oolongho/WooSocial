package com.oolonghoo.woosocial.module.mail.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.BaseGUI;
import com.oolonghoo.woosocial.gui.LoadingState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SendMailGUI extends BaseGUI {
    
    private final UUID receiverUuid;
    private final String receiverName;
    private final LoadingState loadingState;
    private boolean closedBySend = false;
    
    private static final int BACK_SLOT = 0;
    private static final int RECEIVER_INFO_SLOT = 4;
    private static final int[] ITEM_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int SEND_SLOT = 52;
    private static final int CANCEL_SLOT = 45;
    private static final Set<Integer> INPUT_SLOTS = new HashSet<>();
    
    static {
        for (int slot : ITEM_SLOTS) {
            INPUT_SLOTS.add(slot);
        }
    }
    
    public SendMailGUI(WooSocial plugin, Player viewer, UUID receiverUuid, String receiverName, LoadingState loadingState) {
        super(plugin, viewer, "send_mail");
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.loadingState = loadingState;
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("receiver_name", receiverName);
        setPlaceholder("receiver_uuid", receiverUuid.toString());
    }
    
    @Override
    public boolean isInputSlot(int slot) {
        return INPUT_SLOTS.contains(slot);
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(RECEIVER_INFO_SLOT, createReceiverInfoItem());
        inventory.setItem(SEND_SLOT, createSendButton());
        inventory.setItem(CANCEL_SLOT, createCancelButton());
        
        for (int slot : ITEM_SLOTS) {
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                inventory.setItem(slot, createPlaceholderItem());
            }
        }
    }
    
    private ItemStack createReceiverInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(receiverUuid);
        meta.setOwningPlayer(offlinePlayer);
        
        meta.displayName(Component.text("收件人: ", NamedTextColor.GRAY)
                .append(Component.text(receiverName, NamedTextColor.GREEN)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("将物品放入下方格子发送", NamedTextColor.YELLOW));
        lore.add(Component.text("最多可放入 " + ITEM_SLOTS.length + " 个物品", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createPlaceholderItem() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("放入物品", NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("将你要发送的物品", NamedTextColor.GRAY));
        lore.add(Component.text("放入这个格子", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createSendButton() {
        int itemCount = getItemCount();
        
        ItemStack item;
        if (itemCount > 0) {
            item = new ItemStack(Material.LIME_DYE);
        } else {
            item = new ItemStack(Material.GRAY_DYE);
        }
        
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.send-mail"));
        
        List<Component> lore = new ArrayList<>();
        if (itemCount > 0) {
            lore.add(Component.text("发送物品给 ", NamedTextColor.GRAY)
                    .append(Component.text(receiverName, NamedTextColor.GREEN)));
            lore.add(Component.empty());
            lore.add(Component.text("物品数量: ", NamedTextColor.GRAY)
                    .append(Component.text(itemCount, NamedTextColor.YELLOW)));
            lore.add(Component.empty());
            lore.add(Component.text("点击发送", NamedTextColor.AQUA));
        } else {
            lore.add(Component.text("请先放入物品", NamedTextColor.RED));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("取消", NamedTextColor.RED));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("取消发送并返回", NamedTextColor.GRAY));
        lore.add(Component.text("物品将返还到背包", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private int getItemCount() {
        int count = 0;
        for (int slot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                count++;
            }
        }
        return count;
    }
    
    private List<ItemStack> getItemsToSend() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                items.add(item.clone());
            }
        }
        return items;
    }
    
    private void clearItemSlots() {
        for (int slot : ITEM_SLOTS) {
            inventory.setItem(slot, null);
        }
    }
    
    @Override
    public void refresh() {
        inventory.setItem(SEND_SLOT, createSendButton());
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (loadingState.isLoading(player.getUniqueId())) {
            messageManager.send(player, "mail.processing");
            return;
        }
        
        if (slot == BACK_SLOT || slot == CANCEL_SLOT) {
            closedBySend = false;
            player.closeInventory();
            return;
        }
        
        if (slot == SEND_SLOT) {
            handleSend(player);
        }
    }
    
    private void handleSend(Player player) {
        List<ItemStack> itemsToSend = getItemsToSend();
        
        if (itemsToSend.isEmpty()) {
            messageManager.send(player, "mail.no-item");
            return;
        }
        
        loadingState.setLoading(player.getUniqueId(), true);
        
        clearItemSlots();
        
        plugin.getModuleManager().getMailModule().getMailManager()
                .sendMailWithItems(player, receiverUuid, receiverName, itemsToSend)
                .thenAccept(success -> {
                    loadingState.clearLoading(player.getUniqueId());
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            closedBySend = true;
                            player.closeInventory();
                            messageManager.send(player, "mail.send-success", "player", receiverName);
                        } else {
                            returnItemsToPlayer(player, itemsToSend);
                            messageManager.send(player, "mail.send-failed");
                        }
                    });
                });
    }
    
    @Override
    public void onClose(Player player) {
        if (!closedBySend) {
            returnItemsToPlayer(player, getItemsToSend());
        }
    }
    
    private void returnItemsToPlayer(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                var leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
    }
    
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
}
