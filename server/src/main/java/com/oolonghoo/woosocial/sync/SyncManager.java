package com.oolonghoo.woosocial.sync;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.sync.handler.BungeeSyncHandler;
import com.oolonghoo.woosocial.sync.handler.MySQLSyncHandler;
import com.oolonghoo.woosocial.sync.handler.RedisSyncHandler;
import com.oolonghoo.woosocial.sync.handler.SyncHandler;
import com.oolonghoo.woosocial.sync.handler.VelocitySyncHandler;

import java.util.UUID;
import java.util.function.Consumer;

public class SyncManager {
    
    private final WooSocial plugin;
    private final SyncConfig config;
    private final PlatformDetector platformDetector;
    private SyncHandler activeHandler;
    private boolean initialized = false;
    
    private Consumer<SyncMessage> messageHandler;
    
    public SyncManager(WooSocial plugin) {
        this.plugin = plugin;
        this.config = new SyncConfig(plugin);
        this.platformDetector = new PlatformDetector(plugin);
    }
    
    public void initialize() {
        if (!config.isEnabled()) {
            return;
        }
        
        PlatformType platformType = platformDetector.detect();
        SyncMode mode = determineSyncMode(platformType);
        
        activeHandler = createHandler(mode);
        if (activeHandler != null) {
            activeHandler.initialize();
            initialized = true;
        } else {
            plugin.getLogger().warning("[Sync] 无法初始化同步系统，将运行在单机模式");
        }
    }
    
    private SyncMode determineSyncMode(PlatformType platformType) {
        SyncMode configuredMode = config.getMode();
        
        if (configuredMode != SyncMode.AUTO) {
            return configuredMode;
        }
        
        return switch (platformType) {
            case BUNGEECORD -> SyncMode.BUNGEE;
            case VELOCITY -> SyncMode.VELOCITY;
            case BUKKIT -> {
                if (config.isRedisEnabled()) {
                    yield SyncMode.REDIS;
                } else {
                    yield SyncMode.MYSQL;
                }
            }
            default -> SyncMode.DISABLED;
        };
    }
    
    private SyncHandler createHandler(SyncMode mode) {
        return switch (mode) {
            case BUNGEE -> new BungeeSyncHandler(plugin, this);
            case VELOCITY -> new VelocitySyncHandler(plugin, this);
            case REDIS -> {
                if (config.isRedisEnabled()) {
                    yield new RedisSyncHandler(plugin, this);
                } else {
                    plugin.getLogger().warning("[Sync] Redis 未配置，将使用 MySQL 轮询");
                    yield new MySQLSyncHandler(plugin, this);
                }
            }
            case MYSQL -> new MySQLSyncHandler(plugin, this);
            default -> null;
        };
    }
    
    public void shutdown() {
        if (activeHandler != null) {
            activeHandler.shutdown();
            plugin.getLogger().info("[Sync] 同步系统已关闭");
        }
        initialized = false;
    }
    
    public void sendMessage(SyncMessage message) {
        if (!initialized || activeHandler == null) return;
        activeHandler.sendMessage(message);
    }
    
    public void broadcast(SyncMessage message) {
        if (!initialized || activeHandler == null) return;
        activeHandler.broadcast(message);
    }
    
    public void broadcastFriendRequest(UUID senderUuid, String senderName, UUID receiverUuid) {
        SyncMessage message = new SyncMessage(SyncMessageType.FRIEND_REQUEST, config.getServerName())
                .set("sender_uuid", senderUuid.toString())
                .set("sender_name", senderName)
                .set("receiver_uuid", receiverUuid.toString());
        broadcast(message);
    }
    
