package com.oolonghoo.woosocial.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MailData {
    
    private int id;
    private final UUID senderUuid;
    private String senderName;
    private final UUID receiverUuid;
    private String receiverName;
    private ItemStack item;
    private String itemData;
    private final long sendTime;
    private long expireTime;
    private boolean read;
    private boolean claimed;
    private boolean bulk;
    private String bulkId;
    
    public MailData(UUID senderUuid, UUID receiverUuid) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.sendTime = System.currentTimeMillis();
        this.read = false;
        this.claimed = false;
        this.bulk = false;
    }
    
    public MailData(int id, UUID senderUuid, UUID receiverUuid, long sendTime) {
        this.id = id;
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.sendTime = sendTime;
        this.read = false;
        this.claimed = false;
        this.bulk = false;
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
                '}';
    }
}
