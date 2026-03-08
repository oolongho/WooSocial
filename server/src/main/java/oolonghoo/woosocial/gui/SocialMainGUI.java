package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.mail.MailDataManager;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.teleport.TeleportDataManager;
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
    private final UUID viewerUUID;
    
    private static final int FRIEND_LIST_SLOT = 10;
    private static final int FRIEND_REQUESTS_SLOT = 11;
    private static final int MAIL_SLOT = 12;
    private static final int RELATION_LIST_SLOT = 13;
    private static final int GIFT_HISTORY_SLOT = 14;
    private static final int PERSONAL_INFO_SLOT = 4;
    
    public SocialMainGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "social_main");
        this.friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.mailDataManager = plugin.getModuleManager().getMailModule().getDataManager();
        this.relationDataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.teleportDataManager = plugin.getModuleManager().getTeleportModule().getDataManager();
        this.viewerUUID = viewer.getUniqueId();
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(FRIEND_LIST_SLOT, createFriendListButton());
        inventory.setItem(FRIEND_REQUESTS_SLOT, createFriendRequestsButton());
        inventory.setItem(MAIL_SLOT, createMailButton());
        inventory.setItem(RELATION_LIST_SLOT, createRelationListButton());
        inventory.setItem(GIFT_HISTORY_SLOT, createGiftHistoryButton());
        inventory.setItem(PERSONAL_INFO_SLOT, createPersonalInfoButton());
    }
    
    private ItemStack createFriendListButton() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-friend-list"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-friend-list"));
        
        int friendCount = friendDataManager.getFriendCount(viewerUUID);
        lore.add(Component.empty());
        lore.add(Component.text("好友数量: ", NamedTextColor.GRAY)
                .append(Component.text(friendCount, NamedTextColor.YELLOW)));
        
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
        lore.add(Component.empty());
        lore.add(Component.text("点击打开邮箱", NamedTextColor.AQUA));
        
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
    
    private ItemStack createGiftHistoryButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("礼物记录", NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("查看收到的礼物", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击查看", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
        lore.add(Component.empty());
        lore.add(Component.text("点击打开设置", NamedTextColor.AQUA));
        
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
        switch (slot) {
            case BACK_SLOT:
                player.closeInventory();
                break;
            
            case FRIEND_LIST_SLOT:
                new FriendListGUI(plugin, player).open(player);
                break;
            
            case FRIEND_REQUESTS_SLOT:
                new FriendRequestsGUI(plugin, player).open(player);
                break;
            
            case MAIL_SLOT:
                plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, 1);
                break;
            
            case RELATION_LIST_SLOT:
                new RelationListGUI(plugin, player).open(player);
                break;
            
            case GIFT_HISTORY_SLOT:
                new GiftHistoryGUI(plugin, player).open(player);
                break;
            
            case PERSONAL_INFO_SLOT:
                new SocialSettingsGUI(plugin, player).open(player);
                break;
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
}