    public void broadcastFriendAccept(UUID playerUuid, String playerName, UUID friendUuid, String friendName) {
        SyncMessage message = new SyncMessage(SyncMessageType.FRIEND_ACCEPT, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("player_name", playerName)
                .set("friend_uuid", friendUuid.toString())
                .set("friend_name", friendName);
        broadcast(message);
    }
    
    public void broadcastFriendRemove(UUID playerUuid, UUID friendUuid) {
        SyncMessage message = new SyncMessage(SyncMessageType.FRIEND_REMOVE, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("friend_uuid", friendUuid.toString());
        broadcast(message);
    }
    
    public void broadcastPlayerOnline(UUID playerUuid, String playerName, String serverName) {
        SyncMessage message = new SyncMessage(SyncMessageType.PLAYER_ONLINE, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("player_name", playerName)
                .set("server_name", serverName);
        broadcast(message);
    }
    
    public void broadcastPlayerOffline(UUID playerUuid, String playerName) {
        SyncMessage message = new SyncMessage(SyncMessageType.PLAYER_OFFLINE, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("player_name", playerName);
        broadcast(message);
    }
    
    public void broadcastPlayerSwitchServer(UUID playerUuid, String playerName, String fromServer, String toServer) {
        SyncMessage message = new SyncMessage(SyncMessageType.PLAYER_SWITCH_SERVER, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("player_name", playerName)
                .set("from_server", fromServer)
                .set("to_server", toServer);
        broadcast(message);
    }
    
    public void broadcastBlockPlayer(UUID playerUuid, UUID blockedUuid) {
        SyncMessage message = new SyncMessage(SyncMessageType.BLOCK_PLAYER, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("blocked_uuid", blockedUuid.toString());
        broadcast(message);
    }
    
    public void broadcastUnblockPlayer(UUID playerUuid, UUID unblockedUuid) {
        SyncMessage message = new SyncMessage(SyncMessageType.UNBLOCK_PLAYER, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("unblocked_uuid", unblockedUuid.toString());
        broadcast(message);
    }
    
    public void broadcastRelationPropose(UUID proposerUuid, String proposerName, UUID targetUuid, String relationType) {
        SyncMessage message = new SyncMessage(SyncMessageType.RELATION_PROPOSE, config.getServerName())
                .set("proposer_uuid", proposerUuid.toString())
                .set("proposer_name", proposerName)
                .set("target_uuid", targetUuid.toString())
                .set("relation_type", relationType);
        broadcast(message);
    }
    
    public void broadcastRelationAccept(UUID playerUuid, UUID friendUuid, String relationType) {
        SyncMessage message = new SyncMessage(SyncMessageType.RELATION_ACCEPT, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("friend_uuid", friendUuid.toString())
                .set("relation_type", relationType);
        broadcast(message);
    }
    
    public void broadcastRelationRemove(UUID playerUuid, UUID friendUuid) {
        SyncMessage message = new SyncMessage(SyncMessageType.RELATION_REMOVE, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("friend_uuid", friendUuid.toString());
        broadcast(message);
    }
    
    public void broadcastIntimacyUpdate(UUID playerUuid, UUID friendUuid, int newIntimacy) {
        SyncMessage message = new SyncMessage(SyncMessageType.RELATION_INTIMACY_UPDATE, config.getServerName())
                .set("player_uuid", playerUuid.toString())
                .set("friend_uuid", friendUuid.toString())
                .set("intimacy", String.valueOf(newIntimacy));
        broadcast(message);
    }
    
    public void broadcastGiftSend(UUID senderUuid, String senderName, UUID receiverUuid, String giftId, int amount, int intimacyGained) {
        SyncMessage message = new SyncMessage(SyncMessageType.GIFT_SEND, config.getServerName())
                .set("sender_uuid", senderUuid.toString())
                .set("sender_name", senderName)
                .set("receiver_uuid", receiverUuid.toString())
                .set("gift_id", giftId)
                .set("amount", String.valueOf(amount))
                .set("intimacy_gained", String.valueOf(intimacyGained));
        broadcast(message);
    }
    
    public void handleIncomingMessage(SyncMessage message) {
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }
    
    public void setMessageHandler(Consumer<SyncMessage> handler) {
        this.messageHandler = handler;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public SyncConfig getConfig() {
        return config;
    }
    
    public PlatformDetector getPlatformDetector() {
        return platformDetector;
    }
    
    public String getServerName() {
        return config.getServerName();
    }
}
