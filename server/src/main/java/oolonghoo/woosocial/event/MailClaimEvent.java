package oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.MailData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 邮件领取事件
 * <p>
 * 当玩家尝试领取邮件附件时触发此事件，此事件可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class MailClaimEvent extends WooSocialEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 领取者 UUID
     */
    private final UUID playerUuid;
    
    /**
     * 领取者名称
     */
    private final String playerName;
    
    /**
     * 邮件数据
     */
    private final MailData mailData;
    
    /**
     * 是否取消领取
     */
    private boolean cancelled;
    
    /**
     * 取消原因（用于向玩家显示）
     */
    private String cancelReason;
    
    /**
     * 构造函数
     *
     * @param playerUuid 领取者 UUID
     * @param playerName 领取者名称
     * @param mailData   邮件数据
     */
    public MailClaimEvent(UUID playerUuid, String playerName, MailData mailData) {
        super(true); // 邮件领取涉及数据库操作，使用异步事件
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.mailData = mailData;
        this.cancelled = false;
        this.cancelReason = null;
    }
    
    /**
     * 获取领取者 UUID
     *
     * @return 领取者 UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * 获取领取者名称
     *
     * @return 领取者名称
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * 获取邮件数据
     *
     * @return 邮件数据实例
     */
    public MailData getMailData() {
        return mailData;
    }
    
    /**
     * 获取发件人 UUID
     * <p>
     * 便捷方法，从邮件数据中获取发件人 UUID
     * </p>
     *
     * @return 发件人 UUID
     */
    public UUID getSenderUuid() {
        return mailData.getSenderUuid();
    }
    
    /**
     * 获取发件人名称
     * <p>
     * 便捷方法，从邮件数据中获取发件人名称
     * </p>
     *
     * @return 发件人名称
     */
    public String getSenderName() {
        return mailData.getSenderName();
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
