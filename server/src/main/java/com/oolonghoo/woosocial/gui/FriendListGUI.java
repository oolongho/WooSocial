package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.listener.GUIListener;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.teleport.TeleportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendListGUI extends BaseGUI {
    
    private final FriendDataManager dataManager;
    private final TeleportManager teleportManager;
    private final UUID viewerUUID;
    private List<FriendData> friends;
    
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;
    
    public FriendListGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "friend_list");
        this.dataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.teleportManager = plugin.getModuleManager().getTeleportModule().getTeleportManager();
        this.viewerUUID = viewer.getUniqueId();
        
        initInventory();
        this.friends = dataManager.getFriendList(viewerUUID);
        this.totalPages = calculateTotalPages(friends.size(), ITEMS_PER_PAGE);
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        inventory.clear();
        
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        
        int startIndex = getPageStartIndex(currentPage);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, friends.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            FriendData friend = friends.get(i);
            int slot = CONTENT_SLOTS[i - startIndex];
            inventory.setItem(slot, createFriendItem(friend));
        }
        
        if (friends.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            var meta = emptyItem.getItemMeta();
            meta.displayName(Component.text("暂无好友", NamedTextColor.GRAY));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("使用 /friend add <玩家>", NamedTextColor.YELLOW));
            lore.add(Component.text("来添加好友", NamedTextColor.YELLOW));
            meta.lore(lore);
            
            emptyItem.setItemMeta(meta);
            inventory.setItem(22, emptyItem);
        }
        
        setupNavigation();
    }
    
    private ItemStack createFriendItem(FriendData friend) {
        UUID friendUuid = friend.getFriendUuid();
        String friendName = friend.getFriendName();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUuid);
        boolean isOnline = offlinePlayer.isOnline();
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        
        if (isOnline) {
            meta.displayName(Component.text(friendName, NamedTextColor.GREEN));
        } else {
            meta.displayName(Component.text(friendName, NamedTextColor.GRAY));
        }
        
        List<Component> lore = new ArrayList<>();
        
        if (isOnline) {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("在线", NamedTextColor.GREEN)));
            
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                lore.add(Component.text("服务器: ", NamedTextColor.GRAY)
                        .append(Component.text(onlinePlayer.getServer().getName(), NamedTextColor.YELLOW)));
            }
        } else {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("离线", NamedTextColor.RED)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("好友时间: ", NamedTextColor.GRAY)
                .append(Component.text(formatTime(friend.getAddTime()), NamedTextColor.YELLOW)));
        
        lore.add(Component.empty());
        lore.add(Component.text("左键 ", NamedTextColor.AQUA)
                .append(Component.text("查看详情", NamedTextColor.GRAY)));
        if (isOnline) {
            lore.add(Component.text("右键 ", NamedTextColor.GREEN)
                    .append(Component.text("传送到好友", NamedTextColor.GRAY)));
        }
        
        meta.lore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    @Override
    public void refresh() {
        this.friends = dataManager.getFriendList(viewerUUID);
        this.totalPages = calculateTotalPages(friends.size(), ITEMS_PER_PAGE);
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            goBack(player);
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
                int startIndex = getPageStartIndex(currentPage);
                int friendIndex = startIndex + i;
                
                if (friendIndex < friends.size()) {
                    FriendData friend = friends.get(friendIndex);
                    
                    if (clickType == GUIListener.RIGHT_CLICK) {
                        handleTeleport(player, friend);
                    } else {
                        FriendDetailGUI gui = new FriendDetailGUI(plugin, player, friend.getFriendUuid(), friend.getFriendName());
                        gui.setPreviousGUI(this);
                        gui.open(player);
                    }
                }
                return;
            }
        }
    }
    
    private void handleTeleport(Player player, FriendData friend) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friend.getFriendUuid());
        if (!offlinePlayer.isOnline()) {
            messageManager.send(player, "teleport.target-not-online");
            return;
        }
        
        Player target = offlinePlayer.getPlayer();
        if (target == null) {
            messageManager.send(player, "teleport.target-not-online");
            return;
        }
        
        player.closeInventory();
        teleportManager.startTeleport(player, target);
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
    
    public List<FriendData> getFriends() {
        return friends;
    }
}
