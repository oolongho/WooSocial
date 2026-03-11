package com.oolonghoo.woosocial.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

/**
 * 玩家加入服务器事件监听器
 * 处理玩家上线时的数据加载和好友通知
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class PlayerJoinListener implements Listener {
    
    private final WooSocial plugin;
    private final FriendDataManager dataManager;
    private final MessageManager messageManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param dataManager 数据管理器
     */
    public PlayerJoinListener(WooSocial plugin, FriendDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 玩家加入服务器事件
     * 使用NORMAL优先级，确保在其他插件处理之后执行
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 异步加载玩家数据
        dataManager.onPlayerJoin(player);
        
        // 异步处理好友上线通知
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            handleFriendOnlineNotification(player);
        });
    }
    
    /**
     * 处理好友上线通知
     * 通知在线的好友该玩家已上线
     * 
     * @param player 上线的玩家
     */
    private void handleFriendOnlineNotification(Player player) {
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        
        // 获取好友列表
        List<FriendData> friends = dataManager.getFriendList(playerUuid);
        
        if (friends.isEmpty()) {
            return;
        }
        
        // 广播玩家上线消息到其他服务器
        plugin.getSyncManager().broadcastPlayerOnline(playerUuid, playerName, plugin.getConfigManager().getServerName());
        
        // 本地通知在线的好友
        for (FriendData friend : friends) {
            UUID friendUuid = friend.getFriendUuid();
            
            // 检查好友是否在线
            Player onlineFriend = Bukkit.getPlayer(friendUuid);
            if (onlineFriend == null || !onlineFriend.isOnline()) {
                continue;
            }
            
            // 检查好友是否开启了上线提醒
            if (!dataManager.isNotifyOnline(friendUuid)) {
                continue;
            }
            
            // 检查好友是否屏蔽了该玩家
            if (dataManager.isBlocked(friendUuid, playerUuid)) {
                continue;
            }
            
            // 在主线程发送消息
            Bukkit.getScheduler().runTask(plugin, () -> {
                messageManager.send(onlineFriend, "friend.friend-online-notification", 
                        "player", playerName);
            });
        }
    }
}
