package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 好友关系数据模型
 * 表示两个玩家之间的好友关系
 *
 * @author oolongho
 * @since 1.0.0
 */
public class FriendRelation {

    /**
     * 关系唯一标识符（数据库主键）
     */
    private long relationId;

    /**
     * 玩家1的UUID（较小的UUID，用于确保关系唯一性）
     */
    private UUID player1Id;

    /**
     * 玩家2的UUID（较大的UUID）
     */
    private UUID player2Id;

    /**
     * 建立好友关系的时间
     */
    private long establishedTime;

    /**
     * 玩家1对玩家2的备注名称
     */
    private String player1Nickname;

    /**
     * 玩家2对玩家1的备注名称
     */
    private String player2Nickname;

    /**
     * 关系状态（ACTIVE-活跃, BLOCKED-已拉黑, REMOVED-已删除）
     */
    private RelationStatus status;

    /**
     * 亲密度值（可用于特殊功能）
     */
    private int intimacy;

    /**
     * 关系状态枚举
     */
    public enum RelationStatus {
        /**
         * 活跃的好友关系
         */
        ACTIVE,
        /**
         * 已拉黑
         */
        BLOCKED,
        /**
         * 已删除
         */
        REMOVED
    }

    /**
     * 无参构造函数（用于序列化）
     */
    public FriendRelation() {
        this.status = RelationStatus.ACTIVE;
        this.intimacy = 0;
    }

    /**
     * 创建好友关系的构造函数
     *
     * @param player1Id 玩家1 UUID
     * @param player2Id 玩家2 UUID
     */
    public FriendRelation(UUID player1Id, UUID player2Id) {
        // 确保player1Id总是小于player2Id，保证关系唯一性
        if (player1Id.compareTo(player2Id) < 0) {
            this.player1Id = player1Id;
            this.player2Id = player2Id;
        } else {
            this.player1Id = player2Id;
            this.player2Id = player1Id;
        }
        this.establishedTime = System.currentTimeMillis();
        this.player1Nickname = "";
        this.player2Nickname = "";
        this.status = RelationStatus.ACTIVE;
        this.intimacy = 0;
    }

    // ==================== Getter 和 Setter 方法 ====================

    public long getRelationId() {
        return relationId;
    }

    public void setRelationId(long relationId) {
        this.relationId = relationId;
    }

    public UUID getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(UUID player1Id) {
        this.player1Id = player1Id;
    }

    public UUID getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(UUID player2Id) {
        this.player2Id = player2Id;
    }

    public long getEstablishedTime() {
        return establishedTime;
    }

    public void setEstablishedTime(long establishedTime) {
        this.establishedTime = establishedTime;
    }

    public String getPlayer1Nickname() {
        return player1Nickname;
    }

    public void setPlayer1Nickname(String player1Nickname) {
        this.player1Nickname = player1Nickname != null ? player1Nickname : "";
    }

    public String getPlayer2Nickname() {
        return player2Nickname;
    }

    public void setPlayer2Nickname(String player2Nickname) {
        this.player2Nickname = player2Nickname != null ? player2Nickname : "";
    }

    public RelationStatus getStatus() {
        return status;
    }

    public void setStatus(RelationStatus status) {
        this.status = status;
    }

    public int getIntimacy() {
        return intimacy;
    }

    public void setIntimacy(int intimacy) {
        this.intimacy = Math.max(0, intimacy);
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 检查指定玩家是否参与此好友关系
     *
     * @param playerId 玩家UUID
     * @return 是否参与
     */
    public boolean isParticipant(UUID playerId) {
        return player1Id.equals(playerId) || player2Id.equals(playerId);
    }

    /**
     * 获取好友关系中另一方的UUID
     *
     * @param playerId 当前玩家UUID
     * @return 另一方玩家UUID，如果当前玩家不在此关系中则返回null
     */
    public UUID getOtherPlayerId(UUID playerId) {
        if (player1Id.equals(playerId)) {
            return player2Id;
        } else if (player2Id.equals(playerId)) {
            return player1Id;
        }
        return null;
    }

    /**
     * 设置指定玩家对好友的备注名称
     *
     * @param playerId 玩家UUID
     * @param nickname 备注名称
     */
    public void setNickname(UUID playerId, String nickname) {
        if (player1Id.equals(playerId)) {
            setPlayer1Nickname(nickname);
        } else if (player2Id.equals(playerId)) {
            setPlayer2Nickname(nickname);
        }
    }

    /**
     * 获取指定玩家设置的好友备注名称
     *
     * @param playerId 玩家UUID
     * @return 备注名称
     */
    public String getNickname(UUID playerId) {
        if (player1Id.equals(playerId)) {
            return player2Nickname;
        } else if (player2Id.equals(playerId)) {
            return player1Nickname;
        }
        return "";
    }

    /**
     * 增加亲密度
     *
     * @param amount 增加的数量
     */
    public void increaseIntimacy(int amount) {
        this.intimacy += Math.max(0, amount);
    }

    /**
     * 减少亲密度
     *
     * @param amount 减少的数量
     */
    public void decreaseIntimacy(int amount) {
        this.intimacy = Math.max(0, this.intimacy - amount);
    }

    /**
     * 检查关系是否活跃
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return status == RelationStatus.ACTIVE;
    }

    /**
     * 检查关系是否被拉黑
     *
     * @return 是否被拉黑
     */
    public boolean isBlocked() {
        return status == RelationStatus.BLOCKED;
    }

    @Override
    public String toString() {
        return "FriendRelation{" +
                "relationId=" + relationId +
                ", player1Id=" + player1Id +
                ", player2Id=" + player2Id +
                ", establishedTime=" + establishedTime +
                ", status=" + status +
                ", intimacy=" + intimacy +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendRelation that = (FriendRelation) o;
        return player1Id.equals(that.player1Id) && player2Id.equals(that.player2Id);
    }

    @Override
    public int hashCode() {
        int result = player1Id.hashCode();
        result = 31 * result + player2Id.hashCode();
        return result;
    }
}
