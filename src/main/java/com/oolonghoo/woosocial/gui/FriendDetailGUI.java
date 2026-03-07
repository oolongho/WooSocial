package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.listener.GUIListener;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.model.PlayerData;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.teleport.TeleportDataManager;
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

public class FriendDetailGUI extends BaseGUI {
    
    private final FriendDataManager friendDataManager;
    private final TeleportDataManager teleportDataManager;
    private final TeleportManager teleportManager;
    private final UUID viewerUUID;
    private final UUID friendUUID;
    private final String friendName;
    
    private static final int PLAYER_HEAD_SLOT = 4;
    private static final int TELEPORT_SLOT = 10;
    private static final int NOTIFY_ONLINE_SLOT = 11;
    private static final int ALLOW_TELEPORT_SLOT = 12;
    private static final int FAVORITE_SLOT = 13;
    private static final int REMOVE_FRIEND_SLOT = 14;
    
    public FriendDetailGUI(WooSocial plugin, Player viewer, UUID friendUuid, String friendName) {
        super(plugin, viewer, "friend_detail");
        this.friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.teleportDataManager = plugin.getModuleManager().getTeleportModule().getDataManager();
        this.teleportManager = plugin.getModuleManager().getTeleportModule().getTeleportManager();
        this.viewerUUID = viewer.getUniqueId();
        this.friendUUID = friendUuid;
        this.friendName = friendName;
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(PLAYER_HEAD_SLOT, createPlayerInfoItem());
        inventory.setItem(TELEPORT_SLOT, createTeleportButton());
        inventory.setItem(NOTIFY_ONLINE_SLOT, createNotifyOnlineButton());
        inventory.setItem(ALLOW_TELEPORT_SLOT, createAllowTeleportButton());
        inventory.setItem(FAVORITE_SLOT, createFavoriteButton());
        inventory.setItem(REMOVE_FRIEND_SLOT, createRemoveFriendButton());
    }
    
