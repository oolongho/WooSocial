package com.oolonghoo.woosocial.module.teleport;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.FriendDAO;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.oolonghoo.woosocial.model.TeleportCooldown;
import com.oolonghoo.woosocial.model.TeleportSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送数据管理器
 * 负责管理传送设置和冷却数据的缓存和异步操作
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportDataManager {
    
    private final WooSocial plugin;
    private final FriendDAO friendDAO;
    private final ConfigManager configManager;
    
    // 玩家传送设置缓存
    private final Map<UUID, TeleportSettings> settingsCache = new ConcurrentHashMap<>();
    
    // 玩家传送冷却缓存
    private final Map<UUID, TeleportCooldown> cooldownCache = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public TeleportDataManager(WooSocial plugin) {
        this.plugin = plugin;
        this.friendDAO = plugin.getFriendDAO();
        this.configManager = plugin.getConfigManager();
    }
    
    /**
     * 初始化数据管理器
     */
    public void initialize() {
    }
    
    /**
     * 关闭数据管理器
     */
    public void shutdown() {
        saveAllData();
        
        settingsCache.clear();
        cooldownCache.clear();
    }
    
    /**
     * 保存所有数据
     */
    public void saveAllData() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (TeleportSettings settings : settingsCache.values()) {
                if (settings != null) {
                    friendDAO.saveTeleportSettings(settings);
                }
            }
        });
    }
    
    // ==================== 传送设置管理 ====================
    
    /**
     * 加载玩家传送设置到缓存
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadTeleportSettings(UUID playerUuid) {
        return friendDAO.loadTeleportSettings(playerUuid).thenAccept(settingsOpt -> {
            if (settingsOpt.isPresent()) {
                settingsCache.put(playerUuid, settingsOpt.get());
            } else {
                // 创建默认设置
                TeleportSettings settings = new TeleportSettings(playerUuid);
                settings.setTeleportCooldown(configManager.getTeleportCooldown());
                settings.setTeleportCountdown(configManager.getTeleportCountdown());
                settings.setAllowFriendTeleport(configManager.isDefaultAllowTeleport());
                settingsCache.put(playerUuid, settings);
            }
        });
    }
    
    /**
     * 获取玩家传送设置（从缓存）
     * 
     * @param playerUuid 玩家UUID
     * @return 传送设置
     */
    public TeleportSettings getTeleportSettings(UUID playerUuid) {
        return settingsCache.computeIfAbsent(playerUuid, uuid -> {
            TeleportSettings settings = new TeleportSettings(uuid);
            settings.setTeleportCooldown(configManager.getTeleportCooldown());
            settings.setTeleportCountdown(configManager.getTeleportCountdown());
            settings.setAllowFriendTeleport(configManager.isDefaultAllowTeleport());
            return settings;
        });
    }
    
    /**
     * 更新玩家传送设置
     * 
     * @param settings 传送设置
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateTeleportSettings(TeleportSettings settings) {
        settingsCache.put(settings.getPlayerId(), settings);
        return friendDAO.saveTeleportSettings(settings);
    }
    
    /**
     * 检查是否允许好友传送
     * 
     * @param playerUuid 玩家UUID
     * @return 是否允许
     */
    public boolean isAllowFriendTeleport(UUID playerUuid) {
        TeleportSettings settings = getTeleportSettings(playerUuid);
        return settings != null && settings.isAllowFriendTeleport();
    }
    
    /**
     * 设置是否允许好友传送
     * 
     * @param playerUuid 玩家UUID
     * @param allow 是否允许
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setAllowFriendTeleport(UUID playerUuid, boolean allow) {
        TeleportSettings settings = getTeleportSettings(playerUuid);
        if (settings != null) {
            settings.setAllowFriendTeleport(allow);
            return updateTeleportSettings(settings);
        }
        return CompletableFuture.completedFuture(false);
    }
    
    /**
     * 切换好友传送权限
     * 
     * @param playerUuid 玩家UUID
     * @return 切换后的状态
     */
    public boolean toggleFriendTeleport(UUID playerUuid) {
        TeleportSettings settings = getTeleportSettings(playerUuid);
        if (settings != null) {
            settings.toggleFriendTeleport();
            updateTeleportSettings(settings);
            return settings.isAllowFriendTeleport();
        }
        return false;
    }
    
    /**
     * 设置针对单个好友的传送权限
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param allow 是否允许
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setAllowTeleport(UUID playerUuid, UUID friendUuid, boolean allow) {
        TeleportSettings settings = getTeleportSettings(playerUuid);
        if (settings != null) {
            settings.setFriendTeleportPermission(friendUuid, allow);
            return updateTeleportSettings(settings);
        }
        return CompletableFuture.completedFuture(false);
    }
    
    /**
     * 检查是否允许指定好友传送
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 是否允许
     */
    public boolean isAllowTeleport(UUID playerUuid, UUID friendUuid) {
        TeleportSettings settings = getTeleportSettings(playerUuid);
        if (settings == null) {
            return configManager.isDefaultAllowTeleport();
        }
        Boolean friendPermission = settings.getFriendTeleportPermission(friendUuid);
        if (friendPermission != null) {
            return friendPermission;
        }
        return settings.isAllowFriendTeleport();
    }
    
    // ==================== 传送冷却管理 ====================
    
    /**
     * 加载玩家传送冷却到缓存
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadTeleportCooldown(UUID playerUuid) {
        return friendDAO.getTeleportCooldown(playerUuid).thenAccept(cooldownOpt -> {
            if (cooldownOpt.isPresent()) {
                TeleportCooldown cooldown = cooldownOpt.get();
                // 如果冷却已过期，不缓存
                if (!cooldown.isExpired()) {
                    cooldownCache.put(playerUuid, cooldown);
                }
            }
        });
    }
    
    /**
     * 检查玩家是否在传送冷却中
     * 
     * @param playerUuid 玩家UUID
     * @return 是否在冷却中
     */
    public boolean isInCooldown(UUID playerUuid) {
        TeleportCooldown cooldown = cooldownCache.get(playerUuid);
        if (cooldown == null) {
            return false;
        }
        
        // 检查是否过期
        if (cooldown.isExpired()) {
            cooldownCache.remove(playerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取剩余冷却时间（秒）
     * 
     * @param playerUuid 玩家UUID
     * @return 剩余秒数，如果无冷却返回0
     */
    public int getRemainingCooldown(UUID playerUuid) {
        TeleportCooldown cooldown = cooldownCache.get(playerUuid);
        if (cooldown == null) {
            return 0;
        }
        
        int remaining = cooldown.getRemainingSeconds();
        if (remaining <= 0) {
            cooldownCache.remove(playerUuid);
            return 0;
        }
        
        return remaining;
    }
    
    /**
     * 设置传送冷却
     * 
     * @param playerUuid 玩家UUID
     * @param cooldownSeconds 冷却时间（秒）
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setCooldown(UUID playerUuid, int cooldownSeconds) {
        return friendDAO.setTeleportCooldown(playerUuid, cooldownSeconds).thenApply(success -> {
            if (success) {
                TeleportCooldown cooldown = new TeleportCooldown(playerUuid, cooldownSeconds);
                cooldownCache.put(playerUuid, cooldown);
            }
            return success;
        });
    }
    
    /**
     * 清除传送冷却
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> clearCooldown(UUID playerUuid) {
        return friendDAO.removeTeleportCooldown(playerUuid).thenApply(success -> {
            if (success) {
                cooldownCache.remove(playerUuid);
            }
            return success;
        });
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清理玩家的缓存数据
     * 
     * @param playerUuid 玩家UUID
     */
    public void clearCache(UUID playerUuid) {
        settingsCache.remove(playerUuid);
        cooldownCache.remove(playerUuid);
    }
    
    /**
     * 玩家上线时加载数据
     * 
     * @param player 玩家
     */
    public void onPlayerJoin(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // 异步加载所有数据
        CompletableFuture.allOf(
                loadTeleportSettings(playerUuid),
                loadTeleportCooldown(playerUuid)
        );
    }
    
    /**
     * 玩家下线时保存数据
     * 
     * @param player 玩家
     */
    public void onPlayerQuit(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // 保存传送设置
        TeleportSettings settings = settingsCache.get(playerUuid);
        if (settings != null) {
            updateTeleportSettings(settings);
        }
        
        // 延迟清理缓存（给其他操作留出时间）
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            // 检查玩家是否已经离线
            if (Bukkit.getPlayer(playerUuid) == null) {
                clearCache(playerUuid);
            }
        }, 20L * 30); // 30秒后清理
    }
}
