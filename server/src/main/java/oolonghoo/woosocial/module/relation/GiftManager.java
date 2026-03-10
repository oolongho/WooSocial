package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.DailyGiftData;
import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import com.oolonghoo.woosocial.event.GiftSendEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GiftManager {
    
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private final com.oolonghoo.woosocial.module.friend.FriendDataManager friendDataManager;
    
    public GiftManager(WooSocial plugin, RelationDataManager dataManager, RelationManager relationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
        this.friendDataManager = plugin.getModuleManager().getFriendModule().getDataManager();
    }
    
    public CompletableFuture<GiftResult> sendCoins(Player sender, UUID receiverUuid) {
        GiftType coinsGift = relationManager.getGiftType("coins");
        if (coinsGift == null) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-found", 0));
        }
        
        final int amountPerSend = coinsGift.getAmountPerSend();
        
        if (!friendDataManager.isFriend(sender.getUniqueId(), receiverUuid)) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-friend", 0));
        }
        
        return dataManager.getRelation(sender.getUniqueId(), receiverUuid)
                .thenCompose(optRelation -> {
                    if (optRelation.isEmpty()) {
                        RelationData newRelation = new RelationData(sender.getUniqueId(), receiverUuid);
                        newRelation.setIntimacy(0);
                        newRelation.setFriendName(Bukkit.getOfflinePlayer(receiverUuid).getName());
                        return dataManager.createRelation(newRelation).thenApply(v -> newRelation);
                    }
                    return CompletableFuture.completedFuture(optRelation.get());
                })
                .thenCompose(relation -> {
                    
                    return dataManager.getDailyGiftData(sender.getUniqueId(), receiverUuid)
                            .thenCompose(dailyData -> {
                                boolean hasUnlimited = sender.hasPermission(Perms.GIFT_UNLIMITED);
                                
                                if (coinsGift.hasDailyLimit() && !hasUnlimited) {
                                    int alreadySent = dailyData.getGiftCount("coins");
                                    if (alreadySent >= coinsGift.getDailyLimit()) {
                                        int remaining = coinsGift.getDailyLimit() - alreadySent;
                                        return CompletableFuture.completedFuture(
                                                new GiftResult(false, "gift.daily-limit-reached", Math.max(0, remaining)));
                                    }
                                }
                                
                                int intimacyGained = coinsGift.getIntimacy();
                                int newIntimacy = Math.min(relation.getIntimacy() + intimacyGained, 
                                        relationManager.getMaxIntimacy());
                                relation.setIntimacy(newIntimacy);
                                
                                dailyData.addGiftSent("coins", 1);
                                
                                GiftData giftRecord = new GiftData(sender.getUniqueId(), receiverUuid, "coins", amountPerSend);
                                giftRecord.setIntimacyGained(intimacyGained);
                                giftRecord.setSenderName(sender.getName());
                                String recvName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                giftRecord.setReceiverName(recvName != null ? recvName : "Unknown");
                                
                                return dataManager.updateRelation(relation)
                                        .thenCompose(v -> dataManager.saveDailyGiftData(dailyData))
                                        .thenCompose(v -> dataManager.createGiftRecord(giftRecord))
                                        .thenApply(v -> {
                                            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                            if (receiverName == null) receiverName = "Unknown";
                                            GiftSendEvent event = new GiftSendEvent(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, receiverName,
                                                    giftRecord, coinsGift, intimacyGained);
                                            Bukkit.getPluginManager().callEvent(event);
                                            
                                            plugin.getSyncManager().broadcastGiftSend(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, "coins", amountPerSend, intimacyGained);
                                            
                                            return new GiftResult(true, "gift.coins-sent", amountPerSend, intimacyGained);
                                        });
                            });
                });
    }
    
    public CompletableFuture<GiftResult> sendGift(Player sender, UUID receiverUuid, String giftId) {
        GiftType giftType = relationManager.getGiftType(giftId);
        if (giftType == null) {
            return CompletableFuture.completedFuture(
                    new GiftResult(false, "gift.not-found", 0));
        }
        
        if (giftType.isCoinsGift()) {
            return sendCoins(sender, receiverUuid);
        }
        
        final String finalGiftId = giftId;
        final int amountPerSend = giftType.getAmountPerSend();
        
        int totalCostCoins = giftType.getCostCoins();
        int totalCostPoints = giftType.getCostPoints();
        
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
                                    if (alreadySent >= giftType.getDailyLimit()) {
                                        int remaining = giftType.getDailyLimit() - alreadySent;
                                        return CompletableFuture.completedFuture(
                                                new GiftResult(false, "gift.daily-limit-reached", Math.max(0, remaining)));
                                    }
                                }
                                
                                if (totalCostCoins > 0) {
                                    relationManager.withdrawCoins(sender, totalCostCoins);
                                }
                                if (totalCostPoints > 0) {
                                    relationManager.withdrawPoints(sender, totalCostPoints);
                                }
                                
                                int intimacyGained = giftType.getIntimacy();
                                int newIntimacy = Math.min(relation.getIntimacy() + intimacyGained,
                                        relationManager.getMaxIntimacy());
                                relation.setIntimacy(newIntimacy);
                                
                                dailyData.addGiftSent(finalGiftId, 1);
                                
                                GiftData giftRecord = new GiftData(sender.getUniqueId(), receiverUuid, finalGiftId, amountPerSend);
                                giftRecord.setIntimacyGained(intimacyGained);
                                giftRecord.setSenderName(sender.getName());
                                String recvName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                giftRecord.setReceiverName(recvName != null ? recvName : "Unknown");
                                
                                return dataManager.updateRelation(relation)
                                        .thenCompose(v -> dataManager.saveDailyGiftData(dailyData))
                                        .thenCompose(v -> dataManager.createGiftRecord(giftRecord))
                                        .thenApply(v -> {
                                            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                                            if (receiverName == null) receiverName = "Unknown";
                                            GiftSendEvent event = new GiftSendEvent(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, receiverName,
                                                    giftRecord, giftType, intimacyGained);
                                            Bukkit.getPluginManager().callEvent(event);
                                            
                                            plugin.getSyncManager().broadcastGiftSend(
                                                    sender.getUniqueId(), sender.getName(),
                                                    receiverUuid, finalGiftId, amountPerSend, intimacyGained);
                                            
                                            return new GiftResult(true, "gift.sent", amountPerSend, intimacyGained);
                                        });
                            });
                });
    }
    
    public int getRemainingDailyLimit(Player player, UUID targetUuid, String giftId) {
        GiftType giftType = relationManager.getGiftType(giftId);
        if (giftType == null || !giftType.hasDailyLimit()) {
            return -1;
        }
        
        if (player.hasPermission(Perms.GIFT_UNLIMITED)) {
            return -1;
        }
        
        DailyGiftData dailyData = dataManager.getDailyGiftDataSync(player.getUniqueId(), targetUuid);
        if (dailyData == null) {
            return giftType.getDailyLimit();
        }
        
        return Math.max(0, giftType.getDailyLimit() - dailyData.getGiftCount(giftId));
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
        private final int intimacyGained;
        
        public GiftResult(boolean success, String messageKey, int value) {
            this(success, messageKey, value, 0);
        }
        
        public GiftResult(boolean success, String messageKey, int value, int intimacyGained) {
            this.success = success;
            this.messageKey = messageKey;
            this.value = value;
            this.intimacyGained = intimacyGained;
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
        
        public int getIntimacyGained() {
            return intimacyGained;
        }
    }
}
