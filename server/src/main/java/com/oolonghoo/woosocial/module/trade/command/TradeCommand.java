package com.oolonghoo.woosocial.module.trade.command;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.trade.TradeConfig;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.TradeRequestManager;
import com.oolonghoo.woosocial.module.trade.gui.TradeGUI;
import com.oolonghoo.woosocial.module.trade.model.TradeRequest;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.permission.Perms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 交易命令处理器
 */
public class TradeCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final TradeManager tradeManager;
    private final TradeRequestManager requestManager;
    private final TradeConfig config;
    private final MessageManager messageManager;
    
    public TradeCommand(WooSocial plugin, TradeManager tradeManager, TradeRequestManager requestManager, TradeConfig config) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.requestManager = requestManager;
        this.config = config;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        
        if (config.isRequirePermission() && !player.hasPermission(Perms.TRADE_USE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "accept":
            case "a":
                return handleAccept(player);
            case "deny":
            case "d":
                return handleDeny(player);
            case "toggle":
                return handleToggle(player);
            case "help":
                sendHelp(player);
                return true;
            default:
                return handleRequest(player, subCommand);
        }
    }
    
    private boolean handleRequest(Player player, String targetName) {
        UUID playerUuid = player.getUniqueId();
        
        if (tradeManager.isInTrade(playerUuid)) {
            messageManager.send(player, "trade.already-in-trade");
            return true;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            messageManager.send(player, "general.player-not-found", "player", targetName);
            return true;
        }
        
        if (target.equals(player)) {
            messageManager.send(player, "trade.cannot-self");
            return true;
        }
        
        if (tradeManager.isInTrade(target.getUniqueId())) {
            messageManager.send(player, "trade.target-in-trade", "player", target.getName());
            return true;
        }
        
        FriendDataManager friendManager = plugin.getFriendDataManager();
        boolean isFriend = friendManager != null && friendManager.areFriends(playerUuid, target.getUniqueId());
        
        if (!isFriend) {
            double distance = player.getLocation().distanceSquared(target.getLocation());
            double maxDistance = config.getFaceToFaceDistance() * config.getFaceToFaceDistance();
            
            if (distance > maxDistance) {
                messageManager.send(player, "trade.too-far", "distance", String.valueOf(config.getFaceToFaceDistance()));
                return true;
            }
        }
        
        if (requestManager.hasPendingRequest(playerUuid, target.getUniqueId())) {
            return handleAcceptFrom(player, target);
        }
        
        if (requestManager.isOnCooldown(playerUuid)) {
            messageManager.send(player, "trade.request-cooldown");
            return true;
        }
        
        boolean sent = requestManager.sendRequest(player, target, isFriend);
        if (!sent) {
            messageManager.send(player, "trade.request-failed");
            return true;
        }
        
        player.playSound(player.getLocation(), config.getSoundRequestSend(), 1.0f, 1.0f);
        messageManager.send(player, "trade.request-sent", "player", target.getName());
        
        target.playSound(target.getLocation(), config.getSoundRequestReceive(), 1.0f, 1.0f);
        messageManager.send(target, "trade.request-received", "player", player.getName());
        messageManager.send(target, "trade.request-hint");
        
        return true;
    }
    
    private boolean handleAcceptFrom(Player player, Player target) {
        TradeRequest request = requestManager.acceptRequest(player.getUniqueId());
        if (request == null) {
            messageManager.send(player, "trade.no-pending-request");
            return true;
        }
        
        TradeSession session = tradeManager.createSession(target, player);
        if (session == null) {
            messageManager.send(player, "trade.start-failed");
            return true;
        }
        
        TradeGUI playerGUI = new TradeGUI(plugin, tradeManager, config, player, session);
        TradeGUI targetGUI = new TradeGUI(plugin, tradeManager, config, target, session);
        
        player.openInventory(playerGUI.getInventory());
        target.openInventory(targetGUI.getInventory());
        
        return true;
    }
    
    private boolean handleAccept(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        if (tradeManager.isInTrade(playerUuid)) {
            messageManager.send(player, "trade.already-in-trade");
            return true;
        }
        
        TradeRequest request = requestManager.acceptRequest(playerUuid);
        if (request == null) {
            messageManager.send(player, "trade.no-pending-request");
            return true;
        }
        
        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender == null || !sender.isOnline()) {
            messageManager.send(player, "trade.sender-offline");
            return true;
        }
        
        if (tradeManager.isInTrade(sender.getUniqueId())) {
            messageManager.send(player, "trade.target-in-trade", "player", sender.getName());
            return true;
        }
        
        TradeSession session = tradeManager.createSession(sender, player);
        if (session == null) {
            messageManager.send(player, "trade.start-failed");
            return true;
        }
        
        TradeGUI playerGUI = new TradeGUI(plugin, tradeManager, config, player, session);
        TradeGUI senderGUI = new TradeGUI(plugin, tradeManager, config, sender, session);
        
        player.openInventory(playerGUI.getInventory());
        sender.openInventory(senderGUI.getInventory());
        
        return true;
    }
    
    private boolean handleDeny(Player player) {
        TradeRequest request = requestManager.denyRequest(player.getUniqueId());
        if (request == null) {
            messageManager.send(player, "trade.no-pending-request");
            return true;
        }
        
        messageManager.send(player, "trade.request-denied", "player", request.getSenderName());
        
        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender != null && sender.isOnline()) {
            messageManager.send(sender, "trade.request-denied-by", "player", player.getName());
        }
        
        return true;
    }
    
    private boolean handleToggle(Player player) {
        messageManager.send(player, "trade.toggle-hint");
        return true;
    }
    
    private void sendHelp(Player player) {
        messageManager.send(player, "trade.help.header");
        messageManager.send(player, "trade.help.request");
        messageManager.send(player, "trade.help.accept");
        messageManager.send(player, "trade.help.deny");
        messageManager.send(player, "trade.help.toggle");
        messageManager.send(player, "trade.help.footer");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            
            if ("accept".startsWith(prefix)) completions.add("accept");
            if ("deny".startsWith(prefix)) completions.add("deny");
            if ("toggle".startsWith(prefix)) completions.add("toggle");
            if ("help".startsWith(prefix)) completions.add("help");
            
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(sender) && onlinePlayer.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}
