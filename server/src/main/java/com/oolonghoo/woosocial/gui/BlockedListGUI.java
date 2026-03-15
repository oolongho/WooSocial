package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
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

public class BlockedListGUI extends BaseGUI {
    
    private final FriendDataManager dataManager;
    private final UUID viewerUUID;
    private List<UUID> blockedPlayers;
    
    private static final int ITEMS_PER_PAGE = CONTENT_SLOTS.length;
    
    public BlockedListGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "blocked_list");
        this.dataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        this.viewerUUID = viewer.getUniqueId();
        
        this.blockedPlayers = new ArrayList<>(dataManager.getBlockedList(viewerUUID));
        this.totalPages = calculateTotalPages(blockedPlayers.size(), ITEMS_PER_PAGE);
        
        initInventory();
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
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, blockedPlayers.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            UUID blockedUuid = blockedPlayers.get(i);
            int slot = CONTENT_SLOTS[i - startIndex];
            inventory.setItem(slot, createBlockedItem(blockedUuid));
        }
        
        if (blockedPlayers.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            var meta = emptyItem.getItemMeta();
            meta.displayName(Component.text("暂无屏蔽玩家", NamedTextColor.GRAY));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("使用 /friend block <玩家>", NamedTextColor.YELLOW));
            lore.add(Component.text("来屏蔽玩家", NamedTextColor.YELLOW));
            meta.lore(lore);
            
            emptyItem.setItemMeta(meta);
            inventory.setItem(22, emptyItem);
        }
        
        setupNavigation();
    }
    
    private ItemStack createBlockedItem(UUID blockedUuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(blockedUuid);
        String name = player.getName() != null ? player.getName() : blockedUuid.toString().substring(0, 8);
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(name, NamedTextColor.RED));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                .append(Component.text("已屏蔽", NamedTextColor.RED)));
        lore.add(Component.empty());
        lore.add(Component.text("点击取消屏蔽", NamedTextColor.GREEN));
        
        meta.lore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
    
    @Override
    public void refresh() {
        this.blockedPlayers = new ArrayList<>(dataManager.getBlockedList(viewerUUID));
        this.totalPages = calculateTotalPages(blockedPlayers.size(), ITEMS_PER_PAGE);
        if (currentPage > totalPages) currentPage = totalPages;
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
                int blockedIndex = getPageStartIndex(currentPage) + i;
                if (blockedIndex < blockedPlayers.size()) {
                    UUID blockedUuid = blockedPlayers.get(blockedIndex);
                    
                    dataManager.unblockPlayer(viewerUUID, blockedUuid).thenAccept(success -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                OfflinePlayer blocked = Bukkit.getOfflinePlayer(blockedUuid);
                                String blockedName = blocked.getName() != null ? blocked.getName() : blockedUuid.toString().substring(0, 8);
                                messageManager.send(player, "friend.unblocked", "player", blockedName);
                                refresh();
                                player.openInventory(inventory);
                            }
                        });
                    });
                }
                return;
            }
        }
    }
    
    public UUID getViewerUUID() {
        return viewerUUID;
    }
}
