package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.DailyGiftData;
import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import oolonghoo.woosocial.event.GiftSendEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GiftManager {
    
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    
    public GiftManager(WooSocial plugin, RelationDataManager dataManager, RelationManager relationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
    }
    
    public CompletableFuture<GiftResult> sendCoins(Player sender, UUID receiverUuid, int amount) {
        return dataManager.getRelation(sender.getUniqueId(), receiverUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new GiftResult(false, "gift.not-friend", 0));
                    }
                    
                    RelationData relation = optRelation.get();
                    
                    return dataManager.getDailyGiftData(sender.getUniqueId(), receiverUuid)
                            .thenCompose(dailyData -> {
                                int dailyLimit = relationManager.getDailyFreeCoins();
                                int alreadySent = dailyData.getCoinsSent();
                                
                                boolean hasUnlimited = sender.hasPermission(Perms.GIFT_UNLIMITED);
                                
                                if (!hasUnlimited && alreadySent + amount > dailyLimit) {
                                    int remaining = Math.max(0, dailyLimit - alreadySent);
                                    return CompletableFuture.completedFuture(
                                            new GiftResult(false, "gift.daily-limit-reached", remaining));
                                }
                                
                                int intimacyGained = amount * relationManager.getCoinsIntimacyPerUnit();
                                int newIntimacy = Math.min(relation.getIntimacy() + intimacyGained, 
                                        relationManager.getMaxIntimacy());
                                relation.setIntimacy(newIntimacy);
                                
                                dailyData.addCoinsSent(amount);
                                
                                GiftData giftRecord = new GiftData(sender.getUniqueId(), receiverUuid, "coins", amount);
                                giftRecord.setIntimacyGained(intimacyGained);
                                giftRecord.setSenderName(sender.getName());
                                
                                return dataManager.updateRelation(relation)
                                        .thenCompose(v -> dataManager.saveDailyGiftData(dailyData))
                                        .thenCompose(v -> dataManager.createGiftRecord(giftRecord))
                                        .thenApply(v -> {
                                            // 触发赠礼事件（不可取消）
                                            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                            if (receiverName == null) receiverName = "Unknown";
                                            GiftSendEvent event = new GiftSendEvent(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, receiverName,
                                                    giftRecord, null, intimacyGained);
                                            Bukkit.getPluginManager().callEvent(event);
                                            
                                            return new GiftResult(true, "gift.coins-sent", intimacyGained);
                                        });
                            });
                });
    }
    
    public CompletableFuture<GiftResult> sendGift(Player sender, UUID receiverUuid, String giftId, int amount) {
        GiftType giftType = relationManager.getGiftType(giftId);
        if (giftType == null) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-found", 0));
        }
        
        final int finalAmount = Math.max(1, amount);
        final String finalGiftId = giftId;
        
        int totalCostCoins = giftType.getCostCoins() * finalAmount;
        int totalCostPoints = giftType.getCostPoints() * finalAmount;
        
        if (totalCostCoins > 0 && !relationManager.hasEnoughCoins(sender, totalCostCoins)) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-enough-coins", totalCostCoins));
        }
        
        if (totalCostPoints > 0 && !relationManager.hasEnoughPoints(sender, totalCostPoints)) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-enough-points", totalCostPoints));
        }
        
        return dataManager.getRelation(sender.getUniqueId(), receiverUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new GiftResult(false, "gift.not-friend", 0));
                    }
                    
                    RelationData relation = optRelation.get();
                    
                    return dataManager.getDailyGiftData(sender.getUniqueId(), receiverUuid)
                            .thenCompose(dailyData -> {
                                boolean hasUnlimited = sender.hasPermission(Perms.GIFT_UNLIMITED);
                                
                                if (giftType.hasDailyLimit() && !hasUnlimited) {
                                    int alreadySent = dailyData.getGiftCount(finalGiftId);
                                    if (alreadySent + finalAmount > giftType.getDailyLimit()) {
                                        int remaining = Math.max(0, giftType.getDailyLimit() - alreadySent);
                                        return CompletableFuture.completedFuture(
                                                new GiftResult(false, "gift.daily-limit-reached", remaining));
                                    }
                                }
                                
                                if (totalCostCoins > 0) {
                                    relationManager.withdrawCoins(sender, totalCostCoins);
                                }
                                if (totalCostPoints > 0) {
                                    relationManager.withdrawPoints(sender, totalCostPoints);
                                }
                                
                                int intimacyGained = giftType.getIntimacy() * finalAmount;
                                int newIntimacy = Math.min(relation.getIntimacy() + intimacyGained,
                                        relationManager.getMaxIntimacy());
                                relation.setIntimacy(newIntimacy);
                                
                                dailyData.addGiftSent(finalGiftId, finalAmount);
                                
                                GiftData giftRecord = new GiftData(sender.getUniqueId(), receiverUuid, finalGiftId, finalAmount);
                                giftRecord.setIntimacyGained(intimacyGained);
                                giftRecord.setSenderName(sender.getName());
                                
                                return dataManager.updateRelation(relation)
                                        .thenCompose(v -> dataManager.saveDailyGiftData(dailyData))
                                        .thenCompose(v -> dataManager.createGiftRecord(giftRecord))
                                        .thenApply(v -> {
                                            // 触发赠礼事件（不可取消）
                                            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                            if (receiverName == null) receiverName = "Unknown";
                                            GiftSendEvent event = new GiftSendEvent(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, receiverName,
                                                    giftRecord, giftType, intimacyGained);
                                            Bukkit.getPluginManager().callEvent(event);
                                            
                                            return new GiftResult(true, "gift.sent", intimacyGained);
                                        });
                            });
                });
    }
    
    public CompletableFuture<GiftResult> buyMemorialItem(Player buyer, String itemId) {
        var item = relationManager.getMemorialItem(itemId);
        if (item == null) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.item-not-found", 0));
        }
        
        int costCoins = item.getCostCoins();
        int costPoints = item.getCostPoints();
        
        if (costCoins > 0 && !relationManager.hasEnoughCoins(buyer, costCoins)) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-enough-coins", costCoins));
        }
        
        if (costPoints > 0 && !relationManager.hasEnoughPoints(buyer, costPoints)) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-enough-points", costPoints));
        }
        
        if (costCoins > 0) {
            relationManager.withdrawCoins(buyer, costCoins);
        }
        if (costPoints > 0) {
            relationManager.withdrawPoints(buyer, costPoints);
        }
        
        return CompletableFuture.completedFuture(
                new GiftResult(true, "gift.item-bought", 0));
    }
    
    public static class GiftResult {
        private final boolean success;
        private final String messageKey;
        private final int value;
        
        public GiftResult(boolean success, String messageKey, int value) {
            this.success = success;
            this.messageKey = messageKey;
            this.value = value;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessageKey() {
            return messageKey;
        }
        
        public int getValue() {
            return value;
        }
    }
}
