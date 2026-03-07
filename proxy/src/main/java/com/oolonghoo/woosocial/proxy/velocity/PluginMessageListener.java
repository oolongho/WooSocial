package com.oolonghoo.woosocial.proxy.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

public class PluginMessageListener {
    
    private final ProxyServer server;
    private final Logger logger;
    private final SyncMessageHandler messageHandler;
    
    public PluginMessageListener(ProxyServer server, Logger logger, SyncMessageHandler messageHandler) {
        this.server = server;
        this.logger = logger;
        this.messageHandler = messageHandler;
    }
    
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(WooSocialVelocityPlugin.SYNC_CHANNEL)) {
            return;
        }
        
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        
        byte[] data = event.getData();
        
        if (!(event.getSource() instanceof ServerConnection sourceConnection)) {
            return;
        }
        
        String sourceServerName = sourceConnection.getServerInfo().getName();
        
        messageHandler.handleSyncMessage(sourceServerName, data);
    }
}
