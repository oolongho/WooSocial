package com.oolonghoo.woosocial.module.relation.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.relation.GiftManager;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.oolonghoo.woosocial.module.relation.type.GiftType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GiftCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    @SuppressWarnings("unused")
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private final GiftManager giftManager;
    private final MessageManager messageManager;
    
    public GiftCommand(WooSocial plugin, RelationDataManager dataManager,
                      RelationManager relationManager, GiftManager giftManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
        this.giftManager = giftManager;
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
            if (!player.hasPermission(Perms.GIFT)) {
                messageManager.send(player, "general.no-permission");
                return true;
            }
            openGiftShopGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "coins":
                return handleCoins(player, args);
            case "send":
                return handleSend(player, args);
            case "shop":
                return handleShop(player);
            case "list":
                return handleList(player);
            default:
                messageManager.send(player, "gift.usage");
                return true;
        }
    }
    
    private boolean handleCoins(Player player, String[] args) {
        if (!player.hasPermission(Perms.GIFT_COINS)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "gift.coins-usage");
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageManager.send(player, "general.player-not-found");
            return true;
        }
        
        if (target.equals(player)) {
            messageManager.send(player, "gift.cannot-self");
            return true;
        }
        
        GiftType coinsGift = relationManager.getGiftType("coins");
        if (coinsGift == null) {
            messageManager.send(player, "gift.not-found");
            return true;
        }
        
        int remaining = giftManager.getRemainingDailyLimit(player, target.getUniqueId(), "coins");
        
        if (remaining == -1) {
            messageManager.send(player, "gift.remaining-daily", "count", "∞");
        } else {
            messageManager.send(player, "gift.remaining-daily", "count", String.valueOf(remaining));
        }
        
        giftManager.sendCoins(player, target.getUniqueId()).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    messageManager.send(player, result.getMessageKey(),
                            "amount", String.valueOf(result.getValue()),
                            "player", target.getName(),
                            "intimacy", String.valueOf(result.getIntimacyGained()));
                    messageManager.send(target, "gift.coins-received",
                            "amount", String.valueOf(result.getValue()),
                            "player", player.getName());
                } else {
                    messageManager.send(player, result.getMessageKey(), 
                            "value", String.valueOf(result.getValue()));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleSend(Player player, String[] args) {
        if (!player.hasPermission(Perms.GIFT_SHOP)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            messageManager.send(player, "gift.send-usage");
            return true;
        }
        
        String targetName = args[1];
        String giftId = args[2];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            messageManager.send(player, "general.player-not-found");
            return true;
        }
        
        if (target.equals(player)) {
            messageManager.send(player, "gift.cannot-self");
            return true;
        }
        
        GiftType giftType = relationManager.getGiftType(giftId);
        if (giftType == null) {
            messageManager.send(player, "gift.not-found");
            return true;
        }
        
        giftManager.sendGift(player, target.getUniqueId(), giftId).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    messageManager.send(player, result.getMessageKey(),
                            "gift", giftType.getName(),
                            "player", target.getName(),
                            "intimacy", String.valueOf(result.getIntimacyGained()));
                    messageManager.send(target, "gift.received",
                            "gift", giftType.getName(),
                            "player", player.getName());
                } else {
                    messageManager.send(player, result.getMessageKey(),
                            "value", String.valueOf(result.getValue()));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleShop(Player player) {
        if (!player.hasPermission(Perms.GIFT_SHOP)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        openGiftShopGUI(player);
        return true;
    }
    
    private boolean handleList(Player player) {
        if (!player.hasPermission(Perms.GIFT)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        messageManager.send(player, "gift.list-header");
        for (GiftType gift : relationManager.getAllGiftTypes()) {
            String cost = "";
            if (gift.getCostCoins() > 0) {
                cost += gift.getCostCoins() + "金币";
            }
            if (gift.getCostPoints() > 0) {
                if (!cost.isEmpty()) cost += " + ";
                cost += gift.getCostPoints() + "点券";
            }
            if (cost.isEmpty()) {
                cost = "免费";
            }
            
            String limitInfo = "";
            if (gift.hasDailyLimit()) {
                limitInfo = " &7(每日" + gift.getDailyLimit() + "次)";
            }
            
            if (gift.isCoinsGift()) {
                limitInfo = " &7(每次" + gift.getAmountPerSend() + "金币)";
            }
            
            String info = messageManager.getRaw("gift.list-entry")
                    .replace("{name}", gift.getName())
                    .replace("{id}", gift.getId())
                    .replace("{intimacy}", String.valueOf(gift.getIntimacy()))
                    .replace("{cost}", cost) + limitInfo;
            player.sendMessage(messageManager.parseColors(info));
        }
        
        return true;
    }
    
    private void openGiftShopGUI(Player player) {
        messageManager.send(player, "gift.shop-open");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("coins", "send", "shop", "list");
            String input = args[0].toLowerCase();
            subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .forEach(completions::add);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            if (subCommand.equals("coins") || subCommand.equals("send")) {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("send")) {
                String input = args[2].toLowerCase();
                relationManager.getAllGiftTypes().stream()
                        .map(GiftType::getId)
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            }
        }
        
        return completions;
    }
}
