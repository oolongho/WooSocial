package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.MailDAO;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import com.oolonghoo.woosocial.sync.SyncMessageType;
import com.oolonghoo.woosocial.util.ItemSerializer;
import com.oolonghoo.woosocial.event.MailSendEvent;
import com.oolonghoo.woosocial.event.MailClaimEvent;
import com.oolonghoo.woosocial.event.MailDeleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;

import com.oolonghoo.woosocial.util.LRUCache;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MailDataManager {
    
    private final WooSocial plugin;
    private final MailDAO mailDAO;
    
    private LRUCache<UUID, List<MailData>> mailCache;
    private LRUCache<UUID, Integer> unreadCountCache;
    
    private int maxMailsPerPlayer;
    private int expireDays;
    private int maxItemSize;
    
    /**
     * Handling method when inventory space is insufficient during mail claim.
     * deny - Reject claim, mail is preserved
     * drop - Add to inventory and drop excess items on the ground
     */
    private String claimSpaceInsufficient;
    
    public MailDataManager(WooSocial plugin) {
        this.plugin = plugin;
        this.mailDAO = new MailDAO(plugin, plugin.getDatabaseManager());
    }
    
    public void initialize() {
        maxMailsPerPlayer = plugin.getConfig().getInt("mail.max-mails-per-player", 100);
        expireDays = plugin.getConfig().getInt("mail.expire-days", 30);
        maxItemSize = plugin.getConfig().getInt("mail.max-item-size", 32000);
        claimSpaceInsufficient = plugin.getConfig().getString("mail.claim-space-insufficient", "deny");
        
        // 初始化 LRU 缓存
        int mailMaxSize = plugin.getConfig().getInt("cache.mail-max-size", 1000);
        mailCache = new LRUCache<>(mailMaxSize);
        unreadCountCache = new LRUCache<>(mailMaxSize);
        
        // Validate configuration value
        if (!"deny".equals(claimSpaceInsufficient) && !"drop".equals(claimSpaceInsufficient)) {
            plugin.getLogger().warning("[Mail] Invalid claim-space-insufficient value: " + claimSpaceInsufficient + ", using default 'deny'");
            claimSpaceInsufficient = "deny";
        }
        
        setupSyncHandler();
    }
    
    private void setupSyncHandler() {
        SyncManager syncManager = plugin.getSyncManager();
        if (syncManager != null && syncManager.isInitialized()) {
            syncManager.setMessageHandler(this::handleSyncMessage);
        }
    }
    
    private void handleSyncMessage(SyncMessage message) {
        if (message.getType() == SyncMessageType.MAIL_NEW) {
            String receiverUuidStr = message.getString("receiver_uuid");
            if (receiverUuidStr != null) {
                UUID receiverUuid = UUID.fromString(receiverUuidStr);
                loadMails(receiverUuid);
                notifyReceiver(receiverUuid);
            }
        } else if (message.getType() == SyncMessageType.MAIL_CLAIMED || 
                   message.getType() == SyncMessageType.MAIL_DELETED) {
            String receiverUuidStr = message.getString("receiver_uuid");
            if (receiverUuidStr != null) {
                UUID receiverUuid = UUID.fromString(receiverUuidStr);
                loadMails(receiverUuid);
            }
        }
    }
    
    public void shutdown() {
        // 输出缓存统计信息
        if (mailCache != null) {
        }
        saveAll();
        if (mailCache != null) {
            mailCache.clear();
        }
        if (unreadCountCache != null) {
            unreadCountCache.clear();
        }
    }
    
    public void saveAll() {
    }
    
    public CompletableFuture<Void> loadMails(UUID playerUuid) {
        return mailDAO.getMailsForReceiver(playerUuid).thenAccept(mails -> {
            List<MailData> lightweightMails = new ArrayList<>();
            for (MailData mail : mails) {
                MailData light = new MailData(mail.getId(), mail.getSenderUuid(), mail.getReceiverUuid(), mail.getSendTime());
                light.setSenderName(mail.getSenderName());
                light.setReceiverName(mail.getReceiverName());
                light.setExpireTime(mail.getExpireTime());
                light.setRead(mail.isRead());
                light.setClaimed(mail.isClaimed());
                light.setBulk(mail.isBulk());
                light.setBulkId(mail.getBulkId());
                lightweightMails.add(light);
            }
            mailCache.put(playerUuid, lightweightMails);
            updateUnreadCount(playerUuid, lightweightMails);
        });
    }
    
    private void updateUnreadCount(UUID playerUuid, List<MailData> mails) {
        int count = (int) mails.stream().filter(m -> !m.isRead()).count();
        unreadCountCache.put(playerUuid, count);
    }
    
    public List<MailData> getMailList(UUID playerUuid) {
        List<MailData> mails = mailCache.get(playerUuid);
        return mails != null ? mails : new ArrayList<>();
    }
    
    public int getMailCount(UUID playerUuid) {
        List<MailData> mails = mailCache.get(playerUuid);
        return mails != null ? mails.size() : 0;
    }
    
    public int getUnreadCount(UUID playerUuid) {
        Integer count = unreadCountCache.get(playerUuid);
        return count != null ? count : 0;
    }
    
    public Optional<MailData> getMailById(UUID playerUuid, int mailId) {
        List<MailData> mails = mailCache.get(playerUuid);
        if (mails != null) {
            return mails.stream().filter(m -> m.getId() == mailId).findFirst();
        }
        return Optional.empty();
    }
    
    public CompletableFuture<MailData> getFullMailData(int mailId) {
        return mailDAO.getMailById(mailId).thenApply(opt -> opt.orElse(null));
    }
    
    public CompletableFuture<SendResult> sendMail(UUID senderUuid, String senderName, 
                                               UUID receiverUuid, String receiverName, 
                                               ItemStack item) {
        // 优化：使用已序列化的物品数据，避免重复序列化
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return CompletableFuture.completedFuture(new SendResult(false, "serialize-failed"));
        }
        
        if (itemData.length() > maxItemSize) {
            return CompletableFuture.completedFuture(new SendResult(false, "item-too-large"));
        }
        
        return sendMail(senderUuid, senderName, receiverUuid, receiverName, item, itemData);
    }
    
    /**
     * 发送邮件（使用已序列化的物品数据，避免重复序列化）
     * 
     * @param senderUuid 发送者 UUID
     * @param senderName 发送者名称
     * @param receiverUuid 接收者 UUID
     * @param receiverName 接收者名称
     * @param item 物品（用于事件和通知）
     * @param serializedItemData 已序列化的物品数据
     * @return 发送结果
     */
    public CompletableFuture<SendResult> sendMail(UUID senderUuid, String senderName, 
                                               UUID receiverUuid, String receiverName, 
                                               ItemStack item, String serializedItemData) {
        
        MailData mail = new MailData(senderUuid, receiverUuid);
        mail.setSenderName(senderName);
        mail.setReceiverName(receiverName);
        mail.setItemData(serializedItemData);
        mail.setExpireTime(System.currentTimeMillis() + (expireDays * 24L * 60 * 60 * 1000));
        
        MailData finalMail = mail;
        return CompletableFuture.supplyAsync(() -> {
            MailSendEvent event = new MailSendEvent(senderUuid, senderName, receiverUuid, receiverName, finalMail);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return new SendResult(false, event.getCancelReason());
            }
            return null;
        }).thenCompose(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            
            MailData eventMail = finalMail;
            return mailDAO.createMail(eventMail).thenApply(success -> {
                if (success) {
                    loadMails(receiverUuid);
                    notifyReceiver(receiverUuid);
                    broadcastMailNew(receiverUuid);
                    return new SendResult(true, null);
                }
                return new SendResult(false, "database-error");
            });
        });
    }
    
    public CompletableFuture<BulkSendResult> sendBulkMail(UUID senderUuid, String senderName,
                                                   List<UUID> receiverUuids,
                                                   ItemStack item, String bulkId) {
        long startTime = System.currentTimeMillis();
        
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > maxItemSize) {
            return CompletableFuture.completedFuture(new BulkSendResult(0, receiverUuids.size(), "item-too-large"));
        }
        
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return CompletableFuture.completedFuture(new BulkSendResult(0, receiverUuids.size(), "serialize-failed"));
        }
        
        // 准备所有邮件数据
        List<MailData> mailList = new ArrayList<>();
        long expireTime = System.currentTimeMillis() + (expireDays * 24L * 60 * 60 * 1000);
        
        for (UUID receiverUuid : receiverUuids) {
            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
            if (receiverName == null) continue;
            
            MailData mail = new MailData(senderUuid, receiverUuid);
            mail.setSenderName(senderName);
            mail.setReceiverName(receiverName);
            mail.setItemData(itemData);
            mail.setExpireTime(expireTime);
            mail.setBulk(true);
            mail.setBulkId(bulkId);
            
            mailList.add(mail);
        }
        
        if (mailList.isEmpty()) {
            return CompletableFuture.completedFuture(new BulkSendResult(0, receiverUuids.size(), "no-valid-receivers"));
        }
        
        // 使用批量插入
        return mailDAO.bulkInsertMails(mailList).thenApply(successCount -> {
            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("[Mail] 批量发送邮件完成: 成功 " + successCount + "/" + mailList.size() + 
                    ", 总耗时 " + elapsed + "ms");
            
            // 更新缓存并通知接收者
            for (MailData mail : mailList) {
                // 只有成功插入的邮件才更新缓存（通过检查ID是否已设置）
                if (mail.getId() > 0) {
                    loadMails(mail.getReceiverUuid());
                    notifyReceiver(mail.getReceiverUuid());
                    broadcastMailNew(mail.getReceiverUuid());
                }
            }
            
            return new BulkSendResult(successCount, receiverUuids.size(), null);
        });
    }
    
    public CompletableFuture<Boolean> claimMail(UUID playerUuid, int mailId) {
        return mailDAO.getMailById(mailId).thenCompose(mailOpt -> {
            if (mailOpt.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            
            MailData mail = mailOpt.get();
            if (mail.isClaimed()) {
                return CompletableFuture.completedFuture(false);
            }
            
            if (!mail.getReceiverUuid().equals(playerUuid)) {
                return CompletableFuture.completedFuture(false);
            }
            
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 触发邮件领取事件
            MailClaimEvent event = new MailClaimEvent(playerUuid, player.getName(), mail);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }
            
            ItemStack item = ItemSerializer.deserialize(mail.getItemData());
            if (item == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            // In deny mode, check if there's enough inventory space first
            if ("deny".equals(claimSpaceInsufficient)) {
                if (!hasInventorySpace(player, item)) {
                    return CompletableFuture.completedFuture(false);
                }
            }
            
            // Try to add item to player's inventory
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            
            // Handle inventory space insufficient based on configuration
            if (!leftover.isEmpty()) {
                // Drop mode: Drop excess items on the ground
                Location playerLocation = player.getLocation();
                for (ItemStack dropItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(playerLocation, dropItem);
                }
            }
            
            return mailDAO.markAsClaimed(mailId).thenApply(success -> {
                if (success) {
                    loadMails(playerUuid);
                    broadcastMailClaimed(playerUuid);
                }
                return success;
            });
        });
    }
    
    /**
     * Check if player has enough inventory space for the given item.
     * @param player The player to check
     * @param item The item to check space for
     * @return true if there's enough space, false otherwise
     */
    private boolean hasInventorySpace(Player player, ItemStack item) {
        // Create a copy of the item to test
        ItemStack testItem = item.clone();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(testItem);
        
        // Remove the test item
        player.getInventory().removeItem(testItem);
        
        // If there's no leftover, there's enough space
        return leftover.isEmpty();
    }
    
    public CompletableFuture<Boolean> markAsRead(UUID playerUuid, int mailId) {
        Optional<MailData> mailOpt = getMailById(playerUuid, mailId);
        if (mailOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        MailData mail = mailOpt.get();
        if (mail.isRead()) {
            return CompletableFuture.completedFuture(true);
        }
        
        mail.setRead(true);
        return mailDAO.markAsRead(mailId).thenApply(success -> {
            if (success) {
                loadMails(playerUuid);
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> deleteMail(UUID playerUuid, int mailId) {
        Optional<MailData> mailOpt = getMailById(playerUuid, mailId);
        if (mailOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        MailData mail = mailOpt.get();
        
        // 触发邮件删除事件（不可取消）
        Player player = Bukkit.getPlayer(playerUuid);
        String playerName = player != null ? player.getName() : "Unknown";
        MailDeleteEvent event = new MailDeleteEvent(playerUuid, playerName, mail, MailDeleteEvent.DeleteReason.PLAYER_DELETE);
        Bukkit.getPluginManager().callEvent(event);
        
        return mailDAO.deleteMail(mailId).thenApply(success -> {
            if (success) {
                loadMails(playerUuid);
                broadcastMailDeleted(playerUuid);
            }
            return success;
        });
    }
    
    public void cleanExpiredMails() {
        mailDAO.cleanExpiredMails().thenAccept(count -> {
            if (count > 0) {
                plugin.getLogger().info("[Mail] 清理了 " + count + " 封过期邮件");
            }
        });
    }
    
    private void notifyReceiver(UUID receiverUuid) {
        Player player = Bukkit.getPlayer(receiverUuid);
        if (player != null && player.isOnline()) {
            int unread = getUnreadCount(receiverUuid);
            if (unread > 0) {
                plugin.getMessageManager().send(player, "mail.new-mail-notify", "count", String.valueOf(unread));
            }
        }
    }
    
    private void broadcastMailNew(UUID receiverUuid) {
        SyncManager syncManager = plugin.getSyncManager();
        if (syncManager != null && syncManager.isInitialized()) {
            SyncMessage message = new SyncMessage(SyncMessageType.MAIL_NEW, syncManager.getServerName())
                    .set("receiver_uuid", receiverUuid.toString());
            syncManager.broadcast(message);
        }
    }
    
    private void broadcastMailClaimed(UUID receiverUuid) {
        SyncManager syncManager = plugin.getSyncManager();
        if (syncManager != null && syncManager.isInitialized()) {
            SyncMessage message = new SyncMessage(SyncMessageType.MAIL_CLAIMED, syncManager.getServerName())
                    .set("receiver_uuid", receiverUuid.toString());
            syncManager.broadcast(message);
        }
    }
    
    private void broadcastMailDeleted(UUID receiverUuid) {
        SyncManager syncManager = plugin.getSyncManager();
        if (syncManager != null && syncManager.isInitialized()) {
            SyncMessage message = new SyncMessage(SyncMessageType.MAIL_DELETED, syncManager.getServerName())
                    .set("receiver_uuid", receiverUuid.toString());
            syncManager.broadcast(message);
        }
    }
    
    public void onPlayerJoin(Player player) {
        loadMails(player.getUniqueId());
    }
    
    public void onPlayerQuit(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (Bukkit.getPlayer(playerUuid) == null) {
                mailCache.remove(playerUuid);
                unreadCountCache.remove(playerUuid);
            }
        }, 20L * 60);
    }
    
    public int getMaxMailsPerPlayer() {
        return maxMailsPerPlayer;
    }
    
    public int getExpireDays() {
        return expireDays;
    }
    
    public int getMaxItemSize() {
        return maxItemSize;
    }
    
    /**
     * Get the handling method for insufficient inventory space during mail claim.
     * @return "deny" or "drop"
     */
    public String getClaimSpaceInsufficient() {
        return claimSpaceInsufficient;
    }
    
    public MailDAO getMailDAO() {
        return mailDAO;
    }
    
    /**
     * 获取邮件缓存统计信息
     * @return 缓存统计字符串
     */
    public String getCacheStatistics() {
        if (mailCache != null) {
            return mailCache.getStatistics();
        }
        return "LRUCache[not initialized]";
    }
    
    /**
     * 预热缓存 - 加载指定玩家的邮件数据
     * @param playerUuid 玩家UUID
     * @return CompletableFuture
     */
    public CompletableFuture<Void> warmupCache(UUID playerUuid) {
        return loadMails(playerUuid);
    }
    
    public static class SendResult {
        private final boolean success;
        private final String errorCode;
        
        public SendResult(boolean success, String errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
    
    public static class BulkSendResult {
        private final int successCount;
        private final int totalCount;
        private final String errorCode;
        
        public BulkSendResult(int successCount, int totalCount, String errorCode) {
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
    }
}
