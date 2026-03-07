package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SocialMainGUI extends BaseGUI {
    
    private final FriendDataManager dataManager;
    private final UUID viewerUUID;
    
    private static final int FRIEND_LIST_SLOT = 10;
    private static final int FRIEND_REQUESTS_SLOT = 11;
    private static final int SOCIAL_SETTINGS_SLOT = 12;
    private static final int BLOCKED_LIST_SLOT = 13;
    private static final int HELP_SLOT = 14;
    private static final int PERSONAL_INFO_SLOT = 15;
    
    public SocialMainGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "social_main");
        this.dataManager = plugin.getModuleManager().getFriendModule().getDataManager();
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
        inventory.setItem(SOCIAL_SETTINGS_SLOT, createSocialSettingsButton());
        inventory.setItem(BLOCKED_LIST_SLOT, createBlockedListButton());
        inventory.setItem(HELP_SLOT, createHelpButton());
        inventory.setItem(PERSONAL_INFO_SLOT, createPersonalInfoButton());
    }
    
    private ItemStack createFriendListButton() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-friend-list"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-friend-list"));
        
        int friendCount = dataManager.getFriendCount(viewerUUID);
        lore.add(Component.empty());
        lore.add(Component.text("好友数量: ", NamedTextColor.GRAY)
                .append(Component.text(friendCount, NamedTextColor.YELLOW)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createFriendRequestsButton() {
        int requestCount = dataManager.getFriendRequestCount(viewerUUID);
        
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
    
    private ItemStack createSocialSettingsButton() {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-social-settings"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-teleport-settings"));
        lore.add(Component.empty());
        lore.add(Component.text("点击打开设置", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBlockedListButton() {
        int blockedCount = dataManager.getBlockedList(viewerUUID).size();
        
        ItemStack item = new ItemStack(Material.BARRIER);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.button-blocked-list"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("gui.lore-blocked-list"));
        lore.add(Component.empty());
        lore.add(Component.text("已屏蔽: ", NamedTextColor.GRAY)
                .append(Component.text(blockedCount, NamedTextColor.YELLOW)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createHelpButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("帮助", NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("插件使用帮助", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("/friend add <玩家> - 添加好友", NamedTextColor.GRAY));
        lore.add(Component.text("/friend remove <玩家> - 删除好友", NamedTextColor.GRAY));
        lore.add(Component.text("/tpf <好友> - 传送到好友", NamedTextColor.GRAY));
        lore.add(Component.text("/tpftoggle - 切换传送权限", NamedTextColor.GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPersonalInfoButton() {
        int friendCount = dataManager.getFriendCount(viewerUUID);
        int blockedCount = dataManager.getBlockedList(viewerUUID).size();
        
        ItemStack item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("个人信息", NamedTextColor.WHITE));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("查看你的社交信息", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("好友数量: ", NamedTextColor.GRAY)
                .append(Component.text(friendCount, NamedTextColor.YELLOW)));
        lore.add(Component.text("屏蔽数量: ", NamedTextColor.GRAY)
                .append(Component.text(blockedCount, NamedTextColor.YELLOW)));
        
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
                
            case SOCIAL_SETTINGS_SLOT:
                new SocialSettingsGUI(plugin, player).open(player);
                break;
                
            case BLOCKED_LIST_SLOT:
                new BlockedListGUI(plugin, player).open(player);
                break;
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
}
