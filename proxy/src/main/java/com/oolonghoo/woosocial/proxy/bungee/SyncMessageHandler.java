package com.oolonghoo.woosocial.proxy.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.logging.Logger;

public class SyncMessageHandler {
    
    private static final Logger LOGGER = Logger.getLogger("WooSocialProxy");
    
    private final ProxyServer proxy;
    
    public SyncMessageHandler(ProxyServer proxy) {
        this.proxy = proxy;
    }
    
    public void handleSyncMessage(String sourceServer, byte[] data) {
        try {
            broadcastToAllServers(sourceServer, data);
        } catch (Exception e) {
            LOGGER.warning(() -> "[WooSocialProxy] 处理消息失败: " + e.getMessage());
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
