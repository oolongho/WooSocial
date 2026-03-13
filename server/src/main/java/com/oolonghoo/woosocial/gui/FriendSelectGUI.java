package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.FriendData;
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
import java.util.function.BiConsumer;

public class FriendSelectGUI extends BaseGUI {
    
    public enum SelectMode {
        SEND_MAIL,
        APPLY_RELATION
    }
    
    private final FriendDataManager friendDataManager;
    private final UUID viewerUUID;
    private final SelectMode mode;
    private final BiConsumer<Player, FriendData> onSelect;
    private List<FriendData> friends;
    
    private static final int[] FRIEND_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int ITEMS_PER_PAGE = FRIEND_SLOTS.length;
    
    public FriendSelectGUI(WooSocial plugin, Player viewer, SelectMode mode, BiConsumer<Player, FriendData> onSelect) {
        super(plugin, viewer, "friend_select");
        this.friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.viewerUUID = viewer.getUniqueId();
        this.mode = mode;
        this.onSelect = onSelect;
        
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
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(4, createTitleItem());
        
        int startIndex = getPageStartIndex(currentPage);
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, friends.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            FriendData friend = friends.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex < FRIEND_SLOTS.length) {
                inventory.setItem(FRIEND_SLOTS[slotIndex], createFriendItem(friend));
            }
        }
        
        setupNavigation();
    }
    
    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        
        if (mode == SelectMode.SEND_MAIL) {
            meta.displayName(Component.text("选择收件人", NamedTextColor.YELLOW));
        } else {
            meta.displayName(Component.text("选择好友", NamedTextColor.LIGHT_PURPLE));
        }
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("点击好友头像选择", NamedTextColor.GRAY));
        
        meta.lore(lore);
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
        } else {
            lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                    .append(Component.text("离线", NamedTextColor.RED)));
        }
        
        lore.add(Component.empty());
        
        if (mode == SelectMode.SEND_MAIL) {
            lore.add(Component.text("点击发送邮件", NamedTextColor.AQUA));
        } else {
            lore.add(Component.text("点击申请关系", NamedTextColor.LIGHT_PURPLE));
        }
        
        meta.lore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    @Override
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回上一级", NamedTextColor.GRAY));
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
            if (mode == SelectMode.SEND_MAIL) {
                plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, 1);
            } else {
                new RelationListGUI(plugin, player).open(player);
            }
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
        
        for (int i = 0; i < FRIEND_SLOTS.length; i++) {
            if (slot == FRIEND_SLOTS[i]) {
                int startIndex = getPageStartIndex(currentPage);
                int friendIndex = startIndex + i;
                
                if (friendIndex < friends.size()) {
                    FriendData friend = friends.get(friendIndex);
                    onSelect.accept(player, friend);
                }
                return;
            }
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
    
    public SelectMode getMode() {
        return mode;
    }
}