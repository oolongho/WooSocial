package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.listener.GUIListener;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.mail.MailDataManager;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.teleport.TeleportDataManager;
import com.oolonghoo.woosocial.module.teleport.TeleportManager;
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

public class SocialMainGUI extends BaseGUI {
    
    private final FriendDataManager friendDataManager;
    private final MailDataManager mailDataManager;
    private final RelationDataManager relationDataManager;
    private final TeleportDataManager teleportDataManager;
    private final TeleportManager teleportManager;
    private final UUID viewerUUID;
    private List<FriendData> friends;
    
    private static final int PERSONAL_INFO_SLOT = 4;
    
    private static final int[] FRIEND_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
    
    private static final int NEXT_PAGE_SLOT = 25;
    
    private static final int FRIEND_REQUESTS_SLOT = 37;
    private static final int MAIL_SLOT = 40;
    private static final int RELATION_LIST_SLOT = 43;
    
    private static final int ITEMS_PER_PAGE = FRIEND_SLOTS.length;
    
    public SocialMainGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "social_main");
        this.friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.mailDataManager = plugin.getModuleManager().getMailModule().getDataManager();
        this.relationDataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.teleportDataManager = plugin.getModuleManager().getTeleportModule().getDataManager();
        this.teleportManager = plugin.getModuleManager().getTeleportModule().getTeleportManager();
        this.viewerUUID = viewer.getUniqueId();
        
        this.friends = friendDataManager.getFriendList(viewerUUID);
        this.totalPages = calculateTotalPages(friends.size(), ITEMS_PER_PAGE);
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        inventory.clear();
        fillBorder(54);
        fillRowWithGreenGlass(3);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(PERSONAL_INFO_SLOT, createPersonalInfoButton());
        
        int startIndex = getPageStartIndex(currentPage);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, friends.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            FriendData friend = friends.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < FRIEND_SLOTS.length) {
                inventory.setItem(FRIEND_SLOTS[slotIndex], createFriendItem(friend));
            }
        }
        
        if (totalPages > 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createPageSwitchButton());
        }
        
        inventory.setItem(FRIEND_REQUESTS_SLOT, createFriendRequestsButton());
        inventory.setItem(MAIL_SLOT, createMailButton());
        inventory.setItem(RELATION_LIST_SLOT, createRelationListButton());
    }
    
    private ItemStack createPageSwitchButton() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("切换页面", NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("当前: ", NamedTextColor.GRAY)
                .append(Component.text(currentPage + "/" + totalPages, NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("点击切换到下一页", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void fillRowWithGreenGlass(int row) {
        ItemStack greenGlass = createGreenGlassItem();
        int startSlot = row * 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(startSlot + i, greenGlass);
        }
    }
    
    private ItemStack createGreenGlassItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createFriendItem(FriendData friend) {
        UUID friendUuid = friend.getFriendUuid();
        String friendName = friend.getFriendName();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUuid);
        boolean isOnline = offlinePlayer.isOnline();
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) head.getItemMeta();
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
    
    private String formatTime(long timestamp) {
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
    
    private ItemStack createPersonalInfoButton() {
        int friendCount = friendDataManager.getFriendCount(viewerUUID);
        int blockedCount = friendDataManager.getBlockedList(viewerUUID).size();
        int unreadMail = mailDataManager.getUnreadCount(viewerUUID);
        boolean allowTeleport = teleportDataManager.isAllowFriendTeleport(viewerUUID);
        
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(viewerUUID));
        meta.displayName(Component.text(viewer.getName(), NamedTextColor.GREEN));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("查看社交设置", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("好友数量: ", NamedTextColor.GRAY)
                .append(Component.text(friendCount, NamedTextColor.YELLOW)));
        lore.add(Component.text("屏蔽数量: ", NamedTextColor.GRAY)
                .append(Component.text(blockedCount, NamedTextColor.YELLOW)));
        lore.add(Component.text("未读邮件: ", NamedTextColor.GRAY)
                .append(Component.text(unreadMail, NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Component.text("传送权限: ", NamedTextColor.GRAY)
                .append(Component.text(allowTeleport ? "允许" : "禁止", allowTeleport ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createFriendRequestsButton() {
        int requestCount = friendDataManager.getFriendRequestCount(viewerUUID);
        
        ItemStack item;
        if (requestCount > 0) {
            item = new ItemStack(Material.GOLDEN_APPLE);
        } else {
            item = new ItemStack(Material.APPLE);
        }
        
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-friend-requests"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-friend-requests"));
        lore.add(Component.empty());
        lore.add(Component.text("待处理请求: ", NamedTextColor.GRAY)
                .append(Component.text(requestCount, NamedTextColor.YELLOW)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMailButton() {
        int unreadCount = mailDataManager.getUnreadCount(viewerUUID);
        
        ItemStack item;
        if (unreadCount > 0) {
            item = new ItemStack(Material.CHEST_MINECART);
        } else {
            item = new ItemStack(Material.CHEST);
        }
        
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-mail"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-mail"));
        lore.add(Component.empty());
        lore.add(Component.text("未读邮件: ", NamedTextColor.GRAY)
                .append(Component.text(unreadCount, NamedTextColor.YELLOW)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createRelationListButton() {
        ItemStack item = new ItemStack(Material.ROSE_BUSH);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("我的关系", NamedTextColor.LIGHT_PURPLE));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("查看和管理好友关系", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击查看", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        this.friends = friendDataManager.getFriendList(viewerUUID);
        this.totalPages = calculateTotalPages(friends.size(), ITEMS_PER_PAGE);
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            player.closeInventory();
            return;
        }
        
        if (slot == PERSONAL_INFO_SLOT) {
            new SocialSettingsGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == FRIEND_REQUESTS_SLOT) {
            new FriendRequestsGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == MAIL_SLOT) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, 1);
            return;
        }
        
        if (slot == RELATION_LIST_SLOT) {
            new RelationListGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && totalPages > 1) {
            currentPage++;
            if (currentPage > totalPages) {
                currentPage = 1;
            }
            refresh();
            player.openInventory(inventory);
            return;
        }
        
        for (int i = 0; i < FRIEND_SLOTS.length; i++) {
            if (slot == FRIEND_SLOTS[i]) {
                int startIndex = getPageStartIndex(currentPage);
                int friendIndex = startIndex + i;
                
                if (friendIndex < friends.size()) {
                    FriendData friend = friends.get(friendIndex);
                    
                    if (clickType == GUIListener.RIGHT_CLICK) {
                        handleTeleport(player, friend);
                    } else {
                        new FriendDetailGUI(plugin, player, friend.getFriendUuid(), friend.getFriendName()).open(player);
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