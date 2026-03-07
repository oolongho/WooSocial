package com.oolonghoo.woosocial;

import com.oolonghoo.woosocial.command.MainCommand;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.database.DatabaseManager;
import com.oolonghoo.woosocial.database.FriendDAO;
import com.oolonghoo.woosocial.database.PlayerDAO;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.oolonghoo.woosocial.manager.ModuleManager;
import com.oolonghoo.woosocial.module.friend.FriendModule;
import com.oolonghoo.woosocial.module.mail.MailModule;
import com.oolonghoo.woosocial.module.relation.RelationModule;
import com.oolonghoo.woosocial.module.teleport.TeleportModule;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import com.oolonghoo.woosocial.sync.SyncMessageType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * WooSocial插件主类
 * 社交系统插件，提供好友系统和传送功能
 * 
 * @author oolongho
 * @version 1.0.0
 */
public class WooSocial extends JavaPlugin {
    
    private static WooSocial instance;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private ModuleManager moduleManager;
    private PlayerDAO playerDAO;
    private FriendDAO friendDAO;
    private com.oolonghoo.woosocial.gui.config.GUIConfigManager guiConfigManager;
    private com.oolonghoo.woosocial.gui.action.ActionParser actionParser;
    private SyncManager syncManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化消息管理器
        messageManager = new MessageManager(this);
        messageManager.initialize();
        
        // 初始化数据库管理器
        try {
            databaseManager = new DatabaseManager(this, configManager);
            databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "数据库初始化失败，插件将禁用", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化DAO层
        playerDAO = new PlayerDAO(this, databaseManager);
        friendDAO = new FriendDAO(this, databaseManager);
        
        // 初始化模块管理器
        moduleManager = new ModuleManager(this);
        moduleManager.initialize();
        
        // 初始化GUI配置管理器
        guiConfigManager = new com.oolonghoo.woosocial.gui.config.GUIConfigManager(this);
        guiConfigManager.initialize();
        
        // 初始化Action解析器
        actionParser = new com.oolonghoo.woosocial.gui.action.ActionParser(this);
        
        // 初始化同步管理器
        initializeSyncManager();
        
        // 注册模块
        registerModules();
        
        // 加载启用的模块
        moduleManager.loadEnabledModules();
        
        // 注册命令
        registerCommands();
        
        // 启动定时任务
        startScheduledTasks();
        
        // 缓存预热
        warmupCache();
        
        getLogger().info("WooSocial v" + getPluginMeta().getVersion() + " 已启用！");
    }
    
