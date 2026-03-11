package com.oolonghoo.woosocial.model;

import java.util.UUID;

/**
 * 好友请求数据模型
 * 表示一个好友请求的完整信息
 *
 * @author oolongho
 * @since 1.0.0
 */
public class FriendRequest {

    /**
     * 请求唯一标识符（数据库主键）
     */
    private long requestId;

    /**
     * 发送请求的玩家UUID
     */
    private UUID senderId;

    /**
     * 发送请求的玩家名称（冗余字段，便于查询）
     */
    private String senderName;

    /**
     * 接收请求的玩家UUID
     */
    private UUID receiverId;

    /**
     * 接收请求的玩家名称（冗余字段，便于查询）
     */
    private String receiverName;

    /**
     * 请求发送时间
     */
    private long sendTime;

    /**
     * 请求过期时间
     */
    private long expireTime;

    /**
     * 请求状态
     */
    private RequestStatus status;

    /**
     * 附加消息（可选）
     */
    private String message;

    /**
     * 请求状态枚举
     */
    public enum RequestStatus {
        /**
         * 待处理
         */
        PENDING,
        /**
         * 已接受
         */
        ACCEPTED,
        /**
         * 已拒绝
         */
        REJECTED,
        /**
         * 已过期
         */
        EXPIRED,
        /**
         * 已取消
         */
        CANCELLED
    }

    /**
     * 默认请求有效期（7天，单位：毫秒）
     */
    public static final long DEFAULT_EXPIRE_DURATION = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 无参构造函数（用于序列化）
     */
    public FriendRequest() {
        this.status = RequestStatus.PENDING;
        this.message = "";
    }

    /**
     * 创建好友请求的构造函数
     *
     * @param senderId 发送者UUID
     * @param senderName 发送者名称
     * @param receiverId 接收者UUID
     * @param receiverName 接收者名称
     */
    public FriendRequest(UUID senderId, String senderName, UUID receiverId, String receiverName) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.sendTime = System.currentTimeMillis();
        this.expireTime = this.sendTime + DEFAULT_EXPIRE_DURATION;
        this.status = RequestStatus.PENDING;
        this.message = "";
    }

    /**
     * 创建带消息的好友请求
     *
     * @param senderId 发送者UUID
     * @param senderName 发送者名称
     * @param receiverId 接收者UUID
     * @param receiverName 接收者名称
     * @param message 附加消息
     */
    public FriendRequest(UUID senderId, String senderName, UUID receiverId, String receiverName, String message) {
        this(senderId, senderName, receiverId, receiverName);
        this.message = message != null ? message : "";
    }

    // ==================== Getter 和 Setter 方法 ====================

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 检查请求是否已过期
     *
     * @return 是否过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    /**
     * 检查请求是否可以处理（待处理且未过期）
     *
     * @return 是否可以处理
     */
    public boolean canProcess() {
        return status == RequestStatus.PENDING && !isExpired();
    }

    /**
     * 接受请求
     *
     * @return 是否成功接受
     */
    public boolean accept() {
        if (!canProcess()) {
            return false;
        }
        status = RequestStatus.ACCEPTED;
        return true;
    }

    /**
     * 拒绝请求
     *
     * @return 是否成功拒绝
     */
    public boolean reject() {
        if (!canProcess()) {
            return false;
        }
        status = RequestStatus.REJECTED;
        return true;
    }

    /**
     * 取消请求
     *
     * @return 是否成功取消
     */
    public boolean cancel() {
        if (status != RequestStatus.PENDING) {
            return false;
        }
        status = RequestStatus.CANCELLED;
        return true;
    }

    /**
     * 标记为已过期
     */
    public void markAsExpired() {
        if (status == RequestStatus.PENDING) {
            status = RequestStatus.EXPIRED;
        }
    }

    /**
     * 获取剩余有效时间（毫秒）
     *
     * @return 剩余时间，如果已过期返回0
     */
    public long getRemainingTime() {
        if (isExpired()) {
            return 0;
        }
        return expireTime - System.currentTimeMillis();
    }

    /**
     * 检查指定玩家是否是请求的发送者
     *
     * @param playerId 玩家UUID
     * @return 是否是发送者
     */
    public boolean isSender(UUID playerId) {
        return senderId.equals(playerId);
    }

    /**
     * 检查指定玩家是否是请求的接收者
     *
     * @param playerId 玩家UUID
     * @return 是否是接收者
     */
    public boolean isReceiver(UUID playerId) {
        return receiverId.equals(playerId);
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "requestId=" + requestId +
                ", senderName='" + senderName + '\'' +
                ", receiverName='" + receiverName + '\'' +
                ", status=" + status +
                ", sendTime=" + sendTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendRequest that = (FriendRequest) o;
        return senderId.equals(that.senderId) && receiverId.equals(that.receiverId) && status == RequestStatus.PENDING;
    }

    @Override
    public int hashCode() {
        int result = senderId.hashCode();
        result = 31 * result + receiverId.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}
