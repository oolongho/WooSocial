package com.oolonghoo.woosocial.module.friend;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.FriendDAO;
import com.oolonghoo.woosocial.database.PlayerDAO;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.model.FriendRequest;
import com.oolonghoo.woosocial.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 好友数据管理器
 * 负责管理好友数据的缓存和异步操作
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class FriendDataManager {
    
    private final WooSocial plugin;
    private final FriendDAO friendDAO;
    private final PlayerDAO playerDAO;
    
    // 在线玩家的好友数据缓存（列表形式，用于遍历）
    private final Map<UUID, List<FriendData>> friendCache = new ConcurrentHashMap<>();
    
    // 在线玩家的好友数据缓存（Map形式，用于O(1)查询）
    private final Map<UUID, Map<UUID, FriendData>> friendMapCache = new ConcurrentHashMap<>();
    
    // 在线玩家的好友UUID集合（用于O(1)判断好友关系）
    private final Map<UUID, Set<UUID>> friendUuidSetCache = new ConcurrentHashMap<>();
    
    // 在线玩家的好友请求数据缓存
    private final Map<UUID, List<FriendRequest>> requestCache = new ConcurrentHashMap<>();
    
    // 玩家数据缓存
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    
    // 屏蔽列表缓存
    private final Map<UUID, Set<UUID>> blockedCache = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public FriendDataManager(WooSocial plugin) {
        this.plugin = plugin;
        this.friendDAO = plugin.getFriendDAO();
        this.playerDAO = plugin.getPlayerDAO();
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
        
        friendCache.clear();
        friendMapCache.clear();
        friendUuidSetCache.clear();
        requestCache.clear();
        playerDataCache.clear();
        blockedCache.clear();
    }
    
    /**
     * 保存所有数据
     */
    public void saveAllData() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PlayerData playerData : playerDataCache.values()) {
                if (playerData != null) {
                    playerDAO.updatePlayerData(playerData);
                }
            }
        });
    }
    
    // ==================== 好友关系管理 ====================
    
    /**
     * 加载玩家的好友列表到缓存
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadFriendList(UUID playerUuid) {
        return friendDAO.getFriends(playerUuid).thenAccept(friends -> {
            friendCache.put(playerUuid, friends);
            
            Map<UUID, FriendData> friendMap = new ConcurrentHashMap<>();
            Set<UUID> friendUuidSet = ConcurrentHashMap.newKeySet();
            
            for (FriendData friend : friends) {
                friendMap.put(friend.getFriendUuid(), friend);
                friendUuidSet.add(friend.getFriendUuid());
            }
            
            friendMapCache.put(playerUuid, friendMap);
            friendUuidSetCache.put(playerUuid, friendUuidSet);
        });
    }
    
    /**
     * 获取玩家的好友列表（从缓存）
     * 
     * @param playerUuid 玩家UUID
     * @return 好友列表
     */
    public List<FriendData> getFriendList(UUID playerUuid) {
        return friendCache.getOrDefault(playerUuid, new ArrayList<>());
    }
    
    /**
     * 获取指定好友的数据
     * 优化：使用Map实现O(1)查询
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 好友数据，如果不存在返回null
     */
    public FriendData getFriendData(UUID playerUuid, UUID friendUuid) {
        Map<UUID, FriendData> friendMap = friendMapCache.get(playerUuid);
        if (friendMap != null) {
            return friendMap.get(friendUuid);
        }
        return null;
    }
    
    /**
     * 获取好友数量
     * 
     * @param playerUuid 玩家UUID
     * @return 好友数量
     */
    public int getFriendCount(UUID playerUuid) {
        List<FriendData> friends = friendCache.get(playerUuid);
        return friends != null ? friends.size() : 0;
    }
    
    /**
     * 检查是否为好友关系
     * 优化：使用Set实现O(1)查询
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 是否为好友
     */
    public boolean isFriend(UUID playerUuid, UUID friendUuid) {
        Set<UUID> friendUuidSet = friendUuidSetCache.get(playerUuid);
        return friendUuidSet != null && friendUuidSet.contains(friendUuid);
    }
    
    /**
     * 添加好友关系（双向）
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param playerName 玩家名称
     * @param friendName 好友名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> addFriend(UUID playerUuid, UUID friendUuid, 
                                                 String playerName, String friendName) {
        int maxFriends = plugin.getConfigManager().getMaxFriends();
        int currentFriendCount = getFriendCount(playerUuid);
        
        if (currentFriendCount >= maxFriends) {
            return CompletableFuture.completedFuture(false);
        }
        
        int friendFriendCount = getFriendCount(friendUuid);
        if (friendFriendCount >= maxFriends) {
            return CompletableFuture.completedFuture(false);
        }
        
        CompletableFuture<Boolean> future1 = friendDAO.addFriend(playerUuid, friendUuid, friendName);
        CompletableFuture<Boolean> future2 = friendDAO.addFriend(friendUuid, playerUuid, playerName);
        
        return CompletableFuture.allOf(future1, future2).thenApply(v -> {
            loadFriendList(playerUuid);
            loadFriendList(friendUuid);
            return true;
        });
    }
    
    /**
     * 检查是否可以添加好友
     * 
     * @param playerUuid 玩家UUID
     * @return 是否可以添加
     */
    public boolean canAddFriend(UUID playerUuid) {
        int maxFriends = plugin.getConfigManager().getMaxFriends();
        return getFriendCount(playerUuid) < maxFriends;
    }
    
    /**
     * 获取最大好友数量
     * 
     * @return 最大好友数量
     */
    public int getMaxFriends() {
        return plugin.getConfigManager().getMaxFriends();
    }
    
    /**
     * 删除好友关系（双向）
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> removeFriend(UUID playerUuid, UUID friendUuid) {
        return friendDAO.removeFriend(playerUuid, friendUuid).thenApply(success -> {
            if (success) {
                // 更新缓存
                loadFriendList(playerUuid);
                loadFriendList(friendUuid);
            }
            return success;
        });
    }
    
    // ==================== 好友请求管理 ====================
    
    /**
     * 加载玩家的好友请求到缓存
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadFriendRequests(UUID playerUuid) {
        return friendDAO.getPendingRequests(playerUuid).thenAccept(requests -> {
            requestCache.put(playerUuid, requests);
        });
    }
    
    /**
     * 获取玩家的好友请求列表（从缓存）
     * 
     * @param playerUuid 玩家UUID
     * @return 好友请求列表
     */
    public List<FriendRequest> getFriendRequests(UUID playerUuid) {
        return requestCache.getOrDefault(playerUuid, new ArrayList<>());
    }
    
    /**
     * 获取好友请求数量
     * 
     * @param playerUuid 玩家UUID
     * @return 好友请求数量
     */
    public int getFriendRequestCount(UUID playerUuid) {
        List<FriendRequest> requests = requestCache.get(playerUuid);
        return requests != null ? requests.size() : 0;
    }
    
    /**
     * 获取待处理的好友请求列表（异步从数据库查询）
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture<List<FriendRequest>>
     */
    public CompletableFuture<List<FriendRequest>> getPendingRequestsAsync(UUID playerUuid) {
        return friendDAO.getPendingRequests(playerUuid);
    }
    
    /**
     * 获取待处理的好友请求列表（同步，从缓存获取）
     * 
     * @param playerUuid 玩家UUID
     * @return 待处理的好友请求列表
     */
    public List<FriendRequest> getPendingRequests(UUID playerUuid) {
        List<FriendRequest> allRequests = requestCache.getOrDefault(playerUuid, new ArrayList<>());
        List<FriendRequest> pendingRequests = new ArrayList<>();
        
        for (FriendRequest request : allRequests) {
            if (request.getStatus() == FriendRequest.RequestStatus.PENDING) {
                pendingRequests.add(request);
            }
        }
        
        return pendingRequests;
    }
    
    /**
     * 接受好友请求（简化版本)
     * 
     * @param receiverUuid 接收者UUID
     * @param senderUuid 发送者UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> acceptFriendRequest(UUID receiverUuid, UUID senderUuid) {
        // 获取发送者名称
        String senderName = null;
        List<FriendRequest> requests = requestCache.get(receiverUuid);
        if (requests != null) {
            for (FriendRequest req : requests) {
                if (req.getSenderId().equals(senderUuid)) {
                    senderName = req.getSenderName();
                    break;
                }
            }
        }
        
        if (senderName == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return acceptFriendRequest(senderUuid, receiverUuid, senderName, 
                plugin.getServer().getOfflinePlayer(receiverUuid).getName());
    }
    
    /**
     * 拒绝好友请求(简化版本)
     * 
     * @param receiverUuid 接收者UUID
     * @param senderUuid 发送者UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> denyFriendRequest(UUID receiverUuid, UUID senderUuid) {
        return friendDAO.updateRequestStatus(senderUuid, receiverUuid, FriendRequest.RequestStatus.REJECTED)
                .thenApply(success -> {
                    if (success) {
                        loadFriendRequests(receiverUuid);
                    }
                    return success;
                });
    }
    
    /**
     * 发送好友请求
     * 
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @param senderName 发送者名称
     * @param receiverName 接收者名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> sendFriendRequest(UUID senderUuid, UUID receiverUuid,
                                                         String senderName, String receiverName) {
        if (!canAddFriend(senderUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (!canAddFriend(receiverUuid)) {
            return CompletableFuture.completedFuture(false);
        }
        
        return friendDAO.createFriendRequest(senderUuid, receiverUuid, senderName, receiverName)
                .thenApply(success -> {
                    if (success) {
                        loadFriendRequests(receiverUuid);
                    }
                    return success;
                });
    }
    
    /**
     * 接受好友请求
     * 
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @param senderName 发送者名称
     * @param receiverName 接收者名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> acceptFriendRequest(UUID senderUuid, UUID receiverUuid,
                                                           String senderName, String receiverName) {
        // 更新请求状态
        return friendDAO.updateRequestStatus(senderUuid, receiverUuid, FriendRequest.RequestStatus.ACCEPTED)
                .thenCompose(success -> {
                    if (success) {
                        // 添加双向好友关系
                        return addFriend(senderUuid, receiverUuid, senderName, receiverName);
                    }
                    return CompletableFuture.completedFuture(false);
                })
                .thenApply(success -> {
                    if (success) {
                        // 更新缓存
                        loadFriendRequests(receiverUuid);
                    }
                    return success;
                });
    }
    
    /**
     * 检查是否有待处理的好友请求
     * 
     * @param senderUuid 发送者UUID
     * @param receiverUuid 接收者UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> hasPendingRequest(UUID senderUuid, UUID receiverUuid) {
        return friendDAO.getFriendRequest(senderUuid, receiverUuid)
                .thenApply(Optional::isPresent);
    }
    
    // ==================== 玩家数据管理 ====================
    
    /**
     * 加载玩家数据到缓存
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadPlayerData(UUID playerUuid) {
        return playerDAO.getPlayerData(playerUuid).thenAccept(optionalPlayerData -> {
            if (optionalPlayerData.isPresent()) {
                playerDataCache.put(playerUuid, optionalPlayerData.get());
            } else {
                // 创建新的玩家数据
                PlayerData newPlayerData = new PlayerData(playerUuid);
                playerDataCache.put(playerUuid, newPlayerData);
            }
        });
    }
    
    /**
     * 获取玩家数据（从缓存）
     * 
     * @param playerUuid 玩家UUID
     * @return 玩家数据
     */
    public PlayerData getPlayerData(UUID playerUuid) {
        return playerDataCache.get(playerUuid);
    }
    
    /**
     * 更新玩家数据
     * 
     * @param playerData 玩家数据
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updatePlayerData(PlayerData playerData) {
        playerDataCache.put(playerData.getUuid(), playerData);
        return playerDAO.updatePlayerData(playerData);
    }
    
    /**
     * 检查是否开启上线提醒
     * 
     * @param playerUuid 玩家UUID
     * @return 是否开启
     */
    public boolean isNotifyOnline(UUID playerUuid) {
        PlayerData playerData = playerDataCache.get(playerUuid);
        return playerData != null && playerData.isNotifyOnline();
    }
    
    /**
     * 设置上线提醒
     * 
     * @param playerUuid 玩家UUID
     * @param notifyOnline 是否开启
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setNotifyOnline(UUID playerUuid, boolean notifyOnline) {
        PlayerData playerData = playerDataCache.get(playerUuid);
        if (playerData != null) {
            playerData.setNotifyOnline(notifyOnline);
            return updatePlayerData(playerData);
        }
        return CompletableFuture.completedFuture(false);
    }
    
    /**
     * 检查是否开启了对指定好友的上线提醒
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 是否开启
     */
    public boolean isNotifyOnlineForFriend(UUID playerUuid, UUID friendUuid) {
        // 目前使用全局设置，后续可以扩展为针对单个好友的设置
        return isNotifyOnline(playerUuid);
    }
    
    /**
     * 设置对指定好友的上线提醒
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param notifyOnline 是否开启
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setNotifyOnlineForFriend(UUID playerUuid, UUID friendUuid, boolean notifyOnline) {
        // 目前使用全局设置，后续可以扩展为针对单个好友的设置
        return setNotifyOnline(playerUuid, notifyOnline);
    }
    
    // ==================== 屏蔽功能 ====================
    
    /**
     * 加载玩家的屏蔽列表
     * 
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> loadBlockedList(UUID playerUuid) {
        return friendDAO.loadBlockedList(playerUuid).thenAccept(blockedSet -> {
            blockedCache.put(playerUuid, blockedSet);
        });
    }
    
    /**
     * 获取玩家的屏蔽列表
     * 
     * @param playerUuid 玩家UUID
     * @return 屏蔽列表
     */
    public Set<UUID> getBlockedList(UUID playerUuid) {
        return blockedCache.getOrDefault(playerUuid, new HashSet<>());
    }
    
    /**
     * 检查是否屏蔽了某玩家
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return 是否屏蔽
     */
    public boolean isBlocked(UUID playerUuid, UUID targetUuid) {
        Set<UUID> blocked = blockedCache.get(playerUuid);
        return blocked != null && blocked.contains(targetUuid);
    }
    
    /**
     * 屏蔽玩家
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> blockPlayer(UUID playerUuid, UUID targetUuid) {
        return friendDAO.addBlocked(playerUuid, targetUuid).thenApply(success -> {
            if (success) {
                Set<UUID> blocked = blockedCache.computeIfAbsent(playerUuid, k -> new HashSet<>());
                blocked.add(targetUuid);
            }
            return success;
        });
    }
    
    /**
     * 取消屏蔽玩家
     * 
     * @param playerUuid 玩家UUID
     * @param targetUuid 目标玩家UUID
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> unblockPlayer(UUID playerUuid, UUID targetUuid) {
        return friendDAO.removeBlocked(playerUuid, targetUuid).thenApply(success -> {
            if (success) {
                Set<UUID> blocked = blockedCache.get(playerUuid);
                if (blocked != null) {
                    blocked.remove(targetUuid);
                }
            }
            return success;
        });
    }
    
    // ==================== 好友收藏功能 ====================
    
    /**
     * 检查是否收藏了好友
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 是否收藏
     */
    public boolean isFavorite(UUID playerUuid, UUID friendUuid) {
        FriendData friendData = getFriendData(playerUuid, friendUuid);
        return friendData != null && friendData.isFavorite();
    }
    
    /**
     * 设置好友收藏状态
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param favorite 是否收藏
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setFavorite(UUID playerUuid, UUID friendUuid, boolean favorite) {
        return friendDAO.setFavorite(playerUuid, friendUuid, favorite).thenApply(success -> {
            if (success) {
                FriendData friendData = getFriendData(playerUuid, friendUuid);
                if (friendData != null) {
                    friendData.setFavorite(favorite);
                }
            }
            return success;
        });
    }
    
    /**
     * 切换好友收藏状态
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return CompletableFuture<Boolean> 返回切换后的状态
     */
    public CompletableFuture<Boolean> toggleFavorite(UUID playerUuid, UUID friendUuid) {
        boolean currentStatus = isFavorite(playerUuid, friendUuid);
        return setFavorite(playerUuid, friendUuid, !currentStatus);
    }
    
    // ==================== 好友备注功能 ====================
    
    /**
     * 获取好友备注
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 备注名称，如果没有返回null
     */
    public String getNickname(UUID playerUuid, UUID friendUuid) {
        FriendData friendData = getFriendData(playerUuid, friendUuid);
        return friendData != null ? friendData.getNickname() : null;
    }
    
    /**
     * 设置好友备注
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param nickname 备注名称
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setNickname(UUID playerUuid, UUID friendUuid, String nickname) {
        return friendDAO.setNickname(playerUuid, friendUuid, nickname).thenApply(success -> {
            if (success) {
                FriendData friendData = getFriendData(playerUuid, friendUuid);
                if (friendData != null) {
                    friendData.setNickname(nickname);
                }
            }
            return success;
        });
    }
    
    /**
     * 设置是否接收好友消息
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @param receiveMessages 是否接收消息
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> setReceiveMessages(UUID playerUuid, UUID friendUuid, boolean receiveMessages) {
        return friendDAO.setReceiveMessages(playerUuid, friendUuid, receiveMessages).thenApply(success -> {
            if (success) {
                FriendData friendData = getFriendData(playerUuid, friendUuid);
                if (friendData != null) {
                    friendData.setReceiveMessages(receiveMessages);
                }
            }
            return success;
        });
    }
    
    /**
     * 检查是否接收好友消息
     * 
     * @param playerUuid 玩家UUID
     * @param friendUuid 好友UUID
     * @return 是否接收消息
     */
    public boolean isReceiveMessages(UUID playerUuid, UUID friendUuid) {
        FriendData friendData = getFriendData(playerUuid, friendUuid);
        return friendData != null && friendData.isReceiveMessages();
    }
    
    /**
     * 获取需要通知该玩家上线的好友UUID列表
     * 
     * @param playerUuid 上线的玩家UUID
     * @return CompletableFuture<List<UUID>> 需要通知的好友UUID列表
     */
    public CompletableFuture<List<UUID>> getFriendsToNotify(UUID playerUuid) {
        return friendDAO.getFriendsToNotify(playerUuid);
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清理玩家的缓存数据
     * 
     * @param playerUuid 玩家UUID
     */
    public void clearCache(UUID playerUuid) {
        friendCache.remove(playerUuid);
        friendMapCache.remove(playerUuid);
        friendUuidSetCache.remove(playerUuid);
        requestCache.remove(playerUuid);
        playerDataCache.remove(playerUuid);
        blockedCache.remove(playerUuid);
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
                loadPlayerData(playerUuid),
                loadFriendList(playerUuid),
                loadFriendRequests(playerUuid),
                loadBlockedList(playerUuid)
        ).thenRun(() -> {
            // 更新玩家名称和最后在线时间
            PlayerData playerData = playerDataCache.get(playerUuid);
            if (playerData != null) {
                playerData.setLastName(player.getName());
                playerData.setLastOnlineTime(System.currentTimeMillis());
                updatePlayerData(playerData);
            }
        });
    }
    
    /**
     * 玩家下线时保存数据
     * 
     * @param player 玩家
     */
    public void onPlayerQuit(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // 更新最后在线时间
        PlayerData playerData = playerDataCache.get(playerUuid);
        if (playerData != null) {
            playerData.setLastOnlineTime(System.currentTimeMillis());
            playerDAO.updatePlayerData(playerData);
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
