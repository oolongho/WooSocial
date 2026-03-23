package com.oolonghoo.woosocial.module.trade.command;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.TradeConfig;
import com.oolonghoo.woosocial.module.trade.TradeEconomyManager;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.TradeRequestManager;
import com.oolonghoo.woosocial.module.trade.gui.TradeGUI;
import com.oolonghoo.woosocial.module.trade.model.TradeRequest;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final TradeManager tradeManager;
    private final TradeRequestManager requestManager;
    private final TradeConfig config;
    private final TradeEconomyManager economyManager;
    private final MessageManager messageManager;
    
    private final Map<UUID, Boolean> tradeToggle = new HashMap<>();
    
    public TradeCommand(WooSocial plugin, TradeManager tradeManager, TradeRequestManager requestManager, 
                        TradeConfig config, TradeEconomyManager economyManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.requestManager = requestManager;
        this.config = config;
        this.economyManager = economyManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (config.isRequirePermission() && !player.hasPermission(Perms.TRADE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "accept", "a" -> handleAccept(player);
            case "deny", "d" -> handleDeny(player);
            case "toggle", "t" -> handleToggle(player);
            case "help", "?" -> {
                sendHelp(player);
                yield true;
            }
            default -> handleRequest(player, subCommand);
        };
    }
    
    private boolean handleRequest(Player player, String targetName) {
        UUID playerUuid = player.getUniqueId();
        
        if (tradeManager.isInTrade(playerUuid)) {
            messageManager.send(player, "trade.already-in-trade");
            return true;
        }
        
        if (requestManager.hasPendingRequest(playerUuid)) {
            messageManager.send(player, "trade.has-pending-request");
            return true;
        }
        
        if (requestManager.isOnCooldown(playerUuid)) {
            messageManager.send(player, "trade.cooldown");
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
        
        UUID targetUuid = target.getUniqueId();
        
        if (tradeManager.isInTrade(targetUuid)) {
            messageManager.send(player, "trade.target-in-trade", "player", target.getName());
            return true;
        }
        
        if (isTradeDisabled(targetUuid)) {
            messageManager.send(player, "trade.target-disabled", "player", target.getName());
            return true;
        }
        
        boolean isFriend = isFriend(playerUuid, targetUuid);
        boolean isRemote = isFriend;
        
        if (!isFriend) {
            double distance = player.getLocation().distanceSquared(target.getLocation());
            double maxDistance = config.getFaceToFaceDistance() * config.getFaceToFaceDistance();
            
            if (distance > maxDistance) {
                messageManager.send(player, "trade.too-far", "distance", String.valueOf(config.getFaceToFaceDistance()));
                return true;
            }
        }
        
        if (requestManager.sendRequest(player, target, isRemote)) {
            player.playSound(player.getLocation(), config.getSoundRequestSend(), 1.0f, 1.0f);
            target.playSound(target.getLocation(), config.getSoundRequestReceive(), 1.0f, 1.0f);
            
            messageManager.send(player, "trade.request-sent", "player", target.getName());
            messageManager.send(target, "trade.request-received", "player", player.getName());
            
            messageManager.send(target, "trade.request-hint");
        } else {
            messageManager.send(player, "trade.request-failed");
        }
        
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
        
        if (tradeManager.isInTrade(request.getSenderUuid())) {
            messageManager.send(player, "trade.target-in-trade", "player", request.getSenderName());
            return true;
        }
        
        TradeSession session = tradeManager.createSession(sender, player);
        if (session == null) {
            messageManager.send(player, "trade.session-failed");
            return true;
        }
        
        TradeGUI senderGUI = TradeGUI.create(plugin, tradeManager, config, economyManager, sender, session);
        TradeGUI receiverGUI = TradeGUI.create(plugin, tradeManager, config, economyManager, player, session);
        
        sender.openInventory(senderGUI.getInventory());
        player.openInventory(receiverGUI.getInventory());
        
        return true;
    }
    
    private boolean handleDeny(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        TradeRequest request = requestManager.denyRequest(playerUuid);
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
        UUID playerUuid = player.getUniqueId();
        
        boolean currentState = isTradeDisabled(playerUuid);
        tradeToggle.put(playerUuid, !currentState);
        
        if (currentState) {
            messageManager.send(player, "trade.toggle-enabled");
        } else {
            messageManager.send(player, "trade.toggle-disabled");
        }
        
        return true;
    }
    
    private boolean isTradeDisabled(UUID playerUuid) {
        return tradeToggle.getOrDefault(playerUuid, false);
    }
    
    private boolean isFriend(UUID player1, UUID player2) {
        try {
            return plugin.getFriendDAO().isFriend(player1, player2).join();
        } catch (Exception e) {
            return false;
        }
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
            String input = args[0].toLowerCase();
            
            if ("accept".startsWith(input)) completions.add("accept");
            if ("deny".startsWith(input)) completions.add("deny");
            if ("toggle".startsWith(input)) completions.add("toggle");
            if ("help".startsWith(input)) completions.add("help");
            
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}
