package com.oolonghoo.woosocial.proxy.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.logging.Logger;

public class SyncMessageHandler {
    
    private final ProxyServer proxy;
    private final Logger logger;
    
    public SyncMessageHandler(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }
    
    public void handleSyncMessage(String sourceServer, byte[] data) {
        try {
            broadcastToAllServers(sourceServer, data);
        } catch (Exception e) {
            logger.warning("[WooSocialProxy] 处理消息失败: " + e.getMessage());
        }
    }
    
    private void broadcastToAllServers(String sourceServer, byte[] data) {
        for (ServerInfo serverInfo : proxy.getServers().values()) {
            String serverName = serverInfo.getName();
            
            if (serverName.equals(sourceServer)) {
                continue;
            }
            
            serverInfo.sendData(WooSocialBungeePlugin.SYNC_CHANNEL, data);
        }
    }
}
