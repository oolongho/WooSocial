package com.oolonghoo.woosocial.module.trade.model;

import java.util.UUID;

/**
 * 交易请求模型
 * 表示一个交易请求的完整信息
 *
 * @author oolongho
 * @since 1.0.0
 */
public class TradeRequest {

    /**
     * 发送请求的玩家UUID
     */
    private final UUID senderUuid;

    /**
     * 发送请求的玩家名称
     */
    private final String senderName;

    /**
     * 接收请求的玩家UUID
     */
    private final UUID receiverUuid;

    /**
     * 请求发送时间戳
     */
    private final long timestamp;

    /**
     * 是否为跨服交易请求
     */
    private final boolean isRemote;

    /**
     * 创建交易请求
     *
     * @param senderUuid   发送者UUID
     * @param senderName   发送者名称
     * @param receiverUuid 接收者UUID
     * @param isRemote     是否跨服
     */
    public TradeRequest(UUID senderUuid, String senderName, UUID receiverUuid, boolean isRemote) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.timestamp = System.currentTimeMillis();
        this.isRemote = isRemote;
    }

    /**
     * 创建本地交易请求
     *
     * @param senderUuid   发送者UUID
     * @param senderName   发送者名称
     * @param receiverUuid 接收者UUID
     */
    public TradeRequest(UUID senderUuid, String senderName, UUID receiverUuid) {
        this(senderUuid, senderName, receiverUuid, false);
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getReceiverUuid() {
        return receiverUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isRemote() {
        return isRemote;
    }

    /**
     * 检查请求是否已过期
     *
     * @param expireTime 过期时间（毫秒）
     * @return 是否过期
     */
    public boolean isExpired(long expireTime) {
        return System.currentTimeMillis() - timestamp > expireTime;
    }

    /**
     * 获取请求已存在的时间（毫秒）
     *
     * @return 已存在时间
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - timestamp;
    }

    @Override
    public String toString() {
        return "TradeRequest{" +
                "senderName='" + senderName + '\'' +
                ", receiverUuid=" + receiverUuid +
                ", isRemote=" + isRemote +
                ", timestamp=" + timestamp +
                '}';
    }
}
