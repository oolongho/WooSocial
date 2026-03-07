package com.oolonghoo.woosocial.sync.handler;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class VelocitySyncHandler implements SyncHandler, PluginMessageListener {
    
    private static final String CHANNEL_NAME = "woosocial:sync";
    private static final String VELOCITY_CHANNEL = "velocity:main";
    
    private final WooSocial plugin;
    private final SyncManager syncManager;
    private boolean available = false;
    
    public VelocitySyncHandler(WooSocial plugin, SyncManager syncManager) {
        this.plugin = plugin;
        this.syncManager = syncManager;
    }
    
    @Override
    public void initialize() {
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NAME);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_NAME, this);
            
            available = true;
            plugin.getLogger().info("[Sync] Velocity 同步处理器已初始化");
        } catch (Exception e) {
            plugin.getLogger().severe("[Sync] Velocity 同步处理器初始化失败: " + e.getMessage());
            available = false;
        }
    }
    
    @Override
    public void shutdown() {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_NAME, this);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_NAME);
        } catch (Exception ignored) {
        }
        available = false;
    }
    
    @Override
    public void sendMessage(SyncMessage message) {
        broadcast(message);
    }
    
    @Override
    public void broadcast(SyncMessage message) {
        if (!available) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                if (player == null) {
                    plugin.getLogger().warning("[Sync] 没有在线玩家，无法发送消息");
                    return;
                }
                
                byte[] messageData = message.toBytes();
                
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream);
                
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF(CHANNEL_NAME);
                out.writeShort(messageData.length);
                out.write(messageData);
                
                player.sendPluginMessage(plugin, VELOCITY_CHANNEL, stream.toByteArray());
            } catch (Exception e) {
                plugin.getLogger().warning("[Sync] 发送消息失败: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL_NAME)) {
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
                
                String subChannel = in.readUTF();
                if (!subChannel.equals(CHANNEL_NAME)) {
                    return;
                }
                
                short dataLength = in.readShort();
                byte[] data = new byte[dataLength];
                in.readFully(data);
                
                SyncMessage syncMessage = SyncMessage.fromBytes(data);
                if (syncMessage != null && !syncMessage.getSourceServer().equals(syncManager.getServerName())) {
                    syncManager.handleIncomingMessage(syncMessage);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Sync] 处理接收消息失败: " + e.getMessage());
            }
        });
    }
    
    @Override
    public boolean isAvailable() {
        return available && !Bukkit.getOnlinePlayers().isEmpty();
    }
    
    @Override
    public String getName() {
        return "Velocity";
    }
}
