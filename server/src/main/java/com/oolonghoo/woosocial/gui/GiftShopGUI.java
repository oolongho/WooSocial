package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.relation.GiftManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GiftShopGUI extends BaseGUI {
    
    private final UUID receiverUuid;
    private final String receiverName;
    private final RelationManager relationManager;
    private final GiftManager giftManager;
    
    private static final int[] GIFT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int RECEIVER_INFO_SLOT = 4;
    
    public GiftShopGUI(WooSocial plugin, Player viewer, UUID receiverUuid, String receiverName) {
        super(plugin, viewer, "gift_shop");
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.relationManager = plugin.getModuleManager().getRelationModule().getRelationManager();
        this.giftManager = plugin.getModuleManager().getRelationModule().getGiftManager();
        
        initInventory();
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("receiver_name", receiverName);
        setPlaceholder("receiver_uuid", receiverUuid.toString());
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(RECEIVER_INFO_SLOT, createReceiverInfoItem());
        
        List<GiftType> gifts = new ArrayList<>(relationManager.getAllGiftTypes());
        totalPages = calculateTotalPages(gifts.size(), GIFT_SLOTS.length);
        
        int startIndex = (currentPage - 1) * GIFT_SLOTS.length;
        int endIndex = Math.min(startIndex + GIFT_SLOTS.length, gifts.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= GIFT_SLOTS.length) break;
            
            GiftType gift = gifts.get(i);
            inventory.setItem(GIFT_SLOTS[slotIndex], createGiftItem(gift));
        }
        
        setupNavigation();
    }
    
    private ItemStack createReceiverInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(receiverUuid);
        meta.setOwningPlayer(offlinePlayer);
        
        meta.displayName(Component.text("收件人: ", NamedTextColor.GRAY)
                .append(Component.text(receiverName, NamedTextColor.GREEN)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("选择礼物赠送给TA", NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createGiftItem(GiftType gift) {
        ItemStack item = new ItemStack(gift.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(gift.getName(), NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        
        if (gift.getDescription() != null && !gift.getDescription().isEmpty()) {
            lore.add(Component.text(gift.getDescription(), NamedTextColor.GRAY));
            lore.add(Component.empty());
        }
        
        lore.add(Component.text("亲密度: ", NamedTextColor.GRAY)
                .append(Component.text("+" + gift.getIntimacy(), NamedTextColor.YELLOW)));
        
        String costStr = getCostString(gift);
        lore.add(Component.text("价格: ", NamedTextColor.GRAY)
                .append(Component.text(costStr, NamedTextColor.WHITE)));
        
        if (gift.isCoinsGift()) {
            lore.add(Component.text("每次赠送: ", NamedTextColor.GRAY)
                    .append(Component.text(gift.getAmountPerSend() + " 金币", NamedTextColor.YELLOW)));
        }
        
        int remaining = giftManager.getRemainingDailyLimit(viewer, receiverUuid, gift.getId());
        if (remaining >= 0) {
            lore.add(Component.text("今日剩余: ", NamedTextColor.GRAY)
                    .append(Component.text(remaining + " 次", NamedTextColor.AQUA)));
        } else {
            lore.add(Component.text("今日剩余: ", NamedTextColor.GRAY)
                    .append(Component.text("∞", NamedTextColor.AQUA)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("点击赠送", NamedTextColor.GREEN));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getCostString(GiftType gift) {
        if (gift.isFree()) {
            return "免费";
        }
        
        StringBuilder sb = new StringBuilder();
        if (gift.getCostCoins() > 0) {
            sb.append(gift.getCostCoins()).append(" 金币");
        }
        if (gift.getCostPoints() > 0) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(gift.getCostPoints()).append(" 点券");
        }
        return sb.toString();
    }
    
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回好友详情", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            new FriendDetailGUI(plugin, player, receiverUuid, receiverName).open(player);
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 1) {
            currentPage--;
            refresh();
            player.openInventory(inventory);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
            currentPage++;
            refresh();
            player.openInventory(inventory);
            return;
        }
        
        int giftIndex = getGiftIndexFromSlot(slot);
        if (giftIndex >= 0) {
            List<GiftType> gifts = new ArrayList<>(relationManager.getAllGiftTypes());
            int actualIndex = (currentPage - 1) * GIFT_SLOTS.length + giftIndex;
            
            if (actualIndex < gifts.size()) {
                GiftType gift = gifts.get(actualIndex);
                handleSendGift(player, gift);
            }
        }
    }
    
    private int getGiftIndexFromSlot(int slot) {
        for (int i = 0; i < GIFT_SLOTS.length; i++) {
            if (GIFT_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
    
    private void handleSendGift(Player player, GiftType gift) {
        giftManager.sendGift(player, receiverUuid, gift.getId()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    if (gift.isCoinsGift()) {
                        messageManager.send(player, "gift.coins-sent",
                                "amount", String.valueOf(result.getValue()),
                                "player", receiverName,
                                "intimacy", String.valueOf(result.getIntimacyGained()));
                    } else {
                        messageManager.send(player, "gift.sent",
                                "gift", gift.getName(),
                                "player", receiverName,
                                "intimacy", String.valueOf(result.getIntimacyGained()));
                    }
                    
                    Player receiver = Bukkit.getPlayer(receiverUuid);
                    if (receiver != null) {
                        if (gift.isCoinsGift()) {
                            messageManager.send(receiver, "gift.coins-received",
                                    "amount", String.valueOf(result.getValue()),
                                    "player", player.getName());
                        } else {
                            messageManager.send(receiver, "gift.received",
                                    "gift", gift.getName(),
                                    "player", player.getName());
                        }
                    }
                    
                    refresh();
                    player.openInventory(inventory);
                } else {
                    messageManager.send(player, result.getMessageKey(),
                            "value", String.valueOf(result.getValue()));
                }
            });
        });
    }
    
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
}
