package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.ShowcaseData;
import com.oolonghoo.woosocial.module.showcase.ShowcaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class ShowcaseViewGUI extends BaseGUI {
    
    private final ShowcaseManager showcaseManager;
    private final UUID viewerUUID;
    private final UUID targetUUID;
    private final String targetName;
    private ShowcaseData showcaseData;
    
    private static final int LIKE_SLOT = 2;
    private static final int ADD_FRIEND_SLOT = 4;
    private static final int TRADE_SLOT = 6;
    private static final int BLOCK_SLOT = 8;
    
    private static final int HELMET_SLOT = 19;
    private static final int CHESTPLATE_SLOT = 28;
    private static final int LEGGINGS_SLOT = 37;
    private static final int BOOTS_SLOT = 46;
    
    private static final int[] SHOWCASE_SLOTS = {30, 31, 32, 33, 34, 39, 40, 41, 42, 43};
    
    public ShowcaseViewGUI(WooSocial plugin, Player viewer, UUID targetUUID, String targetName) {
        super(plugin, viewer, "showcase_view");
        this.showcaseManager = plugin.getModuleManager().getShowcaseModule().getShowcaseManager();
        this.viewerUUID = viewer.getUniqueId();
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        
        this.showcaseData = showcaseManager.getShowcase(targetUUID, targetName);
        
        initInventory();
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("owner_name", targetName);
        setPlaceholder("likes", showcaseData.getLikes());
        setPlaceholder("has_liked", showcaseManager.hasLiked(viewerUUID, targetUUID));
    }
    
    private void setupItems() {
        inventory.clear();
        fillBorder(54);
        fillRowWithGlass(1);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(LIKE_SLOT, createLikeButton());
        inventory.setItem(ADD_FRIEND_SLOT, createAddFriendButton());
        inventory.setItem(TRADE_SLOT, createTradeButton());
        inventory.setItem(BLOCK_SLOT, createBlockButton());
        
        setupPlayerHead();
        setupEquipmentDisplay();
        setupShowcaseItems();
    }
    
    private void fillRowWithGlass(int row) {
        ItemStack glass = createGlassItem();
        int startSlot = row * 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(startSlot + i, glass);
        }
    }
    
    private ItemStack createGlassItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
    
    private void setupPlayerHead() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
        meta.displayName(Component.text(targetName, NamedTextColor.GREEN));
        
        var lore = List.of(
                Component.text("点赞数: ", NamedTextColor.GRAY)
                        .append(Component.text(showcaseData.getLikes(), NamedTextColor.YELLOW))
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        
        inventory.setItem(9, item);
    }
    
    private void setupEquipmentDisplay() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUUID);
        
        if (offlinePlayer.isOnline()) {
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                var inv = onlinePlayer.getInventory();
                
                setEquipmentSlot(HELMET_SLOT, inv.getHelmet(), "头盔");
                setEquipmentSlot(CHESTPLATE_SLOT, inv.getChestplate(), "胸甲");
                setEquipmentSlot(LEGGINGS_SLOT, inv.getLeggings(), "护腿");
                setEquipmentSlot(BOOTS_SLOT, inv.getBoots(), "靴子");
                return;
            }
        }
        
        inventory.setItem(HELMET_SLOT, createEmptyEquipmentItem("头盔"));
        inventory.setItem(CHESTPLATE_SLOT, createEmptyEquipmentItem("胸甲"));
        inventory.setItem(LEGGINGS_SLOT, createEmptyEquipmentItem("护腿"));
        inventory.setItem(BOOTS_SLOT, createEmptyEquipmentItem("靴子"));
    }
    
    private void setEquipmentSlot(int slot, ItemStack item, String name) {
        if (item != null && item.getType() != Material.AIR) {
            inventory.setItem(slot, item.clone());
        } else {
            inventory.setItem(slot, createEmptyEquipmentItem(name));
        }
    }
    
    private ItemStack createEmptyEquipmentItem(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }
    
    private void setupShowcaseItems() {
        List<ItemStack> items = showcaseData.getItems();
        
        for (int i = 0; i < SHOWCASE_SLOTS.length; i++) {
            if (i < items.size() && items.get(i) != null) {
                inventory.setItem(SHOWCASE_SLOTS[i], items.get(i).clone());
            } else {
                inventory.setItem(SHOWCASE_SLOTS[i], createEmptyShowcaseSlot(i));
            }
        }
    }
    
    private ItemStack createEmptyShowcaseSlot(int index) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("展示格子 " + (index + 1), NamedTextColor.YELLOW));
        var lore = List.of(
                Component.text("暂无物品", NamedTextColor.GRAY)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createLikeButton() {
        boolean hasLiked = showcaseManager.hasLiked(viewerUUID, targetUUID);
        
        ItemStack item = new ItemStack(hasLiked ? Material.LIME_DYE : Material.GRAY_DYE);
        var meta = item.getItemMeta();
        meta.displayName(hasLiked ? Component.text("已点赞", NamedTextColor.GREEN) : Component.text("点赞", NamedTextColor.YELLOW));
        
        var lore = List.of(
                Component.text("点赞数: ", NamedTextColor.GRAY)
                        .append(Component.text(showcaseData.getLikes(), NamedTextColor.YELLOW)),
                Component.empty(),
                hasLiked ? Component.text("点击取消点赞", NamedTextColor.RED) : Component.text("点击点赞", NamedTextColor.AQUA)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAddFriendButton() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.displayName(Component.text("添加好友", NamedTextColor.GREEN));
        
        var lore = List.of(
                Component.text("向 " + targetName + " 发送好友请求", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("点击添加", NamedTextColor.AQUA)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTradeButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("发起交易", NamedTextColor.GOLD));
        
        var lore = List.of(
                Component.text("向 " + targetName + " 发起交易请求", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("功能即将推出", NamedTextColor.YELLOW),
                Component.text("敬请期待...", NamedTextColor.GRAY)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBlockButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("屏蔽此人", NamedTextColor.RED));
        
        var lore = List.of(
                Component.text("屏蔽 " + targetName, NamedTextColor.GRAY),
                Component.empty(),
                Component.text("点击屏蔽", NamedTextColor.RED)
        );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        this.showcaseData = showcaseManager.getShowcase(targetUUID, targetName);
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            if (getPreviousGUI() != null) {
                goBack(player);
            } else {
                player.closeInventory();
            }
            return;
        }
        
        if (slot == LIKE_SLOT) {
            showcaseManager.toggleLike(player, targetUUID);
            refresh();
            player.openInventory(inventory);
            return;
        }
        
        if (slot == ADD_FRIEND_SLOT) {
            handleAddFriend(player);
        }
        
        if (slot == BLOCK_SLOT) {
            handleBlockPlayer(player);
        }
    }
    
    private void handleAddFriend(Player player) {
        var friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        
        if (friendDataManager.isFriend(player.getUniqueId(), targetUUID)) {
            messageManager.send(player, "friend.already-friends");
            return;
        }
        
        friendDataManager.sendFriendRequest(player.getUniqueId(), targetUUID, player.getName(), targetName)
                .thenAccept(success -> {
                    if (success) {
                        messageManager.send(player, "friend.request-sent", "name", targetName);
                    }
                });
    }
    
    private void handleBlockPlayer(Player player) {
        var friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        
        if (friendDataManager.isBlocked(player.getUniqueId(), targetUUID)) {
            messageManager.send(player, "block.already-blocked");
            return;
        }
        
        friendDataManager.blockPlayer(player.getUniqueId(), targetUUID)
                .thenAccept(success -> {
                    if (success) {
                        messageManager.send(player, "block.player-blocked", "name", targetName);
                        player.closeInventory();
                    }
                });
    }
    
    public UUID getTargetUUID() {
        return targetUUID;
    }
    
    public String getTargetName() {
        return targetName;
    }
}
