package com.oolonghoo.woosocial.module.teleport;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送逻辑管理器
 * 负责处理传送倒计时、冷却检查、传送执行等核心逻辑
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportManager {
    
    private final WooSocial plugin;
    private final TeleportDataManager dataManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    
    // 进行中的传送任务（玩家UUID -> 传送任务）
    private final Map<UUID, TeleportTask> pendingTeleports = new ConcurrentHashMap<>();
    
    // 玩家初始位置记录（用于移动检测）
    private final Map<UUID, Location> initialLocations = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param dataManager 数据管理器
     */
    public TeleportManager(WooSocial plugin, TeleportDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * 初始化传送管理器
     */
    public void initialize() {
    }
    
    /**
     * 关闭传送管理器
     */
    public void shutdown() {
        cancelAllTeleports();
        pendingTeleports.clear();
        initialLocations.clear();
    }
    
    /**
     * 取消所有进行中的传送
     */
    public void cancelAllTeleports() {
        for (TeleportTask task : pendingTeleports.values()) {
            if (task != null && task.taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(task.taskId);
            }
        }
        pendingTeleports.clear();
        initialLocations.clear();
    }
    
    // ==================== 传送逻辑 ====================
    
    /**
     * 开始传送到好友
     * 
     * @param player 发起传送的玩家
     * @param target 目标玩家
     * @return 是否成功开始传送
     */
    public boolean startTeleport(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        
        // 检查是否已经在传送中
        if (isTeleporting(playerUuid)) {
            messageManager.send(player, "teleport.teleport-cancelled");
            return false;
        }
        
        // 检查冷却时间
        if (!player.hasPermission(Perms.TELEPORT_COOLDOWN_BYPASS)) {
            if (dataManager.isInCooldown(playerUuid)) {
                int remaining = dataManager.getRemainingCooldown(playerUuid);
                messageManager.send(player, "teleport.teleport-cooldown", "time", remaining);
                return false;
            }
        }
        
        // 检查目标是否允许传送（检查全局设置和针对发起者的单独设置）
        if (!dataManager.isAllowTeleport(targetUuid, playerUuid)) {
            messageManager.send(player, "teleport.teleport-denied", "player", target.getName());
            return false;
        }
        
        // 获取传送倒计时
        int countdown = configManager.getTeleportCountdown();
        if (player.hasPermission(Perms.TELEPORT_COUNTDOWN_BYPASS)) {
            countdown = 0;
        }
        
        // 记录初始位置
        initialLocations.put(playerUuid, player.getLocation().clone());
        
        // 如果倒计时为0，直接传送
        if (countdown <= 0) {
            executeTeleport(player, target);
            return true;
        }
        
        // 发送倒计时消息
        messageManager.send(player, "teleport.teleporting", 
                "player", target.getName(), "time", countdown);
        
        // 创建传送任务
        TeleportTask task = new TeleportTask(player, target, countdown);
        pendingTeleports.put(playerUuid, task);
        
        // 启动倒计时任务
        startCountdown(task);
        
        return true;
    }
    
    /**
     * 启动倒计时任务
     */
    private void startCountdown(TeleportTask task) {
        task.taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 检查任务是否仍然有效
            if (pendingTeleports.containsKey(task.player.getUniqueId())) {
                executeTeleport(task.player, task.target);
            }
        }, task.countdown * 20L).getTaskId();
    }
    
    /**
     * 执行传送
     */
    private void executeTeleport(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        
        // 清理任务记录
        pendingTeleports.remove(playerUuid);
        initialLocations.remove(playerUuid);
        
        // 检查玩家和目标是否在线
        if (!player.isOnline() || !target.isOnline()) {
            messageManager.send(player, "teleport.target-not-online");
            return;
        }
        
        // 执行传送命令（以OP身份）
        String tpCommand = configManager.getConfig().getString("teleport.tp-command", "tp %player% %target%");
        tpCommand = tpCommand.replace("%player%", player.getName()).replace("%target%", target.getName());
        
        // 保存需要的变量
        final String finalTpCommand = tpCommand;
        final UUID finalPlayerUuid = playerUuid;
        final Player finalPlayer = player;
        final Player finalTarget = target;
        final boolean wasOp = player.isOp();
        final int cooldown = configManager.getTeleportCooldown();
        final boolean bypassCooldown = player.hasPermission(Perms.TELEPORT_COOLDOWN_BYPASS);
        
        // 在主线程执行传送命令
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                // 临时给予OP权限执行传送
                if (!wasOp) {
                    finalPlayer.setOp(true);
                }
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalTpCommand);
                
                // 发送成功消息
                messageManager.send(finalPlayer, "teleport.teleport-success", "player", finalTarget.getName());
                
                // 设置冷却时间
                if (!bypassCooldown) {
                    dataManager.setCooldown(finalPlayerUuid, cooldown);
                }
            } finally {
                // 恢复OP状态
                if (!wasOp) {
                    finalPlayer.setOp(false);
                }
            }
        });
    }
    
    /**
     * 取消玩家的传送
     * 
     * @param playerUuid 玩家UUID
     * @param reason 取消原因
     */
    public void cancelTeleport(UUID playerUuid, CancelReason reason) {
        TeleportTask task = pendingTeleports.remove(playerUuid);
        initialLocations.remove(playerUuid);
        
        if (task != null && task.taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(task.taskId);
            
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                switch (reason) {
                    case MOVED:
                        messageManager.send(player, "teleport.teleport-cancelled-move");
                        break;
                    case DAMAGED:
                        messageManager.send(player, "teleport.teleport-cancelled-damage");
                        break;
                    case MANUAL:
                        messageManager.send(player, "teleport.teleport-cancelled");
                        break;
                    case DISCONNECT:
                        // 玩家断开连接，不需要发送消息
                        break;
                }
            }
        }
    }
    
    /**
     * 检查玩家是否正在传送中
     * 
     * @param playerUuid 玩家UUID
     * @return 是否正在传送
     */
    public boolean isTeleporting(UUID playerUuid) {
        return pendingTeleports.containsKey(playerUuid);
    }
    
    /**
     * 检查玩家是否移动了（超过阈值）
     * 
     * @param playerUuid 玩家UUID
     * @param currentLocation 当前位置
     * @return 是否移动超过阈值
     */
    public boolean hasPlayerMoved(UUID playerUuid, Location currentLocation) {
        Location initialLocation = initialLocations.get(playerUuid);
        if (initialLocation == null) {
            return false;
        }
        
        // 检查是否在同一个世界
        if (!initialLocation.getWorld().equals(currentLocation.getWorld())) {
            return true;
        }
        
        // 计算移动距离
        double distance = initialLocation.distanceSquared(currentLocation);
        double threshold = configManager.getMoveThreshold();
        
        return distance > threshold * threshold;
    }
    
    /**
     * 获取玩家的初始位置
     * 
     * @param playerUuid 玩家UUID
     * @return 初始位置
     */
    public Location getInitialLocation(UUID playerUuid) {
        return initialLocations.get(playerUuid);
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 传送任务类
     */
    private static class TeleportTask {
        final Player player;
        final Player target;
        final int countdown;
        int taskId = -1;
        
        TeleportTask(Player player, Player target, int countdown) {
            this.player = player;
            this.target = target;
            this.countdown = countdown;
        }
    }
    
    /**
     * 取消原因枚举
     */
    public enum CancelReason {
        MOVED,      // 玩家移动
        DAMAGED,    // 玩家受伤
        MANUAL,     // 手动取消
        DISCONNECT  // 玩家断开连接
    }
}
