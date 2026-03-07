package com.oolonghoo.woosocial.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.oolonghoo.woosocial.module.teleport.TeleportManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 传送监听器
 * 监听玩家移动、受伤等事件，用于打断传送
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportListener implements Listener {
    
    private final TeleportManager teleportManager;
    private final ConfigManager configManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param teleportManager 传送管理器
     */
    public TeleportListener(WooSocial plugin, TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * 玩家移动事件
     * 检测玩家是否移动超过阈值，如果是则取消传送
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 检查是否启用移动取消
        if (!configManager.isCancelOnMove()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查玩家是否正在传送
        if (!teleportManager.isTeleporting(player.getUniqueId())) {
            return;
        }
        
        // 检查是否真的移动了（只检测坐标变化，不检测视角变化）
        if (event.getFrom().getX() == event.getTo().getX() &&
            event.getFrom().getY() == event.getTo().getY() &&
            event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        
        // 检查移动距离是否超过阈值
        if (teleportManager.hasPlayerMoved(player.getUniqueId(), event.getTo())) {
            // 取消传送
            teleportManager.cancelTeleport(player.getUniqueId(), TeleportManager.CancelReason.MOVED);
        }
    }
    
    /**
     * 玩家受伤事件
     * 受伤时取消传送
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        // 检查是否启用受伤取消
        if (!configManager.isCancelOnDamage()) {
            return;
        }
        
        // 检查是否为玩家
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // 检查玩家是否正在传送
        if (!teleportManager.isTeleporting(player.getUniqueId())) {
            return;
        }
        
        // 取消传送
        teleportManager.cancelTeleport(player.getUniqueId(), TeleportManager.CancelReason.DAMAGED);
    }
    
    /**
     * 玩家退出事件
     * 玩家退出时取消传送
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否正在传送
        if (teleportManager.isTeleporting(player.getUniqueId())) {
            // 取消传送
            teleportManager.cancelTeleport(player.getUniqueId(), TeleportManager.CancelReason.DISCONNECT);
        }
    }
    
    /**
     * 玩家传送事件
     * 防止在传送过程中再次传送
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否正在传送
        if (teleportManager.isTeleporting(player.getUniqueId())) {
            // 如果是插件自己发起的传送，允许通过
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN ||
                event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
                return;
            }
            
            // 其他传送原因，取消传送任务
            teleportManager.cancelTeleport(player.getUniqueId(), TeleportManager.CancelReason.MOVED);
        }
    }
}
