package com.oolonghoo.woosocial.sync.handler;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySQLSyncHandler implements SyncHandler {
    
    private static final String TABLE_NAME = "woosocial_sync_queue";
    
    private final WooSocial plugin;
    private final SyncManager syncManager;
    private final int pollInterval;
    private int taskId = -1;
    private long lastPollTime = 0;
    private boolean available = false;
    
    public MySQLSyncHandler(WooSocial plugin, SyncManager syncManager) {
        this.plugin = plugin;
        this.syncManager = syncManager;
        this.pollInterval = syncManager.getConfig().getMysqlPollInterval();
    }
    
    @Override
    public void initialize() {
        createTableIfNotExists();
        
        startPollingTask();
        
        available = true;
        plugin.getLogger().info("[Sync] MySQL 轮询同步处理器已初始化 (间隔: " + pollInterval + "秒)");
    }
    
    private void createTableIfNotExists() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "message_type VARCHAR(50) NOT NULL, " +
                    "source_server VARCHAR(100) NOT NULL, " +
                    "message_data TEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "processed BOOLEAN DEFAULT FALSE, " +
                    "INDEX idx_created_at (created_at), " +
                    "INDEX idx_processed (processed)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Sync] 创建同步队列表失败: " + e.getMessage());
        }
    }
    
    private void startPollingTask() {
        long intervalTicks = pollInterval * 20L;
        
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            pollMessages();
            cleanupOldMessages();
        }, intervalTicks, intervalTicks).getTaskId();
    }
    
    private void pollMessages() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String selectSql = "SELECT id, message_data FROM " + TABLE_NAME + " " +
                    "WHERE processed = FALSE AND source_server != ? AND created_at > ? " +
                    "ORDER BY created_at ASC LIMIT 100";
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, syncManager.getServerName());
                stmt.setLong(2, lastPollTime > 0 ? lastPollTime : System.currentTimeMillis() - 60000);
                
                ResultSet rs = stmt.executeQuery();
                List<Integer> processedIds = new ArrayList<>();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String messageData = rs.getString("message_data");
                    
                    SyncMessage message = SyncMessage.fromJson(messageData);
                    if (message != null) {
                        syncManager.handleIncomingMessage(message);
                    }
                    processedIds.add(id);
                }
                
                lastPollTime = System.currentTimeMillis();
                
                if (!processedIds.isEmpty()) {
                    markAsProcessed(conn, processedIds);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[Sync] 轮询消息失败: " + e.getMessage());
        }
    }
    
    private void markAsProcessed(Connection conn, List<Integer> ids) {
        if (ids.isEmpty()) return;
        
        try {
            String placeholders = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
            String sql = "UPDATE " + TABLE_NAME + " SET processed = TRUE WHERE id IN (" + placeholders + ")";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[Sync] 标记消息已处理失败: " + e.getMessage());
        }
    }
    
    private void cleanupOldMessages() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "DELETE FROM " + TABLE_NAME + " WHERE processed = TRUE AND created_at < DATE_SUB(NOW(), INTERVAL 1 HOUR)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[Sync] 清理旧消息失败: " + e.getMessage());
        }
    }
    
    @Override
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        available = false;
        plugin.getLogger().info("[Sync] MySQL 轮询同步处理器已关闭");
    }
    
    @Override
    public void sendMessage(SyncMessage message) {
        broadcast(message);
    }
    
    @Override
    public void broadcast(SyncMessage message) {
        if (!available) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "INSERT INTO " + TABLE_NAME + " (message_type, source_server, message_data) VALUES (?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, message.getType().name());
                    stmt.setString(2, message.getSourceServer());
                    stmt.setString(3, message.toJson());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[Sync] 保存同步消息失败: " + e.getMessage());
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    @Override
    public String getName() {
        return "MySQL";
    }
}
