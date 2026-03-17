package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.ShowcaseData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShowcaseDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public ShowcaseDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    public CompletableFuture<ShowcaseData> loadShowcase(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "showcase` WHERE `owner_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, ownerUuid.toString());
                
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        ShowcaseData data = new ShowcaseData(ownerUuid);
                        data.setOwnerName(rs.getString("owner_name"));
                        data.setLikes(rs.getInt("likes"));
                        data.setLastUpdated(rs.getLong("last_updated"));
                        
                        String itemsBase64 = rs.getString("items");
                        if (itemsBase64 != null && !itemsBase64.isEmpty()) {
                            deserializeItems(itemsBase64, data);
                        }
                        
                        return data;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseDAO] 加载展示柜失败: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    public ShowcaseData loadShowcaseSync(UUID ownerUuid) {
        String sql = "SELECT * FROM `" + tablePrefix + "showcase` WHERE `owner_uuid` = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, ownerUuid.toString());
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    ShowcaseData data = new ShowcaseData(ownerUuid);
                    data.setOwnerName(rs.getString("owner_name"));
                    data.setLikes(rs.getInt("likes"));
                    data.setLastUpdated(rs.getLong("last_updated"));
                    
                    String itemsBase64 = rs.getString("items");
                    if (itemsBase64 != null && !itemsBase64.isEmpty()) {
                        deserializeItems(itemsBase64, data);
                    }
                    
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "[ShowcaseDAO] 加载展示柜失败: " + e.getMessage());
        }
        
        return null;
    }
    
    public CompletableFuture<Boolean> saveShowcase(ShowcaseData data) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR REPLACE INTO `" + tablePrefix + "showcase` " +
                    "(`owner_uuid`, `owner_name`, `items`, `likes`, `last_updated`) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, data.getOwnerUuid().toString());
                statement.setString(2, data.getOwnerName());
                statement.setString(3, serializeItems(data));
                statement.setInt(4, data.getLikes());
                statement.setLong(5, data.getLastUpdated());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseDAO] 保存展示柜失败: " + e.getMessage());
                return false;
            }
        });
    }
    
    public boolean saveShowcaseSync(ShowcaseData data) {
        String sql = "INSERT OR REPLACE INTO `" + tablePrefix + "showcase` " +
                "(`owner_uuid`, `owner_name`, `items`, `likes`, `last_updated`) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, data.getOwnerUuid().toString());
            statement.setString(2, data.getOwnerName());
            statement.setString(3, serializeItems(data));
            statement.setInt(4, data.getLikes());
            statement.setLong(5, data.getLastUpdated());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "[ShowcaseDAO] 保存展示柜失败: " + e.getMessage());
            return false;
        }
    }
    
    public CompletableFuture<Boolean> addLike(UUID likerUuid, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String insertSql = "INSERT OR IGNORE INTO `" + tablePrefix + "showcase_likes` " +
                    "(`liker_uuid`, `target_uuid`, `like_time`) VALUES (?, ?, ?)";
            String updateSql = "UPDATE `" + tablePrefix + "showcase` SET `likes` = `likes` + 1 WHERE `owner_uuid` = ?";
            
            try (Connection connection = databaseManager.getConnection()) {
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, likerUuid.toString());
                    insertStmt.setString(2, targetUuid.toString());
                    insertStmt.setLong(3, System.currentTimeMillis());
                    
                    if (insertStmt.executeUpdate() > 0) {
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, targetUuid.toString());
                            updateStmt.executeUpdate();
                        }
                        return true;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseDAO] 添加点赞失败: " + e.getMessage());
            }
            
            return false;
        });
    }
    
    public CompletableFuture<Boolean> removeLike(UUID likerUuid, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String deleteSql = "DELETE FROM `" + tablePrefix + "showcase_likes` " +
                    "WHERE `liker_uuid` = ? AND `target_uuid` = ?";
            String updateSql = "UPDATE `" + tablePrefix + "showcase` SET `likes` = `likes` - 1 WHERE `owner_uuid` = ? AND `likes` > 0";
            
            try (Connection connection = databaseManager.getConnection()) {
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, likerUuid.toString());
                    deleteStmt.setString(2, targetUuid.toString());
                    
                    if (deleteStmt.executeUpdate() > 0) {
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, targetUuid.toString());
                            updateStmt.executeUpdate();
                        }
                        return true;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(() -> "[ShowcaseDAO] 取消点赞失败: " + e.getMessage());
            }
            
            return false;
        });
    }
    
    public boolean hasLikedSync(UUID likerUuid, UUID targetUuid) {
        String sql = "SELECT 1 FROM `" + tablePrefix + "showcase_likes` " +
                "WHERE `liker_uuid` = ? AND `target_uuid` = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, likerUuid.toString());
            statement.setString(2, targetUuid.toString());
            
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "[ShowcaseDAO] 检查点赞状态失败: " + e.getMessage());
        }
        
        return false;
    }
    
    public CompletableFuture<Boolean> hasLiked(UUID likerUuid, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> hasLikedSync(likerUuid, targetUuid));
    }
    
    private String serializeItems(ShowcaseData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            
            boos.writeInt(data.getItems().size());
            for (ItemStack item : data.getItems()) {
                boos.writeObject(item);
            }
            
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning(() -> "[ShowcaseDAO] 序列化物品失败: " + e.getMessage());
            return "";
        }
    }
    
    private void deserializeItems(String base64, ShowcaseData data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            
            int size = bois.readInt();
            for (int i = 0; i < size; i++) {
                Object obj = bois.readObject();
                if (obj instanceof ItemStack item) {
                    data.addItem(item);
                }
                // 跳过非 ItemStack 对象而不是添加 null
            }
        } catch (Exception e) {
            plugin.getLogger().warning(() -> "[ShowcaseDAO] 反序列化物品失败: " + e.getMessage());
        }
    }
}
