package com.oolonghoo.woosocial.model;

import java.util.UUID;

public class GiftData {
    
    private int id;
    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final String giftId;
    private int giftAmount;
    private int intimacyGained;
    private long sendTime;
    private String senderName;
    private String receiverName;
    
    public GiftData(UUID senderUuid, UUID receiverUuid, String giftId) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.giftId = giftId;
        this.giftAmount = 1;
        this.intimacyGained = 0;
        this.sendTime = System.currentTimeMillis();
    }
    
    public GiftData(UUID senderUuid, UUID receiverUuid, String giftId, int giftAmount) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.giftId = giftId;
        this.giftAmount = giftAmount;
        this.intimacyGained = 0;
        this.sendTime = System.currentTimeMillis();
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
    
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    public String getGiftId() {
        return giftId;
    }
    
    public int getGiftAmount() {
        return giftAmount;
    }
    
    public void setGiftAmount(int giftAmount) {
        this.giftAmount = giftAmount;
    }
    
    public int getIntimacyGained() {
        return intimacyGained;
    }
    
    public void setIntimacyGained(int intimacyGained) {
        this.intimacyGained = intimacyGained;
    }
    
    public long getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    @Override
    public String toString() {
        return "GiftData{" +
                "id=" + id +
                ", senderUuid=" + senderUuid +
                ", receiverUuid=" + receiverUuid +
                ", giftId='" + giftId + '\'' +
                ", giftAmount=" + giftAmount +
                ", intimacyGained=" + intimacyGained +
                '}';
    }
}
