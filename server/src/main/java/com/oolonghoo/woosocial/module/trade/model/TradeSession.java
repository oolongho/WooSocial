package com.oolonghoo.woosocial.module.trade.model;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 交易会话
 * 表示一次正在进行的交易
 */
public class TradeSession {
    
    private final UUID sessionId;
    private final UUID player1Uuid;
    private final UUID player2Uuid;
    private final String player1Name;
    private final String player2Name;
    
    private final TradeOffer offer1;
    private final TradeOffer offer2;
    
    private boolean player1Ready = false;
    private boolean player2Ready = false;
    
    private TradeState state = TradeState.PENDING;
    private long startTime;
    private long countdownEndTime;
    
    private boolean isCrossServer = false;
    private String player1Server;
    private String player2Server;
    
    public TradeSession(UUID player1Uuid, String player1Name, UUID player2Uuid, String player2Name) {
        this.sessionId = UUID.randomUUID();
        this.player1Uuid = player1Uuid;
        this.player2Uuid = player2Uuid;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.offer1 = new TradeOffer(player1Uuid);
        this.offer2 = new TradeOffer(player2Uuid);
        this.startTime = System.currentTimeMillis();
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public UUID getPlayer1Uuid() {
        return player1Uuid;
    }
    
    public UUID getPlayer2Uuid() {
        return player2Uuid;
    }
    
    public String getPlayer1Name() {
        return player1Name;
    }
    
    public String getPlayer2Name() {
        return player2Name;
    }
    
    public TradeOffer getOffer1() {
        return offer1;
    }
    
    public TradeOffer getOffer2() {
        return offer2;
    }
    
    public TradeOffer getOffer(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return offer1;
        } else if (playerUuid.equals(player2Uuid)) {
            return offer2;
        }
        return null;
    }
    
    public TradeOffer getOtherOffer(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return offer2;
        } else if (playerUuid.equals(player2Uuid)) {
            return offer1;
        }
        return null;
    }
    
    public UUID getOtherPlayer(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return player2Uuid;
        } else if (playerUuid.equals(player2Uuid)) {
            return player1Uuid;
        }
        return null;
    }
    
    public String getOtherPlayerName(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return player2Name;
        } else if (playerUuid.equals(player2Uuid)) {
            return player1Name;
        }
        return null;
    }
    
    public boolean isPlayer1(UUID playerUuid) {
        return playerUuid.equals(player1Uuid);
    }
    
    public boolean isPlayer2(UUID playerUuid) {
        return playerUuid.equals(player2Uuid);
    }
    
    public boolean isPlayerInSession(UUID playerUuid) {
        return playerUuid.equals(player1Uuid) || playerUuid.equals(player2Uuid);
    }
    
    public boolean isPlayer1Ready() {
        return player1Ready;
    }
    
    public void setPlayer1Ready(boolean ready) {
        this.player1Ready = ready;
    }
    
    public boolean isPlayer2Ready() {
        return player2Ready;
    }
    
    public void setPlayer2Ready(boolean ready) {
        this.player2Ready = ready;
    }
    
    public void setReady(UUID playerUuid, boolean ready) {
        if (playerUuid.equals(player1Uuid)) {
            player1Ready = ready;
        } else if (playerUuid.equals(player2Uuid)) {
            player2Ready = ready;
        }
    }
    
    public boolean isReady(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return player1Ready;
        } else if (playerUuid.equals(player2Uuid)) {
            return player2Ready;
        }
        return false;
    }
    
    public boolean isBothReady() {
        return player1Ready && player2Ready;
    }
    
    public void resetReadyStates() {
        player1Ready = false;
        player2Ready = false;
    }
    
    public TradeState getState() {
        return state;
    }
    
    public void setState(TradeState state) {
        this.state = state;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getCountdownEndTime() {
        return countdownEndTime;
    }
    
    public void setCountdownEndTime(long countdownEndTime) {
        this.countdownEndTime = countdownEndTime;
    }
    
    public boolean isCrossServer() {
        return isCrossServer;
    }
    
    public void setCrossServer(boolean crossServer) {
        isCrossServer = crossServer;
    }
    
    public String getPlayer1Server() {
        return player1Server;
    }
    
    public void setPlayer1Server(String player1Server) {
        this.player1Server = player1Server;
    }
    
    public String getPlayer2Server() {
        return player2Server;
    }
    
    public void setPlayer2Server(String player2Server) {
        this.player2Server = player2Server;
    }
    
    public String getServer(UUID playerUuid) {
        if (playerUuid.equals(player1Uuid)) {
            return player1Server;
        } else if (playerUuid.equals(player2Uuid)) {
            return player2Server;
        }
        return null;
    }
    
    public void setServer(UUID playerUuid, String server) {
        if (playerUuid.equals(player1Uuid)) {
            player1Server = server;
        } else if (playerUuid.equals(player2Uuid)) {
            player2Server = server;
        }
    }
    
    public List<ItemStack> getAllItems(UUID playerUuid) {
        TradeOffer offer = getOffer(playerUuid);
        if (offer != null) {
            return new ArrayList<>(offer.getItems());
        }
        return new ArrayList<>();
    }
}
