package com.oolonghoo.woosocial.proxy.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SyncMessageHandler {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private final ProxyServer server;
    private final Logger logger;
    
    public SyncMessageHandler(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }
    
    public void handleSyncMessage(String sourceServer, byte[] data) {
        try {
            String json = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            SyncMessage message = GSON.fromJson(json, SyncMessage.class);
            
            if (message == null) {
                logger.warn("[WooSocialProxy] 收到无效消息");
                return;
            }
            
            broadcastToAllServers(sourceServer, data);
        } catch (RuntimeException e) {
            logger.error("[WooSocialProxy] 处理消息失败: {}", e.getMessage());
        }
    }
    
    private void broadcastToAllServers(String sourceServer, byte[] data) {
        for (RegisteredServer targetServer : server.getAllServers()) {
            String serverName = targetServer.getServerInfo().getName();
            
            if (serverName.equals(sourceServer)) {
                continue;
            }
            
            targetServer.sendPluginMessage(WooSocialVelocityPlugin.SYNC_CHANNEL, data);
        }
    }
    
    public static class SyncMessage {
        private String type;
        private String sourceServer;
        private long timestamp;
        private final Map<String, Object> data = new HashMap<>();
        
        public String getType() {
            return type;
        }
        
        public String getSourceServer() {
            return sourceServer;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
    }
}
