package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MailDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    
    public MailDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    public CompletableFuture<Boolean> createMail(MailData mail) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "mails` " +
                    "(`sender_uuid`, `sender_name`, `receiver_uuid`, `receiver_name`, " +
                    "`item_data`, `send_time`, `expire_time`, `is_read`, `is_claimed`, `is_bulk`, `bulk_id`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, mail.getSenderUuid().toString());
                statement.setString(2, mail.getSenderName());
                statement.setString(3, mail.getReceiverUuid().toString());
                statement.setString(4, mail.getReceiverName());
                statement.setString(5, mail.getItemData());
                statement.setLong(6, mail.getSendTime());
                statement.setLong(7, mail.getExpireTime());
                statement.setBoolean(8, mail.isRead());
                statement.setBoolean(9, mail.isClaimed());
                statement.setBoolean(10, mail.isBulk());
                statement.setString(11, mail.getBulkId());
                
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            mail.setId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建邮件失败", e);
            }
            return false;
        });
    }
    
    public CompletableFuture<List<MailData>> getMailsForReceiver(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `expire_time` > ? " +
                    "ORDER BY `send_time` DESC";
            List<MailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setLong(2, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取邮件列表失败: " + receiverUuid, e);
            }
            return mails;
        });
    }
    
    public CompletableFuture<List<MailData>> getMailsForReceiver(UUID receiverUuid, int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `expire_time` > ? " +
                    "ORDER BY `send_time` DESC LIMIT ? OFFSET ?";
            List<MailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, pageSize);
                statement.setInt(4, (page - 1) * pageSize);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取邮件列表失败: " + receiverUuid, e);
            }
            return mails;
        });
    }
    
    public CompletableFuture<Integer> getMailCountForReceiver(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `expire_time` > ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setLong(2, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取邮件数量失败: " + receiverUuid, e);
            }
            return 0;
        });
    }
    
    public CompletableFuture<Integer> getUnreadCountForReceiver(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `is_read` = ? AND `expire_time` > ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setBoolean(2, false);
                statement.setLong(3, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取未读邮件数量失败: " + receiverUuid, e);
            }
            return 0;
        });
    }
    
    public CompletableFuture<Optional<MailData>> getMailById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, id);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取邮件失败: " + id, e);
            }
            return Optional.empty();
        });
    }
    
    public CompletableFuture<Boolean> markAsRead(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "mails` SET `is_read` = ? WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setBoolean(1, true);
                statement.setInt(2, id);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "标记邮件已读失败: " + id, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> markAsClaimed(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "mails` SET `is_claimed` = ? WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setBoolean(1, true);
                statement.setInt(2, id);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "标记邮件已领取失败: " + id, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteMail(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "mails` WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, id);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除邮件失败: " + id, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Integer> cleanExpiredMails() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "mails` WHERE `expire_time` < ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setLong(1, System.currentTimeMillis());
                
                return statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "清理过期邮件失败", e);
                return 0;
            }
        });
    }
    
    public CompletableFuture<Boolean> hasReceivedBulkMail(UUID receiverUuid, String bulkId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `bulk_id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setString(2, bulkId);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "检查批量邮件失败", e);
            }
            return false;
        });
    }
    
    private MailData mapResultSetToMailData(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID senderUuid = UUID.fromString(resultSet.getString("sender_uuid"));
        UUID receiverUuid = UUID.fromString(resultSet.getString("receiver_uuid"));
        long sendTime = resultSet.getLong("send_time");
        
        MailData mail = new MailData(id, senderUuid, receiverUuid, sendTime);
        mail.setSenderName(resultSet.getString("sender_name"));
        mail.setReceiverName(resultSet.getString("receiver_name"));
        mail.setItemData(resultSet.getString("item_data"));
        mail.setExpireTime(resultSet.getLong("expire_time"));
        mail.setRead(resultSet.getBoolean("is_read"));
        mail.setClaimed(resultSet.getBoolean("is_claimed"));
        mail.setBulk(resultSet.getBoolean("is_bulk"));
        mail.setBulkId(resultSet.getString("bulk_id"));
        
        return mail;
    }
}