    /**
     * 缓存预热 - 预加载在线玩家的数据
     */
    private void warmupCache() {
        boolean warmupEnabled = getConfig().getBoolean("cache.warmup-on-enable", true);
        if (!warmupEnabled) {
            getLogger().info("[Cache] 缓存预热已禁用");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        var onlinePlayers = Bukkit.getOnlinePlayers();
        
        if (onlinePlayers.isEmpty()) {
            getLogger().info("[Cache] 没有在线玩家，跳过缓存预热");
            return;
        }
        
        getLogger().info("[Cache] 开始缓存预热，在线玩家数: " + onlinePlayers.size());
        
        // 异步预热缓存
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            int mailCount = 0;
            int relationCount = 0;
            
            // 预热邮件缓存
            var mailModule = moduleManager.getModule("mail");
            if (mailModule != null && mailModule.isEnabled()) {
                var mailDataManager = ((com.oolonghoo.woosocial.module.mail.MailModule) mailModule).getDataManager();
                if (mailDataManager != null) {
                    for (var player : onlinePlayers) {
                        mailDataManager.warmupCache(player.getUniqueId());
                        mailCount++;
                    }
                }
            }
            
            // 预热关系缓存
            var relationModule = moduleManager.getModule("relation");
            if (relationModule != null && relationModule.isEnabled()) {
                var relationDataManager = ((com.oolonghoo.woosocial.module.relation.RelationModule) relationModule).getDataManager();
                if (relationDataManager != null) {
                    for (var player : onlinePlayers) {
                        relationDataManager.warmupCache(player.getUniqueId());
                        relationCount++;
                    }
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            getLogger().info("[Cache] 缓存预热完成: 邮件 " + mailCount + " 个, 关系 " + relationCount + " 个, 耗时 " + elapsed + "ms");
        });
    }
    
    private void initializeSyncManager() {
        syncManager = new SyncManager(this);
        syncManager.initialize();
        
        if (syncManager.isInitialized()) {
            syncManager.setMessageHandler(this::handleSyncMessage);
        }
    }
    
    private void handleSyncMessage(SyncMessage message) {
        if (message == null) return;
        
        Bukkit.getScheduler().runTask(this, () -> {
            switch (message.getType()) {
                case FRIEND_REQUEST -> handleFriendRequestSync(message);
                case FRIEND_ACCEPT -> handleFriendAcceptSync(message);
                case FRIEND_REMOVE -> handleFriendRemoveSync(message);
                case PLAYER_ONLINE -> handlePlayerOnlineSync(message);
                case PLAYER_OFFLINE -> handlePlayerOfflineSync(message);
                case BLOCK_PLAYER -> handleBlockPlayerSync(message);
                case UNBLOCK_PLAYER -> handleUnblockPlayerSync(message);
                default -> {}
            }
        });
    }
    
    private void handleFriendRequestSync(SyncMessage message) {
        UUID senderUuid = message.getUUID("sender_uuid");
        String senderName = message.getString("sender_name");
        UUID receiverUuid = message.getUUID("receiver_uuid");
        
        if (senderUuid != null && receiverUuid != null) {
            var receiver = Bukkit.getPlayer(receiverUuid);
            if (receiver != null) {
                messageManager.send(receiver, "friend.request-received", "player", senderName);
            }
        }
    }
    
    private void handleFriendAcceptSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        String playerName = message.getString("player_name");
        UUID friendUuid = message.getUUID("friend_uuid");
        String friendName = message.getString("friend_name");
        
        if (playerUuid != null && friendUuid != null) {
            var player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                messageManager.send(player, "friend.friend-accepted", "player", friendName);
            }
        }
    }
    
    private void handleFriendRemoveSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID friendUuid = message.getUUID("friend_uuid");
        
        if (playerUuid != null && friendUuid != null) {
            friendDAO.removeFriend(playerUuid, friendUuid);
        }
    }
    
    private void handlePlayerOnlineSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        String playerName = message.getString("player_name");
        String serverName = message.getString("server_name");
        
        if (playerUuid != null) {
            playerDAO.updateServerName(playerUuid, serverName);
            
            friendDAO.getFriendsToNotify(playerUuid).thenAccept(friendUuids -> {
                for (UUID friendUuid : friendUuids) {
                    var friend = Bukkit.getPlayer(friendUuid);
                    if (friend != null) {
                        messageManager.send(friend, "friend.online-notification", 
                                "player", playerName,
                                "server", serverName);
                    }
                }
            });
        }
    }
    
    private void handlePlayerOfflineSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        
        if (playerUuid != null) {
            playerDAO.updateLastOnlineTime(playerUuid, System.currentTimeMillis());
            playerDAO.updateServerName(playerUuid, null);
        }
    }
    
    private void handleBlockPlayerSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID blockedUuid = message.getUUID("blocked_uuid");
        
        if (playerUuid != null && blockedUuid != null) {
            friendDAO.blockPlayer(playerUuid, blockedUuid);
        }
    }
    
    private void handleUnblockPlayerSync(SyncMessage message) {
        UUID playerUuid = message.getUUID("player_uuid");
        UUID unblockedUuid = message.getUUID("unblocked_uuid");
        
        if (playerUuid != null && unblockedUuid != null) {
            friendDAO.unblockPlayer(playerUuid, unblockedUuid);
        }
    }
    
    @Override
    public void onDisable() {
        // 关闭同步系统
        if (syncManager != null) {
            syncManager.shutdown();
        }
        
        // 禁用所有模块
        if (moduleManager != null) {
            moduleManager.disableAllModules();
        }
        
        // 关闭数据库连接池
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        getLogger().info("WooSocial 插件已禁用！");
    }
    
    /**
     * 注册模块
     */
    private void registerModules() {
        // 注册好友模块
        moduleManager.registerModule("friend", () -> new FriendModule(this));
        
        // 注册传送模块
        moduleManager.registerModule("teleport", () -> new TeleportModule(this));
        
        // 注册邮箱模块
        moduleManager.registerModule("mail", () -> new MailModule(this));
        
        // 注册关系模块
        moduleManager.registerModule("relation", () -> new RelationModule(this));
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        getCommand("social").setExecutor(mainCommand);
        getCommand("social").setTabCompleter(mainCommand);
    }
    
    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        int autoSaveInterval = configManager.getAutoSaveInterval();
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            friendDAO.cleanExpiredRequests(configManager.getRequestExpireTime());
            friendDAO.cleanExpiredCooldowns();
            
            if (moduleManager != null) {
                moduleManager.saveAllModules();
            }
        }, autoSaveInterval * 20L, autoSaveInterval * 20L);
    }
    
    /**
     * 重载插件配置
     */
    public void reloadPluginConfig() {
        // 重载配置文件
        configManager.reloadConfig();
        
        // 重载消息管理器
        messageManager.reload();
        
        // 重载同步管理器
        if (syncManager != null) {
            syncManager.shutdown();
            syncManager.initialize();
        }
        
        // 重载所有模块
        if (moduleManager != null) {
            moduleManager.reloadAllModules();
        }
        
        getLogger().info("配置已重新加载");
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取插件实例
     */
    public static WooSocial getInstance() {
        return instance;
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取消息管理器
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    /**
     * 获取数据库管理器
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * 获取模块管理器
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    /**
     * 获取关系模块
     */
    public com.oolonghoo.woosocial.module.relation.RelationModule getRelationModule() {
        return (com.oolonghoo.woosocial.module.relation.RelationModule) moduleManager.getModule("relation");
    }
    
    /**
     * 获取邮箱模块
     */
    public com.oolonghoo.woosocial.module.mail.MailModule getMailModule() {
        return (com.oolonghoo.woosocial.module.mail.MailModule) moduleManager.getModule("mail");
    }
    
    public com.oolonghoo.woosocial.gui.config.GUIConfigManager getGuiConfigManager() {
        return guiConfigManager;
    }
    
    public com.oolonghoo.woosocial.gui.action.ActionParser getActionParser() {
        return actionParser;
    }
    
    /**
     * 获取玩家DAO
     */
    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }
    
    /**
     * 获取好友DAO
     */
    public FriendDAO getFriendDAO() {
        return friendDAO;
    }
    
    /**
     * 通过玩家名获取玩家UUID（同步方法，从缓存或在线玩家获取）
     */
    public UUID getPlayerUuid(String playerName) {
        var onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }
        return playerDAO.getPlayerUuidByName(playerName).join();
    }
    
    /**
     * 获取同步管理器
     */
    public SyncManager getSyncManager() {
        return syncManager;
    }
}
