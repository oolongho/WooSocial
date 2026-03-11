package com.oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.MailData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 邮件发送事件
 * <p>
 * 当玩家尝试发送邮件时触发此事件，此事件可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class MailSendEvent extends WooSocialEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 发件人 UUID
     */
    private final UUID senderUuid;
    
    /**
     * 发件人名称
     */
    private final String senderName;
    
    /**
     * 收件人 UUID
     */
    private final UUID receiverUuid;
    
    /**
     * 收件人名称
     */
    private final String receiverName;
    
    /**
     * 邮件数据（包含附件物品信息）
     */
    private MailData mailData;
    
    /**
     * 是否取消发送
     */
    private boolean cancelled;
    
    /**
     * 取消原因（用于向玩家显示）
     */
    private String cancelReason;
    
    /**
     * 构造函数
     *
     * @param senderUuid   发件人 UUID
     * @param senderName   发件人名称
     * @param receiverUuid 收件人 UUID
     * @param receiverName 收件人名称
     * @param mailData     邮件数据
     */
    public MailSendEvent(UUID senderUuid, String senderName, 
                         UUID receiverUuid, String receiverName, 
                         MailData mailData) {
        super(true); // 邮件发送可能涉及数据库操作，使用异步事件
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.mailData = mailData;
        this.cancelled = false;
        this.cancelReason = null;
    }
    
    /**
     * 获取发件人 UUID
     *
     * @return 发件人 UUID
     */
    public UUID getSenderUuid() {
        return senderUuid;
    }
    
    /**
     * 获取发件人名称
     *
     * @return 发件人名称
     */
    public String getSenderName() {
        return senderName;
    }
    
    /**
     * 获取收件人 UUID
     *
     * @return 收件人 UUID
     */
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    /**
     * 获取收件人名称
     *
     * @return 收件人名称
     */
    public String getReceiverName() {
        return receiverName;
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
     * 设置邮件数据
     * <p>
     * 可用于修改邮件内容，如修改附件物品
     * </p>
     *
     * @param mailData 新的邮件数据
     */
    public void setMailData(MailData mailData) {
        this.mailData = mailData;
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
