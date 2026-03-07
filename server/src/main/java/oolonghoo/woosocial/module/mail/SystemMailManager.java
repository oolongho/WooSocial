package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统邮件管理器
 * 负责管理系统邮件的发送，支持发送给所有玩家、在线玩家或指定玩家列表
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class SystemMailManager {
    
    private final WooSocial plugin;
    private final MailDataManager dataManager;
    private final MessageManager messageManager;
    
    /**
     * 系统发送者UUID（全零UUID表示系统）
     */
    private static final UUID SYSTEM_SENDER_UUID = new UUID(0L, 0L);
    
    /**
     * 系统发送者名称
     */
    private static final String SYSTEM_SENDER_NAME = "System";
    
    /**
     * 正在进行的系统邮件发送任务
     */
    private final Map<String, SystemMailTask> runningTasks = new ConcurrentHashMap<>();
    
    public SystemMailManager(WooSocial plugin, MailDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 发送系统邮件给所有玩家（包括离线玩家）
     * 
     * @param sender 命令发送者
     * @param item 要发送的物品
     * @return 异步操作结果
     */
    public CompletableFuture<SystemMailResult> sendToAllPlayers(CommandSender sender, ItemStack item) {
        // 验证物品
        if (!validateItem(sender, item)) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "invalid-item"));
        }
        
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        messageManager.send(sender, "mail.system.sending-all");
        
        // 异步获取所有玩家并发送邮件
        return CompletableFuture.supplyAsync(() -> {
            // 获取所有曾经登录过的玩家
            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            List<UUID> targetUuids = new ArrayList<>();
            
            for (OfflinePlayer offlinePlayer : offlinePlayers) {
                if (offlinePlayer.getUniqueId() != null && 
                    !offlinePlayer.getUniqueId().equals(SYSTEM_SENDER_UUID)) {
                    targetUuids.add(offlinePlayer.getUniqueId());
                }
            }
            
            return sendSystemMailToRecipients(item, targetUuids);
        }).thenCompose(result -> result).thenApply(result -> {
            if (result.getSuccessCount() > 0) {
                messageManager.send(sender, "mail.system.sendall-success", 
                        "count", String.valueOf(result.getSuccessCount()),
                        "total", String.valueOf(result.getTotalCount()));
            } else {
                messageManager.send(sender, "mail.system.sendall-failed");
            }
            return result;
        });
    }
    
    /**
     * 发送系统邮件给所有在线玩家
     * 
     * @param sender 命令发送者
     * @param item 要发送的物品
     * @return 异步操作结果
     */
    public CompletableFuture<SystemMailResult> sendToOnlinePlayers(CommandSender sender, ItemStack item) {
        // 验证物品
        if (!validateItem(sender, item)) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "invalid-item"));
        }
        
        messageManager.send(sender, "mail.system.sending-online");
        
        // 获取所有在线玩家
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        List<UUID> targetUuids = new ArrayList<>();
        
        for (Player player : onlinePlayers) {
            targetUuids.add(player.getUniqueId());
        }
        
        return sendSystemMailToRecipients(item, targetUuids).thenApply(result -> {
            if (result.getSuccessCount() > 0) {
                messageManager.send(sender, "mail.system.sendonline-success", 
                        "count", String.valueOf(result.getSuccessCount()),
                        "total", String.valueOf(result.getTotalCount()));
            } else {
                messageManager.send(sender, "mail.system.sendonline-failed");
            }
            return result;
       });
    }
    
    /**
     * 发送系统邮件给指定玩家列表
     * 
     * @param sender 命令发送者
     * @param item 要发送的物品
     * @param playerNames 目标玩家名称列表
     * @return 异步操作结果
     */
    public CompletableFuture<SystemMailResult> sendToSpecificPlayers(CommandSender sender, ItemStack item, List<String> playerNames) {
        // 验证物品
        if (!validateItem(sender, item)) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "invalid-item"));
        }
        
        if (playerNames == null || playerNames.isEmpty()) {
            messageManager.send(sender, "mail.system.no-targets");
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "no-targets"));
        }
        
        // 解析玩家UUID
        List<UUID> targetUuids = new ArrayList<>();
        List<String> notFoundPlayers = new ArrayList<>();
        
        for (String playerName : playerNames) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
                targetUuids.add(offlinePlayer.getUniqueId());
            } else {
                notFoundPlayers.add(playerName);
            }
        }
        
        // 提示未找到的玩家
        if (!notFoundPlayers.isEmpty()) {
            messageManager.send(sender, "mail.system.players-not-found", 
                    "players", String.join(", ", notFoundPlayers));
        }
        
        if (targetUuids.isEmpty()) {
            messageManager.send(sender, "mail.system.no-valid-targets");
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "no-valid-targets"));
        }
        
        return sendSystemMailToRecipients(item, targetUuids).thenApply(result -> {
            if (result.getSuccessCount() > 0) {
                messageManager.send(sender, "mail.system.send-success", 
                        "count", String.valueOf(result.getSuccessCount()),
                        "total", String.valueOf(result.getTotalCount()));
            } else {
                messageManager.send(sender, "mail.system.send-failed");
            }
            return result;
        });
    }
    
    /**
     * 发送系统邮件给指定接收者列表
     * 
     * @param item 要发送的物品
     * @param receiverUuids 接收者UUID列表
     * @return 发送结果
     */
    private CompletableFuture<SystemMailResult> sendSystemMailToRecipients(ItemStack item, List<UUID> receiverUuids) {
        if (receiverUuids.isEmpty()) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "no-receivers"));
        }
        
        // 验证物品大小
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > dataManager.getMaxItemSize()) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, receiverUuids.size(), "item-too-large"));
        }
        
        // 序列化物品
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, receiverUuids.size(), "serialize-failed"));
        }
        
        // 生成批量ID
        String bulkId = UUID.randomUUID().toString();
        ItemStack clonedItem = item.clone();
        
        // 异步发送邮件
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            int failCount = 0;
            
            for (UUID receiverUuid : receiverUuids) {
                try {
                    // 获取玩家名称
                    String receiverName = null;
                    Player onlinePlayer = Bukkit.getPlayer(receiverUuid);
                    if (onlinePlayer != null) {
                        receiverName = onlinePlayer.getName();
                    } else {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(receiverUuid);
                        if (offlinePlayer != null) {
                            receiverName = offlinePlayer.getName();
                        }
                    }
                    
                    if (receiverName == null) {
                        failCount++;
                        continue;
                    }
                    
                    // 创建邮件数据
                    MailData mail = new MailData(SYSTEM_SENDER_UUID, receiverUuid);
                    mail.setSenderName(SYSTEM_SENDER_NAME);
                    mail.setReceiverName(receiverName);
                    mail.setItemData(itemData);
                    mail.setExpireTime(System.currentTimeMillis() + 
                            (dataManager.getExpireDays() * 24L * 60 * 60 * 1000));
                    mail.setBulk(true);
                    mail.setBulkId(bulkId);
                    
                    // 保存到数据库
                    boolean success = dataManager.getMailDAO().createMail(mail).join();
                    if (success) {
                        successCount++;
                        // 加载邮件到缓存
                        dataManager.loadMails(receiverUuid);
                        // 通知在线玩家
                        Player receiver = Bukkit.getPlayer(receiverUuid);
                        if (receiver != null && receiver.isOnline()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                messageManager.send(receiver, "mail.system.receive-notify",
                                        "item", ItemSerializer.getItemDisplayName(clonedItem));
                            });
                        }
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[SystemMail] Failed to send mail to " + receiverUuid + ": " + e.getMessage());
                    failCount++;
                }
            }
            
            return new SystemMailResult(successCount, receiverUuids.size(), failCount > 0 ? "partial-failure" : null);
        });
    }
    
    /**
     * 验证物品是否有效
     * 
     * @param sender 命令发送者
     * @param item 要验证的物品
     * @return 是否有效
     */
    private boolean validateItem(CommandSender sender, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            messageManager.send(sender, "mail.no-item-in-hand");
            return false;
        }
        
        if (!ItemSerializer.isValidItem(item)) {
            messageManager.send(sender, "mail.invalid-item");
            return false;
        }
        
        return true;
    }
    
    /**
     * 关闭管理器，清理资源
     */
    public void shutdown() {
        runningTasks.clear();
    }
    
    /**
     * 系统邮件发送结果
     */
    public static class SystemMailResult {
        private final int successCount;
        private final int totalCount;
        private final String errorCode;
        
        public SystemMailResult(int successCount, int totalCount, String errorCode) {
            this.successCount = successCount;
            this.totalCount = totalCount;
            this.errorCode = errorCode;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public boolean isSuccess() {
            return successCount > 0;
        }
    }
    
    /**
     * 系统邮件发送任务（用于跟踪长时间运行的任务）
     */
    private static class SystemMailTask {
        private final String taskId;
        private final int totalTargets;
        private int completedCount;
        private int successCount;
        private volatile boolean cancelled;
        
        public SystemMailTask(String taskId, int totalTargets) {
            this.taskId = taskId;
            this.totalTargets = totalTargets;
            this.completedCount = 0;
            this.successCount = 0;
            this.cancelled = false;
        }
        
        public void incrementCompleted(boolean success) {
            completedCount++;
            if (success) {
                successCount++;
            }
        }
        
        public void cancel() {
            this.cancelled = true;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public double getProgress() {
            return totalTargets > 0 ? (double) completedCount / totalTargets : 0;
        }
    }
}
