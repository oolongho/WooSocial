package com.oolonghoo.woosocial.sync.handler;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.util.concurrent.TimeUnit;

public class RedisSyncHandler implements SyncHandler {
    
    private static final String CHANNEL_NAME = "woosocial:sync";
    
    private final WooSocial plugin;
    private final SyncManager syncManager;
    private final String redisUri;
    private final String redisPassword;
    
    private RedisClient redisClient;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private io.lettuce.core.api.StatefulRedisConnection<String, String> publishConnection;
    private boolean available = false;
    
    public RedisSyncHandler(WooSocial plugin, SyncManager syncManager) {
        this.plugin = plugin;
        this.syncManager = syncManager;
        this.redisUri = syncManager.getConfig().getRedisUri();
        this.redisPassword = syncManager.getConfig().getRedisPassword();
    }
    
    @Override
    public void initialize() {
        try {
            String connectionUri = buildConnectionUri();
            redisClient = RedisClient.create(connectionUri);
            
            publishConnection = redisClient.connect();
            
            pubSubConnection = redisClient.connectPubSub();
            pubSubConnection.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    if (channel.equals(CHANNEL_NAME)) {
                        handleIncomingMessage(message);
                    }
                }
                
                @Override
                public void subscribed(String channel, long count) {
                    plugin.getLogger().info("[Sync] 已订阅 Redis Channel: " + channel);
                }
                
                @Override
                public void unsubscribed(String channel, long count) {
                    plugin.getLogger().info("[Sync] 已取消订阅 Redis Channel: " + channel);
                }
            });
            
            RedisPubSubAsyncCommands<String, String> async = pubSubConnection.async();
            async.subscribe(CHANNEL_NAME).get(5, TimeUnit.SECONDS);
            
            available = true;
            plugin.getLogger().info("[Sync] Redis 同步处理器已初始化");
        } catch (RedisConnectionException e) {
            plugin.getLogger().severe("[Sync] Redis 连接失败: " + e.getMessage());
            shutdown();
        } catch (Exception e) {
            plugin.getLogger().severe("[Sync] Redis 同步处理器初始化失败: " + e.getMessage());
            shutdown();
        }
    }
    
    private String buildConnectionUri() {
        if (redisPassword != null && !redisPassword.isEmpty()) {
            if (redisUri.startsWith("redis://")) {
                return redisUri.replace("redis://", "redis://:" + redisPassword + "@");
            } else if (redisUri.startsWith("rediss://")) {
                return redisUri.replace("rediss://", "rediss://:" + redisPassword + "@");
            }
        }
        return redisUri;
    }
    
    @Override
    public void shutdown() {
        try {
            if (pubSubConnection != null) {
                pubSubConnection.close();
            }
            if (publishConnection != null) {
                publishConnection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
        } catch (Exception ignored) {
        }
        available = false;
        plugin.getLogger().info("[Sync] Redis 同步处理器已关闭");
    }
    
    @Override
    public void sendMessage(SyncMessage message) {
        broadcast(message);
    }
    
    @Override
    public void broadcast(SyncMessage message) {
        if (!available || publishConnection == null) return;
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                publishConnection.async().publish(CHANNEL_NAME, message.toJson()).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("[Sync] Redis 发布消息失败: " + e.getMessage());
                if (e instanceof RedisConnectionException) {
                    attemptReconnect();
                }
            }
        });
    }
    
    private void handleIncomingMessage(String messageJson) {
        try {
            SyncMessage message = SyncMessage.fromJson(messageJson);
            if (message != null && !message.getSourceServer().equals(syncManager.getServerName())) {
                syncManager.handleIncomingMessage(message);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Sync] 处理 Redis 消息失败: " + e.getMessage());
        }
    }
    
    private void attemptReconnect() {
        plugin.getLogger().info("[Sync] 尝试重新连接 Redis...");
        shutdown();
        
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            initialize();
        }, 100L);
    }
    
    @Override
    public boolean isAvailable() {
        return available && publishConnection != null && publishConnection.isOpen();
    }
    
    @Override
    public String getName() {
        return "Redis";
    }
}
