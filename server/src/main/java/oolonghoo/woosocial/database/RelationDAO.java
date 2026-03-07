package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.DailyGiftData;
import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.model.RelationData;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RelationDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public RelationDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    public CompletableFuture<Boolean> createRelation(RelationData relation) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "relations` " +
                    "(`player_uuid`, `friend_uuid`, `relation_type`, `intimacy`, " +
                    "`create_time`, `update_time`, `is_mutual`, `proposal_time`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, relation.getPlayerUuid().toString());
                statement.setString(2, relation.getFriendUuid().toString());
                statement.setString(3, relation.getRelationType());
                statement.setInt(4, relation.getIntimacy());
                statement.setLong(5, relation.getCreateTime());
                statement.setLong(6, relation.getUpdateTime());
                statement.setBoolean(7, relation.isMutual());
                statement.setLong(8, relation.getProposalTime());
                
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            relation.setId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建关系失败", e);
            }
            return false;
        });
    }
    
    public CompletableFuture<Optional<RelationData>> getRelation(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "relations` " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToRelationData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取关系失败: " + playerUuid + " -> " + friendUuid, e);
            }
            return Optional.empty();
        });
    }
    
    public CompletableFuture<List<RelationData>> getRelationsForPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "relations` WHERE `player_uuid` = ?";
            List<RelationData> relations = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        relations.add(mapResultSetToRelationData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取玩家关系列表失败: " + playerUuid, e);
            }
            return relations;
        });
    }
    
    public CompletableFuture<List<RelationData>> getRelationsByType(UUID playerUuid, String relationType) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "relations` " +
                    "WHERE `player_uuid` = ? AND `relation_type` = ?";
            List<RelationData> relations = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, relationType);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        relations.add(mapResultSetToRelationData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取指定类型关系列表失败: " + playerUuid, e);
            }
            return relations;
        });
    }
    
    public CompletableFuture<Boolean> updateRelation(RelationData relation) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "relations` SET " +
                    "`relation_type` = ?, `intimacy` = ?, `update_time` = ?, " +
                    "`is_mutual` = ?, `proposal_time` = ? WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, relation.getRelationType());
                statement.setInt(2, relation.getIntimacy());
                statement.setLong(3, relation.getUpdateTime());
                statement.setBoolean(4, relation.isMutual());
                statement.setLong(5, relation.getProposalTime());
                statement.setInt(6, relation.getId());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新关系失败: " + relation.getId(), e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> updateIntimacy(int relationId, int intimacy) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "relations` SET " +
                    "`intimacy` = ?, `update_time` = ? WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, intimacy);
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, relationId);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新默契度失败: " + relationId, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteRelation(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "relations` " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除关系失败", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteRelationBoth(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "relations` " +
                    "WHERE (`player_uuid` = ? AND `friend_uuid` = ?) OR " +
                    "(`player_uuid` = ? AND `friend_uuid` = ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                statement.setString(3, friendUuid.toString());
                statement.setString(4, playerUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除双向关系失败", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Integer> getRelationCountByType(UUID playerUuid, String relationType) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "relations` " +
                    "WHERE `player_uuid` = ? AND `relation_type` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, relationType);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取关系数量失败", e);
            }
            return 0;
        });
    }
    
    public CompletableFuture<Boolean> createGiftRecord(GiftData gift) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "gifts` " +
                    "(`sender_uuid`, `receiver_uuid`, `gift_id`, `gift_amount`, " +
                    "`intimacy_gained`, `send_time`) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, gift.getSenderUuid().toString());
                statement.setString(2, gift.getReceiverUuid().toString());
                statement.setString(3, gift.getGiftId());
                statement.setInt(4, gift.getGiftAmount());
                statement.setInt(5, gift.getIntimacyGained());
                statement.setLong(6, gift.getSendTime());
                
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            gift.setId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建赠礼记录失败", e);
            }
            return false;
        });
    }
    
    public CompletableFuture<List<GiftData>> getGiftHistory(UUID senderUuid, UUID receiverUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "gifts` " +
                    "WHERE `sender_uuid` = ? AND `receiver_uuid` = ? " +
                    "ORDER BY `send_time` DESC LIMIT ?";
            List<GiftData> gifts = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, senderUuid.toString());
                statement.setString(2, receiverUuid.toString());
                statement.setInt(3, limit);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        gifts.add(mapResultSetToGiftData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取赠礼历史失败", e);
            }
            return gifts;
        });
    }
    
    public CompletableFuture<List<GiftData>> getReceivedGifts(UUID receiverUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "gifts` " +
                    "WHERE `receiver_uuid` = ? " +
                    "ORDER BY `send_time` DESC LIMIT ?";
            List<GiftData> gifts = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setInt(2, limit);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        gifts.add(mapResultSetToGiftData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取收到的礼物列表失败", e);
            }
            return gifts;
        });
    }
    
    public CompletableFuture<Optional<DailyGiftData>> getDailyGiftData(UUID playerUuid, UUID targetUuid, String date) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "daily_gifts` " +
                    "WHERE `player_uuid` = ? AND `target_uuid` = ? AND `date` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, targetUuid.toString());
                statement.setString(3, date);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToDailyGiftData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取每日赠礼数据失败", e);
            }
            return Optional.empty();
        });
    }
    
    public CompletableFuture<Boolean> saveDailyGiftData(DailyGiftData data) {
        return CompletableFuture.supplyAsync(() -> {
            if (data.getId() > 0) {
                return updateDailyGiftData(data);
            } else {
                return createDailyGiftData(data);
            }
        });
    }
    
    private boolean createDailyGiftData(DailyGiftData data) {
        String sql = "INSERT INTO `" + tablePrefix + "daily_gifts` " +
                "(`player_uuid`, `target_uuid`, `date`, `coins_sent`, `gifts_sent`) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, data.getPlayerUuid().toString());
            statement.setString(2, data.getTargetUuid().toString());
            statement.setString(3, data.getDate());
            statement.setInt(4, data.getCoinsSent());
            statement.setString(5, data.getGiftsJson());
            
            int affected = statement.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        data.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "创建每日赠礼数据失败", e);
        }
        return false;
    }
    
    private boolean updateDailyGiftData(DailyGiftData data) {
        String sql = "UPDATE `" + tablePrefix + "daily_gifts` SET " +
                "`coins_sent` = ?, `gifts_sent` = ? WHERE `id` = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, data.getCoinsSent());
            statement.setString(2, data.getGiftsJson());
            statement.setInt(3, data.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "更新每日赠礼数据失败", e);
            return false;
        }
    }
    
    public CompletableFuture<Integer> cleanOldDailyGifts(int daysToKeep) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "daily_gifts` WHERE `date` < DATE('now', '-' || ? || ' days')";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, daysToKeep);
                return statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "清理旧每日赠礼数据失败", e);
                return 0;
            }
        });
    }
    
    private RelationData mapResultSetToRelationData(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
        UUID friendUuid = UUID.fromString(resultSet.getString("friend_uuid"));
        
        RelationData relation = new RelationData(playerUuid, friendUuid);
        relation.setId(id);
        relation.setRelationType(resultSet.getString("relation_type"));
        relation.setIntimacy(resultSet.getInt("intimacy"));
        relation.setCreateTime(resultSet.getLong("create_time"));
        relation.setUpdateTime(resultSet.getLong("update_time"));
        relation.setMutual(resultSet.getBoolean("is_mutual"));
        relation.setProposalTime(resultSet.getLong("proposal_time"));
        
        return relation;
    }
    
    private GiftData mapResultSetToGiftData(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID senderUuid = UUID.fromString(resultSet.getString("sender_uuid"));
        UUID receiverUuid = UUID.fromString(resultSet.getString("receiver_uuid"));
        String giftId = resultSet.getString("gift_id");
        
        GiftData gift = new GiftData(senderUuid, receiverUuid, giftId);
        gift.setId(id);
        gift.setGiftAmount(resultSet.getInt("gift_amount"));
        gift.setIntimacyGained(resultSet.getInt("intimacy_gained"));
        gift.setSendTime(resultSet.getLong("send_time"));
        
        return gift;
    }
    
    private DailyGiftData mapResultSetToDailyGiftData(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
        UUID targetUuid = UUID.fromString(resultSet.getString("target_uuid"));
        String date = resultSet.getString("date");
        
        DailyGiftData data = new DailyGiftData(playerUuid, targetUuid, date);
        data.setId(id);
        data.setCoinsSent(resultSet.getInt("coins_sent"));
        data.setGiftsSent(DailyGiftData.parseGiftsJson(resultSet.getString("gifts_sent")));
        
        return data;
    }
}
