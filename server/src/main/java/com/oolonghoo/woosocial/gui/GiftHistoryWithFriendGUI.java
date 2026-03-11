package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GiftHistoryWithFriendGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private final UUID playerUuid;
    private final UUID friendUuid;
    private final String friendName;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private final List<GiftData> gifts;
    
    private static final int[] GIFT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int SEND_GIFT_SLOT = 49;
    
    public GiftHistoryWithFriendGUI(WooSocial plugin, Player viewer, UUID friendUuid, String friendName) {
        super(plugin, viewer, "gift_history_friend");
        this.playerUuid = viewer.getUniqueId();
        this.friendUuid = friendUuid;
        this.friendName = friendName;
        this.dataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.relationManager = plugin.getModuleManager().getRelationModule().getRelationManager();
        this.gifts = new ArrayList<>();
        
        loadGifts();
    }
    
    private void loadGifts() {
        dataManager.getGiftHistory(playerUuid, friendUuid, 50).thenAccept(giftList -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                gifts.clear();
                gifts.addAll(giftList);
                totalPages = calculateTotalPages(gifts.size(), GIFT_SLOTS.length);
                setupItems();
            });
        });
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("friend_name", friendName);
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(SEND_GIFT_SLOT, createSendGiftButton());
        
        int startIndex = (currentPage - 1) * GIFT_SLOTS.length;
        int endIndex = Math.min(startIndex + GIFT_SLOTS.length, gifts.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= GIFT_SLOTS.length) break;
            
            GiftData gift = gifts.get(i);
            inventory.setItem(GIFT_SLOTS[slotIndex], createGiftItem(gift));
        }
        
        setupNavigation();
    }
    
    private ItemStack createGiftItem(GiftData gift) {
        boolean isSent = gift.getSenderUuid().equals(playerUuid);
        
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        String otherName;
        UUID otherUuid;
        if (isSent) {
            otherUuid = gift.getReceiverUuid();
            if (gift.getReceiverName() != null && !gift.getReceiverName().isEmpty() && !"Unknown".equals(gift.getReceiverName())) {
                otherName = gift.getReceiverName();
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(otherUuid);
                otherName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "未知";
            }
        } else {
            otherUuid = gift.getSenderUuid();
            if (gift.getSenderName() != null && !gift.getSenderName().isEmpty() && !"Unknown".equals(gift.getSenderName())) {
                otherName = gift.getSenderName();
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(otherUuid);
                otherName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "未知";
            }
        }
        
        OfflinePlayer other = Bukkit.getOfflinePlayer(otherUuid);
        meta.setOwningPlayer(other);
        
        GiftType giftType = relationManager.getGiftType(gift.getGiftId());
        String giftName = giftType != null ? giftType.getName() : gift.getGiftId();
        
        if (isSent) {
            meta.displayName(Component.text("→ " + otherName, NamedTextColor.YELLOW));
        } else {
            meta.displayName(Component.text("← " + otherName, NamedTextColor.GREEN));
        }
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if ("coins".equalsIgnoreCase(gift.getGiftId())) {
            lore.add(Component.text((isSent ? "赠送了 " : "收到了 ") + gift.getGiftAmount() + " 金币", NamedTextColor.GOLD));
        } else {
            lore.add(Component.text((isSent ? "赠送了 " : "收到了 ") + giftName, NamedTextColor.GOLD));
        }
        
        lore.add(Component.text("亲密度: ", NamedTextColor.GRAY)
                .append(Component.text("+" + gift.getIntimacyGained(), NamedTextColor.YELLOW)));
        
        String sendTime = DATE_FORMAT.format(new Date(gift.getSendTime()));
        lore.add(Component.text("时间: ", NamedTextColor.GRAY)
                .append(Component.text(sendTime, NamedTextColor.WHITE)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createSendGiftButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("赠送礼物", NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("赠送礼物给 " + friendName, NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击选择礼物", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回关系详情", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        loadGifts();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            new RelationDetailGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == SEND_GIFT_SLOT) {
            new GiftShopGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 1) {
            currentPage--;
            setupItems();
            player.openInventory(inventory);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
            currentPage++;
            setupItems();
            player.openInventory(inventory);
            return;
        }
    }
}
