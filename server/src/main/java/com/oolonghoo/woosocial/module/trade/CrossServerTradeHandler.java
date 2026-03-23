package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.gui.TradeGUI;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import com.oolonghoo.woosocial.sync.SyncMessageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrossServerTradeHandler {
    
    private final WooSocial plugin;
    private final TradeManager tradeManager;
    private final TradeConfig config;
    private final MessageManager messageManager;
    private final SyncManager syncManager;
    
    private final Map<UUID, PendingCrossServerRequest> pendingRequests = new ConcurrentHashMap<>();
    
    public CrossServerTradeHandler(WooSocial plugin, TradeManager tradeManager, TradeConfig config) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.config = config;
        this.messageManager = plugin.getMessageManager();
        this.syncManager = plugin.getSyncManager();
    }
    
    public void sendTradeRequest(Player sender, UUID receiverUuid, String receiverName, String receiverServer) {
        UUID senderUuid = sender.getUniqueId();
        
        if (tradeManager.isInTrade(senderUuid)) {
            messageManager.send(sender, "trade.already-in-trade");
            return;
        }
        
        PendingCrossServerRequest request = new PendingCrossServerRequest(
                senderUuid, sender.getName(),
                receiverUuid, receiverName,
                receiverServer,
                System.currentTimeMillis()
        );
        
        pendingRequests.put(senderUuid, request);
        
        if (syncManager != null && syncManager.isInitialized()) {
            syncManager.broadcastTradeRequest(senderUuid, sender.getName(), receiverUuid);
        }
        
        messageManager.send(sender, "trade.request-sent", "player", receiverName);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingCrossServerRequest pending = pendingRequests.remove(senderUuid);
            if (pending != null && !pending.isAccepted()) {
                messageManager.send(sender, "trade.request-expired");
            }
        }, config.getRequestExpireTime() * 20L);
    }
    
    public void handleSyncMessage(SyncMessage message) {
        switch (message.getType()) {
            case TRADE_REQUEST -> handleTradeRequest(message);
            case TRADE_ACCEPT -> handleTradeAccept(message);
            case TRADE_DENY -> handleTradeDeny(message);
            case TRADE_CANCEL -> handleTradeCancel(message);
            default -> {}
        }
    }
    
    private void handleTradeRequest(SyncMessage message) {
        UUID senderUuid = message.getUUID("sender_uuid");
        String senderName = message.getString("sender_name");
        UUID receiverUuid = message.getUUID("receiver_uuid");
        
        if (receiverUuid == null) return;
        
        Player receiver = Bukkit.getPlayer(receiverUuid);
        if (receiver == null) return;
        
        if (tradeManager.isInTrade(receiverUuid)) {
            return;
        }
        
        PendingCrossServerRequest request = new PendingCrossServerRequest(
                senderUuid, senderName,
                receiverUuid, receiver.getName(),
                message.getSourceServer(),
                System.currentTimeMillis()
        );
        
        pendingRequests.put(receiverUuid, request);
        
        receiver.playSound(receiver.getLocation(), config.getSoundRequestReceive(), 1.0f, 1.0f);
        
        messageManager.sendWithClickableButtons(receiver,
                "trade.request-received",
                "/trade accept " + senderName,
                "/trade deny " + senderName,
                "player", senderName);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingCrossServerRequest pending = pendingRequests.remove(receiverUuid);
            if (pending != null && !pending.isAccepted()) {
                messageManager.send(receiver, "trade.request-expired");
            }
        }, config.getRequestExpireTime() * 20L);
    }
    
    private void handleTradeAccept(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID partnerUuid = message.getUUID("partner_uuid");
        
        if (playerUuid == null || partnerUuid == null) return;
        
        PendingCrossServerRequest request = pendingRequests.remove(playerUuid);
        if (request == null) return;
        
        request.setAccepted(true);
        
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            messageManager.send(player, "trade.remote-accepted", "player", request.getPartnerName());
            
            // 在本地创建交易会话并打开 GUI
            startCrossServerTrade(player, request);
        }
    }
    
    /**
     * 开始跨服交易
     */
    private void startCrossServerTrade(Player player, PendingCrossServerRequest request) {
        if (!config.isVaultEnabled()) {
            plugin.getLogger().warning("[Trade] 跨服交易需要启用 Vault 支持");
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID partnerUuid = request.getSenderUuid();
        
        // 创建跨服交易会话
        CrossServerTradeSession crossSession = new CrossServerTradeSession(
                partnerUuid, request.getSenderName(), request.getTargetServer(),
                playerUuid, player.getName(), getServerName()
        );
        
        // 创建本地交易会话
        TradeSession session = crossSession.toLocalSession(false);
        tradeManager.getActiveSessions().put(playerUuid, session);
        
        // 获取经济管理器
        var tradeModule = plugin.getModuleManager().getModule("trade", TradeModule.class);
        TradeEconomyManager economyManager = tradeModule != null ? 
                tradeModule.getEconomyManager() : new TradeEconomyManager(plugin);
        
        // 打开 GUI
        TradeGUI gui = TradeGUI.create(plugin, tradeManager, config, economyManager, player, session);
        player.openInventory(gui.getInventory());
        
        // 广播交易开始
        if (syncManager != null && syncManager.isInitialized()) {
            syncManager.broadcastTradeStart(crossSession);
        }
    }
    
    private void handleTradeDeny(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID partnerUuid = message.getUUID("partner_uuid");
        
        if (playerUuid == null || partnerUuid == null) return;
        
        PendingCrossServerRequest request = pendingRequests.remove(playerUuid);
        if (request == null) return;
        
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            messageManager.send(player, "trade.remote-denied", "player", request.getPartnerName());
        }
    }
    
    private void handleTradeCancel(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID partnerUuid = message.getUUID("partner_uuid");
        String reason = message.getString("reason");
        
        if (playerUuid == null || partnerUuid == null) return;
        
        PendingCrossServerRequest request = pendingRequests.remove(playerUuid);
        if (request != null) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                messageManager.send(player, "trade.cancelled", "reason", reason != null ? reason : "对方取消");
            }
        }
    }
    
    public boolean acceptRequest(Player receiver, String senderName) {
        UUID receiverUuid = receiver.getUniqueId();
        PendingCrossServerRequest request = pendingRequests.get(receiverUuid);
        
        if (request == null) {
            messageManager.send(receiver, "trade.no-pending-request");
            return false;
        }
        
        if (!request.getSenderName().equalsIgnoreCase(senderName)) {
            messageManager.send(receiver, "trade.no-pending-request");
            return false;
        }
        
        request.setAccepted(true);
        pendingRequests.remove(receiverUuid);
        
        if (syncManager != null && syncManager.isInitialized()) {
            syncManager.broadcastTradeAccept(receiverUuid, request.getSenderUuid());
        }
        
        return true;
    }
    
    public boolean denyRequest(Player receiver, String senderName) {
        UUID receiverUuid = receiver.getUniqueId();
        PendingCrossServerRequest request = pendingRequests.get(receiverUuid);
        
        if (request == null) {
            messageManager.send(receiver, "trade.no-pending-request");
            return false;
        }
        
        if (!request.getSenderName().equalsIgnoreCase(senderName)) {
            messageManager.send(receiver, "trade.no-pending-request");
            return false;
        }
        
        pendingRequests.remove(receiverUuid);
        
        if (syncManager != null && syncManager.isInitialized()) {
            syncManager.broadcastTradeDeny(receiverUuid, request.getSenderUuid());
        }
        
        messageManager.send(receiver, "trade.request-denied", "player", senderName);
        
        return true;
    }
    
    public boolean hasPendingRequest(UUID playerUuid) {
        PendingCrossServerRequest request = pendingRequests.get(playerUuid);
        if (request == null) return false;
        
        long elapsed = System.currentTimeMillis() - request.getTimestamp();
        if (elapsed > config.getRequestExpireTime() * 1000L) {
            pendingRequests.remove(playerUuid);
            return false;
        }
        
        return true;
    }
    
    public PendingCrossServerRequest getPendingRequest(UUID playerUuid) {
        return pendingRequests.get(playerUuid);
    }
    
    private String getServerName() {
        return plugin.getConfig().getString("server.name", "unknown");
    }
    
    public void cleanup() {
        pendingRequests.clear();
    }
    
    public static class PendingCrossServerRequest {
        private final UUID senderUuid;
        private final String senderName;
        private final UUID receiverUuid;
        private final String receiverName;
        private final String targetServer;
        private final long timestamp;
        private boolean accepted;
        
        public PendingCrossServerRequest(UUID senderUuid, String senderName,
                                         UUID receiverUuid, String receiverName,
                                         String targetServer, long timestamp) {
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.receiverUuid = receiverUuid;
            this.receiverName = receiverName;
            this.targetServer = targetServer;
            this.timestamp = timestamp;
            this.accepted = false;
        }
        
        public UUID getSenderUuid() { return senderUuid; }
        public String getSenderName() { return senderName; }
        public UUID getReceiverUuid() { return receiverUuid; }
        public String getReceiverName() { return receiverName; }
        public String getTargetServer() { return targetServer; }
        public long getTimestamp() { return timestamp; }
        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }
        
        public String getPartnerName() {
            return senderName;
        }
    }
}
