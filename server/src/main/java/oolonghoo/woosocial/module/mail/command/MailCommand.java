package com.oolonghoo.woosocial.module.mail.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.model.ScheduledMailData;
import com.oolonghoo.woosocial.module.mail.MailDataManager;
import com.oolonghoo.woosocial.module.mail.MailManager;
import com.oolonghoo.woosocial.module.mail.ScheduledMailManager;
import com.oolonghoo.woosocial.module.mail.SystemMailManager;
import com.oolonghoo.woosocial.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MailCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final MailDataManager dataManager;
    private final MailManager mailManager;
    private final SystemMailManager systemMailManager;
    private final ScheduledMailManager scheduledMailManager;
    private final MessageManager messageManager;
    
    public MailCommand(WooSocial plugin, MailDataManager dataManager, MailManager mailManager, 
                       SystemMailManager systemMailManager, ScheduledMailManager scheduledMailManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.mailManager = mailManager;
        this.systemMailManager = systemMailManager;
        this.scheduledMailManager = scheduledMailManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 无参数时打开邮箱GUI（仅玩家）
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                messageManager.send(sender, "general.player-only");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission(Perms.MAIL)) {
                messageManager.send(player, "general.no-permission");
                return true;
            }
            mailManager.openMailListGUI(player, 1);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // 系统邮件命令（支持控制台执行）
        switch (subCommand) {
            case "sendall":
                return handleSendAll(sender, args);
            case "sendonline":
                return handleSendOnline(sender, args);
        }
        
        // 其他命令仅玩家执行
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        switch (subCommand) {
            case "send":
                return handleSend(player, args);
            case "list":
                return handleList(player, args);
            case "claim":
                return handleClaim(player, args);
            case "delete":
                return handleDelete(player, args);
            case "bulk":
                return handleBulk(player, args);
            case "schedule":
                return handleSchedule(player, args);
            case "cancelschedule":
                return handleCancelSchedule(player, args);
            case "listschedule":
                return handleListSchedule(player);
            case "help":
                return handleHelp(player);
            default:
                messageManager.send(player, "mail.usage");
                return true;
        }
    }
    
    /**
     * 处理发送系统邮件给所有玩家（包括离线玩家）
     * 支持控制台执行
     */
    private boolean handleSendAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perms.MAIL_SENDALL)) {
            messageManager.send(sender, "general.no-permission");
            return true;
        }
        
        // 获取物品
        ItemStack item;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                messageManager.send(sender, "mail.no-item-in-hand");
                return true;
            }
        } else {
            // 控制台执行时，需要从参数获取物品
            messageManager.send(sender, "mail.system.console-item-required");
            return true;
        }
        
        systemMailManager.sendToAllPlayers(sender, item.clone());
        return true;
    }
    
    /**
     * 处理发送系统邮件给在线玩家
     * 支持控制台执行
     */
    private boolean handleSendOnline(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Perms.MAIL_SENDONLINE)) {
            messageManager.send(sender, "general.no-permission");
            return true;
        }
        
        // 获取物品
        ItemStack item;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                messageManager.send(sender, "mail.no-item-in-hand");
                return true;
            }
        } else {
            // 控制台执行时，需要从参数获取物品
            messageManager.send(sender, "mail.system.console-item-required");
            return true;
        }
        
        systemMailManager.sendToOnlinePlayers(sender, item.clone());
        return true;
    }
    
    private boolean handleSend(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_SEND)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "mail.send-usage");
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageManager.send(player, "general.player-not-found");
            return true;
        }
        
        if (target.equals(player)) {
            messageManager.send(player, "mail.cannot-send-self");
            return true;
        }
        
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            messageManager.send(player, "mail.no-item-in-hand");
            return true;
        }
        
        mailManager.sendMail(player, target, handItem).thenAccept(success -> {
            if (success) {
                player.getInventory().setItemInMainHand(null);
            }
        });
        
        return true;
    }
    
    private boolean handleList(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                messageManager.send(player, "general.invalid-page");
                return true;
            }
        }
        
        mailManager.openMailListGUI(player, page);
        return true;
    }
    
    private boolean handleClaim(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_CLAIM)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "mail.claim-usage");
            return true;
        }
        
        int mailId;
        try {
            mailId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            messageManager.send(player, "mail.invalid-mail-id");
            return true;
        }
        
        mailManager.claimMail(player, mailId);
        return true;
    }
    
    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_DELETE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "mail.delete-usage");
            return true;
        }
        
        int mailId;
        try {
            mailId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            messageManager.send(player, "mail.invalid-mail-id");
            return true;
        }
        
        if (args.length >= 3 && args[2].equalsIgnoreCase("force")) {
            if (player.hasPermission(Perms.MAIL_ADMIN)) {
                mailManager.forceDeleteMail(player, mailId);
            } else {
                messageManager.send(player, "general.no-permission");
            }
        } else {
            mailManager.deleteMail(player, mailId);
        }
        
        return true;
    }
    
    private boolean handleBulk(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_BULK)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (!mailManager.isBulkEnabled()) {
            messageManager.send(player, "mail.bulk-disabled");
            return true;
        }
        
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            messageManager.send(player, "mail.no-item-in-hand");
            return true;
        }
        
        List<Player> receivers = new ArrayList<>();
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            receivers.addAll(Bukkit.getOnlinePlayers());
            receivers.remove(player);
        } else {
            receivers.addAll(Bukkit.getOnlinePlayers());
            receivers.remove(player);
        }
        
        if (receivers.isEmpty()) {
            messageManager.send(player, "mail.no-receivers");
            return true;
        }
        
        mailManager.sendBulkMail(player, receivers, handItem).thenAccept(success -> {
            if (success) {
                player.getInventory().setItemInMainHand(null);
            }
        });
        
        return true;
    }
    
    /**
     * 处理创建定时邮件
     * 用法: /mail schedule <时间> <玩家> [玩家2...]
     */
    private boolean handleSchedule(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_SCHEDULE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            messageManager.send(player, "mail.schedule-usage");
            return true;
        }
        
        String timeStr = args[1];
        
        // 收集所有接收者名称
        List<String> receiverNames = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            receiverNames.add(args[i]);
        }
        
        // 检查物品
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            messageManager.send(player, "mail.no-item-in-hand");
            return true;
        }
        
        // 创建定时邮件
        scheduledMailManager.createScheduledMail(player, receiverNames, handItem, timeStr)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        messageManager.send(player, "mail.schedule-created",
                                "id", result.getData(),
                                "time", scheduledMailManager.formatTime(
                                        scheduledMailManager.parseTime(timeStr)));
                    } else {
                        switch (result.getErrorCode()) {
                            case "invalid-time":
                                messageManager.send(player, "mail.schedule-invalid-time",
                                        "reason", result.getData() != null ? result.getData() : "");
                                break;
                            case "time-in-past":
                                messageManager.send(player, "mail.schedule-time-past");
                                break;
                            case "time-too-far":
                                messageManager.send(player, "mail.schedule-time-too-far",
                                        "days", result.getData());
                                break;
                            case "no-item":
                                messageManager.send(player, "mail.no-item-in-hand");
                                break;
                            case "item-too-large":
                                messageManager.send(player, "mail.item-too-large");
                                break;
                            case "limit-reached":
                                messageManager.send(player, "mail.schedule-limit-reached",
                                        "max", result.getData());
                                break;
                            case "no-receivers":
                                messageManager.send(player, "mail.no-receivers");
                                break;
                            default:
                                messageManager.send(player, "mail.schedule-failed");
                                break;
                        }
                    }
                });
        
        return true;
    }
    
    /**
     * 处理取消定时邮件
     * 用法: /mail cancelschedule <id>
     */
    private boolean handleCancelSchedule(Player player, String[] args) {
        if (!player.hasPermission(Perms.MAIL_SCHEDULE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "mail.cancelschedule-usage");
            return true;
        }
        
        int mailId;
        try {
            mailId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            messageManager.send(player, "mail.invalid-mail-id");
            return true;
        }
        
        scheduledMailManager.cancelScheduledMail(player, mailId);
        return true;
    }
    
    /**
     * 处理列出定时邮件
     * 用法: /mail listschedule
     */
    private boolean handleListSchedule(Player player) {
        if (!player.hasPermission(Perms.MAIL_SCHEDULE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        scheduledMailManager.getPlayerScheduledMails(player).thenAccept(mails -> {
            if (mails.isEmpty()) {
                messageManager.send(player, "mail.schedule-list-empty");
                return;
            }
            
            messageManager.send(player, "mail.schedule-list-header");
            for (ScheduledMailData mail : mails) {
                String itemName = "物品";
                if (mail.getAttachments() != null) {
                    ItemStack item = com.oolonghoo.woosocial.util.ItemSerializer.deserialize(mail.getAttachments());
                    if (item != null) {
                        itemName = ItemSerializer.getItemDisplayName(item);
                    }
                }
                
                String remaining = scheduledMailManager.formatRemainingTime(mail.getRemainingTime());
                String receivers = mail.getReceiverNames() != null ? mail.getReceiverNames() : "未知";
                
                messageManager.send(player, "mail.schedule-list-entry",
                        "id", String.valueOf(mail.getId()),
                        "item", itemName,
                        "receivers", receivers,
                        "time", scheduledMailManager.formatTime(mail.getScheduledTime()),
                        "remaining", remaining);
            }
        });
        
        return true;
    }
    
    private boolean handleHelp(Player player) {
        messageManager.send(player, "mail.help-header");
        messageManager.send(player, "mail.help-list");
        messageManager.send(player, "mail.help-send");
        messageManager.send(player, "mail.help-claim");
        messageManager.send(player, "mail.help-delete");
        if (player.hasPermission(Perms.MAIL_BULK)) {
            messageManager.send(player, "mail.help-bulk");
        }
        if (player.hasPermission(Perms.MAIL_SCHEDULE)) {
            messageManager.send(player, "mail.help-schedule");
            messageManager.send(player, "mail.help-cancelschedule");
            messageManager.send(player, "mail.help-listschedule");
        }
        if (player.hasPermission(Perms.MAIL_SENDALL)) {
            messageManager.send(player, "mail.help-sendall");
        }
        if (player.hasPermission(Perms.MAIL_SENDONLINE)) {
            messageManager.send(player, "mail.help-sendonline");
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("send", "list", "claim", "delete", "help"));
            
            // 系统邮件命令
            if (sender.hasPermission(Perms.MAIL_SENDALL)) {
                subCommands.add("sendall");
            }
            if (sender.hasPermission(Perms.MAIL_SENDONLINE)) {
                subCommands.add("sendonline");
            }
            if (sender.hasPermission(Perms.MAIL_BULK)) {
                subCommands.add("bulk");
            }
            // 定时邮件命令
            if (sender.hasPermission(Perms.MAIL_SCHEDULE)) {
                subCommands.add("schedule");
                subCommands.add("cancelschedule");
                subCommands.add("listschedule");
            }
            
            String input = args[0].toLowerCase();
            subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .forEach(completions::add);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("send")) {
                // 玩家名称补全
                String input = args[1].toLowerCase();
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            } else if (subCommand.equals("bulk") && sender.hasPermission(Perms.MAIL_BULK)) {
                String input = args[1].toLowerCase();
                Arrays.asList("all").stream()
                        .filter(s -> s.startsWith(input))
                        .forEach(completions::add);
            } else if (subCommand.equals("schedule") && sender.hasPermission(Perms.MAIL_SCHEDULE)) {
                // 时间格式提示
                String input = args[1].toLowerCase();
                Arrays.asList("1h", "2h", "1d", "7d").stream()
                        .filter(s -> s.startsWith(input))
                        .forEach(completions::add);
            }
        } else if (args.length >= 3) {
            String subCommand = args[0].toLowerCase();
            
            // schedule 命令的玩家名称补全
            if (subCommand.equals("schedule") && sender.hasPermission(Perms.MAIL_SCHEDULE)) {
                String input = args[args.length - 1].toLowerCase();
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            }
        }
        
        return completions;
    }
}
