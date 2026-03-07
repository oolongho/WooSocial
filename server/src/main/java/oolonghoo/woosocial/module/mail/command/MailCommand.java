package com.oolonghoo.woosocial.module.mail.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.mail.MailDataManager;
import com.oolonghoo.woosocial.module.mail.MailManager;
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

public class MailCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final MailDataManager dataManager;
    private final MailManager mailManager;
    private final MessageManager messageManager;
    
    public MailCommand(WooSocial plugin, MailDataManager dataManager, MailManager mailManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.mailManager = mailManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            if (!player.hasPermission(Perms.MAIL)) {
                messageManager.send(player, "general.no-permission");
                return true;
            }
            mailManager.openMailListGUI(player, 1);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
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
            case "help":
                return handleHelp(player);
            default:
                messageManager.send(player, "mail.usage");
                return true;
        }
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
    
    private boolean handleHelp(Player player) {
        messageManager.send(player, "mail.help-header");
        messageManager.send(player, "mail.help-list");
        messageManager.send(player, "mail.help-send");
        messageManager.send(player, "mail.help-claim");
        messageManager.send(player, "mail.help-delete");
        if (player.hasPermission(Perms.MAIL_BULK)) {
            messageManager.send(player, "mail.help-bulk");
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("send", "list", "claim", "delete", "help"));
            if (sender.hasPermission(Perms.MAIL_BULK)) {
                subCommands.add("bulk");
            }
            
            String input = args[0].toLowerCase();
            subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .forEach(completions::add);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("send")) {
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
            }
        }
        
        return completions;
    }
}
