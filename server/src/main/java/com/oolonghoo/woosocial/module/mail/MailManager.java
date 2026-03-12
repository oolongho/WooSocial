package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.LoadingState;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MailManager {
    
    private final WooSocial plugin;
    private final MailDataManager dataManager;
    private final MessageManager messageManager;
    private final LoadingState loadingState;
    
    private int maxSendCount;
    private boolean notifyOnReceive;
    private boolean bulkEnabled;
    private String bulkPermission;
    
    public MailManager(WooSocial plugin, MailDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
        this.loadingState = new LoadingState();
        
        loadConfig();
    }
    
    private void loadConfig() {
        maxSendCount = plugin.getConfig().getInt("mail.max-send-count", 10);
        notifyOnReceive = plugin.getConfig().getBoolean("mail.notify-on-receive", true);
        bulkEnabled = plugin.getConfig().getBoolean("mail.bulk.enabled", true);
        bulkPermission = plugin.getConfig().getString("mail.bulk.permission", "woosocial.mail.bulk");
    }
    
    public CompletableFuture<Boolean> sendMail(Player sender, Player receiver, ItemStack item) {
        if (!ItemSerializer.isValidItem(item)) {
            messageManager.send(sender, "mail.invalid-item");
            return CompletableFuture.completedFuture(false);
        }
        
        if (item.getAmount() > maxSendCount) {
            messageManager.send(sender, "mail.too-many-items", "max", String.valueOf(maxSendCount));
            return CompletableFuture.completedFuture(false);
        }
        
        int currentCount = dataManager.getMailCount(receiver.getUniqueId());
        if (currentCount >= dataManager.getMaxMailsPerPlayer()) {
            messageManager.send(sender, "mail.receiver-mailbox-full");
            return CompletableFuture.completedFuture(false);
        }
        
        // 优化：只序列化一次，避免重复计算
        String serializedItem = ItemSerializer.serialize(item);
        if (serializedItem == null) {
            messageManager.send(sender, "mail.serialize-failed");
            return CompletableFuture.completedFuture(false);
        }
        
        if (serializedItem.length() > dataManager.getMaxItemSize()) {
            messageManager.send(sender, "mail.item-too-large");
            return CompletableFuture.completedFuture(false);
        }
        
        ItemStack clonedItem = item.clone();
        
        return dataManager.sendMail(
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName(),
                clonedItem,
                serializedItem  // 传递已序列化的物品数据
        ).thenApply(result -> {
            if (result.isSuccess()) {
                messageManager.send(sender, "mail.send-success", 
                        "player", receiver.getName(),
                        "item", ItemSerializer.getItemDisplayName(clonedItem));
                
                if (receiver.isOnline() && notifyOnReceive) {
                    messageManager.send(receiver, "mail.receive-notify",
                            "player", sender.getName(),
                            "item", ItemSerializer.getItemDisplayName(clonedItem));
                }
            } else {
                String errorCode = result.getErrorCode();
                if ("item-too-large".equals(errorCode)) {
                    messageManager.send(sender, "mail.item-too-large");
                } else {
                    messageManager.send(sender, "mail.send-failed");
                }
            }
            return result.isSuccess();
        });
    }
    
    public CompletableFuture<Boolean> sendBulkMail(Player sender, List<Player> receivers, ItemStack item) {
        if (!bulkEnabled) {
            messageManager.send(sender, "mail.bulk-disabled");
            return CompletableFuture.completedFuture(false);
        }
        
        if (!sender.hasPermission(bulkPermission)) {
            messageManager.send(sender, "general.no-permission");
            return CompletableFuture.completedFuture(false);
        }
        
        if (!ItemSerializer.isValidItem(item)) {
            messageManager.send(sender, "mail.invalid-item");
            return CompletableFuture.completedFuture(false);
        }
        
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > dataManager.getMaxItemSize()) {
            messageManager.send(sender, "mail.item-too-large");
            return CompletableFuture.completedFuture(false);
        }
        
        ItemStack clonedItem = item.clone();
        String bulkId = UUID.randomUUID().toString();
        List<UUID> receiverUuids = new ArrayList<>();
        
        for (Player receiver : receivers) {
            receiverUuids.add(receiver.getUniqueId());
        }
        
        messageManager.send(sender, "mail.bulk-sending", "count", String.valueOf(receiverUuids.size()));
        
        return dataManager.sendBulkMail(
                sender.getUniqueId(),
                sender.getName(),
                receiverUuids,
                clonedItem,
                bulkId
        ).thenApply(result -> {
            if (result.getErrorCode() != null) {
                messageManager.send(sender, "mail.bulk-failed");
                return false;
            }
            messageManager.send(sender, "mail.bulk-success", 
                    "count", String.valueOf(result.getSuccessCount()));
            return result.getSuccessCount() > 0;
        });
    }
    
    public CompletableFuture<Boolean> claimMail(Player player, int mailId) {
        return dataManager.claimMail(player.getUniqueId(), mailId).thenApply(success -> {
            // 清除处理中状态
            loadingState.clearLoading(player.getUniqueId());
            
            if (success) {
                messageManager.send(player, "mail.claim-success");
            } else {
                messageManager.send(player, "mail.claim-failed");
            }
            return success;
        }).exceptionally(throwable -> {
            // 发生异常时也要清除状态
            loadingState.clearLoading(player.getUniqueId());
            messageManager.send(player, "mail.claim-failed");
            return false;
        });
    }
    
    public CompletableFuture<Boolean> deleteMail(Player player, int mailId) {
        Optional<MailData> mailOpt = dataManager.getMailById(player.getUniqueId(), mailId);
        
        if (mailOpt.isEmpty()) {
            messageManager.send(player, "mail.not-found");
            return CompletableFuture.completedFuture(false);
        }
        
        MailData mail = mailOpt.get();
        
        if (!mail.isClaimed()) {
            messageManager.send(player, "mail.delete-unclaimed");
            return CompletableFuture.completedFuture(false);
        }
        
        return dataManager.deleteMail(player.getUniqueId(), mailId).thenApply(success -> {
            if (success) {
                messageManager.send(player, "mail.delete-success");
            } else {
                messageManager.send(player, "mail.delete-failed");
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> forceDeleteMail(Player player, int mailId) {
        return dataManager.deleteMail(player.getUniqueId(), mailId).thenApply(success -> {
            if (success) {
                messageManager.send(player, "mail.delete-success");
            } else {
                messageManager.send(player, "mail.delete-failed");
            }
            return success;
        });
    }
    
    public CompletableFuture<Boolean> markAsRead(Player player, int mailId) {
        return dataManager.markAsRead(player.getUniqueId(), mailId);
    }
    
    public void openMailListGUI(Player player, int page) {
        final int finalPage = page;
        final UUID playerUuid = player.getUniqueId();
        
        // 设置加载状态
        loadingState.setLoading(playerUuid, true);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MailData> mails = dataManager.getMailList(playerUuid);
            int totalPages = (int) Math.ceil((double) mails.size() / 45);
            if (totalPages < 1) totalPages = 1;
            final int finalTotalPages = totalPages;
            final List<MailData> finalMails = new ArrayList<>(mails);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 清除加载状态
                loadingState.clearLoading(playerUuid);
                new com.oolonghoo.woosocial.module.mail.gui.MailListGUI(plugin, player, finalMails, finalPage, finalTotalPages, loadingState).open(player);
            });
        });
    }
    
    public void openMailDetailGUI(Player player, int mailId) {
        final UUID playerUuid = player.getUniqueId();
        
        // 设置加载状态
        loadingState.setLoading(playerUuid, true);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataManager.getFullMailData(mailId).thenAccept(mail -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // 清除加载状态
                    loadingState.clearLoading(playerUuid);
                    
                    if (mail != null && mail.getReceiverUuid().equals(playerUuid)) {
                        new com.oolonghoo.woosocial.module.mail.gui.MailDetailGUI(plugin, player, mail, loadingState).open(player);
                    } else {
                        messageManager.send(player, "mail.not-found");
                    }
                });
            });
        });
    }
    
    private boolean hasInventorySpace(Player player, ItemStack item) {
        int emptySlots = 0;
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots >= 1;
    }
    
    public int getMaxSendCount() {
        return maxSendCount;
    }
    
    public boolean isBulkEnabled() {
        return bulkEnabled;
    }
    
    public String getBulkPermission() {
        return bulkPermission;
    }
    
    /**
     * 获取加载状态管理器
     * 
     * @return LoadingState实例
     */
    public LoadingState getLoadingState() {
        return loadingState;
    }
    
    /**
     * 发送包含多个物品的邮件
     * 
     * @param sender 发送者
     * @param receiverUuid 收件人UUID
     * @param receiverName 收件人名称
     * @param items 物品列表
     * @return 是否成功
     */
    public CompletableFuture<Boolean> sendMailWithItems(Player sender, UUID receiverUuid, String receiverName, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            messageManager.send(sender, "mail.no-item");
            return CompletableFuture.completedFuture(false);
        }
        
        int currentCount = dataManager.getMailCount(receiverUuid);
        if (currentCount + items.size() > dataManager.getMaxMailsPerPlayer()) {
            messageManager.send(sender, "mail.receiver-mailbox-full");
            return CompletableFuture.completedFuture(false);
        }
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                int estimatedSize = ItemSerializer.estimateSize(item);
                if (estimatedSize > dataManager.getMaxItemSize()) {
                    messageManager.send(sender, "mail.item-too-large");
                    continue;
                }
                
                ItemStack clonedItem = item.clone();
                CompletableFuture<Boolean> future = dataManager.sendMail(
                        sender.getUniqueId(),
                        sender.getName(),
                        receiverUuid,
                        receiverName,
                        clonedItem
                ).thenApply(result -> {
                    if (result.isSuccess()) {
                        return true;
                    } else {
                        return false;
                    }
                });
                futures.add(future);
            }
        }
        
        if (futures.isEmpty()) {
            messageManager.send(sender, "mail.no-item");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int successCount = 0;
                    for (CompletableFuture<Boolean> f : futures) {
                        try {
                            if (f.get()) successCount++;
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    
                    if (successCount > 0) {
                        messageManager.send(sender, "mail.send-success-multiple",
                                "count", String.valueOf(successCount),
                                "player", receiverName);
                        
                        Player receiver = Bukkit.getPlayer(receiverUuid);
                        if (receiver != null && notifyOnReceive) {
                            messageManager.send(receiver, "mail.receive-notify-multiple",
                                    "player", sender.getName(),
                                    "count", String.valueOf(successCount));
                        }
                        return true;
                    }
                    return false;
                });
    }
    
    /**
     * 打开发送邮件 GUI（指定收件人）
     * 
     * @param player 发送者
     * @param receiverUuid 收件人UUID
     * @param receiverName 收件人名称
     */
    public void openSendMailGUI(Player player, UUID receiverUuid, String receiverName) {
        new com.oolonghoo.woosocial.module.mail.gui.SendMailGUI(plugin, player, receiverUuid, receiverName, loadingState).open(player);
    }
    
    /**
     * 领取所有未领取的邮件附件（异步操作）
     * 
     * @param player 玩家
     * @return 领取的邮件数量
     */
    public CompletableFuture<Integer> claimAllMails(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        return CompletableFuture.supplyAsync(() -> {
            List<MailData> mails = dataManager.getMailList(playerUuid);
            
            int claimedCount = 0;
            List<ItemStack> itemsToGive = new ArrayList<>();
            
            for (MailData mail : mails) {
                if (!mail.isClaimed() && mail.getItemData() != null && !mail.getItemData().isEmpty()) {
                    ItemStack item = ItemSerializer.deserialize(mail.getItemData());
                    if (item != null && item.getType() != Material.AIR) {
                        itemsToGive.add(item);
                        dataManager.claimMail(playerUuid, mail.getId());
                        claimedCount++;
                    }
                }
            }
            
            final int count = claimedCount;
            final List<ItemStack> items = itemsToGive;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!items.isEmpty() && player.isOnline()) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), item);
                    }
                }
            });
            
            return count;
        });
    }
    
    /**
     * 删除所有已读且已领取的邮件（异步操作）
     * 
     * @param player 玩家
     * @return 删除的邮件数量
     */
    public CompletableFuture<Integer> deleteReadMails(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        return CompletableFuture.supplyAsync(() -> {
            List<MailData> mails = dataManager.getMailList(playerUuid);
            
            int deletedCount = 0;
            
            for (MailData mail : mails) {
                if (mail.isRead() && (mail.isClaimed() || mail.getItemData() == null || mail.getItemData().isEmpty())) {
                    dataManager.deleteMail(playerUuid, mail.getId());
                    deletedCount++;
                }
            }
            
            return deletedCount;
        });
    }
}
