package com.oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.module.relation.type.RelationType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 关系申请事件
 * <p>
 * 当玩家向另一玩家申请建立关系时触发此事件，此事件可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class RelationProposeEvent extends WooSocialEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 申请人 UUID
     */
    private final UUID proposerUuid;
    
    /**
     * 申请人名称
     */
    private final String proposerName;
    
    /**
     * 目标玩家 UUID
     */
    private final UUID targetUuid;
    
    /**
     * 目标玩家名称
     */
    private final String targetName;
    
    /**
     * 关系类型
     */
    private RelationType relationType;
    
    /**
     * 是否取消申请
     */
    private boolean cancelled;
    
    /**
     * 取消原因（用于向玩家显示）
     */
    private String cancelReason;
    
    /**
     * 构造函数
     *
     * @param proposerUuid 申请人 UUID
     * @param proposerName 申请人名称
     * @param targetUuid   目标玩家 UUID
     * @param targetName   目标玩家名称
     * @param relationType 关系类型
     */
    public RelationProposeEvent(UUID proposerUuid, String proposerName,
                                UUID targetUuid, String targetName,
                                RelationType relationType) {
        super(true); // 关系申请涉及数据库操作，使用异步事件
        this.proposerUuid = proposerUuid;
        this.proposerName = proposerName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.relationType = relationType;
        this.cancelled = false;
        this.cancelReason = null;
    }
    
    /**
     * 获取申请人 UUID
     *
     * @return 申请人 UUID
     */
    public UUID getProposerUuid() {
        return proposerUuid;
    }
    
    /**
     * 获取申请人名称
     *
     * @return 申请人名称
     */
    public String getProposerName() {
        return proposerName;
    }
    
    /**
     * 获取目标玩家 UUID
     *
     * @return 目标玩家 UUID
     */
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    /**
     * 获取目标玩家名称
     *
     * @return 目标玩家名称
     */
    public String getTargetName() {
        return targetName;
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
     * 设置关系类型
     * <p>
     * 可用于修改申请的关系类型
     * </p>
     *
     * @param relationType 新的关系类型
     */
    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
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
     * 获取取消原因
     *
     * @return 取消原因，如果未设置则返回 null
     */
    public String getCancelReason() {
        return cancelReason;
    }
    
    /**
     * 设置取消原因
     *
     * @param cancelReason 取消原因
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
