package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.module.trade.model.TradeOffer;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 跨服交易会话管理器
 * 管理跨服交易的状态和同步
 */
public class CrossServerTradeSession {
    
    public enum CrossServerTradeState {
        PENDING,      // 等待对方接受
        ACCEPTED,     // 双方已接受，等待创建交易
        IN_PROGRESS,  // 交易进行中
        COMPLETED,    // 交易完成
        FAILED,       // 交易失败
        EXPIRED       // 交易超时
    }
    
    private final String sessionId;
    private final UUID player1Uuid;
    private final String player1Name;
    private final String player1Server;
    private final UUID player2Uuid;
    private final String player2Name;
    private final String player2Server;
    
    private volatile CrossServerTradeState state;
    private final long createTime;
    private final long expireTime;
    
    private TradeOffer player1Offer;
    private TradeOffer player2Offer;
    
    private volatile boolean player1Ready = false;
    private volatile boolean player2Ready = false;
    
    public CrossServerTradeSession(UUID player1Uuid, String player1Name, String player1Server,
                                   UUID player2Uuid, String player2Name, String player2Server) {
        this.sessionId = generateSessionId(player1Uuid, player2Uuid);
        this.player1Uuid = player1Uuid;
        this.player1Name = player1Name;
        this.player1Server = player1Server;
        this.player2Uuid = player2Uuid;
        this.player2Name = player2Name;
        this.player2Server = player2Server;
        
        this.state = CrossServerTradeState.PENDING;
        this.createTime = System.currentTimeMillis();
        this.expireTime = createTime + TimeUnit.MINUTES.toMillis(5); // 5 分钟超时
    }
    
    private String generateSessionId(UUID uuid1, UUID uuid2) {
        String s1 = uuid1.toString();
        String s2 = uuid2.toString();
        return s1.compareTo(s2) < 0 ? s1 + "_" + s2 : s2 + "_" + s1;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public UUID getPlayer1Uuid() {
        return player1Uuid;
    }
    
    public String getPlayer1Name() {
        return player1Name;
    }
    
    public String getPlayer1Server() {
        return player1Server;
    }
    
    public UUID getPlayer2Uuid() {
        return player2Uuid;
    }
    
    public String getPlayer2Name() {
        return player2Name;
    }
    
    public String getPlayer2Server() {
        return player2Server;
    }
    
    public CrossServerTradeState getState() {
        return state;
    }
    
    public void setState(CrossServerTradeState state) {
        this.state = state;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public long getExpireTime() {
        return expireTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
    
    public TradeOffer getPlayer1Offer() {
        return player1Offer;
    }
    
    public void setPlayer1Offer(TradeOffer player1Offer) {
        this.player1Offer = player1Offer;
    }
    
    public TradeOffer getPlayer2Offer() {
        return player2Offer;
    }
    
    public void setPlayer2Offer(TradeOffer player2Offer) {
        this.player2Offer = player2Offer;
    }
    
    public boolean isPlayer1Ready() {
        return player1Ready;
    }
    
    public void setPlayer1Ready(boolean player1Ready) {
        this.player1Ready = player1Ready;
    }
    
    public boolean isPlayer2Ready() {
        return player2Ready;
    }
    
    public void setPlayer2Ready(boolean player2Ready) {
        this.player2Ready = player2Ready;
    }
    
    public boolean isBothReady() {
        return player1Ready && player2Ready;
    }
    
    /**
     * 转换为本地交易会话
     */
    public TradeSession toLocalSession(boolean isPlayer1) {
        UUID localUuid = isPlayer1 ? player1Uuid : player2Uuid;
        String localName = isPlayer1 ? player1Name : player2Name;
        UUID partnerUuid = isPlayer1 ? player2Uuid : player1Uuid;
        String partnerName = isPlayer1 ? player2Name : player1Name;
        
        TradeSession session = new TradeSession(localUuid, localName, partnerUuid, partnerName);
        
        TradeOffer offer1 = player1Offer != null ? player1Offer : new TradeOffer(localUuid);
        TradeOffer offer2 = player2Offer != null ? player2Offer : new TradeOffer(partnerUuid);
        
        if (isPlayer1) {
            session.getOffer1().getItems().addAll(offer1.getItems());
            session.getOffer2().getItems().addAll(offer2.getItems());
        } else {
            session.getOffer1().getItems().addAll(offer2.getItems());
            session.getOffer2().getItems().addAll(offer1.getItems());
        }
        
        return session;
    }
}
