package com.oolonghoo.woosocial.model;

import java.util.List;
import java.util.UUID;

/**
 * 定时邮件数据模型
 * 用于存储定时发送的邮件信息
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class ScheduledMailData {
    
    /**
     * 定时邮件状态枚举
     */
    public enum Status {
        /**
         * 待发送
         */
        PENDING,
        /**
         * 已发送
         */
        SENT,
        /**
         * 已取消
         */
        CANCELLED
    }
    
    private int id;
    private final UUID senderUuid;
    private String senderName;
    private List<UUID> receiverUuids;
    private String receiverNames;
    private String attachments;
    private final long scheduledTime;
    private final long createTime;
    private Status status;
    
    /**
     * 创建新的定时邮件
     * 
     * @param senderUuid 发送者UUID
     * @param scheduledTime 计划发送时间（时间戳）
     */
    public ScheduledMailData(UUID senderUuid, long scheduledTime) {
        this.senderUuid = senderUuid;
        this.scheduledTime = scheduledTime;
        this.createTime = System.currentTimeMillis();
        this.status = Status.PENDING;
    }
    
    /**
     * 从数据库加载的定时邮件
     * 
     * @param id 邮件ID
     * @param senderUuid 发送者UUID
     * @param scheduledTime 计划发送时间
     * @param createTime 创建时间
     */
    public ScheduledMailData(int id, UUID senderUuid, long scheduledTime, long createTime) {
        this.id = id;
        this.senderUuid = senderUuid;
        this.scheduledTime = scheduledTime;
        this.createTime = createTime;
        this.status = Status.PENDING;
    }
    
    // ==================== Getters and Setters ====================
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getSenderUuid() {
        return senderUuid;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public List<UUID> getReceiverUuids() {
        return receiverUuids;
    }
    
    public void setReceiverUuids(List<UUID> receiverUuids) {
        this.receiverUuids = receiverUuids;
    }
    
    public String getReceiverNames() {
        return receiverNames;
    }
    
    public void setReceiverNames(String receiverNames) {
        this.receiverNames = receiverNames;
    }
    
    public String getAttachments() {
        return attachments;
    }
    
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }
    
    public long getScheduledTime() {
        return scheduledTime;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    /**
     * 检查是否到达发送时间
     * 
     * @return 是否到达发送时间
     */
    public boolean isTimeToSend() {
        return System.currentTimeMillis() >= scheduledTime;
    }
    
    /**
     * 获取距离发送时间的剩余毫秒数
     * 
     * @return 剩余毫秒数，负数表示已过期
     */
    public long getRemainingTime() {
        return scheduledTime - System.currentTimeMillis();
    }
    
    /**
     * 检查邮件是否待发送
     * 
     * @return 是否待发送
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }
    
    @Override
    public String toString() {
        return "ScheduledMailData{" +
                "id=" + id +
                ", senderUuid=" + senderUuid +
                ", senderName='" + senderName + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", createTime=" + createTime +
                ", status=" + status +
                '}';
    }
}
