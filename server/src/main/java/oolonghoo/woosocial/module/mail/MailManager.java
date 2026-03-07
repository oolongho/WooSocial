package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
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
    
    private int maxSendCount;
    private boolean notifyOnReceive;
    private boolean bulkEnabled;
    private String bulkPermission;
    
    public MailManager(WooSocial plugin, MailDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
        
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
        
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > dataManager.getMaxItemSize()) {
            messageManager.send(sender, "mail.item-too-large");
            return CompletableFuture.completedFuture(false);
        }
        
        ItemStack clonedItem = item.clone();
        
        return dataManager.sendMail(
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName(),
                clonedItem
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
            if (success) {
                messageManager.send(player, "mail.claim-success");
            } else {
                messageManager.send(player, "mail.claim-failed");
            }
            return success;
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
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MailData> mails = dataManager.getMailList(playerUuid);
            int totalPages = (int) Math.ceil((double) mails.size() / 45);
            if (totalPages < 1) totalPages = 1;
            final int finalTotalPages = totalPages;
            final List<MailData> finalMails = new ArrayList<>(mails);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                new com.oolonghoo.woosocial.module.mail.gui.MailListGUI(plugin, player, finalMails, finalPage, finalTotalPages).open(player);
            });
        });
    }
    
    public void openMailDetailGUI(Player player, int mailId) {
        final UUID playerUuid = player.getUniqueId();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataManager.getFullMailData(mailId).thenAccept(mail -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (mail != null && mail.getReceiverUuid().equals(playerUuid)) {
                        new com.oolonghoo.woosocial.module.mail.gui.MailDetailGUI(plugin, player, mail).open(player);
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
}
