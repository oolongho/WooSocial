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

public class RelationListGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private final UUID playerUuid;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private final List<RelationData> relations;
    
    private static final int[] RELATION_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int PROPOSAL_SLOT = 49;
    private static final int GIFT_HISTORY_SLOT = 8;
    
    public RelationListGUI(WooSocial plugin, Player viewer) {
        super(plugin, viewer, "relation_list");
        this.playerUuid = viewer.getUniqueId();
        this.dataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.relationManager = plugin.getModuleManager().getRelationModule().getRelationManager();
        this.relations = new ArrayList<>();
        
        initInventory();
        loadRelations();
    }
    
    private void loadRelations() {
        dataManager.getRelationsForPlayer(playerUuid).thenAccept(relationList -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                relations.clear();
                relations.addAll(relationList);
                totalPages = calculateTotalPages(relations.size(), RELATION_SLOTS.length);
                setupItems();
            });
        });
    }
    
    @Override
    protected void setupPlaceholders() {
        setPlaceholder("player_name", viewer.getName());
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(PROPOSAL_SLOT, createProposalButton());
        inventory.setItem(GIFT_HISTORY_SLOT, createGiftHistoryButton());
        
        int startIndex = (currentPage - 1) * RELATION_SLOTS.length;
        int endIndex = Math.min(startIndex + RELATION_SLOTS.length, relations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= RELATION_SLOTS.length) break;
            
            RelationData relation = relations.get(i);
            inventory.setItem(RELATION_SLOTS[slotIndex], createRelationItem(relation));
        }
        
        setupNavigation();
    }
    
    private ItemStack createRelationItem(RelationData relation) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        String friendName = relation.getFriendName() != null ? relation.getFriendName() : "未知";
        OfflinePlayer friend = Bukkit.getOfflinePlayer(relation.getFriendUuid());
        meta.setOwningPlayer(friend);
        
        RelationType relationType = relationManager.getRelationType(relation.getRelationType());
        String typeName = relationType != null ? relationType.getDisplayName() : "好友";
        
        meta.displayName(Component.text(friendName, NamedTextColor.GREEN));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("关系: ", NamedTextColor.GRAY)
                .append(Component.text(typeName, NamedTextColor.LIGHT_PURPLE)));
        
        int intimacy = relation.getIntimacy();
        lore.add(Component.text("亲密度: ", NamedTextColor.GRAY)
                .append(Component.text(intimacy, NamedTextColor.YELLOW)));
        
        if (relation.getCreateTime() > 0) {
            String since = DATE_FORMAT.format(new Date(relation.getCreateTime()));
            lore.add(Component.text("建立于: ", NamedTextColor.GRAY)
                    .append(Component.text(since, NamedTextColor.WHITE)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("点击查看详情", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createProposalButton() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("申请关系", NamedTextColor.AQUA));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("向好友申请特殊关系", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击查看可申请的关系", NamedTextColor.YELLOW));
        
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
    
    @Override
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回社交菜单", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        loadRelations();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            goBack(player);
            return;
        }
        
        if (slot == PROPOSAL_SLOT) {
            FriendSelectGUI gui = new FriendSelectGUI(plugin, player, FriendSelectGUI.SelectMode.APPLY_RELATION, (p, friend) -> {
                RelationProposalGUI proposalGUI = new RelationProposalGUI(plugin, p, friend.getFriendUuid(), friend.getFriendName());
                proposalGUI.setPreviousGUI(this);
                proposalGUI.open(p);
            });
            gui.setPreviousGUI(this);
            gui.open(player);
            return;
        }
        
        if (slot == GIFT_HISTORY_SLOT) {
            GiftHistoryGUI gui = new GiftHistoryGUI(plugin, player);
            gui.setPreviousGUI(this);
            gui.open(player);
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 1) {
            currentPage--;
            setupItems();
            player.openInventory(inventory);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
            currentPage++;
            setupItems();
            player.openInventory(inventory);
            return;
        }
        
        int relationIndex = getRelationIndexFromSlot(slot);
        if (relationIndex >= 0) {
            int actualIndex = (currentPage - 1) * RELATION_SLOTS.length + relationIndex;
            
            if (actualIndex < relations.size()) {
                RelationData relation = relations.get(actualIndex);
                String friendName = relation.getFriendName() != null ? relation.getFriendName() : "未知";
                RelationDetailGUI gui = new RelationDetailGUI(plugin, player, relation.getFriendUuid(), friendName);
                gui.setPreviousGUI(this);
                gui.open(player);
            }
        }
    }
    
    private int getRelationIndexFromSlot(int slot) {
        for (int i = 0; i < RELATION_SLOTS.length; i++) {
            if (RELATION_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
