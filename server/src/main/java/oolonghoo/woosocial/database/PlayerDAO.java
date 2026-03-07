package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.PlayerData;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 玩家数据访问对象
 * 负责玩家数据的CRUD操作
 * 所有数据库操作都是异步的
 */
public class PlayerDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public PlayerDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    /**
     * 异步获取玩家数据
     * @param uuid 玩家UUID
     * @return CompletableFuture包含Optional<PlayerData>
     */
    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "players` WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToPlayerData(resultSet));
                    }
                }
                
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取玩家数据失败: " + uuid, e);
                return Optional.empty();
            }
        });
    }
    
    /**
     * 异步获取玩家数据（便捷方法，返回PlayerData或null）
     * @param uuid 玩家UUID
     * @return CompletableFuture包含PlayerData，如果不存在则返回null
     */
    public CompletableFuture<PlayerData> getPlayerDataOrNull(UUID uuid) {
        return getPlayerData(uuid).thenApply(optional -> optional.orElse(null));
    }
    
    /**
     * 异步通过玩家名获取玩家数据
     * @param playerName 玩家名
     * @return CompletableFuture包含Optional<PlayerData>
     */
    public CompletableFuture<Optional<PlayerData>> getPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "players` WHERE `last_name` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerName);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToPlayerData(resultSet));
                    }
                }
                
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "通过玩家名获取玩家数据失败: " + playerName, e);
                return Optional.empty();
            }
        });
    }
    
    /**
     * 异步通过玩家名获取玩家UUID
     * @param playerName 玩家名
     * @return CompletableFuture包含UUID，如果不存在则返回null
     */
    public CompletableFuture<UUID> getPlayerUuidByName(String playerName) {
        return getPlayerDataByName(playerName).thenApply(opt -> opt.map(PlayerData::getUuid).orElse(null));
    }
    
    /**
     * 异步保存或更新玩家数据
     * @param playerData 玩家数据
     * @return CompletableFuture<Boolean> 表示操作是否成功
     */
    public CompletableFuture<Boolean> savePlayerData(PlayerData playerData) {
        return CompletableFuture.supplyAsync(() -> {
            // 先尝试更新，如果没有记录则插入
            String updateSql = "UPDATE `" + tablePrefix + "players` SET " +
                    "`last_name` = ?, `first_join_time` = ?, `last_online_time` = ?, " +
                    "`notify_online` = ?, `allow_teleport` = ?, `settings` = ? " +
                    "WHERE `uuid` = ?";
            
            String insertSql = "INSERT INTO `" + tablePrefix + "players` " +
                    "(`uuid`, `last_name`, `first_join_time`, `last_online_time`, " +
                    "`notify_online`, `allow_teleport`, `settings`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection()) {
                // 先尝试更新
                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, playerData.getLastName());
                    updateStatement.setLong(2, playerData.getFirstJoinTime());
                    updateStatement.setLong(3, playerData.getLastOnlineTime());
                    updateStatement.setBoolean(4, playerData.isNotifyOnline());
                    updateStatement.setBoolean(5, playerData.isAllowTeleport());
                    updateStatement.setString(6, playerData.getSettings());
                    updateStatement.setString(7, playerData.getUuid().toString());
                    
                    int rowsUpdated = updateStatement.executeUpdate();
                    
                    if (rowsUpdated > 0) {
                        return true;
                    }
                }
                
                // 如果没有更新任何行，则插入新记录
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    insertStatement.setString(1, playerData.getUuid().toString());
                    insertStatement.setString(2, playerData.getLastName());
                    insertStatement.setLong(3, playerData.getFirstJoinTime());
                    insertStatement.setLong(4, playerData.getLastOnlineTime());
                    insertStatement.setBoolean(5, playerData.isNotifyOnline());
                    insertStatement.setBoolean(6, playerData.isAllowTeleport());
                    insertStatement.setString(7, playerData.getSettings());
                    
                    int rowsInserted = insertStatement.executeUpdate();
                    return rowsInserted > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "保存玩家数据失败: " + playerData.getUuid(), e);
                return false;
            }
        });
    }
    
    /**
     * 异步更新玩家在线时间
     * @param uuid 玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateLastOnlineTime(UUID uuid) {
        return updateLastOnlineTime(uuid, System.currentTimeMillis());
    }
    
    /**
     * 异步更新玩家在线时间
     * @param uuid 玩家UUID
     * @param timestamp 时间戳
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateLastOnlineTime(UUID uuid, long timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "players` SET `last_online_time` = ? WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setLong(1, timestamp);
                statement.setString(2, uuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新玩家在线时间失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 异步更新玩家所在服务器
     * @param uuid 玩家UUID
     * @param serverName 服务器名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateServerName(UUID uuid, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "players` SET `server_name` = ? WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, serverName);
                statement.setString(2, uuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新玩家服务器失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 异步更新玩家名称
     * @param uuid 玩家UUID
     * @param playerName 玩家名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updatePlayerName(UUID uuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "players` SET `last_name` = ? WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerName);
                statement.setString(2, uuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新玩家名称失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 异步更新玩家设置
     * @param uuid 玩家UUID
     * @param notifyOnline 是否通知在线
     * @param allowTeleport 是否允许传送
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updatePlayerSettings(UUID uuid, boolean notifyOnline, boolean allowTeleport) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "players` SET `notify_online` = ?, `allow_teleport` = ? WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setBoolean(1, notifyOnline);
                statement.setBoolean(2, allowTeleport);
                statement.setString(3, uuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新玩家设置失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 异步更新玩家数据（便捷方法，用于更新PlayerData对象）
     * @param playerData 玩家数据
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updatePlayerData(PlayerData playerData) {
        return updatePlayerSettings(playerData.getUuid(), playerData.isNotifyOnline(), playerData.isAllowTeleport());
    }
    
    /**
     * 异步删除玩家数据
     * @param uuid 玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> deletePlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "players` WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, uuid.toString());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除玩家数据失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 检查玩家是否存在
     * @param uuid 玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "players` WHERE `uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
                
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "检查玩家是否存在失败: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * 将ResultSet映射到PlayerData对象
     * @param resultSet 数据库结果集
     * @return PlayerData对象
     * @throws SQLException SQL异常
     */
    private PlayerData mapResultSetToPlayerData(ResultSet resultSet) throws SQLException {
        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        String lastName = resultSet.getString("last_name");
        long firstJoinTime = resultSet.getLong("first_join_time");
        long lastOnlineTime = resultSet.getLong("last_online_time");
        boolean notifyOnline = resultSet.getBoolean("notify_online");
        boolean allowTeleport = resultSet.getBoolean("allow_teleport");
        String settings = resultSet.getString("settings");
        
        return new PlayerData(uuid, lastName, firstJoinTime, lastOnlineTime, 
                             notifyOnline, allowTeleport, settings);
    }
}
