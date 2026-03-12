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
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

/**
 * 玩家退出服务器事件监听器
 * 处理玩家下线时的数据保存和好友通知
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class PlayerQuitListener implements Listener {
    
    private final WooSocial plugin;
    private final FriendDataManager dataManager;
    private final MessageManager messageManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param dataManager 数据管理器
     */
    public PlayerQuitListener(WooSocial plugin, FriendDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 玩家退出服务器事件
     * 使用LOWEST优先级，确保在其他插件之前执行
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 立即关闭 GUI
        player.closeInventory();
        
        // 清理 GUI 缓存
        plugin.getGuiManager().onPlayerQuit(player.getUniqueId());
        
        // 异步处理好友下线通知
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            handleFriendOfflineNotification(player);
        });
        
        // 保存玩家数据
        dataManager.onPlayerQuit(player);
    }
    
    /**
     * 处理好友下线通知
     * 通知在线的好友该玩家已下线
     * 
     * @param player 下线的玩家
     */
    private void handleFriendOfflineNotification(Player player) {
        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        
        // 获取好友列表
        List<FriendData> friends = dataManager.getFriendList(playerUuid);
        
        if (friends.isEmpty()) {
            return;
        }
        
        // 遍历好友，通知在线的好友
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
                messageManager.send(onlineFriend, "friend.friend-offline-notification", 
                        "player", playerName);
            });
        }
    }
}
