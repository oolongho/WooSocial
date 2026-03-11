package com.oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.GiftData;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 赠礼事件
 * <p>
 * 当玩家向好友赠送礼物时触发此事件，此事件不可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class GiftSendEvent extends WooSocialEvent {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 赠送者 UUID
     */
    private final UUID senderUuid;
    
    /**
     * 赠送者名称
     */
    private final String senderName;
    
    /**
     * 接收者 UUID
     */
    private final UUID receiverUuid;
    
    /**
     * 接收者名称
     */
    private final String receiverName;
    
    /**
     * 礼物数据
     */
    private final GiftData giftData;
    
    /**
     * 礼物类型
     */
    private final GiftType giftType;
    
    /**
     * 获得的亲密度
     */
    private final int intimacyGained;
    
    /**
     * 构造函数
     *
     * @param senderUuid     赠送者 UUID
     * @param senderName     赠送者名称
     * @param receiverUuid   接收者 UUID
     * @param receiverName   接收者名称
     * @param giftData       礼物数据
     * @param giftType       礼物类型
     * @param intimacyGained 获得的亲密度
     */
    public GiftSendEvent(UUID senderUuid, String senderName,
                         UUID receiverUuid, String receiverName,
                         GiftData giftData, GiftType giftType,
                         int intimacyGained) {
        super(true); // 赠礼涉及数据库操作，使用异步事件
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.giftData = giftData;
        this.giftType = giftType;
        this.intimacyGained = intimacyGained;
    }
    
    /**
     * 获取赠送者 UUID
     *
     * @return 赠送者 UUID
     */
    public UUID getSenderUuid() {
        return senderUuid;
    }
    
    /**
     * 获取赠送者名称
     *
     * @return 赠送者名称
     */
    public String getSenderName() {
        return senderName;
    }
    
    /**
     * 获取接收者 UUID
     *
     * @return 接收者 UUID
     */
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    /**
     * 获取接收者名称
     *
     * @return 接收者名称
     */
    public String getReceiverName() {
        return receiverName;
    }
    
    /**
     * 获取礼物数据
     *
     * @return 礼物数据实例
     */
    public GiftData getGiftData() {
        return giftData;
    }
    
    /**
     * 获取礼物类型
     *
     * @return 礼物类型实例
     */
    public GiftType getGiftType() {
        return giftType;
    }
    
    /**
     * 获取礼物类型 ID
     * <p>
     * 便捷方法，获取礼物类型的 ID
     * </p>
     *
     * @return 礼物类型 ID
     */
    public String getGiftTypeId() {
        return giftType != null ? giftType.getId() : null;
    }
    
    /**
     * 获取礼物名称
     * <p>
     * 便捷方法，获取礼物类型的显示名称
     * </p>
     *
     * @return 礼物名称
     */
    public String getGiftName() {
        return giftType != null ? giftType.getName() : null;
    }
    
    /**
     * 获得的亲密度
     *
     * @return 亲密度增加值
     */
    public int getIntimacyGained() {
        return intimacyGained;
    }
    
    /**
     * 获取礼物数量
     * <p>
     * 便捷方法，从礼物数据中获取数量
     * </p>
     *
     * @return 礼物数量
     */
    public int getGiftAmount() {
        return giftData != null ? giftData.getGiftAmount() : 1;
    }
    
    /**
     * 获取礼物 ID
     * <p>
     * 便捷方法，从礼物数据中获取 ID
     * </p>
     *
     * @return 礼物 ID
     */
    public int getGiftId() {
        return giftData != null ? giftData.getId() : 0;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
