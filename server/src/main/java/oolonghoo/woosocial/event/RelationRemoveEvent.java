package com.oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.type.RelationType;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 关系解除事件
 * <p>
 * 当玩家之间的关系被解除时触发此事件，此事件不可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class RelationRemoveEvent extends WooSocialEvent {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 发起解除的玩家 UUID
     */
    private final UUID initiatorUuid;
    
    /**
     * 发起解除的玩家名称
     */
    private final String initiatorName;
    
    /**
     * 被解除关系的玩家 UUID
     */
    private final UUID targetUuid;
    
    /**
     * 被解除关系的玩家名称
     */
    private final String targetName;
    
    /**
     * 关系数据（解除前的数据快照）
     */
    private final RelationData relationData;
    
    /**
     * 关系类型
     */
    private final RelationType relationType;
    
    /**
     * 解除原因
     */
    private final RemoveReason reason;
    
    /**
     * 解除原因枚举
     */
    public enum RemoveReason {
        /**
         * 玩家主动解除
         */
        PLAYER_REMOVE,
        
        /**
         * 双方同意解除
         */
        MUTUAL_AGREEMENT,
        
        /**
         * 玩家被拉黑
         */
        BLOCKED,
        
        /**
         * 系统操作
         */
        SYSTEM,
        
        /**
         * 其他原因
         */
        OTHER
    }
    
    /**
     * 构造函数
     *
     * @param initiatorUuid 发起解除的玩家 UUID
     * @param initiatorName 发起解除的玩家名称
     * @param targetUuid    被解除关系的玩家 UUID
     * @param targetName    被解除关系的玩家名称
     * @param relationData  关系数据
     * @param relationType  关系类型
     * @param reason        解除原因
     */
    public RelationRemoveEvent(UUID initiatorUuid, String initiatorName,
                               UUID targetUuid, String targetName,
                               RelationData relationData, RelationType relationType,
                               RemoveReason reason) {
        super(true); // 关系解除涉及数据库操作，使用异步事件
        this.initiatorUuid = initiatorUuid;
        this.initiatorName = initiatorName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.relationData = relationData;
        this.relationType = relationType;
        this.reason = reason;
    }
    
    /**
     * 获取发起解除的玩家 UUID
     *
     * @return 发起解除的玩家 UUID
     */
    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }
    
    /**
     * 获取发起解除的玩家名称
     *
     * @return 发起解除的玩家名称
     */
    public String getInitiatorName() {
        return initiatorName;
    }
    
    /**
     * 获取被解除关系的玩家 UUID
     *
     * @return 被解除关系的玩家 UUID
     */
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    /**
     * 获取被解除关系的玩家名称
     *
     * @return 被解除关系的玩家名称
     */
    public String getTargetName() {
        return targetName;
    }
    
    /**
     * 获取关系数据
     *
     * @return 关系数据实例
     */
    public RelationData getRelationData() {
        return relationData;
    }
    
    /**
     * 获取关系类型
     *
     * @return 关系类型实例
     */
    public RelationType getRelationType() {
        return relationType;
    }
    
    /**
     * 获取关系类型 ID
     * <p>
     * 便捷方法，获取关系类型的 ID
     * </p>
     *
     * @return 关系类型 ID
     */
    public String getRelationTypeId() {
        return relationType != null ? relationType.getId() : null;
    }
    
    /**
     * 获取解除原因
     *
     * @return 解除原因
     */
    public RemoveReason getReason() {
        return reason;
    }
    
    /**
     * 检查是否为玩家主动解除
     *
     * @return true 如果是玩家主动解除
     */
    public boolean isPlayerRemove() {
        return reason == RemoveReason.PLAYER_REMOVE;
    }
    
    /**
     * 检查是否为拉黑导致的解除
     *
     * @return true 如果是拉黑导致的解除
     */
    public boolean isBlocked() {
        return reason == RemoveReason.BLOCKED;
    }
    
    /**
     * 获取解除前的亲密度
     * <p>
     * 便捷方法，从关系数据中获取亲密度
     * </p>
     *
     * @return 亲密度值
     */
    public int getIntimacy() {
        return relationData != null ? relationData.getIntimacy() : 0;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
