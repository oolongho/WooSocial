package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.listener.GUIListener;
import com.oolonghoo.woosocial.model.FriendRequest;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendRequestsGUI extends BaseGUI {
    
    private final FriendDataManager dataManager;
    private final UUID viewerUUID;
    private List<FriendRequest> requests;
    
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;
    
    public FriendRequestsGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "friend_requests");
        this.dataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.viewerUUID = viewer.getUniqueId();
        
        loadRequests();
    }
    
    private void loadRequests() {
        dataManager.getPendingRequestsAsync(viewerUUID).thenAccept(requestsList -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                this.requests = requestsList;
                this.totalPages = calculateTotalPages(requests.size(), ITEMS_PER_PAGE);
                initInventory();
                setupItems();
            });
        });
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        inventory.clear();
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        
        if (requests == null || requests.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            var meta = emptyItem.getItemMeta();
            meta.displayName(Component.text("暂无好友请求", NamedTextColor.GRAY));
            emptyItem.setItemMeta(meta);
            inventory.setItem(22, emptyItem);
            return;
        }
        
        int startIndex = getPageStartIndex(currentPage);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, requests.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            FriendRequest request = requests.get(i);
            int slot = CONTENT_SLOTS[i - startIndex];
            inventory.setItem(slot, createRequestItem(request));
        }
        
        setupNavigation();
    }
    
    private ItemStack createRequestItem(FriendRequest request) {
        UUID senderUuid = request.getSenderId();
        String senderName = request.getSenderName();
        
        OfflinePlayer sender = Bukkit.getOfflinePlayer(senderUuid);
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(sender);
        meta.displayName(Component.text(senderName, NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("发送时间: ", NamedTextColor.GRAY)
                .append(Component.text(formatTime(request.getSendTime()), NamedTextColor.YELLOW)));
        
        long remaining = request.getExpireTime() - System.currentTimeMillis();
        if (remaining > 0) {
            lore.add(Component.text("剩余时间: ", NamedTextColor.GRAY)
                    .append(Component.text(formatRemaining(remaining), NamedTextColor.YELLOW)));
        } else {
            lore.add(Component.text("已过期", NamedTextColor.RED));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("左键 ", NamedTextColor.GREEN)
                .append(Component.text("接受请求", NamedTextColor.GRAY)));
        lore.add(Component.text("右键 ", NamedTextColor.RED)
                .append(Component.text("拒绝请求", NamedTextColor.GRAY)));
        
        meta.lore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    private String formatTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60000) return "刚刚";
        if (diff < 3600000) return (diff / 60000) + "分钟前";
        if (diff < 86400000) return (diff / 3600000) + "小时前";
        return (diff / 86400000) + "天前";
    }
    
    private String formatRemaining(long remaining) {
        if (remaining < 60000) return (remaining / 1000) + "秒";
        if (remaining < 3600000) return (remaining / 60000) + "分钟";
        return (remaining / 3600000) + "小时";
    }
    
    @Override
    public void refresh() {
        loadRequests();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            new SocialMainGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 1) {
            currentPage--;
            refresh();
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
            currentPage++;
            refresh();
            return;
        }
        
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (slot == CONTENT_SLOTS[i]) {
                int requestIndex = getPageStartIndex(currentPage) + i;
                if (requestIndex < requests.size()) {
                    FriendRequest request = requests.get(requestIndex);
                    
                    if (clickType == GUIListener.RIGHT_CLICK) {
                        dataManager.denyFriendRequest(viewerUUID, request.getSenderId())
                                .thenAccept(success -> {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        if (success) {
                                            messageManager.send(player, "friend.request-denied", 
                                                    "player", request.getSenderName());
                                            refresh();
                                            player.openInventory(inventory);
                                        }
                                    });
                                });
                    } else {
                        dataManager.acceptFriendRequest(viewerUUID, request.getSenderId())
                                .thenAccept(success -> {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        if (success) {
                                            messageManager.send(player, "friend.request-accepted", 
                                                    "player", request.getSenderName());
                                            refresh();
                                            player.openInventory(inventory);
                                        }
                                    });
                                });
                    }
                }
                return;
            }
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
}
