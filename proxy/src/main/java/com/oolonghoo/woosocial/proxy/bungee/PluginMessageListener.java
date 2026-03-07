package com.oolonghoo.woosocial.proxy.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Logger;

public class PluginMessageListener implements Listener {
    
    private final ProxyServer proxy;
    private final Logger logger;
    private final SyncMessageHandler messageHandler;
    
    public PluginMessageListener(ProxyServer proxy, Logger logger, SyncMessageHandler messageHandler) {
        this.proxy = proxy;
        this.logger = logger;
        this.messageHandler = messageHandler;
    }
    
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(WooSocialBungeePlugin.SYNC_CHANNEL)) {
            return;
        }
        
        if (!(event.getSender() instanceof Server server)) {
            return;
        }
        
        String sourceServer = server.getInfo().getName();
        byte[] data = event.getData();
        
        messageHandler.handleSyncMessage(sourceServer, data);
    }
}
