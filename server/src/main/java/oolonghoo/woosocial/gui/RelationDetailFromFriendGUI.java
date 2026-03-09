package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.oolonghoo.woosocial.module.relation.type.RelationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RelationDetailFromFriendGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private final UUID friendUuid;
    private final String friendName;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    
    private RelationData relationData;
    private boolean relationExists = false;
    
    private static final int PLAYER_HEAD_SLOT = 4;
    private static final int INTIMACY_BAR_SLOT = 22;
    private static final int GIFT_SLOT = 28;
    private static final int PROPOSAL_SLOT = 30;
    private static final int HISTORY_SLOT = 32;
    private static final int REMOVE_SLOT = 34;
    
    public RelationDetailFromFriendGUI(WooSocial plugin, Player viewer, UUID friendUuid, String friendName) {
        super(plugin, viewer, "relation_detail");
        this.friendUuid = friendUuid;
        this.friendName = friendName;
        this.dataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.relationManager = plugin.getModuleManager().getRelationModule().getRelationManager();
        
        loadRelationData();
    }
    
    private void loadRelationData() {
        dataManager.getRelation(viewer.getUniqueId(), friendUuid).thenAccept(optRelation -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (optRelation.isPresent()) {
                    relationData = optRelation.get();
                    relationExists = true;
                } else {
                    relationData = null;
                    relationExists = false;
                }
                setupItems();
            });
        });
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("friend_name", friendName);
        setPlaceholder("friend_uuid", friendUuid.toString());
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(PLAYER_HEAD_SLOT, createPlayerInfoItem());
        inventory.setItem(INTIMACY_BAR_SLOT, createIntimacyBarItem());
        inventory.setItem(GIFT_SLOT, createGiftButton());
        inventory.setItem(PROPOSAL_SLOT, createProposalButton());
        inventory.setItem(HISTORY_SLOT, createHistoryButton());
        
        if (relationExists) {
            inventory.setItem(REMOVE_SLOT, createRemoveButton());
        }
    }
    
    private ItemStack createPlayerInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer friend = Bukkit.getOfflinePlayer(friendUuid);
        meta.setOwningPlayer(friend);
        
        boolean isOnline = friend.isOnline();
        String typeName = "未绑定关系";
        
        if (relationData != null && relationData.getRelationType() != null) {
            RelationType relationType = relationManager.getRelationType(relationData.getRelationType());
            if (relationType != null) {
                typeName = relationType.getDisplayName();
            }
        }
        
        meta.displayName(Component.text(friendName, isOnline ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("关系: ", NamedTextColor.GRAY)
                .append(Component.text(typeName, relationExists ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY)));
        
        if (relationData != null) {
            int intimacy = relationData.getIntimacy();
            lore.add(Component.text("亲密度: ", NamedTextColor.GRAY)
                    .append(Component.text(intimacy, NamedTextColor.YELLOW)));
            
            if (relationData.getCreateTime() > 0) {
                String since = DATE_FORMAT.format(new Date(relationData.getCreateTime()));
                lore.add(Component.text("建立于: ", NamedTextColor.GRAY)
                        .append(Component.text(since, NamedTextColor.WHITE)));
            }
        } else {
            lore.add(Component.text("亲密度: ", NamedTextColor.GRAY)
                    .append(Component.text("0", NamedTextColor.YELLOW)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                .append(isOnline ? Component.text("在线", NamedTextColor.GREEN) : Component.text("离线", NamedTextColor.RED)));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createIntimacyBarItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("亲密度", NamedTextColor.GREEN));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if (relationData != null) {
            int intimacy = relationData.getIntimacy();
            lore.add(Component.text("当前亲密度: ", NamedTextColor.GRAY)
                    .append(Component.text(intimacy, NamedTextColor.YELLOW)));
        } else {
            lore.add(Component.text("当前亲密度: ", NamedTextColor.GRAY)
                    .append(Component.text("0", NamedTextColor.YELLOW)));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createGiftButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("赠送礼物", NamedTextColor.GOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("赠送礼物增加亲密度", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击选择礼物", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createProposalButton() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("申请关系", NamedTextColor.AQUA));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("申请建立特殊关系", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击查看可申请的关系", NamedTextColor.YELLOW));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createHistoryButton() {
        ItemStack item = new ItemStack(Material.CHEST);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("礼物记录", NamedTextColor.YELLOW));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("查看与TA的礼物往来", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击查看", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createRemoveButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("解除关系", NamedTextColor.RED));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("解除与TA的关系", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Shift+点击确认", NamedTextColor.RED));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
        loadRelationData();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            new FriendDetailGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == GIFT_SLOT) {
            new GiftShopGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == PROPOSAL_SLOT) {
            new RelationProposalGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == HISTORY_SLOT) {
            new GiftHistoryWithFriendGUI(plugin, player, friendUuid, friendName).open(player);
            return;
        }
        
        if (slot == REMOVE_SLOT && clickType == 1 && relationExists) {
            handleRemoveRelation(player);
        }
    }
    
    private void handleRemoveRelation(Player player) {
        dataManager.removeRelation(player.getUniqueId(), friendUuid).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    messageManager.send(player, "relation.removed", "player", friendName);
                    relationExists = false;
                    relationData = null;
                    setupItems();
                    player.openInventory(inventory);
                } else {
                    messageManager.send(player, "relation.remove-failed");
                }
            });
        });
    }
}
