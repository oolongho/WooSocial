package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.attachment.AttachmentSerializer;
import com.oolonghoo.woosocial.model.MailData;
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
                    "`item_data`, `attachments`, `send_time`, `expire_time`, `is_read`, `is_claimed`, " +
                    "`is_bulk`, `bulk_id`, `is_system`, `scheduled_time`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, mail.getSenderUuid().toString());
                statement.setString(2, mail.getSenderName());
                statement.setString(3, mail.getReceiverUuid().toString());
                statement.setString(4, mail.getReceiverName());
                statement.setString(5, mail.getItemData()); // 保留用于向后兼容
                statement.setString(6, AttachmentSerializer.serializeList(mail.getAttachments()));
                statement.setLong(7, mail.getSendTime());
                statement.setLong(8, mail.getExpireTime());
                statement.setBoolean(9, mail.isRead());
                statement.setBoolean(10, mail.isClaimed());
                statement.setBoolean(11, mail.isBulk());
                statement.setString(12, mail.getBulkId());
                statement.setBoolean(13, mail.isSystem());
                statement.setLong(14, mail.getScheduledTime());
                
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
    
    /**
     * 批量插入邮件
     * 根据数据库类型使用不同的优化策略：
     * - MySQL: 使用单条多值INSERT语句（INSERT INTO ... VALUES (...), (...), ...）
     * - SQLite: 使用事务批量提交
     * 
     * @param mails 要插入的邮件列表
     * @return 成功插入的邮件数量
     */
    public CompletableFuture<Integer> bulkInsertMails(List<MailData> mails) {
        if (mails == null || mails.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int successCount;
            
            // 根据数据库类型选择不同的批量插入策略
            if (databaseManager.isMySQL()) {
                successCount = bulkInsertMySQL(mails);
            } else {
                successCount = bulkInsertSQLite(mails);
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info(() -> "[Mail] 批量插入完成: 成功 " + successCount + "/" + mails.size() + 
                    " 封邮件, 耗时 " + elapsed + "ms");
            
            return successCount;
        });
    }
    
    /**
     * MySQL批量插入 - 使用单条多值INSERT语句
     * 这种方式比逐条插入性能更高，因为减少了网络往返次数
     */
    private int bulkInsertMySQL(List<MailData> mails) {
        // 每批次最多插入100条，避免SQL语句过长
        int batchSize = 100;
        int totalSuccess = 0;
        
        for (int i = 0; i < mails.size(); i += batchSize) {
            int end = Math.min(i + batchSize, mails.size());
            List<MailData> batch = mails.subList(i, end);
            
            // 构建多值INSERT语句: INSERT INTO table (...) VALUES (?,...), (?,...), ...
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO `").append(tablePrefix).append("mails` ")
               .append("(`sender_uuid`, `sender_name`, `receiver_uuid`, `receiver_name`, ")
               .append("`item_data`, `attachments`, `send_time`, `expire_time`, `is_read`, `is_claimed`, ")
               .append("`is_bulk`, `bulk_id`, `is_system`, `scheduled_time`) VALUES ");
            
            // 添加占位符
            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) {
                    sql.append(", ");
                }
                sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            }
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                
                // 设置参数
                int paramIndex = 1;
                for (MailData mail : batch) {
                    statement.setString(paramIndex++, mail.getSenderUuid().toString());
                    statement.setString(paramIndex++, mail.getSenderName());
                    statement.setString(paramIndex++, mail.getReceiverUuid().toString());
                    statement.setString(paramIndex++, mail.getReceiverName());
                    statement.setString(paramIndex++, mail.getItemData());
                    statement.setString(paramIndex++, AttachmentSerializer.serializeList(mail.getAttachments()));
                    statement.setLong(paramIndex++, mail.getSendTime());
                    statement.setLong(paramIndex++, mail.getExpireTime());
                    statement.setBoolean(paramIndex++, mail.isRead());
                    statement.setBoolean(paramIndex++, mail.isClaimed());
                    statement.setBoolean(paramIndex++, mail.isBulk());
                    statement.setString(paramIndex++, mail.getBulkId());
                    statement.setBoolean(paramIndex++, mail.isSystem());
                    statement.setLong(paramIndex++, mail.getScheduledTime());
                }
                
                // 执行插入
                statement.executeUpdate();
                totalSuccess += batch.size();
                
                // 获取生成的主键
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    int keyIndex = 0;
                    while (keys.next() && keyIndex < batch.size()) {
                        batch.get(keyIndex).setId(keys.getInt(1));
                        keyIndex++;
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "MySQL批量插入邮件失败 (批次 " + (i/batchSize + 1) + ")", e);
                // 如果批量插入失败，回退到逐条插入
                for (MailData mail : batch) {
                    if (createMailSync(mail)) {
                        totalSuccess++;
                    }
                }
            }
        }
        
        return totalSuccess;
    }
    
    /**
     * SQLite批量插入 - 使用事务批量提交
     * SQLite不支持多值INSERT，但可以通过事务显著提高性能
     */
    private int bulkInsertSQLite(List<MailData> mails) {
        String sql = "INSERT INTO `" + tablePrefix + "mails` " +
                "(`sender_uuid`, `sender_name`, `receiver_uuid`, `receiver_name`, " +
                "`item_data`, `attachments`, `send_time`, `expire_time`, `is_read`, `is_claimed`, " +
                "`is_bulk`, `bulk_id`, `is_system`, `scheduled_time`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // ✅ 使用 try-with-resources 确保连接自动关闭
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false); // 开启事务
            
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (MailData mail : mails) {
                    statement.setString(1, mail.getSenderUuid().toString());
                    statement.setString(2, mail.getSenderName());
                    statement.setString(3, mail.getReceiverUuid().toString());
                    statement.setString(4, mail.getReceiverName());
                    statement.setString(5, mail.getItemData());
                    statement.setString(6, AttachmentSerializer.serializeList(mail.getAttachments()));
                    statement.setLong(7, mail.getSendTime());
                    statement.setLong(8, mail.getExpireTime());
                    statement.setBoolean(9, mail.isRead());
                    statement.setBoolean(10, mail.isClaimed());
                    statement.setBoolean(11, mail.isBulk());
                    statement.setString(12, mail.getBulkId());
                    statement.setBoolean(13, mail.isSystem());
                    statement.setLong(14, mail.getScheduledTime());
                    
                    statement.addBatch();
                }
                
                statement.executeBatch();
                connection.commit(); // 提交事务
                
                // 获取生成的主键
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    int keyIndex = 0;
                    while (keys.next() && keyIndex < mails.size()) {
                        mails.get(keyIndex).setId(keys.getInt(1));
                        keyIndex++;
                    }
                }
                
                return mails.size();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite 批量插入邮件失败", e);
            // 回退到逐条插入
            int successCount = 0;
            for (MailData mail : mails) {
                if (createMailSync(mail)) {
                    successCount++;
                }
            }
            return successCount;
        }
    }
    
    /**
     * 同步创建单封邮件（内部使用）
     */
    private boolean createMailSync(MailData mail) {
        String sql = "INSERT INTO `" + tablePrefix + "mails` " +
                "(`sender_uuid`, `sender_name`, `receiver_uuid`, `receiver_name`, " +
                "`item_data`, `attachments`, `send_time`, `expire_time`, `is_read`, `is_claimed`, " +
                "`is_bulk`, `bulk_id`, `is_system`, `scheduled_time`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            statement.setString(1, mail.getSenderUuid().toString());
            statement.setString(2, mail.getSenderName());
            statement.setString(3, mail.getReceiverUuid().toString());
            statement.setString(4, mail.getReceiverName());
            statement.setString(5, mail.getItemData());
            statement.setString(6, AttachmentSerializer.serializeList(mail.getAttachments()));
            statement.setLong(7, mail.getSendTime());
            statement.setLong(8, mail.getExpireTime());
            statement.setBoolean(9, mail.isRead());
            statement.setBoolean(10, mail.isClaimed());
            statement.setBoolean(11, mail.isBulk());
            statement.setString(12, mail.getBulkId());
            statement.setBoolean(13, mail.isSystem());
            statement.setLong(14, mail.getScheduledTime());
            
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
        mail.setItemData(resultSet.getString("item_data")); // 向后兼容，会自动转换为附件
        
        // 读取新的附件字段
        String attachmentsJson = resultSet.getString("attachments");
        if (attachmentsJson != null && !attachmentsJson.isEmpty()) {
            mail.setAttachments(AttachmentSerializer.deserializeList(attachmentsJson));
        }
        
        mail.setExpireTime(resultSet.getLong("expire_time"));
        mail.setRead(resultSet.getBoolean("is_read"));
        mail.setClaimed(resultSet.getBoolean("is_claimed"));
        mail.setBulk(resultSet.getBoolean("is_bulk"));
        mail.setBulkId(resultSet.getString("bulk_id"));
        
        // 读取新字段
        try {
            mail.setSystem(resultSet.getBoolean("is_system"));
        } catch (SQLException e) {
            // 字段可能不存在（旧数据库），使用默认值
            mail.setSystem(false);
        }
        
        try {
            mail.setScheduledTime(resultSet.getLong("scheduled_time"));
        } catch (SQLException e) {
            // 字段可能不存在（旧数据库），使用默认值
            mail.setScheduledTime(0);
        }
        
        return mail;
    }
    
    /**
     * 获取待发送的定时邮件列表
     * 查询所有已到发送时间的定时邮件
     * 
     * @return 待发送的邮件列表
     */
    public CompletableFuture<List<MailData>> getScheduledMailsToSend() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` " +
                    "WHERE `scheduled_time` > 0 AND `scheduled_time` <= ? " +
                    "ORDER BY `scheduled_time` ASC";
            List<MailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setLong(1, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取待发送定时邮件失败", e);
            }
            return mails;
        });
    }
    
    /**
     * 获取指定玩家的定时邮件列表
     * 
     * @param receiverUuid 收件人UUID
     * @return 定时邮件列表
     */
    public CompletableFuture<List<MailData>> getScheduledMailsForReceiver(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `scheduled_time` > ? " +
                    "ORDER BY `scheduled_time` ASC";
            List<MailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setLong(2, 0);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取玩家定时邮件失败: " + receiverUuid, e);
            }
            return mails;
        });
    }
    
    /**
     * 获取所有系统邮件
     * 
     * @param receiverUuid 收件人UUID
     * @return 系统邮件列表
     */
    public CompletableFuture<List<MailData>> getSystemMailsForReceiver(UUID receiverUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "mails` " +
                    "WHERE `receiver_uuid` = ? AND `is_system` = ? AND `expire_time` > ? " +
                    "ORDER BY `send_time` DESC";
            List<MailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, receiverUuid.toString());
                statement.setBoolean(2, true);
                statement.setLong(3, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取系统邮件失败: " + receiverUuid, e);
            }
            return mails;
        });
    }
}
