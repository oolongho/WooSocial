package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.model.FriendRequest;
import com.oolonghoo.woosocial.model.TeleportCooldown;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 好友数据访问对象
 * 负责好友关系、好友请求和传送冷却的CRUD操作
 * 所有数据库操作都是异步的
 */
public class FriendDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public FriendDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    // ==================== 好友关系操作 ====================
    
    /**
     * 异步添加好友关系
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param friendName 好友名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> addFriend(UUID playerUuid, UUID friendUuid, String friendName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "friends` " +
                    "(`player_uuid`, `friend_uuid`, `add_time`, `friend_name`, `favorite`, `nickname`, `receive_messages`) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                statement.setLong(3, System.currentTimeMillis());
                statement.setString(4, friendName);
                statement.setBoolean(5, false);
                statement.setString(6, null);
                statement.setBoolean(7, true);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "添加好友关系失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步删除好友关系（双向删除）
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> removeFriend(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "friends` " +
                    "WHERE (`player_uuid` = ? AND `friend_uuid` = ?) " +
                    "OR (`player_uuid` = ? AND `friend_uuid` = ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                statement.setString(3, friendUuid.toString());
                statement.setString(4, playerUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除好友关系失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步获取玩家的所有好友
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<List<FriendData>>
     */
    public CompletableFuture<List<FriendData>> getFriends(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "friends` WHERE `player_uuid` = ?";
            List<FriendData> friends = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        friends.add(mapResultSetToFriendData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取好友列表失败: " + playerUuid, e);
            }
            
            return friends;
        });
    }
    
    /**
     * 异步检查是否为好友关系
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> isFriend(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "friends` " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, friendUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "检查好友关系失败", e);
            }
            
            return false;
        });
    }
    
    /**
     * 异步获取好友数量
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Integer>
     */
    public CompletableFuture<Integer> getFriendCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "friends` WHERE `player_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取好友数量失败: " + playerUuid, e);
            }
            
            return 0;
        });
    }
    
    /**
     * 异步更新好友名称
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param friendName 新的好友名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateFriendName(UUID playerUuid, UUID friendUuid, String friendName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "friends` SET `friend_name` = ? " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, friendName);
                statement.setString(2, playerUuid.toString());
                statement.setString(3, friendUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新好友名称失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步设置好友收藏状态
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param favorite 是否收藏
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setFavorite(UUID playerUuid, UUID friendUuid, boolean favorite) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "friends` SET `favorite` = ? " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setBoolean(1, favorite);
                statement.setString(2, playerUuid.toString());
                statement.setString(3, friendUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "设置好友收藏状态失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步设置好友备注
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param nickname 备注名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setNickname(UUID playerUuid, UUID friendUuid, String nickname) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "friends` SET `nickname` = ? " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, nickname);
                statement.setString(2, playerUuid.toString());
                statement.setString(3, friendUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "设置好友备注失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步设置是否接收好友消息
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param receiveMessages 是否接收消息
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setReceiveMessages(UUID playerUuid, UUID friendUuid, boolean receiveMessages) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "friends` SET `receive_messages` = ? " +
                    "WHERE `player_uuid` = ? AND `friend_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setBoolean(1, receiveMessages);
                statement.setString(2, playerUuid.toString());
                statement.setString(3, friendUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "设置好友消息接收失败", e);
                return false;
            }
        });
    }
    
    // ==================== 好友请求操作 ====================
    
    /**
     * 异步创建好友请求
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @param senderName 发送者名称
     * @param receiverName 接收者名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> createFriendRequest(UUID senderUuid, UUID receiverUuid, 
                                                          String senderName, String receiverName) {
        return CompletableFuture.supplyAsync(() -> {
            // 先删除旧的请求（如果存在）
            String deleteSql = "DELETE FROM `" + tablePrefix + "requests` " +
                    "WHERE `sender_uuid` = ? AND `receiver_uuid` = ?";
            
            String insertSql = "INSERT INTO `" + tablePrefix + "requests` " +
                    "(`sender_uuid`, `receiver_uuid`, `sender_name`, `receiver_name`, " +
                    "`request_time`, `status`) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection()) {
                // 删除旧请求
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                    deleteStatement.setString(1, senderUuid.toString());
                    deleteStatement.setString(2, receiverUuid.toString());
                    deleteStatement.executeUpdate();
                }
                
                // 插入新请求
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    insertStatement.setString(1, senderUuid.toString());
                    insertStatement.setString(2, receiverUuid.toString());
                    insertStatement.setString(3, senderName);
                    insertStatement.setString(4, receiverName);
                    insertStatement.setLong(5, System.currentTimeMillis());
                    insertStatement.setString(6, FriendRequest.RequestStatus.PENDING.name());
                    
                    return insertStatement.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建好友请求失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步获取玩家的待处理好友请求
     * @param receiverUuid 接收者UUID
     * @return CompletableFuture<List<FriendRequest>>
     */
    public CompletableFuture<List<FriendRequest>> getPendingRequests(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "requests` " +
                    "WHERE `receiver_uuid` = ? AND `status` = ?";
            List<FriendRequest> requests = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setString(2, FriendRequest.RequestStatus.PENDING.name());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        requests.add(mapResultSetToFriendRequest(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取好友请求失败: " + receiverUuid, e);
            }
            
            return requests;
        });
    }
    
    /**
     * 异步获取两个玩家之间的好友请求
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @return CompletableFuture<Optional<FriendRequest>>
     */
    public CompletableFuture<Optional<FriendRequest>> getFriendRequest(UUID senderUuid, UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "requests` " +
                    "WHERE `sender_uuid` = ? AND `receiver_uuid` = ? AND `status` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, senderUuid.toString());
                statement.setString(2, receiverUuid.toString());
                statement.setString(3, FriendRequest.RequestStatus.PENDING.name());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToFriendRequest(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取好友请求失败", e);
            }
            
            return Optional.empty();
        });
    }
    
    /**
     * 异步更新好友请求状态
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @param status 新状态
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateRequestStatus(UUID senderUuid, UUID receiverUuid, 
                                                          FriendRequest.RequestStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "requests` SET `status` = ? " +
                    "WHERE `sender_uuid` = ? AND `receiver_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, status.name());
                statement.setString(2, senderUuid.toString());
                statement.setString(3, receiverUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新好友请求状态失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步删除好友请求
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deleteFriendRequest(UUID senderUuid, UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "requests` " +
                    "WHERE `sender_uuid` = ? AND `receiver_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, senderUuid.toString());
                statement.setString(2, receiverUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除好友请求失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步清理过期的好友请求
     * @param expireTime 过期时间（秒）
     * @return CompletableFuture<Integer> 清理的请求数量
     */
    public CompletableFuture<Integer> cleanExpiredRequests(int expireTime) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "requests` " +
                    "WHERE `status` = ? AND `request_time` < ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, FriendRequest.RequestStatus.PENDING.name());
                statement.setLong(2, System.currentTimeMillis() - expireTime * 1000L);
                
                return statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "清理过期好友请求失败", e);
                return 0;
            }
        });
    }
    
    // ==================== 传送冷却操作 ====================
    
    /**
     * 异步设置传送冷却
     * @param playerUuid 玩家UUID
     * @param cooldownSeconds 冷却时间（秒）
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setTeleportCooldown(UUID playerUuid, int cooldownSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR REPLACE INTO `" + tablePrefix + "teleport_cooldown` " +
                    "(`uuid`, `cooldown_end_time`, `cooldown_type`) VALUES (?, ?, ?)";
            
            // SQLite语法：INSERT OR REPLACE
            // MySQL需要使用 INSERT ... ON DUPLICATE KEY UPDATE
            if (databaseManager.getDatabaseType().equals("mysql")) {
                sql = "INSERT INTO `" + tablePrefix + "teleport_cooldown` " +
                        "(`uuid`, `cooldown_end_time`, `cooldown_type`) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `cooldown_end_time` = VALUES(`cooldown_end_time`), " +
                        "`cooldown_type` = VALUES(`cooldown_type`)";
            }
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setLong(2, System.currentTimeMillis() + cooldownSeconds * 1000L);
                statement.setString(3, TeleportCooldown.CooldownType.TP_FRIEND.name());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "设置传送冷却失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步获取传送冷却
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Optional<TeleportCooldown>>
     */
    public CompletableFuture<Optional<TeleportCooldown>> getTeleportCooldown(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "teleport_cooldown` WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToTeleportCooldown(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取传送冷却失败: " + playerUuid, e);
            }
            
            return Optional.empty();
        });
    }
    
    /**
     * 异步删除传送冷却
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> removeTeleportCooldown(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "teleport_cooldown` WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除传送冷却失败", e);
                return false;
            }
        });
    }
    
    /**
     * 异步清理过期的传送冷却
     * @return CompletableFuture<Integer> 清理的记录数
     */
    public CompletableFuture<Integer> cleanExpiredCooldowns() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "teleport_cooldown` WHERE `cooldown_end_time` < ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setLong(1, System.currentTimeMillis());
                
                return statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "清理过期传送冷却失败", e);
                return 0;
            }
        });
    }
    
    // ==================== 结果集映射方法 ====================
    
    /**
     * 将ResultSet映射到FriendData对象
     */
    private FriendData mapResultSetToFriendData(ResultSet resultSet) throws SQLException {
        UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
        UUID friendUuid = UUID.fromString(resultSet.getString("friend_uuid"));
        long addTime = resultSet.getLong("add_time");
        String friendName = resultSet.getString("friend_name");
        boolean favorite = resultSet.getBoolean("favorite");
        String nickname = resultSet.getString("nickname");
        boolean receiveMessages = resultSet.getBoolean("receive_messages");
        
        return new FriendData(playerUuid, friendUuid, addTime, friendName, favorite, nickname, receiveMessages);
    }
    
    /**
     * 将ResultSet映射到FriendRequest对象
     */
    private FriendRequest mapResultSetToFriendRequest(ResultSet resultSet) throws SQLException {
        UUID senderUuid = UUID.fromString(resultSet.getString("sender_uuid"));
        UUID receiverUuid = UUID.fromString(resultSet.getString("receiver_uuid"));
        String senderName = resultSet.getString("sender_name");
        String receiverName = resultSet.getString("receiver_name");
        long requestTime = resultSet.getLong("request_time");
        FriendRequest.RequestStatus status = FriendRequest.RequestStatus.valueOf(resultSet.getString("status"));
        
        FriendRequest request = new FriendRequest(senderUuid, senderName, receiverUuid, receiverName);
        request.setSendTime(requestTime);
        request.setStatus(status);
        
        return request;
    }
    
    /**
     * 将ResultSet映射到TeleportCooldown对象
     */
    private TeleportCooldown mapResultSetToTeleportCooldown(ResultSet resultSet) throws SQLException {
        UUID playerUuid = UUID.fromString(resultSet.getString("uuid"));
        long cooldownEndTime = resultSet.getLong("cooldown_end_time");
        TeleportCooldown.CooldownType cooldownType = 
                TeleportCooldown.CooldownType.valueOf(resultSet.getString("cooldown_type"));
        
        return new TeleportCooldown(playerUuid, cooldownEndTime, cooldownType);
    }
    
    // ==================== 传送设置操作 ====================
    
    /**
     * 保存传送设置
     * 
     * @param settings 传送设置
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> saveTeleportSettings(com.oolonghoo.woosocial.model.TeleportSettings settings) {
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (databaseManager.isMySQL()) {
                sql = "INSERT INTO `" + tablePrefix + "teleport_settings` " +
                        "(`player_uuid`, `allow_friend_teleport`, `allow_stranger_teleport`, " +
                        "`teleport_cooldown`, `teleport_countdown`, `friend_permissions`) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "`allow_friend_teleport` = VALUES(`allow_friend_teleport`), " +
                        "`allow_stranger_teleport` = VALUES(`allow_stranger_teleport`), " +
                        "`teleport_cooldown` = VALUES(`teleport_cooldown`), " +
                        "`teleport_countdown` = VALUES(`teleport_countdown`), " +
                        "`friend_permissions` = VALUES(`friend_permissions`)";
            } else {
                sql = "INSERT OR REPLACE INTO `" + tablePrefix + "teleport_settings` " +
                        "(`player_uuid`, `allow_friend_teleport`, `allow_stranger_teleport`, " +
                        "`teleport_cooldown`, `teleport_countdown`, `friend_permissions`) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            }
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, settings.getPlayerId().toString());
                statement.setBoolean(2, settings.isAllowFriendTeleport());
                statement.setBoolean(3, settings.isAllowStrangerTeleport());
                statement.setInt(4, settings.getTeleportCooldown());
                statement.setInt(5, settings.getTeleportCountdown());
                
                // 将好友权限映射序列化为JSON字符串
                StringBuilder permissionsJson = new StringBuilder();
                java.util.Map<java.util.UUID, Boolean> permissions = settings.getFriendTeleportPermissions();
                if (!permissions.isEmpty()) {
                    permissionsJson.append("{");
                    boolean first = true;
                    for (java.util.Map.Entry<java.util.UUID, Boolean> entry : permissions.entrySet()) {
                        if (!first) permissionsJson.append(",");
                        permissionsJson.append("\"").append(entry.getKey().toString()).append("\":").append(entry.getValue());
                        first = false;
                    }
                    permissionsJson.append("}");
                }
                statement.setString(6, permissionsJson.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 保存传送设置失败: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 加载传送设置
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Optional<TeleportSettings>>
     */
    public CompletableFuture<java.util.Optional<com.oolonghoo.woosocial.model.TeleportSettings>> loadTeleportSettings(java.util.UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "teleport_settings` WHERE `player_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        com.oolonghoo.woosocial.model.TeleportSettings settings = new com.oolonghoo.woosocial.model.TeleportSettings(playerUuid);
                        settings.setAllowFriendTeleport(resultSet.getBoolean("allow_friend_teleport"));
                        settings.setAllowStrangerTeleport(resultSet.getBoolean("allow_stranger_teleport"));
                        settings.setTeleportCooldown(resultSet.getInt("teleport_cooldown"));
                        settings.setTeleportCountdown(resultSet.getInt("teleport_countdown"));
                        
                        // 解析好友权限JSON
                        String permissionsJson = resultSet.getString("friend_permissions");
                        if (permissionsJson != null && !permissionsJson.isEmpty()) {
                            parseFriendPermissions(settings, permissionsJson);
                        }
                        
                        return java.util.Optional.of(settings);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 加载传送设置失败: " + e.getMessage());
            }
            
            return java.util.Optional.empty();
        });
    }
    
    /**
     * 解析好友权限JSON
     */
    private void parseFriendPermissions(com.oolonghoo.woosocial.model.TeleportSettings settings, String json) {
        if (json == null || json.isEmpty() || !json.startsWith("{")) return;
        
        try {
            // 简单解析JSON格式: {"uuid":true/false,"uuid":true/false}
            json = json.substring(1, json.length() - 1); // 移除{}
            if (json.isEmpty()) return;
            
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                int colonIndex = pair.indexOf(':');
                if (colonIndex > 0) {
                    String uuidStr = pair.substring(1, colonIndex - 1); // 移除引号
                    boolean value = Boolean.parseBoolean(pair.substring(colonIndex + 1));
                    try {
                        java.util.UUID friendUuid = java.util.UUID.fromString(uuidStr);
                        settings.setFriendTeleportPermission(friendUuid, value);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FriendDAO] 解析好友权限JSON失败: " + e.getMessage());
        }
    }
    
    // ==================== 屏蔽列表操作 ====================
    
    /**
     * 加载玩家的屏蔽列表
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Set<UUID>>
     */
    public CompletableFuture<java.util.Set<java.util.UUID>> loadBlockedList(java.util.UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.Set<java.util.UUID> blockedSet = new java.util.HashSet<>();
            String sql = "SELECT `blocked_uuid` FROM `" + tablePrefix + "blocked` WHERE `player_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String blockedUuidStr = resultSet.getString("blocked_uuid");
                        try {
                            java.util.UUID blockedUuid = java.util.UUID.fromString(blockedUuidStr);
                            blockedSet.add(blockedUuid);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 加载屏蔽列表失败: " + e.getMessage());
            }
            
            return blockedSet;
        });
    }
    
    /**
     * 添加屏蔽玩家
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> addBlocked(java.util.UUID playerUuid, java.util.UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (databaseManager.isMySQL()) {
                sql = "INSERT IGNORE INTO `" + tablePrefix + "blocked` (`player_uuid`, `blocked_uuid`) VALUES (?, ?)";
            } else {
                sql = "INSERT OR IGNORE INTO `" + tablePrefix + "blocked` (`player_uuid`, `blocked_uuid`) VALUES (?, ?)";
            }
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, targetUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 添加屏蔽失败: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 移除屏蔽玩家
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> removeBlocked(java.util.UUID playerUuid, java.util.UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "blocked` WHERE `player_uuid` = ? AND `blocked_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, targetUuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 移除屏蔽失败: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 屏蔽玩家（同步方法别名）
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> blockPlayer(java.util.UUID playerUuid, java.util.UUID targetUuid) {
        return addBlocked(playerUuid, targetUuid);
    }
    
    /**
     * 取消屏蔽玩家（同步方法别名）
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> unblockPlayer(java.util.UUID playerUuid, java.util.UUID targetUuid) {
        return removeBlocked(playerUuid, targetUuid);
    }
    
    /**
     * 获取需要通知该玩家上线的好友UUID列表
     * 
     * @param playerUuid 上线的玩家UUID
     * @return CompletableFuture<List<UUID>> 需要通知的好友UUID列表
     */
    public CompletableFuture<List<java.util.UUID>> getFriendsToNotify(java.util.UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<java.util.UUID> friendsToNotify = new ArrayList<>();
            
            String sql = "SELECT f.player_uuid FROM `" + tablePrefix + "friends` f " +
                    "INNER JOIN `" + tablePrefix + "players` p ON f.player_uuid = p.uuid " +
                    "WHERE f.friend_uuid = ? AND p.notify_online = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setBoolean(2, true);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String uuidStr = resultSet.getString("player_uuid");
                        try {
                            friendsToNotify.add(java.util.UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[FriendDAO] 获取需要通知的好友列表失败: " + e.getMessage());
            }
            
            return friendsToNotify;
        });
    }
}
