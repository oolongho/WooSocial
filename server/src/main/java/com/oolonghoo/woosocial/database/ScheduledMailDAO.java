package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.model.ScheduledMailData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 定时邮件数据访问对象
 * 负责定时邮件的数据库操作
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class ScheduledMailDAO {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    private final String tablePrefix;
    private final Gson gson;
    
    public ScheduledMailDAO(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
        this.gson = new Gson();
    }
    
    /**
     * 创建定时邮件
     * 
     * @param scheduledMail 定时邮件数据
     * @return 是否创建成功
     */
    public CompletableFuture<Boolean> createScheduledMail(ScheduledMailData scheduledMail) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO `" + tablePrefix + "scheduled_mails` " +
                    "(`sender_uuid`, `sender_name`, `receiver_uuids`, `receiver_names`, " +
                    "`attachments`, `scheduled_time`, `create_time`, `status`) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, scheduledMail.getSenderUuid().toString());
                statement.setString(2, scheduledMail.getSenderName());
                
                // 将接收者UUID列表序列化为JSON
                String receiverUuidsJson = gson.toJson(scheduledMail.getReceiverUuids());
                statement.setString(3, receiverUuidsJson);
                
                statement.setString(4, scheduledMail.getReceiverNames());
                statement.setString(5, scheduledMail.getAttachments());
                statement.setLong(6, scheduledMail.getScheduledTime());
                statement.setLong(7, scheduledMail.getCreateTime());
                statement.setString(8, scheduledMail.getStatus().name());
                
                int affected = statement.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            scheduledMail.setId(keys.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建定时邮件失败", e);
            }
            return false;
        });
    }
    
    /**
     * 获取所有待发送的定时邮件
     * 
     * @return 待发送的定时邮件列表
     */
    public CompletableFuture<List<ScheduledMailData>> getPendingScheduledMails() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "scheduled_mails` " +
                    "WHERE `status` = ? AND `scheduled_time` <= ? " +
                    "ORDER BY `scheduled_time` ASC";
            List<ScheduledMailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, ScheduledMailData.Status.PENDING.name());
                statement.setLong(2, System.currentTimeMillis());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToScheduledMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取待发送定时邮件失败", e);
            }
            return mails;
        });
    }
    
    /**
     * 获取指定发送者的所有待发送定时邮件
     * 
     * @param senderUuid 发送者UUID
     * @return 定时邮件列表
     */
    public CompletableFuture<List<ScheduledMailData>> getPendingMailsBySender(UUID senderUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "scheduled_mails` " +
                    "WHERE `sender_uuid` = ? AND `status` = ? " +
                    "ORDER BY `scheduled_time` ASC";
            List<ScheduledMailData> mails = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, senderUuid.toString());
                statement.setString(2, ScheduledMailData.Status.PENDING.name());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        mails.add(mapResultSetToScheduledMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取发送者的定时邮件失败: " + senderUuid, e);
            }
            return mails;
        });
    }
    
    /**
     * 根据ID获取定时邮件
     * 
     * @param id 定时邮件ID
     * @return 定时邮件数据
     */
    public CompletableFuture<Optional<ScheduledMailData>> getScheduledMailById(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM `" + tablePrefix + "scheduled_mails` WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setInt(1, id);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToScheduledMailData(resultSet));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取定时邮件失败: " + id, e);
            }
            return Optional.empty();
        });
    }
    
    /**
     * 更新定时邮件状态
     * 
     * @param id 定时邮件ID
     * @param status 新状态
     * @return 是否更新成功
     */
    public CompletableFuture<Boolean> updateStatus(int id, ScheduledMailData.Status status) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "scheduled_mails` SET `status` = ? WHERE `id` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, status.name());
                statement.setInt(2, id);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新定时邮件状态失败: " + id, e);
                return false;
            }
        });
    }
    
    /**
     * 取消定时邮件
     * 
     * @param id 定时邮件ID
     * @param senderUuid 发送者UUID（用于验证权限）
     * @return 是否取消成功
     */
    public CompletableFuture<Boolean> cancelScheduledMail(int id, UUID senderUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE `" + tablePrefix + "scheduled_mails` " +
                    "SET `status` = ? WHERE `id` = ? AND `sender_uuid` = ? AND `status` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, ScheduledMailData.Status.CANCELLED.name());
                statement.setInt(2, id);
                statement.setString(3, senderUuid.toString());
                statement.setString(4, ScheduledMailData.Status.PENDING.name());
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "取消定时邮件失败: " + id, e);
                return false;
            }
        });
    }
    
    /**
     * 删除已发送或已取消的定时邮件（清理用）
     * 
     * @param daysAgo 多少天前的记录
     * @return 删除的记录数
     */
    public CompletableFuture<Integer> cleanOldScheduledMails(int daysAgo) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM `" + tablePrefix + "scheduled_mails` " +
                    "WHERE `status` != ? AND `create_time` < ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, ScheduledMailData.Status.PENDING.name());
                statement.setLong(2, System.currentTimeMillis() - (daysAgo * 24L * 60 * 60 * 1000));
                
                return statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "清理定时邮件失败", e);
                return 0;
            }
        });
    }
    
    /**
     * 获取待发送定时邮件的数量
     * 
     * @param senderUuid 发送者UUID
     * @return 待发送数量
     */
    public CompletableFuture<Integer> getPendingCountBySender(UUID senderUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM `" + tablePrefix + "scheduled_mails` " +
                    "WHERE `sender_uuid` = ? AND `status` = ?";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, senderUuid.toString());
                statement.setString(2, ScheduledMailData.Status.PENDING.name());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "获取定时邮件数量失败: " + senderUuid, e);
            }
            return 0;
        });
    }
    
    /**
     * 将ResultSet映射为ScheduledMailData对象
     */
    private ScheduledMailData mapResultSetToScheduledMailData(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID senderUuid = UUID.fromString(resultSet.getString("sender_uuid"));
        long scheduledTime = resultSet.getLong("scheduled_time");
        long createTime = resultSet.getLong("create_time");
        
        ScheduledMailData mail = new ScheduledMailData(id, senderUuid, scheduledTime, createTime);
        mail.setSenderName(resultSet.getString("sender_name"));
        mail.setReceiverNames(resultSet.getString("receiver_names"));
        mail.setAttachments(resultSet.getString("attachments"));
        mail.setStatus(ScheduledMailData.Status.valueOf(resultSet.getString("status")));
        
        // 反序列化接收者UUID列表
        String receiverUuidsJson = resultSet.getString("receiver_uuids");
        if (receiverUuidsJson != null && !receiverUuidsJson.isEmpty()) {
            try {
                Type listType = new TypeToken<List<UUID>>() {}.getType();
                List<UUID> receiverUuids = gson.fromJson(receiverUuidsJson, listType);
                mail.setReceiverUuids(receiverUuids);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "反序列化接收者UUID列表失败: " + id, e);
                mail.setReceiverUuids(new ArrayList<>());
            }
        } else {
            mail.setReceiverUuids(new ArrayList<>());
        }
        
        return mail;
    }
}
