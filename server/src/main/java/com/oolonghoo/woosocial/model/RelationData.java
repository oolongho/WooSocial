package com.oolonghoo.woosocial.model;

import java.util.UUID;

public class RelationData {
    
    private int id;
    private final UUID playerUuid;
    private final UUID friendUuid;
    private String relationType;
    private int intimacy;
    private long createTime;
    private long updateTime;
    private boolean isMutual;
    private long proposalTime;
    private String friendName;
    
    public RelationData(UUID playerUuid, UUID friendUuid) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.relationType = "friend";
        this.intimacy = 0;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.isMutual = false;
        this.proposalTime = 0;
    }
    
    public RelationData(UUID playerUuid, UUID friendUuid, String relationType) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.relationType = relationType;
        this.intimacy = 0;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.isMutual = false;
        this.proposalTime = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public UUID getFriendUuid() {
        return friendUuid;
    }
    
    public String getRelationType() {
        return relationType;
    }
    
    public void setRelationType(String relationType) {
        this.relationType = relationType;
        this.updateTime = System.currentTimeMillis();
    }
    
    public int getIntimacy() {
        return intimacy;
    }
    
    public void setIntimacy(int intimacy) {
        this.intimacy = intimacy;
        this.updateTime = System.currentTimeMillis();
    }
    
    public void addIntimacy(int amount) {
        this.intimacy += amount;
        this.updateTime = System.currentTimeMillis();
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    public boolean isMutual() {
        return isMutual;
    }
    
    public void setMutual(boolean mutual) {
        isMutual = mutual;
        this.updateTime = System.currentTimeMillis();
    }
    
    public long getProposalTime() {
        return proposalTime;
    }
    
    public void setProposalTime(long proposalTime) {
        this.proposalTime = proposalTime;
    }
    
    public String getFriendName() {
        return friendName;
    }
    
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }
    
    public boolean hasPendingProposal() {
        return proposalTime > 0 && !isMutual;
    }
    
    @Override
    public String toString() {
        return "RelationData{" +
                "id=" + id +
                ", playerUuid=" + playerUuid +
                ", friendUuid=" + friendUuid +
                ", relationType='" + relationType + '\'' +
                ", intimacy=" + intimacy +
                ", isMutual=" + isMutual +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationData that = (RelationData) obj;
        return playerUuid.equals(that.playerUuid) && friendUuid.equals(that.friendUuid);
    }
    
    @Override
    public int hashCode() {
        return 31 * playerUuid.hashCode() + friendUuid.hashCode();
    }
}
