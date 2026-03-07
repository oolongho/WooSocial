package com.oolonghoo.woosocial.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.oolonghoo.woosocial.gui.FriendListGUI;
import com.oolonghoo.woosocial.model.FriendData;
import com.oolonghoo.woosocial.model.FriendRequest;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 好友命令处理器
 * 处理所有好友相关的命令
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class FriendCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final FriendDataManager dataManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param dataManager 数据管理器
     */
    public FriendCommand(WooSocial plugin, FriendDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查是否为玩家
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.closeInventory();
            FriendListGUI friendListGUI = new FriendListGUI(plugin, player);
            friendListGUI.open(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                handleAdd(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "deny":
            case "reject":
                handleDeny(player, args);
                break;
            case "remove":
            case "delete":
                handleRemove(player, args);
                break;
            case "list":
                handleList(player, args);
                break;
            case "requests":
                handleRequests(player);
                break;
            case "notify":
                handleNotify(player, args);
                break;
            case "block":
                handleBlock(player, args);
                break;
            case "unblock":
                handleUnblock(player, args);
                break;
            case "blocked":
                handleBlockedList(player, args);
                break;
            case "help":
                handleHelp(player);
                break;
            default:
                messageManager.send(sender, "general.unknown-command");
                break;
        }
        
        return true;
    }
    
    /**
     * 处理添加好友命令
     */
    private void handleAdd(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_ADD)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            messageManager.send(player, "general.invalid-argument", "arg", "玩家名称");
            return;
        }
        
        String targetName = args[1];
        
        // 不能添加自己
        if (targetName.equalsIgnoreCase(player.getName())) {
            messageManager.send(player, "friend.cannot-add-self");
            return;
        }
        
        // 获取目标玩家
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer == null || (targetPlayer.getName() == null && !targetPlayer.hasPlayedBefore())) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = targetPlayer.getUniqueId();
        String targetPlayerName = targetPlayer.getName() != null ? targetPlayer.getName() : targetName;
        
        // 检查是否已屏蔽
        if (dataManager.isBlocked(playerUuid, targetUuid)) {
            messageManager.send(player, "block.cannot-add-blocked");
            return;
        }
        
        if (dataManager.isBlocked(targetUuid, playerUuid)) {
            messageManager.send(player, "block.cannot-add-blocker");
            return;
        }
        
        // 检查是否已经是好友
        if (dataManager.isFriend(playerUuid, targetUuid)) {
            messageManager.send(player, "friend.already-friend");
            return;
        }
        
        // 检查好友数量上限
        int maxFriends = configManager.getMaxFriends();
        if (!player.hasPermission(Perms.FRIEND_LIMIT_BYPASS) && 
            dataManager.getFriendCount(playerUuid) >= maxFriends) {
            messageManager.send(player, "friend.max-friends-reached", "max", maxFriends);
            return;
        }
        
        // 检查目标玩家好友数量上限
        if (dataManager.getFriendCount(targetUuid) >= maxFriends) {
            messageManager.send(player, "friend.target-max-friends-reached", "player", targetPlayerName);
            return;
        }
        
        // 检查是否已有待处理的请求
        dataManager.hasPendingRequest(playerUuid, targetUuid).thenAccept(hasPending -> {
            if (hasPending) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messageManager.send(player, "friend.request-already-sent");
                });
                return;
            }
            
            // 检查对方是否已发送请求
            dataManager.hasPendingRequest(targetUuid, playerUuid).thenAccept(hasReversePending -> {
                if (hasReversePending) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        messageManager.send(player, "friend.request-pending-exists");
                    });
                    return;
                }
                
                // 发送好友请求
                dataManager.sendFriendRequest(playerUuid, targetUuid, player.getName(), targetPlayerName)
                        .thenAccept(success -> {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    // 发送成功消息
                                    messageManager.send(player, "friend.request-sent", "player", targetPlayerName);
                                    
                                    // 如果目标玩家在线，发送通知
                                    Player onlineTarget = Bukkit.getPlayer(targetUuid);
                                    if (onlineTarget != null) {
                                        messageManager.send(onlineTarget, "friend.request-received", 
                                                "player", player.getName());
                                    }
                                } else {
                                    messageManager.send(player, "general.internal-error");
                                }
                            });
                        });
            });
        });
    }
    
    /**
     * 处理接受好友请求命令
     */
    private void handleAccept(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_ACCEPT)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // 如果没有指定玩家，接受所有请求
        if (args.length < 2) {
            List<FriendRequest> requests = dataManager.getFriendRequests(playerUuid);
            if (requests.isEmpty()) {
                messageManager.send(player, "friend.no-pending-request");
                return;
            }
            
            // 接受第一个请求
            FriendRequest request = requests.get(0);
            acceptRequest(player, request);
            return;
        }
        
        // 接受指定玩家的请求
        String targetName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 查找请求
        List<FriendRequest> requests = dataManager.getFriendRequests(playerUuid);
        Optional<FriendRequest> requestOpt = requests.stream()
                .filter(r -> r.getSenderId().equals(targetUuid))
                .findFirst();
        
        if (requestOpt.isEmpty()) {
            messageManager.send(player, "friend.no-pending-request-from", "player", targetName);
            return;
        }
        
        acceptRequest(player, requestOpt.get());
    }
    
    /**
     * 接受好友请求
     */
    private void acceptRequest(Player player, FriendRequest request) {
        dataManager.acceptFriendRequest(request.getSenderId(), request.getReceiverId(),
                request.getSenderName(), request.getReceiverName())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            // 发送成功消息
                            messageManager.send(player, "friend.request-accepted", 
                                    "player", request.getSenderName());
                            
                            // 通知发送者
                            Player sender = Bukkit.getPlayer(request.getSenderId());
                            if (sender != null) {
                                messageManager.send(sender, "friend.request-accepted",
                                        "player", player.getName());
                            }
                        } else {
                            messageManager.send(player, "general.internal-error");
                        }
                    });
                });
    }
    
    /**
     * 处理拒绝好友请求命令
     */
    private void handleDeny(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_DENY)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // 如果没有指定玩家，拒绝所有请求
        if (args.length < 2) {
            List<FriendRequest> requests = dataManager.getFriendRequests(playerUuid);
            if (requests.isEmpty()) {
                messageManager.send(player, "friend.no-pending-request");
                return;
            }
            
            // 拒绝第一个请求
            FriendRequest request = requests.get(0);
            denyRequest(player, request);
            return;
        }
        
        // 拒绝指定玩家的请求
        String targetName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 查找请求
        List<FriendRequest> requests = dataManager.getFriendRequests(playerUuid);
        Optional<FriendRequest> requestOpt = requests.stream()
                .filter(r -> r.getSenderId().equals(targetUuid))
                .findFirst();
        
        if (requestOpt.isEmpty()) {
            messageManager.send(player, "friend.no-pending-request-from", "player", targetName);
            return;
        }
        
        denyRequest(player, requestOpt.get());
    }
    
    /**
     * 拒绝好友请求
     */
    private void denyRequest(Player player, FriendRequest request) {
        dataManager.denyFriendRequest(request.getSenderId(), request.getReceiverId())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messageManager.send(player, "friend.request-denied", 
                                    "player", request.getSenderName());
                        } else {
                            messageManager.send(player, "general.internal-error");
                        }
                    });
                });
    }
    
    /**
     * 处理删除好友命令
     */
    private void handleRemove(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_REMOVE)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            messageManager.send(player, "general.invalid-argument", "arg", "玩家名称");
            return;
        }
        
        String targetName = args[1];
        
        // 不能删除自己
        if (targetName.equalsIgnoreCase(player.getName())) {
            messageManager.send(player, "friend.cannot-remove-self");
            return;
        }
        
        // 获取目标玩家
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 检查是否为好友
        if (!dataManager.isFriend(playerUuid, targetUuid)) {
            messageManager.send(player, "friend.not-friend");
            return;
        }
        
        // 删除好友关系
        dataManager.removeFriend(playerUuid, targetUuid).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    String targetPlayerName = targetPlayer.getName() != null ? 
                            targetPlayer.getName() : targetName;
                    messageManager.send(player, "friend.friend-removed", "player", targetPlayerName);
                    
                    // 通知对方
                    Player onlineTarget = Bukkit.getPlayer(targetUuid);
                    if (onlineTarget != null) {
                        messageManager.send(onlineTarget, "friend.friend-removed-by", 
                                "player", player.getName());
                    }
                } else {
                    messageManager.send(player, "general.internal-error");
                }
            });
        });
    }
    
    /**
     * 处理好友列表命令
     */
    private void handleList(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_LIST)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 获取页码
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                messageManager.send(player, "general.invalid-number");
                return;
            }
        }
        
        // 获取好友列表
        List<FriendData> friends = dataManager.getFriendList(player.getUniqueId());
        
        if (friends.isEmpty()) {
            messageManager.send(player, "friend.list-empty");
            return;
        }
        
        // 分页显示
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) friends.size() / pageSize);
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, friends.size());
        
        // 显示列表头
        messageManager.sendNoPrefix(player, "friend.list-header", 
                "page", page, "total", totalPages);
        
        // 显示好友列表
        for (int i = startIndex; i < endIndex; i++) {
            FriendData friend = friends.get(i);
            String friendName = friend.getFriendName();
            
            // 检查好友是否在线
            Player onlineFriend = Bukkit.getPlayer(friend.getFriendUuid());
            if (onlineFriend != null && onlineFriend.isOnline()) {
                messageManager.sendNoPrefix(player, "friend.list-online", 
                        "player", friendName, "server", "当前服务器");
            } else {
                messageManager.sendNoPrefix(player, "friend.list-offline", 
                        "player", friendName);
            }
        }
        
        // 显示列表尾
        messageManager.sendNoPrefix(player, "friend.list-footer");
    }
    
    /**
     * 处理好友请求列表命令
     */
    private void handleRequests(Player player) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_REQUESTS)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        List<FriendRequest> requests = dataManager.getFriendRequests(player.getUniqueId());
        
        if (requests.isEmpty()) {
            messageManager.send(player, "friend.request-list-empty");
            return;
        }
        
        // 显示请求列表头
        messageManager.sendNoPrefix(player, "friend.request-list-header",
                "page", 1, "total", 1);
        
        // 显示请求列表
        for (FriendRequest request : requests) {
            long timeAgo = System.currentTimeMillis() - request.getSendTime();
            String timeStr = formatTimeAgo(timeAgo);
            
            messageManager.sendNoPrefix(player, "friend.request-list-entry",
                    "player", request.getSenderName(), "time", timeStr);
        }
    }
    
    /**
     * 处理上线提醒设置命令
     */
    private void handleNotify(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.FRIEND_NOTIFY)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            messageManager.send(player, "general.invalid-argument", "arg", "玩家名称");
            return;
        }
        
        String targetName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 检查是否为好友
        if (!dataManager.isFriend(player.getUniqueId(), targetUuid)) {
            messageManager.send(player, "friend.not-friend");
            return;
        }
        
        // 获取当前设置或从参数获取
        boolean currentNotify = dataManager.isNotifyOnline(targetUuid);
        boolean newNotify;
        
        if (args.length >= 3) {
            newNotify = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true");
        } else {
            newNotify = !currentNotify;
        }
        
        // 更新设置
        dataManager.setNotifyOnline(targetUuid, newNotify).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    if (newNotify) {
                        messageManager.send(player, "friend.notify-online-enabled", 
                                "player", targetPlayer.getName());
                    } else {
                        messageManager.send(player, "friend.notify-online-disabled",
                                "player", targetPlayer.getName());
                    }
                } else {
                    messageManager.send(player, "general.internal-error");
                }
            });
        });
    }
    
    /**
     * 处理屏蔽玩家命令
     */
    private void handleBlock(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.BLOCK_ADD)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            messageManager.send(player, "general.invalid-argument", "arg", "玩家名称");
            return;
        }
        
        String targetName = args[1];
        
        // 不能屏蔽自己
        if (targetName.equalsIgnoreCase(player.getName())) {
            messageManager.send(player, "block.cannot-block-self");
            return;
        }
        
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 检查是否已屏蔽
        if (dataManager.isBlocked(playerUuid, targetUuid)) {
            messageManager.send(player, "block.already-blocked");
            return;
        }
        
        // 屏蔽玩家
        dataManager.blockPlayer(playerUuid, targetUuid).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    messageManager.send(player, "block.blocked", "player", targetName);
                } else {
                    messageManager.send(player, "general.internal-error");
                }
            });
        });
    }
    
    /**
     * 处理取消屏蔽命令
     */
    private void handleUnblock(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.BLOCK_REMOVE)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            messageManager.send(player, "general.invalid-argument", "arg", "玩家名称");
            return;
        }
        
        String targetName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        if (targetPlayer == null) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = targetPlayer.getUniqueId();
        
        // 检查是否已屏蔽
        if (!dataManager.isBlocked(playerUuid, targetUuid)) {
            messageManager.send(player, "block.not-blocked");
            return;
        }
        
        // 取消屏蔽
        dataManager.unblockPlayer(playerUuid, targetUuid).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    messageManager.send(player, "block.unblocked", "player", targetName);
                } else {
                    messageManager.send(player, "general.internal-error");
                }
            });
        });
    }
    
    /**
     * 处理屏蔽列表命令
     */
    private void handleBlockedList(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission(Perms.BLOCK_LIST)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        // 获取屏蔽列表
        Set<UUID> blocked = dataManager.getBlockedList(player.getUniqueId());
        
        if (blocked == null || blocked.isEmpty()) {
            messageManager.send(player, "block.list-empty");
            return;
        }
        
        // 显示列表头
        messageManager.sendNoPrefix(player, "block.list-header", "page", 1, "total", 1);
        
        // 显示屏蔽列表
        for (UUID uuid : blocked) {
            OfflinePlayer blockedPlayer = Bukkit.getOfflinePlayer(uuid);
            String name = blockedPlayer.getName() != null ? blockedPlayer.getName() : uuid.toString();
            messageManager.sendNoPrefix(player, "block.list-entry", "player", name);
        }
    }
    
    /**
     * 处理帮助命令
     */
    private void handleHelp(Player player) {
        if (!player.hasPermission(Perms.HELP)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        messageManager.sendList(player, "help.header");
        messageManager.sendNoPrefix(player, "help.friend");
        messageManager.sendNoPrefix(player, "help.friend-add");
        messageManager.sendNoPrefix(player, "help.friend-accept");
        messageManager.sendNoPrefix(player, "help.friend-deny");
        messageManager.sendNoPrefix(player, "help.friend-remove");
        messageManager.sendNoPrefix(player, "help.friend-list");
        messageManager.sendNoPrefix(player, "help.friend-notify");
        messageManager.sendNoPrefix(player, "help.friend-requests");
        messageManager.sendNoPrefix(player, "help.friend-block");
        messageManager.sendNoPrefix(player, "help.friend-unblock");
        messageManager.sendNoPrefix(player, "help.friend-blocked");
        messageManager.sendList(player, "help.footer");
    }
    
    /**
     * 格式化时间差
     */
    private String formatTimeAgo(long timeAgo) {
        long seconds = timeAgo / 1000;
        
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + " 分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " 小时前";
        } else {
            return (seconds / 86400) + " 天前";
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            // 子命令补全
            completions.addAll(Arrays.asList(
                    "add", "accept", "deny", "remove", "list", 
                    "requests", "notify", "block", "unblock", "blocked", "help"
            ));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                case "remove":
                case "block":
                case "unblock":
                case "notify":
                    // 在线玩家名称补全
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> !p.equals(player))
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .forEach(completions::add);
                    break;
                    
                case "accept":
                case "deny":
                    // 好友请求发送者名称补全
                    List<FriendRequest> requests = dataManager.getFriendRequests(player.getUniqueId());
                    requests.stream()
                            .map(FriendRequest::getSenderName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .forEach(completions::add);
                    break;
                    
                case "list":
                case "blocked":
                    // 页码补全
                    completions.addAll(Arrays.asList("1", "2", "3"));
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("notify")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        }
        
        return completions;
    }
}
