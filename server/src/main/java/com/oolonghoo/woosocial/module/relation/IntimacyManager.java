package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.RelationData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IntimacyManager {
    
    @SuppressWarnings("unused")
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    
    public IntimacyManager(WooSocial plugin, RelationDataManager dataManager, RelationManager relationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
    }
    
    public CompletableFuture<Integer> getIntimacy(UUID playerUuid, UUID friendUuid) {
        return dataManager.getRelation(playerUuid, friendUuid)
                .thenApply(optRelation -> optRelation.map(RelationData::getIntimacy).orElse(0));
    }
    
    public CompletableFuture<Boolean> addIntimacy(UUID playerUuid, UUID friendUuid, int amount) {
        return dataManager.getRelation(playerUuid, friendUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    RelationData relation = optRelation.get();
                    int maxIntimacy = relationManager.getMaxIntimacy();
                    int newIntimacy = Math.min(relation.getIntimacy() + amount, maxIntimacy);
                    relation.setIntimacy(newIntimacy);
                    
                    return dataManager.updateRelation(relation);
                });
    }
    
    public CompletableFuture<Boolean> setIntimacy(UUID playerUuid, UUID friendUuid, int value) {
        return dataManager.getRelation(playerUuid, friendUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    RelationData relation = optRelation.get();
                    int maxIntimacy = relationManager.getMaxIntimacy();
                    int newIntimacy = Math.max(0, Math.min(value, maxIntimacy));
                    relation.setIntimacy(newIntimacy);
                    
                    return dataManager.updateRelation(relation);
                });
    }
    
    public int getIntimacyLevel(int intimacy) {
        if (intimacy >= 1000) return 5;
        if (intimacy >= 500) return 4;
        if (intimacy >= 200) return 3;
        if (intimacy >= 100) return 2;
        if (intimacy >= 50) return 1;
        return 0;
    }
    
    public String getIntimacyLevelName(int intimacy) {
        int level = getIntimacyLevel(intimacy);
        return switch (level) {
            case 5 -> "至交";
            case 4 -> "密友";
            case 3 -> "好友";
            case 2 -> "相识";
            case 1 -> "点头之交";
            default -> "陌生人";
        };
    }
    
    public String getIntimacyColor(int intimacy) {
        int level = getIntimacyLevel(intimacy);
        return switch (level) {
            case 5 -> "<dark_purple>";
            case 4 -> "<light_purple>";
            case 3 -> "<gold>";
            case 2 -> "<yellow>";
            case 1 -> "<green>";
            default -> "<gray>";
        };
    }
    
    public double getIntimacyProgress(int intimacy) {
        return (double) intimacy / relationManager.getMaxIntimacy() * 100;
    }
    
    public boolean canUpgradeRelation(int intimacy, int requiredIntimacy) {
        return intimacy >= requiredIntimacy;
    }
}
