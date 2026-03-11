package com.oolonghoo.woosocial.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "woosocial-proxy",
        name = "WooSocialProxy",
        version = "1.0.0",
        description = "WooSocial 跨服同步代理端插件",
        authors = {"oolonghoo"}
)
public class WooSocialVelocityPlugin {
    
    public static final ChannelIdentifier SYNC_CHANNEL = MinecraftChannelIdentifier.create("woosocial", "sync");
    
    private final ProxyServer server;
    private final Logger logger;
    @SuppressWarnings("unused")
    private final Path dataDirectory;
    
    private SyncMessageHandler messageHandler;
    
    @Inject
    public WooSocialVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(SYNC_CHANNEL);
        
        messageHandler = new SyncMessageHandler(server, logger);
        server.getEventManager().register(this, new PluginMessageListener(server, logger, messageHandler));
        
        logger.info("[WooSocialProxy] Velocity 代理端插件已初始化");
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(SYNC_CHANNEL);
        logger.info("[WooSocialProxy] Velocity 代理端插件已关闭");
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
}
