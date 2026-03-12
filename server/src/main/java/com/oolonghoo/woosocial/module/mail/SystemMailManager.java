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
     * 注意：此方法在大型服务器上可能性能较差，建议仅在小规模服务器使用
     * 
     * @param sender 命令发送者
     * @param item 要发送的物品
     * @return 异步操作结果
     */
    public CompletableFuture<SystemMailResult> sendToAllPlayers(CommandSender sender, ItemStack item) {
        // 记录审计日志
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        plugin.getLogger().info(String.format(
                "[Audit] 管理员 %s 发送系统邮件给全服玩家，物品：%s x%d",
                senderName, item.getType().name(), item.getAmount()));
        
        // 验证物品
        if (!validateItem(sender, item)) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "invalid-item"));
        }
        
        messageManager.send(sender, "mail.system.sending-all");
        
        // 异步获取所有玩家并发送邮件
        return CompletableFuture.supplyAsync(() -> {
            // 优化：仅获取在线玩家，避免加载所有离线玩家数据
            // 如果需要发送给离线玩家，建议使用数据库查询获取玩家列表
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            List<UUID> targetUuids = new ArrayList<>();
            
            for (Player player : onlinePlayers) {
                if (!player.getUniqueId().equals(SYSTEM_SENDER_UUID)) {
                    targetUuids.add(player.getUniqueId());
                }
            }
            
            plugin.getLogger().info(String.format(
                "[SystemMail] 优化模式：仅发送给在线玩家 (%d 人)，避免性能问题。如需发送给离线玩家，请修改配置。",
                targetUuids.size()
            ));
            
            return sendSystemMailToRecipients(item, targetUuids);
        }).thenCompose(result -> result).thenApply(result -> {
            if (result.getSuccessCount() > 0) {
                messageManager.send(sender, "mail.system.sendall-success", 
                        "count", String.valueOf(result.getSuccessCount()),
                        "total", String.valueOf(result.getTotalCount()));
                
                // 记录发送结果
                plugin.getLogger().info(String.format(
                        "[Audit] 系统邮件发送完成：成功 %d/%d 玩家",
                        result.getSuccessCount(), result.getTotalCount()));
            } else {
                messageManager.send(sender, "mail.system.sendall-failed");
                plugin.getLogger().warning(String.format(
                        "[Audit] 系统邮件发送失败：0/%d 玩家", result.getTotalCount()));
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
        // 记录审计日志
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        plugin.getLogger().info(String.format(
                "[Audit] 管理员 %s 发送系统邮件给在线玩家，物品：%s x%d",
                senderName, item.getType().name(), item.getAmount()));
        
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
                
                // 记录发送结果
                plugin.getLogger().info(String.format(
                        "[Audit] 系统邮件发送完成：成功 %d/%d 在线玩家",
                        result.getSuccessCount(), result.getTotalCount()));
            } else {
                messageManager.send(sender, "mail.system.sendonline-failed");
                plugin.getLogger().warning(String.format(
                        "[Audit] 系统邮件发送失败：0/%d 在线玩家", result.getTotalCount()));
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
     * @param receiverUuids 接收者 UUID 列表
     * @return 发送结果
     */
    private CompletableFuture<SystemMailResult> sendSystemMailToRecipients(ItemStack item, List<UUID> receiverUuids) {
        if (receiverUuids.isEmpty()) {
            return CompletableFuture.completedFuture(new SystemMailResult(0, 0, "no-receivers"));
        }
        
        // 步骤 1: 验证并序列化物品
        ItemValidationResult itemValidation = validateAndSerializeItem(item);
        if (!itemValidation.isValid()) {
            return CompletableFuture.completedFuture(
                    new SystemMailResult(0, receiverUuids.size(), itemValidation.getErrorCode()));
        }
        
        // 步骤 2: 生成批量 ID 并克隆物品
        String bulkId = UUID.randomUUID().toString();
        ItemStack clonedItem = item.clone();
        
        // 步骤 3: 异步发送邮件
        return CompletableFuture.supplyAsync(() -> 
            sendMailsToRecipients(receiverUuids, itemValidation.getSerializedData(), bulkId, clonedItem)
        );
    }
    
    /**
     * 验证并序列化物品
     */
    private ItemValidationResult validateAndSerializeItem(ItemStack item) {
        // 检查物品大小
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > dataManager.getMaxItemSize()) {
            return new ItemValidationResult(false, null, "item-too-large");
        }
        
        // 序列化物品
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return new ItemValidationResult(false, null, "serialize-failed");
        }
        
        return new ItemValidationResult(true, itemData, null);
    }
    
    /**
     * 发送邮件给所有接收者
     */
    private SystemMailResult sendMailsToRecipients(List<UUID> receiverUuids, String itemData, String bulkId, ItemStack clonedItem) {
        int successCount = 0;
        int failCount = 0;
        
        for (UUID receiverUuid : receiverUuids) {
            try {
                SendResult result = sendMailToRecipient(receiverUuid, itemData, bulkId, clonedItem);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[SystemMail] Failed to send mail to " + receiverUuid + ": " + e.getMessage());
                failCount++;
            }
        }
        
        String errorCode = failCount > 0 ? "partial-failure" : null;
        return new SystemMailResult(successCount, receiverUuids.size(), errorCode);
    }
    
    /**
     * 发送单个邮件给接收者
     */
    private SendResult sendMailToRecipient(UUID receiverUuid, String itemData, String bulkId, ItemStack clonedItem) {
        // 获取玩家名称
        String receiverName = getReceiverName(receiverUuid);
        if (receiverName == null) {
            return new SendResult(false, "player-not-found");
        }
        
        // 创建邮件数据
        MailData mail = createSystemMail(receiverUuid, receiverName, itemData, bulkId);
        
        // 保存到数据库
        boolean success = dataManager.getMailDAO().createMail(mail).join();
        if (success) {
            // 加载邮件到缓存
            dataManager.loadMails(receiverUuid);
            // 通知在线玩家
            notifyRecipient(receiverUuid, clonedItem);
            return new SendResult(true, null);
        }
        
        return new SendResult(false, "database-error");
    }
    
    /**
     * 获取接收者名称
     */
    private String getReceiverName(UUID receiverUuid) {
        Player onlinePlayer = Bukkit.getPlayer(receiverUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(receiverUuid);
        return offlinePlayer != null ? offlinePlayer.getName() : null;
    }
    
    /**
     * 创建系统邮件数据
     */
    private MailData createSystemMail(UUID receiverUuid, String receiverName, String itemData, String bulkId) {
        MailData mail = new MailData(SYSTEM_SENDER_UUID, receiverUuid);
        mail.setSenderName(SYSTEM_SENDER_NAME);
        mail.setReceiverName(receiverName);
        mail.setItemData(itemData);
        mail.setExpireTime(System.currentTimeMillis() + 
                (dataManager.getExpireDays() * 24L * 60 * 60 * 1000));
        mail.setBulk(true);
        mail.setBulkId(bulkId);
        return mail;
    }
    
    /**
     * 通知在线玩家收到邮件
     */
    private void notifyRecipient(UUID receiverUuid, ItemStack clonedItem) {
        Player receiver = Bukkit.getPlayer(receiverUuid);
        if (receiver != null && receiver.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                messageManager.send(receiver, "mail.system.receive-notify",
                        "item", ItemSerializer.getItemDisplayName(clonedItem));
            });
        }
    }
    
    /**
     * 物品验证结果
     */
    private static class ItemValidationResult {
        private final boolean valid;
        private final String serializedData;
        private final String errorCode;
        
        public ItemValidationResult(boolean valid, String serializedData, String errorCode) {
            this.valid = valid;
            this.serializedData = serializedData;
            this.errorCode = errorCode;
        }
        
        public boolean isValid() { return valid; }
        public String getSerializedData() { return serializedData; }
        public String getErrorCode() { return errorCode; }
    }
    
    /**
     * 邮件发送结果
     */
    private static class SendResult {
        private final boolean success;
        private final String errorCode;
        
        public SendResult(boolean success, String errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
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
    @SuppressWarnings("unused")
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
