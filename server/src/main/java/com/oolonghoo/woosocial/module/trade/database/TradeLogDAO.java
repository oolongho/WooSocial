package com.oolonghoo.woosocial.module.trade.database;

import com.google.gson.Gson;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.DatabaseManager;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TradeLogDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    private final Gson gson;
    
    public TradeLogDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
        this.gson = new Gson();
    }
    
    /**
     * 记录交易日志
     */
    public CompletableFuture<Void> logTrade(
            UUID player1Uuid, String player1Name,
            UUID player2Uuid, String player2Name,
            List<ItemStack> player1Items, List<ItemStack> player2Items,
            double player1Money, double player2Money,
            int player1Points, int player2Points,
            String status, String cancelReason, String server) {
        
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "trade_log` " +
                    "(player1_uuid, player1_name, player2_uuid, player2_name, " +
                    "player1_items, player2_items, player1_money, player2_money, " +
                    "player1_points, player2_points, status, cancel_reason, server, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, player1Uuid.toString());
                statement.setString(2, player1Name);
                statement.setString(3, player2Uuid.toString());
                statement.setString(4, player2Name);
                
                statement.setString(5, serializeItems(player1Items));
                statement.setString(6, serializeItems(player2Items));
                
                statement.setDouble(7, player1Money);
                statement.setDouble(8, player2Money);
                statement.setInt(9, player1Points);
                statement.setInt(10, player2Points);
                
                statement.setString(11, status);
                statement.setString(12, cancelReason);
                statement.setString(13, server);
                statement.setLong(14, System.currentTimeMillis());
                
                statement.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 记录交易日志失败", e);
            }
        });
    }
    
    /**
     * 记录完成的交易
     */
    public CompletableFuture<Void> logCompletedTrade(
            UUID player1Uuid, String player1Name,
            UUID player2Uuid, String player2Name,
            List<ItemStack> player1Items, List<ItemStack> player2Items,
            double player1Money, double player2Money,
            int player1Points, int player2Points,
            String server) {
        
        return logTrade(player1Uuid, player1Name, player2Uuid, player2Name,
                player1Items, player2Items, player1Money, player2Money,
                player1Points, player2Points, "completed", null, server);
    }
    
    /**
     * 记录取消的交易
     */
    public CompletableFuture<Void> logCancelledTrade(
            UUID player1Uuid, String player1Name,
            UUID player2Uuid, String player2Name,
            String cancelReason, String server) {
        
        return logTrade(player1Uuid, player1Name, player2Uuid, player2Name,
                Collections.emptyList(), Collections.emptyList(),
                0, 0, 0, 0, "cancelled", cancelReason, server);
    }
    
    /**
     * 查询玩家的交易历史
     */
    public CompletableFuture<List<Map<String, Object>>> getTradeHistory(UUID playerUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new ArrayList<>();
            
            String sql = "SELECT * FROM `" + tablePrefix + "trade_log` " +
                    "WHERE player1_uuid = ? OR player2_uuid = ? " +
                    "ORDER BY timestamp DESC LIMIT ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerUuid.toString());
                statement.setInt(3, limit);
                
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", rs.getLong("id"));
                        row.put("player1_uuid", rs.getString("player1_uuid"));
                        row.put("player1_name", rs.getString("player1_name"));
                        row.put("player2_uuid", rs.getString("player2_uuid"));
                        row.put("player2_name", rs.getString("player2_name"));
                        row.put("player1_money", rs.getDouble("player1_money"));
                        row.put("player2_money", rs.getDouble("player2_money"));
                        row.put("player1_points", rs.getInt("player1_points"));
                        row.put("player2_points", rs.getInt("player2_points"));
                        row.put("status", rs.getString("status"));
                        row.put("cancel_reason", rs.getString("cancel_reason"));
                        row.put("server", rs.getString("server"));
                        row.put("timestamp", rs.getLong("timestamp"));
                        results.add(row);
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 查询交易历史失败", e);
            }
            
            return results;
        });
    }
    
    /**
     * 检查两个玩家是否有过交易记录
     */
    public CompletableFuture<Boolean> haveTraded(UUID player1, UUID player2) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "trade_log` " +
                    "WHERE ((player1_uuid = ? AND player2_uuid = ?) OR " +
                    "(player1_uuid = ? AND player2_uuid = ?)) AND status = 'completed'";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, player1.toString());
                statement.setString(2, player2.toString());
                statement.setString(3, player2.toString());
                statement.setString(4, player1.toString());
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 检查交易记录失败", e);
            }
            
            return false;
        });
    }
    
    /**
     * 清理过期的交易日志
     */
    public CompletableFuture<Integer> cleanOldLogs(long olderThanTimestamp) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "trade_log` WHERE timestamp < ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setLong(1, olderThanTimestamp);
                return statement.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 清理旧日志失败", e);
                return 0;
            }
        });
    }
    
    /**
     * 获取玩家的交易历史记录
     */
    public CompletableFuture<List<TradeRecord>> getPlayerTradeHistory(UUID playerUuid, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<TradeRecord> records = new ArrayList<>();
            String sql = "SELECT * FROM `" + tablePrefix + "trade_log` " +
                    "WHERE player1_uuid = ? OR player2_uuid = ? " +
                    "ORDER BY timestamp DESC LIMIT ? OFFSET ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerUuid.toString());
                statement.setInt(3, limit);
                statement.setInt(4, offset);
                
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        records.add(TradeRecord.fromResultSet(rs, this));
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 查询交易历史失败", e);
            }
            
            return records;
        });
    }
    
    /**
     * 获取交易总数
     */
    public CompletableFuture<Integer> getPlayerTradeCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "trade_log` " +
                    "WHERE player1_uuid = ? OR player2_uuid = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerUuid.toString());
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TradeLog] 查询交易数量失败", e);
            }
            
            return 0;
        });
    }
    
    /**
     * 交易记录类
     */
    public static class TradeRecord {
        private final long id;
        private final UUID player1Uuid;
        private final String player1Name;
        private final UUID player2Uuid;
        private final String player2Name;
        private final List<ItemStack> player1Items;
        private final List<ItemStack> player2Items;
        private final double player1Money;
        private final double player2Money;
        private final int player1Points;
        private final int player2Points;
        private final String status;
        private final String cancelReason;
        private final String serverName;
        private final long timestamp;
        
        private TradeRecord(long id, UUID player1Uuid, String player1Name, UUID player2Uuid, String player2Name,
                           List<ItemStack> player1Items, List<ItemStack> player2Items,
                           double player1Money, double player2Money, int player1Points, int player2Points,
                           String status, String cancelReason, String serverName, long timestamp) {
            this.id = id;
            this.player1Uuid = player1Uuid;
            this.player1Name = player1Name;
            this.player2Uuid = player2Uuid;
            this.player2Name = player2Name;
            this.player1Items = player1Items;
            this.player2Items = player2Items;
            this.player1Money = player1Money;
            this.player2Money = player2Money;
            this.player1Points = player1Points;
            this.player2Points = player2Points;
            this.status = status;
            this.cancelReason = cancelReason;
            this.serverName = serverName;
            this.timestamp = timestamp;
        }
        
        public static TradeRecord fromResultSet(ResultSet rs, TradeLogDAO dao) throws SQLException {
            return new TradeRecord(
                    rs.getLong("id"),
                    UUID.fromString(rs.getString("player1_uuid")),
                    rs.getString("player1_name"),
                    UUID.fromString(rs.getString("player2_uuid")),
                    rs.getString("player2_name"),
                    dao.deserializeItems(rs.getString("player1_items")),
                    dao.deserializeItems(rs.getString("player2_items")),
                    rs.getDouble("player1_money"),
                    rs.getDouble("player2_money"),
                    rs.getInt("player1_points"),
                    rs.getInt("player2_points"),
                    rs.getString("status"),
                    rs.getString("cancel_reason"),
                    rs.getString("server"),
                    rs.getLong("timestamp")
            );
        }
        
        // Getters
        public long getId() { return id; }
        public UUID getPlayer1Uuid() { return player1Uuid; }
        public String getPlayer1Name() { return player1Name; }
        public UUID getPlayer2Uuid() { return player2Uuid; }
        public String getPlayer2Name() { return player2Name; }
        public List<ItemStack> getPlayer1Items() { return player1Items; }
        public List<ItemStack> getPlayer2Items() { return player2Items; }
        public double getPlayer1Money() { return player1Money; }
        public double getPlayer2Money() { return player2Money; }
        public int getPlayer1Points() { return player1Points; }
        public int getPlayer2Points() { return player2Points; }
        public String getStatus() { return status; }
        public String getCancelReason() { return cancelReason; }
        public String getServerName() { return serverName; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isCompleted() {
            return "completed".equals(status);
        }
        
        public boolean isCancelled() {
            return "cancelled".equals(status);
        }
    }
    
    /**
     * 序列化物品列表
     */
    private String serializeItems(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ItemStack item : items) {
            if (item != null && item.getType().isItem()) {
                try {
                    Map<String, Object> serialized = item.serialize();
                    String json = gson.toJson(serialized);
                    sb.append(json).append("|||");
                } catch (Exception e) {
                    plugin.getLogger().warning(() -> "[TradeLog] 序列化物品失败: " + e.getMessage());
                }
            }
        }
        
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    public List<ItemStack> deserializeItems(String data) {
        List<ItemStack> items = new ArrayList<>();
        
        if (data == null || data.isEmpty()) {
            return items;
        }
        
        String[] parts = data.split("\\|\\|\\|");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            try {
                Map<String, Object> map = gson.fromJson(part, Map.class);
                ItemStack item = ItemStack.deserialize(map);
                items.add(item);
            } catch (RuntimeException e) {
                plugin.getLogger().warning(() -> "[TradeLog] 反序列化物品失败：" + e.getMessage());
            }
        }
        
        return items;
    }
}
