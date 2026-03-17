package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.ShowcaseLikeCooldown;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShowcaseLikeCooldownDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public ShowcaseLikeCooldownDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    public CompletableFuture<ShowcaseLikeCooldown> getCooldown(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "showcase_like_cooldown` WHERE `player_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, playerUuid.toString());
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        ShowcaseLikeCooldown cooldown = new ShowcaseLikeCooldown(playerUuid);
                        cooldown.setLastLikeTime(rs.getLong("last_like_time"));
                        cooldown.setDailyCount(rs.getInt("daily_count"));
                        cooldown.setDailyResetTime(rs.getLong("daily_reset_time"));
                        
                        if (cooldown.shouldResetDaily()) {
                            cooldown.resetDailyData();
                            updateCooldown(cooldown);
                        }
                        
                        return cooldown;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseLikeCooldownDAO] 获取冷却数据失败：" + e.getMessage());
            }
            
            return new ShowcaseLikeCooldown(playerUuid);
        });
    }
    
    public ShowcaseLikeCooldown getCooldownSync(UUID playerUuid) {
        String sql = "SELECT * FROM `" + tablePrefix + "showcase_like_cooldown` WHERE `player_uuid` = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    ShowcaseLikeCooldown cooldown = new ShowcaseLikeCooldown(playerUuid);
                    cooldown.setLastLikeTime(rs.getLong("last_like_time"));
                    cooldown.setDailyCount(rs.getInt("daily_count"));
                    cooldown.setDailyResetTime(rs.getLong("daily_reset_time"));
                    
                    if (cooldown.shouldResetDaily()) {
                        cooldown.resetDailyData();
                        updateCooldown(cooldown);
                    }
                    
                    return cooldown;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "[ShowcaseLikeCooldownDAO] 获取冷却数据失败：" + e.getMessage());
        }
        
        return new ShowcaseLikeCooldown(playerUuid);
    }
    
    public CompletableFuture<Boolean> updateCooldown(ShowcaseLikeCooldown cooldown) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR REPLACE INTO `" + tablePrefix + "showcase_like_cooldown` " +
                    "(`player_uuid`, `last_like_time`, `daily_count`, `daily_reset_time`) VALUES (?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, cooldown.getPlayerUuid().toString());
                statement.setLong(2, cooldown.getLastLikeTime());
                statement.setInt(3, cooldown.getDailyCount());
                statement.setLong(4, cooldown.getDailyResetTime());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseLikeCooldownDAO] 更新冷却数据失败：" + e.getMessage());
                return false;
            }
        });
    }
    
    public boolean updateCooldownSync(ShowcaseLikeCooldown cooldown) {
        String sql = "INSERT OR REPLACE INTO `" + tablePrefix + "showcase_like_cooldown` " +
                "(`player_uuid`, `last_like_time`, `daily_count`, `daily_reset_time`) VALUES (?, ?, ?, ?)";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, cooldown.getPlayerUuid().toString());
            statement.setLong(2, cooldown.getLastLikeTime());
            statement.setInt(3, cooldown.getDailyCount());
            statement.setLong(4, cooldown.getDailyResetTime());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "[ShowcaseLikeCooldownDAO] 更新冷却数据失败：" + e.getMessage());
            return false;
        }
    }
}
