package com.oolonghoo.woosocial.model;

import com.oolonghoo.woosocial.attachment.IAttachment;
import com.oolonghoo.woosocial.attachment.ItemAttachment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailData {
    
    private int id;
    private final UUID senderUuid;
    private String senderName;
    private final UUID receiverUuid;
    private String receiverName;
    private ItemStack item;
    private String itemData; // 保留用于向后兼容
    private List<IAttachment> attachments; // 新的附件列表
    private final long sendTime;
    private long expireTime;
    private boolean read;
    private boolean claimed;
    private boolean bulk;
    private String bulkId;
    private boolean isSystem; // 是否为系统邮件
    private long scheduledTime; // 定时发送时间戳，0表示立即发送
    
    public MailData(UUID senderUuid, UUID receiverUuid) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.sendTime = System.currentTimeMillis();
        this.read = false;
        this.claimed = false;
        this.bulk = false;
        this.attachments = new ArrayList<>();
        this.isSystem = false;
        this.scheduledTime = 0;
    }
    
    public MailData(int id, UUID senderUuid, UUID receiverUuid, long sendTime) {
        this.id = id;
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.sendTime = sendTime;
        this.read = false;
        this.claimed = false;
        this.bulk = false;
        this.attachments = new ArrayList<>();
        this.isSystem = false;
        this.scheduledTime = 0;
    }
    
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
    
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public ItemStack getItem() {
        return item;
    }
    
    public void setItem(ItemStack item) {
        this.item = item;
    }
    
    public String getItemData() {
        return itemData;
    }
    
    public void setItemData(String itemData) {
        this.itemData = itemData;
        // 向后兼容：如果设置了itemData且attachments为空，自动转换为ItemAttachment
        if (itemData != null && !itemData.isEmpty() && (attachments == null || attachments.isEmpty())) {
            ItemAttachment attachment = new ItemAttachment();
            attachment.deserialize(itemData);
            if (attachment.isLegal()) {
                if (attachments == null) {
                    attachments = new ArrayList<>();
                }
                attachments.add(attachment);
            }
        }
    }
    
    /**
     * 获取附件列表
     * 
     * @return 附件列表
     */
    public List<IAttachment> getAttachments() {
        return attachments;
    }
    
    /**
     * 设置附件列表
     * 
     * @param attachments 附件列表
     */
    public void setAttachments(List<IAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }
    
    /**
     * 添加单个附件
     * 
     * @param attachment 附件
     */
    public void addAttachment(IAttachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
    }
    
    /**
     * 检查是否有附件
     * 
     * @return 是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    public long getSendTime() {
        return sendTime;
    }
    
    public long getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    public boolean isClaimed() {
        return claimed;
    }
    
    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
    
    public boolean isBulk() {
        return bulk;
    }
    
    public void setBulk(boolean bulk) {
        this.bulk = bulk;
    }
    
    public String getBulkId() {
        return bulkId;
    }
    
    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }
    
    /**
     * 检查是否为系统邮件
     * 
     * @return 是否为系统邮件
     */
    public boolean isSystem() {
        return isSystem;
    }
    
    /**
     * 设置是否为系统邮件
     * 
     * @param system 是否为系统邮件
     */
    public void setSystem(boolean system) {
        isSystem = system;
    }
    
    /**
     * 获取定时发送时间戳
     * 
     * @return 定时发送时间戳，0表示立即发送
     */
    public long getScheduledTime() {
        return scheduledTime;
    }
    
    /**
     * 设置定时发送时间戳
     * 
     * @param scheduledTime 定时发送时间戳，0表示立即发送
     */
    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    /**
     * 检查是否为定时邮件
     * 
     * @return 是否为定时邮件
     */
    public boolean isScheduled() {
        return scheduledTime > 0;
    }
    
    /**
     * 检查定时邮件是否已到发送时间
     * 
     * @return 是否可以发送
     */
    public boolean canSend() {
        if (scheduledTime <= 0) {
            return true; // 非定时邮件，可以发送
        }
        return System.currentTimeMillis() >= scheduledTime;
    }
    
    public boolean isExpired() {
        return expireTime > 0 && System.currentTimeMillis() > expireTime;
    }
    
    @Override
    public String toString() {
        return "MailData{" +
                "id=" + id +
                ", senderUuid=" + senderUuid +
                ", receiverUuid=" + receiverUuid +
                ", sendTime=" + sendTime +
                ", read=" + read +
                ", claimed=" + claimed +
                ", isSystem=" + isSystem +
                ", scheduledTime=" + scheduledTime +
                ", attachmentsCount=" + (attachments != null ? attachments.size() : 0) +
                '}';
    }
}
