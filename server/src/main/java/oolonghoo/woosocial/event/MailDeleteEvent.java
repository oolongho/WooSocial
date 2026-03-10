package com.oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.MailData;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 邮件删除事件
 * <p>
 * 当邮件被删除时触发此事件，此事件不可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class MailDeleteEvent extends WooSocialEvent {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 删除操作执行者 UUID
     */
    private final UUID playerUuid;
    
    /**
     * 删除操作执行者名称
     */
    private final String playerName;
    
    /**
     * 被删除的邮件数据
     */
    private final MailData mailData;
    
    /**
     * 删除原因
     */
    private final DeleteReason reason;
    
    /**
     * 删除原因枚举
     */
    public enum DeleteReason {
        /**
         * 玩家手动删除
         */
        PLAYER_DELETE,
        
        /**
         * 邮件过期自动删除
         */
        EXPIRED,
        
        /**
         * 系统清理
         */
        SYSTEM_CLEANUP,
        
        /**
         * 其他原因
         */
        OTHER
    }
    
    /**
     * 构造函数
     *
     * @param playerUuid 删除操作执行者 UUID
     * @param playerName 删除操作执行者名称
     * @param mailData   被删除的邮件数据
     * @param reason     删除原因
     */
    public MailDeleteEvent(UUID playerUuid, String playerName, 
                           MailData mailData, DeleteReason reason) {
        super(true); // 邮件删除涉及数据库操作，使用异步事件
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.mailData = mailData;
        this.reason = reason;
    }
    
    /**
     * 获取删除操作执行者 UUID
     *
     * @return 删除操作执行者 UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * 获取删除操作执行者名称
     *
     * @return 删除操作执行者名称
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * 获取被删除的邮件数据
     *
     * @return 邮件数据实例
     */
    public MailData getMailData() {
        return mailData;
    }
    
    /**
     * 获取邮件 ID
     * <p>
     * 便捷方法，从邮件数据中获取邮件 ID
     * </p>
     *
     * @return 邮件 ID
     */
    public int getMailId() {
        return mailData.getId();
    }
    
    /**
     * 获取删除原因
     *
     * @return 删除原因
     */
    public DeleteReason getReason() {
        return reason;
    }
    
    /**
     * 检查是否为玩家手动删除
     *
     * @return true 如果是玩家手动删除
     */
    public boolean isPlayerDelete() {
        return reason == DeleteReason.PLAYER_DELETE;
    }
    
    /**
     * 检查是否为过期删除
     *
     * @return true 如果是过期删除
     */
    public boolean isExpired() {
        return reason == DeleteReason.EXPIRED;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