    private ItemStack createPlayerInfoItem() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUUID);
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
            lore.add(messageManager.getComponent("gui.lore-status")
                    .append(messageManager.getComponent("gui.status-online")));
            
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                lore.add(messageManager.getComponent("gui.lore-server")
                        .append(Component.text(onlinePlayer.getServer().getName(), NamedTextColor.YELLOW)));
            }
        } else {
            lore.add(messageManager.getComponent("gui.lore-status")
                    .append(messageManager.getComponent("gui.status-offline")));
            
            PlayerData friendPlayerData = friendDataManager.getPlayerData(friendUUID);
            if (friendPlayerData != null && friendPlayerData.getLastOnlineTime() > 0) {
                lore.add(messageManager.getComponent("gui.lore-last-online")
                        .append(Component.text(formatTime(friendPlayerData.getLastOnlineTime()), NamedTextColor.YELLOW)));
            }
        }
        
        lore.add(Component.empty());
        FriendData fd = friendDataManager.getFriendData(viewerUUID, friendUUID);
        if (fd != null) {
            lore.add(messageManager.getComponent("gui.lore-friends-since")
                    .append(Component.text(formatTime(fd.getAddTime()), NamedTextColor.YELLOW)));
        }
        
        meta.lore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    private ItemStack createTeleportButton() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUUID);
        boolean isOnline = offlinePlayer.isOnline();
        
        ItemStack item;
        if (isOnline) {
            item = new ItemStack(Material.ENDER_PEARL);
        } else {
            item = new ItemStack(Material.GRAY_DYE);
        }
        
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-teleport"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-teleport"));
        
        if (!isOnline) {
            lore.add(Component.empty());
            lore.add(Component.text("好友当前离线", NamedTextColor.RED));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createNotifyOnlineButton() {
        boolean notifyOnline = isNotifyOnlineForFriend();
        
        ItemStack item = new ItemStack(Material.BELL);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-notify-online"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-notify-online"));
        lore.add(Component.empty());
        
        if (notifyOnline) {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("已启用", NamedTextColor.GREEN)));
        } else {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("已禁用", NamedTextColor.RED)));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createAllowTeleportButton() {
        boolean allowTeleport = isAllowTeleportForFriend();
        
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
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
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createFavoriteButton() {
        boolean isFavorite = isFavoriteFriend();
        
        ItemStack item;
        if (isFavorite) {
            item = new ItemStack(Material.GOLD_BLOCK);
            var meta = item.getItemMeta();
            meta.displayName(Component.text("已收藏", NamedTextColor.GOLD));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("此好友已被收藏", NamedTextColor.GRAY));
            lore.add(Component.text("将显示在列表顶部", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("点击取消收藏", NamedTextColor.YELLOW));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.NETHER_STAR);
            var meta = item.getItemMeta();
            meta.displayName(messageManager.getComponent("gui.button-favorite"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("将此好友添加到收藏", NamedTextColor.GRAY));
            lore.add(Component.text("收藏的好友将显示在", NamedTextColor.GRAY));
            lore.add(Component.text("列表顶部", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("点击收藏", NamedTextColor.AQUA));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createRemoveFriendButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-remove-friend"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-remove-friend"));
        lore.add(Component.empty());
        lore.add(Component.text("Shift+点击删除", NamedTextColor.RED));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private boolean isNotifyOnlineForFriend() {
        return friendDataManager.isNotifyOnlineForFriend(viewerUUID, friendUUID);
    }
    
    private boolean isAllowTeleportForFriend() {
        return teleportDataManager.isAllowTeleport(viewerUUID, friendUUID);
    }
    
    private boolean isFavoriteFriend() {
        return friendDataManager.isFavorite(viewerUUID, friendUUID);
    }
    
    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return messageManager.get("gui.placeholder-never");
        }
        
        long diff = System.currentTimeMillis() - timestamp;
        
        if (diff < 60000) {
            return messageManager.get("gui.placeholder-just-now");
        } else if (diff < 3600000) {
            int minutes = (int) (diff / 60000);
            return messageManager.get("gui.placeholder-minutes-ago", "count", String.valueOf(minutes));
        } else if (diff < 86400000) {
            int hours = (int) (diff / 3600000);
            return messageManager.get("gui.placeholder-hours-ago", "count", String.valueOf(hours));
        } else {
            int days = (int) (diff / 86400000);
            return messageManager.get("gui.placeholder-days-ago", "count", String.valueOf(days));
        }
    }
    
    @Override
    public void refresh() {
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        switch (slot) {
            case BACK_SLOT:
                new FriendListGUI(plugin, player).open(player);
                break;
                
            case TELEPORT_SLOT:
                handleTeleport(player);
                break;
                
            case NOTIFY_ONLINE_SLOT:
                toggleNotifyOnline(player);
                break;
                
            case ALLOW_TELEPORT_SLOT:
                toggleAllowTeleport(player);
                break;
                
            case FAVORITE_SLOT:
                toggleFavorite(player);
                break;
                
            case REMOVE_FRIEND_SLOT:
                if (clickType == GUIListener.SHIFT_CLICK) {
                    handleRemoveFriend(player);
                }
                break;
        }
    }
    
    private void handleTeleport(Player player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUUID);
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
    
    private void toggleNotifyOnline(Player player) {
        boolean currentState = isNotifyOnlineForFriend();
        friendDataManager.setNotifyOnlineForFriend(viewerUUID, friendUUID, !currentState)
                .thenAccept(success -> {
                    if (success) {
                        if (!currentState) {
                            messageManager.send(player, "friend.notify-online-enabled", "player", friendName);
                        } else {
                            messageManager.send(player, "friend.notify-online-disabled", "player", friendName);
                        }
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            refresh();
                            player.openInventory(inventory);
                        });
                    }
                });
    }
    
    private void toggleAllowTeleport(Player player) {
        boolean currentState = isAllowTeleportForFriend();
        teleportDataManager.setAllowTeleport(viewerUUID, friendUUID, !currentState)
                .thenAccept(success -> {
                    if (success) {
                        if (!currentState) {
                            messageManager.send(player, "teleport.allow-teleport", "player", friendName);
                        } else {
                            messageManager.send(player, "teleport.deny-teleport", "player", friendName);
                        }
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            refresh();
                            player.openInventory(inventory);
                        });
                    }
                });
    }
    
    private void toggleFavorite(Player player) {
        boolean currentState = isFavoriteFriend();
        friendDataManager.setFavorite(viewerUUID, friendUUID, !currentState)
                .thenAccept(success -> {
                    if (success) {
                        if (!currentState) {
                            messageManager.send(player, "friend.favorite-added", "player", friendName);
                        } else {
                            messageManager.send(player, "friend.favorite-removed", "player", friendName);
                        }
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            refresh();
                            player.openInventory(inventory);
                        });
                    }
                });
    }
    
    private void handleRemoveFriend(Player player) {
        friendDataManager.removeFriend(viewerUUID, friendUUID)
                .thenAccept(success -> {
                    if (success) {
                        messageManager.send(player, "friend.friend-removed", "player", friendName);
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.closeInventory();
                            new FriendListGUI(plugin, player).open(player);
                        });
                    } else {
                        messageManager.send(player, "friend.not-friend");
                    }
                });
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
    
    public UUID getFriendUUID() {
        return friendUUID;
    }
}
