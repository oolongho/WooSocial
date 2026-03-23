package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.trade.model.TradeRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 交易请求管理器
 * 管理交易请求的发送、接受、拒绝和过期
 */
public class TradeRequestManager {
    
    private final WooSocial plugin;
    private final TradeConfig config;
    
    private final Map<UUID, TradeRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    
    public TradeRequestManager(WooSocial plugin, TradeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /**
     * 发送交易请求
     */
    public boolean sendRequest(Player sender, Player receiver, boolean isRemote) {
        UUID senderUuid = sender.getUniqueId();
        UUID receiverUuid = receiver.getUniqueId();
        
        if (hasPendingRequest(senderUuid, receiverUuid)) {
            return false;
        }
        
        if (isOnCooldown(senderUuid)) {
            return false;
        }
        
        TradeRequest request = new TradeRequest(
            senderUuid, 
            sender.getName(), 
            receiverUuid, 
            isRemote
        );
        
        pendingRequests.put(receiverUuid, request);
        requestCooldowns.put(senderUuid, System.currentTimeMillis());
        
        scheduleExpiration(receiverUuid, request);
        
        return true;
    }
    
    /**
     * 发送跨服交易请求
     */
    public void sendCrossServerRequest(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String server) {
        TradeRequest request = new TradeRequest(senderUuid, senderName, receiverUuid, true);
        pendingRequests.put(receiverUuid, request);
        
        scheduleExpiration(receiverUuid, request);
    }
    
    /**
     * 接受交易请求
     */
    public TradeRequest acceptRequest(UUID receiverUuid) {
        TradeRequest request = pendingRequests.remove(receiverUuid);
        return request;
    }
    
    /**
     * 拒绝交易请求
     */
    public TradeRequest denyRequest(UUID receiverUuid) {
        TradeRequest request = pendingRequests.remove(receiverUuid);
        return request;
    }
    
    /**
     * 取消交易请求
     */
    public void cancelRequest(UUID receiverUuid) {
        pendingRequests.remove(receiverUuid);
    }
    
    /**
     * 获取待处理的请求
     */
    public TradeRequest getPendingRequest(UUID receiverUuid) {
        return pendingRequests.get(receiverUuid);
    }
    
    /**
     * 检查是否有待处理的请求
     */
    public boolean hasPendingRequest(UUID receiverUuid) {
        return pendingRequests.containsKey(receiverUuid);
    }
    
    /**
     * 检查是否有来自特定玩家的请求
     */
    public boolean hasPendingRequest(UUID senderUuid, UUID receiverUuid) {
        TradeRequest request = pendingRequests.get(receiverUuid);
        return request != null && request.getSenderUuid().equals(senderUuid);
    }
    
    /**
     * 检查是否在冷却中
     */
    public boolean isOnCooldown(UUID playerUuid) {
        Long cooldownTime = requestCooldowns.get(playerUuid);
        if (cooldownTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() - cooldownTime > 5000) {
            requestCooldowns.remove(playerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理玩家相关的请求
     */
    public void clearPlayerRequests(UUID playerUuid) {
        pendingRequests.entrySet().removeIf(entry -> 
            entry.getValue().getSenderUuid().equals(playerUuid) || 
            entry.getKey().equals(playerUuid)
        );
        requestCooldowns.remove(playerUuid);
    }
    
    /**
     * 清理所有请求
     */
    public void clearAllRequests() {
        pendingRequests.clear();
        requestCooldowns.clear();
    }
    
    /**
     * 安排请求过期任务
     */
    private void scheduleExpiration(UUID receiverUuid, TradeRequest request) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                TradeRequest existing = pendingRequests.get(receiverUuid);
                if (existing != null && existing.equals(request)) {
                    pendingRequests.remove(receiverUuid);
                }
            }, config.getRequestExpireTime() * 20L);
    }
    
    /**
     * 获取所有待处理请求
     */
    public Map<UUID, TradeRequest> getAllPendingRequests() {
        return new HashMap<>(pendingRequests);
    }
}
