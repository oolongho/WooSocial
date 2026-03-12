package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.GiftConfig;
import com.oolonghoo.woosocial.config.MemorialItemConfig;
import com.oolonghoo.woosocial.config.RelationTypeConfig;
import com.oolonghoo.woosocial.hook.EconomyHook;
import com.oolonghoo.woosocial.hook.PlayerPointsHook;
import com.oolonghoo.woosocial.hook.VaultHook;
import com.oolonghoo.woosocial.hook.WooEcoHook;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import com.oolonghoo.woosocial.module.relation.type.MemorialItem;
import com.oolonghoo.woosocial.module.relation.type.RelationType;
import com.oolonghoo.woosocial.event.RelationProposeEvent;
import com.oolonghoo.woosocial.event.RelationAcceptEvent;
import com.oolonghoo.woosocial.event.RelationRemoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RelationManager {
    
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final ConfigManager configManager;
    
    private GiftConfig giftConfig;
    private MemorialItemConfig memorialItemConfig;
    private RelationTypeConfig relationTypeConfig;
    
    private WooEcoHook wooEcoHook;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;
    private EconomyHook primaryEconomyHook;
    
    private int dailyFreeCoins;
    private int maxIntimacy;
    private int coinsIntimacyPerUnit;
    
    public RelationManager(WooSocial plugin, RelationDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = plugin.getConfigManager();
    }
    
    public void initialize() {
        giftConfig = new GiftConfig(plugin);
        giftConfig.initialize();
        
        memorialItemConfig = new MemorialItemConfig(plugin);
        memorialItemConfig.initialize();
        
        relationTypeConfig = new RelationTypeConfig(plugin);
        relationTypeConfig.initialize();
        
        setupEconomy();
        
        loadConfig();
    }
    
    private void setupEconomy() {
        wooEcoHook = new WooEcoHook();
        if (wooEcoHook.setup()) {
            primaryEconomyHook = wooEcoHook;
        } else {
            vaultHook = new VaultHook(plugin);
            if (vaultHook.setup()) {
                primaryEconomyHook = vaultHook;
            } else {
                plugin.getLogger().warning("未找到经济系统提供者，部分功能将不可用");
            }
        }
        
        playerPointsHook = new PlayerPointsHook(plugin);
        playerPointsHook.setup();
    }
    
    private void loadConfig() {
        dailyFreeCoins = configManager.getConfig().getInt("relation.daily-free-coins", 100);
        maxIntimacy = configManager.getConfig().getInt("relation.max-intimacy", 10000);
        coinsIntimacyPerUnit = configManager.getConfig().getInt("relation.coins-intimacy-per-unit", 1);
    }
    
    public void reload() {
        giftConfig.reload();
        memorialItemConfig.reload();
        relationTypeConfig.reload();
        loadConfig();
    }
    
    public RelationType getRelationType(String id) {
        return relationTypeConfig.getType(id);
    }
    
    public Collection<RelationType> getAllRelationTypes() {
        return relationTypeConfig.getAllTypes();
    }
    
    public RelationType getDefaultRelationType() {
        return relationTypeConfig.getDefaultType();
    }
    
    public GiftType getGiftType(String id) {
        return giftConfig.getGift(id);
    }
    
    public Collection<GiftType> getAllGiftTypes() {
        return giftConfig.getAllGifts();
    }
    
    public MemorialItem getMemorialItem(String id) {
        return memorialItemConfig.getItem(id);
    }
    
    public Collection<MemorialItem> getAllMemorialItems() {
        return memorialItemConfig.getAllItems();
    }
    
    public boolean hasEnoughCoins(Player player, int amount) {
        return primaryEconomyHook != null && primaryEconomyHook.has(player, amount);
    }
    
    public boolean hasEnoughPoints(Player player, int amount) {
        return playerPointsHook.isEnabled() && playerPointsHook.has(player.getUniqueId(), amount);
    }
    
    public boolean withdrawCoins(Player player, int amount) {
        return primaryEconomyHook != null && primaryEconomyHook.withdraw(player, amount);
    }
    
    public boolean withdrawPoints(Player player, int amount) {
        return playerPointsHook.isEnabled() && playerPointsHook.withdraw(player.getUniqueId(), amount);
    }
    
    public boolean depositCoins(Player player, int amount) {
        return primaryEconomyHook != null && primaryEconomyHook.deposit(player, amount);
    }
    
    public String formatCoins(int amount) {
        return primaryEconomyHook != null ? primaryEconomyHook.format(amount) : String.valueOf(amount);
    }
    
    public EconomyHook getPrimaryEconomyHook() {
        return primaryEconomyHook;
    }
    
    public WooEcoHook getWooEcoHook() {
        return wooEcoHook;
    }
    
    public VaultHook getVaultHook() {
        return vaultHook;
    }
    
    public EconomyHook getPlayerPointsHook() {
        return playerPointsHook;
    }
    
    public boolean isEconomyEnabled() {
        return primaryEconomyHook != null && primaryEconomyHook.isEnabled();
    }
    
    public boolean isWooEcoEnabled() {
        return wooEcoHook != null && wooEcoHook.isEnabled();
    }
    
    public boolean isVaultEnabled() {
        return vaultHook != null && vaultHook.isEnabled();
    }
    
    public boolean isPlayerPointsEnabled() {
        return playerPointsHook.isEnabled();
    }
    
    public String getEconomyProviderName() {
        return primaryEconomyHook != null ? primaryEconomyHook.getName() : "None";
    }
    
    public int getDailyFreeCoins() {
        return dailyFreeCoins;
    }
    
    public int getMaxIntimacy() {
        return maxIntimacy;
    }
    
    public int getCoinsIntimacyPerUnit() {
        return coinsIntimacyPerUnit;
    }
    
    public CompletableFuture<Optional<RelationData>> getRelation(UUID playerUuid, UUID friendUuid) {
        return dataManager.getRelation(playerUuid, friendUuid);
    }
    
    public CompletableFuture<List<RelationData>> getRelationsForPlayer(UUID playerUuid) {
        return dataManager.getRelationsForPlayer(playerUuid);
    }
    
    public CompletableFuture<Boolean> canSetRelation(UUID playerUuid, RelationType type) {
        if (type.getMaxSlots() <= 0) {
            return CompletableFuture.completedFuture(true);
        }
        return dataManager.getRelationCountByType(playerUuid, type.getId())
                .thenApply(count -> count < type.getMaxSlots());
    }
    
    public CompletableFuture<RelationResult> proposeRelation(Player proposer, UUID targetUuid, RelationType type) {
        final RelationType originalType = type;
        
        // 检查是否为好友
        if (!plugin.getModuleManager().getFriendModule().getDataManager().isFriend(proposer.getUniqueId(), targetUuid)) {
            return CompletableFuture.completedFuture(
                    new RelationResult(false, "relation.not-friend"));
        }
        
        // 步骤 1: 获取或创建关系数据
        return getOrCreateRelationData(proposer.getUniqueId(), targetUuid)
                .thenCompose(relation -> checkRelationConditions(relation, originalType))
                .thenCompose(result -> {
                    if (!result.isSuccess()) {
                        return CompletableFuture.completedFuture(result);
                    }
                    // 步骤 2: 检查是否可以设置关系类型
                    return canSetRelation(proposer.getUniqueId(), originalType).thenCompose(canSet -> {
                        if (!canSet) {
                            return CompletableFuture.completedFuture(
                                    new RelationResult(false, "relation.slots-full"));
                        }
                        // 步骤 3: 检查反向关系并处理
                        return processRelationProposal(proposer, targetUuid, originalType);
                    });
                });
    }
    
    /**
     * 获取或创建关系数据
     */
    private CompletableFuture<RelationData> getOrCreateRelationData(UUID proposerUuid, UUID targetUuid) {
        return dataManager.getRelation(proposerUuid, targetUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        RelationData newRelation = new RelationData(proposerUuid, targetUuid);
                        newRelation.setIntimacy(0);
                        newRelation.setFriendName(Bukkit.getOfflinePlayer(targetUuid).getName());
                        return dataManager.createRelation(newRelation).thenApply(v -> newRelation);
                    }
                    return CompletableFuture.completedFuture(optRelation.get());
                });
    }
    
    /**
     * 检查关系条件（亲密度、纪念物品等）
     */
    private CompletableFuture<RelationResult> checkRelationConditions(RelationData relation, RelationType type) {
        // 检查亲密度
        if (relation.getIntimacy() < type.getRequiredIntimacy()) {
            return CompletableFuture.completedFuture(
                    new RelationResult(false, "relation.intimacy-not-enough"));
        }
        
        // 检查纪念物品
        if (type.requiresItem()) {
            MemorialItem item = getMemorialItem(type.getRequireItem());
            if (item == null) {
                return CompletableFuture.completedFuture(
                        new RelationResult(false, "relation.item-not-found"));
            }
        }
        
        return CompletableFuture.completedFuture(new RelationResult(true, null));
    }
    
    /**
     * 处理关系提案（检查反向关系、触发事件、更新数据）
     */
    private CompletableFuture<RelationResult> processRelationProposal(Player proposer, UUID targetUuid, RelationType type) {
        return dataManager.getRelation(targetUuid, proposer.getUniqueId()).thenCompose(optReverseRelation -> {
            // 检查反向关系
            if (optReverseRelation.isPresent()) {
                RelationData reverseRelation = optReverseRelation.get();
                if (type.getId().equals(reverseRelation.getRelationType())) {
                    return CompletableFuture.completedFuture(
                            new RelationResult(false, "relation.target-already-has-same-type"));
                }
            }
            
            // 获取目标玩家名称
            String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
            if (targetName == null) {
                targetName = "Unknown";
            }
            
            // 触发事件
            RelationProposeEvent event = new RelationProposeEvent(
                    proposer.getUniqueId(), proposer.getName(),
                    targetUuid, targetName, type);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return CompletableFuture.completedFuture(
                        new RelationResult(false, event.getCancelReason()));
            }
            
            // 更新关系数据
            return updateRelationData(proposer.getUniqueId(), targetUuid, event.getRelationType());
        });
    }
    
    /**
     * 更新关系数据并广播
     */
    private CompletableFuture<RelationResult> updateRelationData(UUID proposerUuid, UUID targetUuid, RelationType type) {
        return dataManager.getRelation(proposerUuid, targetUuid).thenCompose(optRelation -> {
            if (optRelation.isEmpty()) {
                return CompletableFuture.completedFuture(new RelationResult(false, "relation.not-found"));
            }
            
            RelationData relation = optRelation.get();
            relation.setRelationType(type.getId());
            relation.setProposalTime(System.currentTimeMillis());
            
            return dataManager.updateRelation(relation).thenApply(success -> {
                if (success) {
                    plugin.getSyncManager().broadcastRelationPropose(
                            proposerUuid, relation.getFriendName(),
                            targetUuid, type.getId());
                    return new RelationResult(true, "relation.proposal-sent");
                }
                return new RelationResult(false, "relation.update-failed");
            });
        });
    }
    
    public CompletableFuture<RelationResult> acceptRelation(UUID playerUuid, UUID proposerUuid) {
        return dataManager.getRelation(playerUuid, proposerUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.not-found"));
                    }
                    
                    RelationData relation = optRelation.get();
                    
                    if (!relation.hasPendingProposal()) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.no-proposal"));
                    }
                    
                    RelationType type = getRelationType(relation.getRelationType());
                    if (type == null) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.type-not-found"));
                    }
                    
                    return canSetRelation(playerUuid, type).thenCompose(canSet -> {
                        if (!canSet) {
                            return CompletableFuture.completedFuture(
                                    new RelationResult(false, "relation.slots-full"));
                        }
                        
                        relation.setMutual(true);
                        
                        return dataManager.updateRelation(relation).thenCompose(success -> {
                            if (!success) {
                                return CompletableFuture.completedFuture(
                                        new RelationResult(false, "relation.update-failed"));
                            }
                            
                            return dataManager.getRelation(proposerUuid, playerUuid)
                                    .thenCompose(optReverse -> {
                                        if (optReverse.isPresent()) {
                                            RelationData reverse = optReverse.get();
                                            reverse.setRelationType(type.getId());
                                            reverse.setMutual(true);
                                            return dataManager.updateRelation(reverse);
                                        }
                                        return CompletableFuture.completedFuture(true);
                                    }).thenApply(v -> {
                                        // 触发关系确认事件（不可取消）
                                        String accepterName = Bukkit.getOfflinePlayer(playerUuid).getName();
                                        String proposerName = Bukkit.getOfflinePlayer(proposerUuid).getName();
                                        if (accepterName == null) accepterName = "Unknown";
                                        if (proposerName == null) proposerName = "Unknown";
                                        RelationAcceptEvent event = new RelationAcceptEvent(
                                                playerUuid, accepterName,
                                                proposerUuid, proposerName,
                                                relation, type);
                                        Bukkit.getPluginManager().callEvent(event);
                                        
                                        plugin.getSyncManager().broadcastRelationAccept(
                                                playerUuid, proposerUuid, type.getId());
                                        
                                        return new RelationResult(true, "relation.accepted");
                                    });
                        });
                    });
                });
    }
    
    public CompletableFuture<RelationResult> removeRelation(UUID playerUuid, UUID friendUuid) {
        return dataManager.getRelation(playerUuid, friendUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.not-found"));
                    }
                    
                    RelationData relation = optRelation.get();
                    RelationType type = getRelationType(relation.getRelationType());
                    
                    return dataManager.deleteRelationBoth(playerUuid, friendUuid)
                            .thenApply(success -> {
                                if (success) {
                                    // 触发关系解除事件（不可取消）
                                    String initiatorName = Bukkit.getOfflinePlayer(playerUuid).getName();
                                    String targetName = Bukkit.getOfflinePlayer(friendUuid).getName();
                                    if (initiatorName == null) initiatorName = "Unknown";
                                    if (targetName == null) targetName = "Unknown";
                                    RelationRemoveEvent event = new RelationRemoveEvent(
                                            playerUuid, initiatorName,
                                            friendUuid, targetName,
                                            relation, type,
                                            RelationRemoveEvent.RemoveReason.PLAYER_REMOVE);
                                    Bukkit.getPluginManager().callEvent(event);
                                    
                                    plugin.getSyncManager().broadcastRelationRemove(playerUuid, friendUuid);
                                    
                                    return new RelationResult(true, "relation.removed");
                                }
                                return new RelationResult(false, "relation.remove-failed");
                            });
                });
    }
    
    public static class RelationResult {
        private final boolean success;
        private final String messageKey;
        
        public RelationResult(boolean success, String messageKey) {
            this.success = success;
            this.messageKey = messageKey;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessageKey() {
            return messageKey;
        }
    }
    
    public GiftConfig getGiftConfig() {
        return giftConfig;
    }
    
    public MemorialItemConfig getMemorialItemConfig() {
        return memorialItemConfig;
    }
    
    public RelationTypeConfig getRelationTypeConfig() {
        return relationTypeConfig;
    }
}
