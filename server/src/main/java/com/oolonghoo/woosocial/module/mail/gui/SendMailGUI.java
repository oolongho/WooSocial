package com.oolonghoo.woosocial.module.mail.gui;

import com.oolonghoo.woosocial.WooSocial;
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
    private final LoadingState mailLoadingState;
    private boolean closedBySend = false;
    
    private static final int SEND_MAIL_BACK_SLOT = 0;
    private static final int RECEIVER_INFO_SLOT = 4;
    private static final int SEND_SLOT = 49;
    
    private static final int[] ITEM_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
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
        this.mailLoadingState = loadingState;
        
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
        fillSecondRow();
        fillFifthRow();
        fillSecondColumn();
        fillEighthColumn();
        
        inventory.setItem(SEND_MAIL_BACK_SLOT, createBackButton());
        inventory.setItem(RECEIVER_INFO_SLOT, createReceiverInfoItem());
        inventory.setItem(SEND_SLOT, createSendButton());
    }
    
    private void fillSecondRow() {
        ItemStack borderItem = createYellowGreenBorderItem();
        for (int col = 0; col < 9; col++) {
            int slot = 9 + col;
            inventory.setItem(slot, borderItem);
        }
    }
    
    private void fillFifthRow() {
        ItemStack borderItem = createYellowGreenBorderItem();
        for (int col = 0; col < 9; col++) {
            int slot = 36 + col;
            inventory.setItem(slot, borderItem);
        }
    }
    
    private void fillSecondColumn() {
        ItemStack borderItem = createYellowGreenBorderItem();
        for (int row = 0; row < 6; row++) {
            int slot = row * 9 + 1;
            inventory.setItem(slot, borderItem);
        }
    }
    
    private void fillEighthColumn() {
        ItemStack borderItem = createYellowGreenBorderItem();
        for (int row = 0; row < 6; row++) {
            int slot = row * 9 + 7;
            inventory.setItem(slot, borderItem);
        }
    }
    
    private ItemStack createYellowGreenBorderItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
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
    
    private int getItemCount() {
        int count = 0;
        for (int slot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }
    
    private List<ItemStack> getItemsToSend() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
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
        if (mailLoadingState.isLoading(player.getUniqueId())) {
            messageManager.send(player, "mail.processing");
            return;
        }
        
        if (slot == SEND_MAIL_BACK_SLOT) {
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
        
        mailLoadingState.setLoading(player.getUniqueId(), true);
        
        clearItemSlots();
        
        plugin.getModuleManager().getMailModule().getMailManager()
                .sendMailWithItems(player, receiverUuid, receiverName, itemsToSend)
                .thenAccept(success -> {
                    mailLoadingState.clearLoading(player.getUniqueId());
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            closedBySend = true;
                            player.closeInventory();
                        } else {
                            returnItemsToPlayer(player, itemsToSend);
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
            if (item != null && item.getType() != Material.AIR) {
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