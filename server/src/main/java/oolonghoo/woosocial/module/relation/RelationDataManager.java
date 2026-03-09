package com.oolonghoo.woosocial.module.relation;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.RelationDAO;
import com.oolonghoo.woosocial.model.DailyGiftData;
import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.util.LRUCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RelationDataManager {
    
    private final WooSocial plugin;
    private final RelationDAO relationDAO;
    private LRUCache<UUID, Map<UUID, RelationData>> relationCache;
    private LRUCache<UUID, Map<UUID, DailyGiftData>> dailyGiftCache;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public RelationDataManager(WooSocial plugin) {
        this.plugin = plugin;
        this.relationDAO = new RelationDAO(plugin, plugin.getDatabaseManager());
    }
    
    public void initialize() {
        // 初始化 LRU 缓存
        int relationMaxSize = plugin.getConfig().getInt("cache.relation-max-size", 500);
        relationCache = new LRUCache<>(relationMaxSize);
        dailyGiftCache = new LRUCache<>(relationMaxSize);
    }
    
    public void shutdown() {
        // 输出缓存统计信息
        if (relationCache != null) {
        }
        saveAll();
        if (relationCache != null) {
            relationCache.clear();
        }
        if (dailyGiftCache != null) {
            dailyGiftCache.clear();
        }
    }
    
    public void saveAll() {
    }
    
    public Optional<RelationData> getRelationSync(UUID playerUuid, UUID friendUuid) {
        Map<UUID, RelationData> playerRelations = relationCache.get(playerUuid);
        if (playerRelations != null) {
            RelationData cached = playerRelations.get(friendUuid);
            if (cached != null) {
                return Optional.of(cached);
            }
        }
        return Optional.empty();
    }
    
    public CompletableFuture<Optional<RelationData>> getRelation(UUID playerUuid, UUID friendUuid) {
        Map<UUID, RelationData> playerRelations = relationCache.get(playerUuid);
        if (playerRelations != null) {
            RelationData cached = playerRelations.get(friendUuid);
            if (cached != null) {
                return CompletableFuture.completedFuture(Optional.of(cached));
            }
        }
        
        return relationDAO.getRelation(playerUuid, friendUuid).thenApply(opt -> {
            opt.ifPresent(relation -> cacheRelation(relation));
            return opt;
        });
    }
    
    public CompletableFuture<List<RelationData>> getRelationsForPlayer(UUID playerUuid) {
        Map<UUID, RelationData> cached = relationCache.get(playerUuid);
        if (cached != null && !cached.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>(cached.values()));
        }
        
        return relationDAO.getRelationsForPlayer(playerUuid).thenApply(relations -> {
            for (RelationData relation : relations) {
                cacheRelation(relation);
            }
            return relations;
        });
    }
    
    public CompletableFuture<Boolean> createRelation(RelationData relation) {
        return relationDAO.createRelation(relation).thenApply(success -> {
            if (success) {
                cacheRelation(relation);
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> updateRelation(RelationData relation) {
        return relationDAO.updateRelation(relation);
    }
    
    public CompletableFuture<Boolean> updateIntimacy(int relationId, int intimacy) {
        return relationDAO.updateIntimacy(relationId, intimacy);
    }
    
    public CompletableFuture<Boolean> deleteRelation(UUID playerUuid, UUID friendUuid) {
        return relationDAO.deleteRelation(playerUuid, friendUuid).thenApply(success -> {
            if (success) {
                uncacheRelation(playerUuid, friendUuid);
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> deleteRelationBoth(UUID playerUuid, UUID friendUuid) {
        return relationDAO.deleteRelationBoth(playerUuid, friendUuid).thenApply(success -> {
            if (success) {
                uncacheRelation(playerUuid, friendUuid);
                uncacheRelation(friendUuid, playerUuid);
            }
            return success;
        });
    }
    
    public CompletableFuture<Integer> getRelationCountByType(UUID playerUuid, String relationType) {
        return relationDAO.getRelationCountByType(playerUuid, relationType);
    }
    
    public CompletableFuture<Boolean> createGiftRecord(GiftData gift) {
        return relationDAO.createGiftRecord(gift);
    }
    
    public CompletableFuture<DailyGiftData> getDailyGiftData(UUID playerUuid, UUID targetUuid) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        Map<UUID, DailyGiftData> playerDaily = dailyGiftCache.get(playerUuid);
        if (playerDaily != null) {
            DailyGiftData cached = playerDaily.get(targetUuid);
            if (cached != null && cached.getDate().equals(today)) {
                return CompletableFuture.completedFuture(cached);
            }
        }
        
        return relationDAO.getDailyGiftData(playerUuid, targetUuid, today).thenApply(opt -> {
            DailyGiftData data = opt.orElseGet(() -> new DailyGiftData(playerUuid, targetUuid, today));
            cacheDailyGiftData(data);
            return data;
        });
    }
    
    public DailyGiftData getDailyGiftDataSync(UUID playerUuid, UUID targetUuid) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        Map<UUID, DailyGiftData> playerDaily = dailyGiftCache.get(playerUuid);
        if (playerDaily != null) {
            DailyGiftData cached = playerDaily.get(targetUuid);
            if (cached != null && cached.getDate().equals(today)) {
                return cached;
            }
        }
        
        return null;
    }
    
    public CompletableFuture<Boolean> saveDailyGiftData(DailyGiftData data) {
        return relationDAO.saveDailyGiftData(data);
    }
    
    public CompletableFuture<List<GiftData>> getGiftHistory(UUID senderUuid, UUID receiverUuid, int limit) {
        return relationDAO.getGiftHistory(senderUuid, receiverUuid, limit);
    }
    
    public CompletableFuture<List<GiftData>> getReceivedGifts(UUID receiverUuid, int limit) {
        return relationDAO.getReceivedGifts(receiverUuid, limit);
    }
    
    public CompletableFuture<Boolean> removeRelation(UUID playerUuid, UUID friendUuid) {
        return relationDAO.deleteRelation(playerUuid, friendUuid).thenApply(success -> {
            if (success) {
                uncacheRelation(playerUuid, friendUuid);
            }
            return success;
        });
    }
    
    private void cacheRelation(RelationData relation) {
        Map<UUID, RelationData> playerRelations = relationCache.get(relation.getPlayerUuid());
        if (playerRelations == null) {
            playerRelations = new java.util.concurrent.ConcurrentHashMap<>();
            relationCache.put(relation.getPlayerUuid(), playerRelations);
        }
        playerRelations.put(relation.getFriendUuid(), relation);
    }
    
    private void uncacheRelation(UUID playerUuid, UUID friendUuid) {
        Map<UUID, RelationData> playerRelations = relationCache.get(playerUuid);
        if (playerRelations != null) {
            playerRelations.remove(friendUuid);
        }
    }
    
    private void cacheDailyGiftData(DailyGiftData data) {
        Map<UUID, DailyGiftData> playerDaily = dailyGiftCache.get(data.getPlayerUuid());
        if (playerDaily == null) {
            playerDaily = new java.util.concurrent.ConcurrentHashMap<>();
            dailyGiftCache.put(data.getPlayerUuid(), playerDaily);
        }
        playerDaily.put(data.getTargetUuid(), data);
    }
    
    public void clearCache(UUID playerUuid) {
        relationCache.remove(playerUuid);
        dailyGiftCache.remove(playerUuid);
    }
    
    public RelationDAO getRelationDAO() {
        return relationDAO;
    }
    
    /**
     * 获取关系缓存统计信息
     * @return 缓存统计字符串
     */
    public String getCacheStatistics() {
        if (relationCache != null) {
            return relationCache.getStatistics();
        }
        return "LRUCache[not initialized]";
    }
    
    /**
     * 预热缓存 - 加载指定玩家的关系数据
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> warmupCache(UUID playerUuid) {
        return getRelationsForPlayer(playerUuid).thenAccept(relations -> {
            // 关系数据已在 getRelationsForPlayer 中缓存
            plugin.getLogger().fine("[Relation] 已为玩家 " + playerUuid + " 预热 " + relations.size() + " 条关系数据");
        });
    }
}
