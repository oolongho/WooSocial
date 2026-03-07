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
import oolonghoo.woosocial.event.RelationProposeEvent;
import oolonghoo.woosocial.event.RelationAcceptEvent;
import oolonghoo.woosocial.event.RelationRemoveEvent;
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
        
        plugin.getLogger().info("关系管理器已初始化");
    }
    
    private void setupEconomy() {
        wooEcoHook = new WooEcoHook();
        if (wooEcoHook.setup()) {
            primaryEconomyHook = wooEcoHook;
            plugin.getLogger().info("已连接 WooEco 经济系统（直接API模式）");
        } else {
            vaultHook = new VaultHook(plugin);
            if (vaultHook.setup()) {
                primaryEconomyHook = vaultHook;
            } else {
                plugin.getLogger().warning("未找到经济系统提供者，部分功能将不可用");
                plugin.getLogger().info("提示：请安装 WooEco 或 EssentialsX+CMI 等经济插件");
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
        
        return dataManager.getRelation(proposer.getUniqueId(), targetUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.not-friend"));
                    }
                    
                    RelationData relation = optRelation.get();
                    
                    if (relation.getIntimacy() < originalType.getRequiredIntimacy()) {
                        return CompletableFuture.completedFuture(
                                new RelationResult(false, "relation.intimacy-not-enough"));
                    }
                    
                    if (originalType.requiresItem()) {
                        MemorialItem item = getMemorialItem(originalType.getRequireItem());
                        if (item == null) {
                            return CompletableFuture.completedFuture(
                                    new RelationResult(false, "relation.item-not-found"));
                        }
                    }
                    
                    return canSetRelation(proposer.getUniqueId(), originalType).thenCompose(canSet -> {
                        if (!canSet) {
                            return CompletableFuture.completedFuture(
                                    new RelationResult(false, "relation.slots-full"));
                        }
                        
                        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
                        if (targetName == null) {
                            targetName = "Unknown";
                        }
                        RelationProposeEvent event = new RelationProposeEvent(
                                proposer.getUniqueId(), proposer.getName(),
                                targetUuid, targetName, originalType);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return CompletableFuture.completedFuture(
                                    new RelationResult(false, event.getCancelReason()));
                        }
                        
                        final RelationType finalType = event.getRelationType();
                        
                        relation.setRelationType(finalType.getId());
                        relation.setProposalTime(System.currentTimeMillis());
                        
                        return dataManager.updateRelation(relation).thenApply(success -> {
                            if (success) {
                                plugin.getSyncManager().broadcastRelationPropose(
                                        proposer.getUniqueId(), proposer.getName(),
                                        targetUuid, finalType.getId());
                                return new RelationResult(true, "relation.proposal-sent");
                            }
                            return new RelationResult(false, "relation.update-failed");
                        });
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
