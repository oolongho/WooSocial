package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.database.ScheduledMailDAO;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.model.ScheduledMailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 定时邮件管理器
 * 负责定时邮件的创建、发送、取消等操作
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class ScheduledMailManager {
    
    private final WooSocial plugin;
    private final ScheduledMailDAO scheduledMailDAO;
    private final MailDataManager mailDataManager;
    private final MessageManager messageManager;
    
    /**
     * 检查定时邮件的任务
     */
    private BukkitRunnable checkTask;
    
    /**
     * 检查间隔（秒）
     */
    private int checkInterval;
    
    /**
     * 最大定时邮件数量（每个玩家）
     */
    private int maxScheduledPerPlayer;
    
    /**
     * 最大预约时间（天）
     */
    private int maxScheduleDays;
    
    /**
     * 时间格式解析器
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    /**
     * 相对时间格式正则：如 1h, 2d, 30m
     */
    private static final Pattern RELATIVE_TIME_PATTERN = Pattern.compile("^(\\d+)([hdm])$", Pattern.CASE_INSENSITIVE);
    
    public ScheduledMailManager(WooSocial plugin, MailDataManager mailDataManager) {
        this.plugin = plugin;
        this.scheduledMailDAO = new ScheduledMailDAO(plugin, plugin.getDatabaseManager());
        this.mailDataManager = mailDataManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 初始化定时邮件管理器
     */
    public void initialize() {
        loadConfig();
        startCheckTask();
        
        // 服务器启动时恢复待发送的定时邮件
        restorePendingMails();
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        checkInterval = plugin.getConfig().getInt("mail.scheduled.check-interval", 60);
        maxScheduledPerPlayer = plugin.getConfig().getInt("mail.scheduled.max-per-player", 10);
        maxScheduleDays = plugin.getConfig().getInt("mail.scheduled.max-days", 30);
    }
    
    /**
     * 启动检查任务
     */
    private void startCheckTask() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndSendScheduledMails();
            }
        };
        
        // 使用异步任务检查
        checkTask.runTaskTimerAsynchronously(plugin, checkInterval * 20L, checkInterval * 20L);
        
        plugin.getLogger().info("[ScheduledMail] 定时邮件检查任务已启动，检查间隔: " + checkInterval + "秒");
    }
    
    /**
     * 恢复待发送的定时邮件
     */
    private void restorePendingMails() {
        scheduledMailDAO.getPendingScheduledMails().thenAccept(mails -> {
            if (!mails.isEmpty()) {
                plugin.getLogger().info("[ScheduledMail] 恢复 " + mails.size() + " 封待发送的定时邮件");
            }
        });
    }
    
    /**
     * 检查并发送到期的定时邮件
     */
    private void checkAndSendScheduledMails() {
        scheduledMailDAO.getPendingScheduledMails().thenAccept(mails -> {
            for (ScheduledMailData mail : mails) {
                if (mail.isTimeToSend()) {
                    sendScheduledMail(mail);
                }
            }
        });
    }
    
    /**
     * 发送定时邮件
     * 
     * @param scheduledMail 定时邮件数据
     */
    private void sendScheduledMail(ScheduledMailData scheduledMail) {
        // 在主线程执行发送操作
        Bukkit.getScheduler().runTask(plugin, () -> {
            String attachments = scheduledMail.getAttachments();
            List<UUID> receiverUuids = scheduledMail.getReceiverUuids();
            
            if (receiverUuids == null || receiverUuids.isEmpty()) {
                scheduledMailDAO.updateStatus(scheduledMail.getId(), ScheduledMailData.Status.CANCELLED);
                return;
            }
            
            // 解析附件
            ItemStack item = null;
            if (attachments != null && !attachments.isEmpty()) {
                item = ItemSerializer.deserialize(attachments);
            }
            
            if (item == null || item.getType() == Material.AIR) {
                scheduledMailDAO.updateStatus(scheduledMail.getId(), ScheduledMailData.Status.CANCELLED);
                plugin.getLogger().warning("[ScheduledMail] 定时邮件 " + scheduledMail.getId() + " 附件无效，已取消");
                return;
            }
            
            final ItemStack finalItem = item;
            
            // 发送邮件给所有接收者
            int successCount = 0;
            for (UUID receiverUuid : receiverUuids) {
                String receiverName = Bukkit.getOfflinePlayer(receiverUuid).getName();
                if (receiverName == null) {
                    continue;
                }
                
                // 异步发送邮件
                mailDataManager.sendMail(
                        scheduledMail.getSenderUuid(),
                        scheduledMail.getSenderName(),
                        receiverUuid,
                        receiverName,
                        finalItem.clone()
                ).thenAccept(result -> {
                    if (result.isSuccess()) {
                        // 通知接收者
                        Player receiver = Bukkit.getPlayer(receiverUuid);
                        if (receiver != null && receiver.isOnline()) {
                            messageManager.send(receiver, "mail.receive-notify",
                                    "player", scheduledMail.getSenderName(),
                                    "item", ItemSerializer.getItemDisplayName(finalItem));
                        }
                    }
                });
                
                successCount++;
            }
            
            // 更新状态为已发送
            scheduledMailDAO.updateStatus(scheduledMail.getId(), ScheduledMailData.Status.SENT);
            
            // 通知发送者
            Player sender = Bukkit.getPlayer(scheduledMail.getSenderUuid());
            if (sender != null && sender.isOnline()) {
                messageManager.send(sender, "mail.scheduled.sent",
                        "count", String.valueOf(successCount));
            }
            
            plugin.getLogger().info("[ScheduledMail] 定时邮件 " + scheduledMail.getId() + " 已发送给 " + successCount + " 位玩家");
        });
    }
    
    /**
     * 创建定时邮件
     * 
     * @param sender 发送者
     * @param receiverNames 接收者名称列表
     * @param item 物品
     * @param timeStr 时间字符串
     * @return 是否创建成功
     */
    public CompletableFuture<CreateResult> createScheduledMail(Player sender, List<String> receiverNames, ItemStack item, String timeStr) {
        // 解析时间
        long scheduledTime;
        try {
            scheduledTime = parseTime(timeStr);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(new CreateResult(false, "invalid-time", e.getMessage()));
        }
        
        // 检查时间是否有效
        if (scheduledTime <= System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(new CreateResult(false, "time-in-past", null));
        }
        
        // 检查是否超过最大预约时间
        long maxTime = System.currentTimeMillis() + (maxScheduleDays * 24L * 60 * 60 * 1000);
        if (scheduledTime > maxTime) {
            return CompletableFuture.completedFuture(new CreateResult(false, "time-too-far", String.valueOf(maxScheduleDays)));
        }
        
        // 检查物品有效性
        if (item == null || item.getType() == Material.AIR) {
            return CompletableFuture.completedFuture(new CreateResult(false, "no-item", null));
        }
        
        // 检查物品大小
        int estimatedSize = ItemSerializer.estimateSize(item);
        if (estimatedSize > mailDataManager.getMaxItemSize()) {
            return CompletableFuture.completedFuture(new CreateResult(false, "item-too-large", null));
        }
        
        // 检查玩家定时邮件数量限制
        return scheduledMailDAO.getPendingCountBySender(sender.getUniqueId()).thenCompose(count -> {
            if (count >= maxScheduledPerPlayer) {
                return CompletableFuture.completedFuture(
                        new CreateResult(false, "limit-reached", String.valueOf(maxScheduledPerPlayer)));
            }
            
            // 解析接收者UUID
            List<UUID> receiverUuids = new ArrayList<>();
            List<String> validReceiverNames = new ArrayList<>();
            
            for (String name : receiverNames) {
                Player onlinePlayer = Bukkit.getPlayer(name);
                if (onlinePlayer != null) {
                    receiverUuids.add(onlinePlayer.getUniqueId());
                    validReceiverNames.add(name);
                } else {
                    // 尝试从数据库获取离线玩家UUID
                    UUID offlineUuid = plugin.getPlayerUuid(name);
                    if (offlineUuid != null) {
                        receiverUuids.add(offlineUuid);
                        validReceiverNames.add(name);
                    }
                }
            }
            
            if (receiverUuids.isEmpty()) {
                return CompletableFuture.completedFuture(new CreateResult(false, "no-receivers", null));
            }
            
            // 创建定时邮件数据
            ScheduledMailData scheduledMail = new ScheduledMailData(sender.getUniqueId(), scheduledTime);
            scheduledMail.setSenderName(sender.getName());
            scheduledMail.setReceiverUuids(receiverUuids);
            scheduledMail.setReceiverNames(String.join(", ", validReceiverNames));
            scheduledMail.setAttachments(ItemSerializer.serialize(item));
            
            // 保存到数据库
            return scheduledMailDAO.createScheduledMail(scheduledMail).thenApply(success -> {
                if (success) {
                    // 扣除物品
                    sender.getInventory().setItemInMainHand(null);
                    return new CreateResult(true, null, String.valueOf(scheduledMail.getId()));
                }
                return new CreateResult(false, "database-error", null);
            });
        });
    }
    
    /**
     * 取消定时邮件
     * 
     * @param player 玩家
     * @param mailId 邮件ID
     * @return 是否取消成功
     */
    public CompletableFuture<Boolean> cancelScheduledMail(Player player, int mailId) {
        return scheduledMailDAO.getScheduledMailById(mailId).thenCompose(mailOpt -> {
            if (mailOpt.isEmpty()) {
                messageManager.send(player, "mail.scheduled.not-found");
                return CompletableFuture.completedFuture(false);
            }
            
            ScheduledMailData mail = mailOpt.get();
            
            // 检查是否是发送者
            if (!mail.getSenderUuid().equals(player.getUniqueId())) {
                messageManager.send(player, "mail.scheduled.not-owner");
                return CompletableFuture.completedFuture(false);
            }
            
            // 检查状态
            if (mail.getStatus() != ScheduledMailData.Status.PENDING) {
                messageManager.send(player, "mail.scheduled.already-sent");
                return CompletableFuture.completedFuture(false);
            }
            
            return scheduledMailDAO.cancelScheduledMail(mailId, player.getUniqueId()).thenApply(success -> {
                if (success) {
                    messageManager.send(player, "mail.scheduled.cancel-success", "id", String.valueOf(mailId));
                    return true;
                } else {
                    messageManager.send(player, "mail.scheduled.cancel-failed");
                    return false;
                }
            });
        });
    }
    
    /**
     * 获取玩家的定时邮件列表
     * 
     * @param player 玩家
     * @return 定时邮件列表
     */
    public CompletableFuture<List<ScheduledMailData>> getPlayerScheduledMails(Player player) {
        return scheduledMailDAO.getPendingMailsBySender(player.getUniqueId());
    }
    
    /**
     * 解析时间字符串
     * 支持格式：
     * - 相对时间：1h（1小时后）、30m（30分钟后）、1d（1天后）
     * - 绝对时间：2024-12-31 12:00
     * 
     * @param timeStr 时间字符串
     * @return 时间戳
     * @throws IllegalArgumentException 如果时间格式无效
     */
    public long parseTime(String timeStr) throws IllegalArgumentException {
        // 尝试解析相对时间
        Matcher matcher = RELATIVE_TIME_PATTERN.matcher(timeStr.trim());
        if (matcher.matches()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            long millis;
            switch (unit) {
                case "m":
                    millis = amount * 60L * 1000;
                    break;
                case "h":
                    millis = amount * 60L * 60 * 1000;
                    break;
                case "d":
                    millis = amount * 24L * 60 * 60 * 1000;
                    break;
                default:
                    throw new IllegalArgumentException("未知的时间单位: " + unit);
            }
            
            return System.currentTimeMillis() + millis;
        }
        
        // 尝试解析绝对时间
        try {
            Date date = DATE_FORMAT.parse(timeStr.trim());
            return date.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("无法解析时间格式: " + timeStr + 
                    "。支持的格式: 1h（1小时后）、1d（1天后）、2024-12-31 12:00");
        }
    }
    
    /**
     * 格式化时间显示
     * 
     * @param timestamp 时间戳
     * @return 格式化的时间字符串
     */
    public String formatTime(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * 格式化剩余时间
     * 
     * @param remainingMillis 剩余毫秒数
     * @return 格式化的剩余时间字符串
     */
    public String formatRemainingTime(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "即将发送";
        }
        
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }
    
    /**
     * 关闭定时邮件管理器
     */
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
        startCheckTask();
    }
    
    public int getMaxScheduledPerPlayer() {
        return maxScheduledPerPlayer;
    }
    
    public int getMaxScheduleDays() {
        return maxScheduleDays;
    }
    
    /**
     * 创建结果
     */
    public static class CreateResult {
        private final boolean success;
        private final String errorCode;
        private final String data;
        
        public CreateResult(boolean success, String errorCode, String data) {
            this.success = success;
            this.errorCode = errorCode;
            this.data = data;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getData() {
            return data;
        }
    }
}
