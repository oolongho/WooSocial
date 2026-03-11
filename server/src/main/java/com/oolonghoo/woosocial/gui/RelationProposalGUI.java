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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RelationProposalGUI extends BaseGUI {
    
    private final UUID targetUuid;
    private final String targetName;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private RelationData relationData;
    private boolean relationExists = false;
    
    private static final int[] TYPE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    
    public RelationProposalGUI(WooSocial plugin, Player viewer) {
        this(plugin, viewer, null, null);
    }
    
    public RelationProposalGUI(WooSocial plugin, Player viewer, UUID targetUuid, String targetName) {
        super(plugin, viewer, "relation_proposal");
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.dataManager = plugin.getModuleManager().getRelationModule().getDataManager();
        this.relationManager = plugin.getModuleManager().getRelationModule().getRelationManager();
        
        if (targetUuid != null) {
            loadRelationData();
        } else {
            setupItems();
        }
    }
    
    private void loadRelationData() {
        dataManager.getRelation(viewer.getUniqueId(), targetUuid).thenAccept(optRelation -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (optRelation.isPresent()) {
                    relationData = optRelation.get();
                    relationExists = true;
                } else {
                    relationData = createBasicRelation();
                    relationExists = false;
                }
                setupItems();
            });
        });
    }
    
    private RelationData createBasicRelation() {
        RelationData data = new RelationData(viewer.getUniqueId(), targetUuid);
        data.setIntimacy(0);
        data.setFriendName(targetName);
        return data;
    }
    
    @Override
    protected void setupPlaceholders() {
        if (targetName != null) {
            setPlaceholder("target_name", targetName);
        }
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        
        if (targetUuid != null) {
            inventory.setItem(4, createTargetInfoItem());
        }
        
        List<RelationType> types = new ArrayList<>(relationManager.getAllRelationTypes());
        types.removeIf(RelationType::isDefault);
        
        totalPages = calculateTotalPages(types.size(), TYPE_SLOTS.length);
        
        int startIndex = (currentPage - 1) * TYPE_SLOTS.length;
        int endIndex = Math.min(startIndex + TYPE_SLOTS.length, types.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= TYPE_SLOTS.length) break;
            
            RelationType type = types.get(i);
            inventory.setItem(TYPE_SLOTS[slotIndex], createTypeItem(type));
        }
        
        setupNavigation();
    }
    
    private ItemStack createTargetInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        meta.setOwningPlayer(target);
        
        meta.displayName(Component.text(targetName, NamedTextColor.GREEN));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        if (relationData != null) {
            lore.add(Component.text("当前亲密度: ", NamedTextColor.GRAY)
                    .append(Component.text(relationData.getIntimacy(), NamedTextColor.YELLOW)));
        }
        
        lore.add(Component.empty());
        lore.add(Component.text("选择要申请的关系类型", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createTypeItem(RelationType type) {
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(type.getDisplayName(), NamedTextColor.LIGHT_PURPLE));
        
        List<Component> lore = new ArrayList<>();
        
        if (type.getDescription() != null && !type.getDescription().isEmpty()) {
            lore.add(Component.text(type.getDescription(), NamedTextColor.GRAY));
            lore.add(Component.empty());
        }
        
        lore.add(Component.text("所需亲密度: ", NamedTextColor.GRAY)
                .append(Component.text(type.getRequiredIntimacy(), NamedTextColor.YELLOW)));
        
        if (type.isRequireMutual()) {
            lore.add(Component.text("需要双方确认", NamedTextColor.AQUA));
        }
        
        if (relationData != null && relationData.getIntimacy() < type.getRequiredIntimacy()) {
            lore.add(Component.empty());
            lore.add(Component.text("亲密度不足", NamedTextColor.RED));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("点击申请", NamedTextColor.GREEN));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        if (relationExists && targetUuid != null) {
            lore.add(Component.text("返回关系详情", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("返回关系列表", NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        if (targetUuid != null) {
            loadRelationData();
        } else {
            setupItems();
        }
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            if (relationExists && targetUuid != null) {
                new RelationDetailGUI(plugin, player, targetUuid, targetName).open(player);
            } else {
                new RelationListGUI(plugin, player).open(player);
            }
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
        
        if (targetUuid == null) {
            messageManager.send(player, "relation.select-friend-first");
            return;
        }
        
        int typeIndex = getTypeIndexFromSlot(slot);
        if (typeIndex >= 0) {
            List<RelationType> types = new ArrayList<>(relationManager.getAllRelationTypes());
            types.removeIf(RelationType::isDefault);
            
            int actualIndex = (currentPage - 1) * TYPE_SLOTS.length + typeIndex;
            
            if (actualIndex < types.size()) {
                RelationType type = types.get(actualIndex);
                handleApplyRelation(player, type);
            }
        }
    }
    
    private int getTypeIndexFromSlot(int slot) {
        for (int i = 0; i < TYPE_SLOTS.length; i++) {
            if (TYPE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
    
    private void handleApplyRelation(Player player, RelationType type) {
        if (relationData == null) {
            relationData = createBasicRelation();
        }
        
        if (!relationExists) {
            dataManager.createRelation(relationData).thenCompose(v -> {
                relationExists = true;
                return doProposeRelation(player, type);
            });
        } else {
            doProposeRelation(player, type);
        }
    }
    
    private CompletableFuture<Void> doProposeRelation(Player player, RelationType type) {
        if (relationData.getIntimacy() < type.getRequiredIntimacy()) {
            messageManager.send(player, "relation.intimacy-not-enough",
                    "required", String.valueOf(type.getRequiredIntimacy()));
            return CompletableFuture.completedFuture(null);
        }
        
        return relationManager.proposeRelation(player, targetUuid, type)
                .thenAccept(result -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (result.isSuccess()) {
                            messageManager.send(player, "relation.proposal-sent",
                                    "type", type.getDisplayName(),
                                    "player", targetName);
                            player.closeInventory();
                        } else {
                            messageManager.send(player, result.getMessageKey());
                        }
                    });
                });
    }
}
