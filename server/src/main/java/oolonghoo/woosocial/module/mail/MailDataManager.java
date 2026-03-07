package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.MailDAO;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.sync.SyncManager;
import com.oolonghoo.woosocial.sync.SyncMessage;
import com.oolonghoo.woosocial.sync.SyncMessageType;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MailDataManager {
    
    private final WooSocial plugin;
    private final MailDAO mailDAO;
    
    private final Map<UUID, List<MailData>> mailCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> unreadCountCache = new ConcurrentHashMap<>();
    
    private int maxMailsPerPlayer;
    private int expireDays;
    private int maxItemSize;
    
    public MailDataManager(WooSocial plugin) {
        this.plugin = plugin;
        this.mailDAO = new MailDAO(plugin, plugin.getDatabaseManager());
    }
    
    public void initialize() {
        maxMailsPerPlayer = plugin.getConfig().getInt("mail.max-mails-per-player", 100);
        expireDays = plugin.getConfig().getInt("mail.expire-days", 30);
        maxItemSize = plugin.getConfig().getInt("mail.max-item-size", 32000);
        
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
        saveAll();
        mailCache.clear();
        unreadCountCache.clear();
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
        return mailCache.getOrDefault(playerUuid, new ArrayList<>());
    }
    
    public int getMailCount(UUID playerUuid) {
        List<MailData> mails = mailCache.get(playerUuid);
        return mails != null ? mails.size() : 0;
    }
    
    public int getUnreadCount(UUID playerUuid) {
        return unreadCountCache.getOrDefault(playerUuid, 0);
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
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > maxItemSize) {
            return CompletableFuture.completedFuture(new SendResult(false, "item-too-large"));
        }
        
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return CompletableFuture.completedFuture(new SendResult(false, "serialize-failed"));
        }
        
        MailData mail = new MailData(senderUuid, receiverUuid);
        mail.setSenderName(senderName);
        mail.setReceiverName(receiverName);
        mail.setItemData(itemData);
        mail.setExpireTime(System.currentTimeMillis() + (expireDays * 24L * 60 * 60 * 1000));
        
        return mailDAO.createMail(mail).thenApply(success -> {
            if (success) {
                loadMails(receiverUuid);
                notifyReceiver(receiverUuid);
                broadcastMailNew(receiverUuid);
                return new SendResult(true, null);
            }
            return new SendResult(false, "database-error");
        });
    }
    
    public CompletableFuture<BulkSendResult> sendBulkMail(UUID senderUuid, String senderName,
                                                   List<UUID> receiverUuids,
                                                   ItemStack item, String bulkId) {
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > maxItemSize) {
            return CompletableFuture.completedFuture(new BulkSendResult(0, receiverUuids.size(), "item-too-large"));
        }
        
        String itemData = ItemSerializer.serialize(item);
        if (itemData == null) {
            return CompletableFuture.completedFuture(new BulkSendResult(0, receiverUuids.size(), "serialize-failed"));
        }
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (UUID receiverUuid : receiverUuids) {
            String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
            if (receiverName == null) continue;
            
            MailData mail = new MailData(senderUuid, receiverUuid);
            mail.setSenderName(senderName);
            mail.setReceiverName(receiverName);
            mail.setItemData(itemData);
            mail.setExpireTime(System.currentTimeMillis() + (expireDays * 24L * 60 * 60 * 1000));
            mail.setBulk(true);
            mail.setBulkId(bulkId);
            
            futures.add(mailDAO.createMail(mail).thenApply(success -> {
                if (success) {
                    loadMails(receiverUuid);
                    notifyReceiver(receiverUuid);
                    broadcastMailNew(receiverUuid);
                }
                return success;
            }));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int successes = (int) futures.stream().filter(f -> f.join()).count();
                    return new BulkSendResult(successes, receiverUuids.size(), null);
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
            
            ItemStack item = ItemSerializer.deserialize(mail.getItemData());
            if (item == null) {
                return CompletableFuture.completedFuture(false);
            }
            
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                return CompletableFuture.completedFuture(false);
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
    
    public MailDAO getMailDAO() {
        return mailDAO;
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
