package com.oolonghoo.woosocial.module.relation.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.IntimacyManager;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.oolonghoo.woosocial.module.relation.type.RelationType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RelationCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    private final IntimacyManager intimacyManager;
    private final MessageManager messageManager;
    
    public RelationCommand(WooSocial plugin, RelationDataManager dataManager, 
                          RelationManager relationManager, IntimacyManager intimacyManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
        this.intimacyManager = intimacyManager;
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
            if (!player.hasPermission(Perms.RELATION)) {
                messageManager.send(player, "general.no-permission");
                return true;
            }
            openRelationListGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleList(player);
            case "propose":
                return handlePropose(player, args);
            case "accept":
                return handleAccept(player, args);
            case "remove":
                return handleRemove(player, args);
            case "info":
                return handleInfo(player, args);
            case "types":
                return handleTypes(player);
            default:
                messageManager.send(player, "relation.usage");
                return true;
        }
    }
    
    private boolean handleList(Player player) {
        if (!player.hasPermission(Perms.RELATION)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        openRelationListGUI(player);
        return true;
    }
    
    private void openRelationListGUI(Player player) {
        dataManager.getRelationsForPlayer(player.getUniqueId()).thenAccept(relations -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (relations.isEmpty()) {
                    messageManager.send(player, "relation.no-relations");
                    return;
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append(messageManager.getRaw("relation.list-header"));
                
                for (RelationData relation : relations) {
                    String friendName = Bukkit.getOfflinePlayer(relation.getFriendUuid()).getName();
                    if (friendName == null) friendName = relation.getFriendUuid().toString().substring(0, 8);
                    
                    RelationType type = relationManager.getRelationType(relation.getRelationType());
                    String typeName = type != null ? type.getDisplayName() : relation.getRelationType();
                    
                    String line = messageManager.getRaw("relation.list-entry")
                            .replace("{friend}", friendName)
                            .replace("{type}", typeName)
                            .replace("{intimacy}", String.valueOf(relation.getIntimacy()));
                    sb.append("\n").append(line);
                }
                
                player.sendMessage(messageManager.parseColors(sb.toString()));
            });
        });
    }
    
    private boolean handlePropose(Player player, String[] args) {
        if (!player.hasPermission(Perms.RELATION_PROPOSE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            messageManager.send(player, "relation.propose-usage");
            return true;
        }
        
        String targetName = args[1];
        String typeName = args[2];
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            messageManager.send(player, "general.player-not-found");
            return true;
        }
        
        if (target.equals(player)) {
            messageManager.send(player, "relation.cannot-self");
            return true;
        }
        
        RelationType type = relationManager.getRelationType(typeName);
        if (type == null) {
            messageManager.send(player, "relation.type-not-found");
            return true;
        }
        
        relationManager.proposeRelation(player, target.getUniqueId(), type).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    messageManager.send(player, result.getMessageKey());
                    messageManager.send(target, "relation.proposal-received",
                            "{player}", player.getName(),
                            "{type}", type.getDisplayName());
                } else {
                    messageManager.send(player, result.getMessageKey());
                }
            });
        });
        
        return true;
    }
    
    private boolean handleAccept(Player player, String[] args) {
        if (!player.hasPermission(Perms.RELATION_ACCEPT)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "relation.accept-usage");
            return true;
        }
        
        String proposerName = args[1];
        Player proposer = Bukkit.getPlayer(proposerName);
        
        if (proposer == null) {
            messageManager.send(player, "general.player-not-found");
            return true;
        }
        
        relationManager.acceptRelation(player.getUniqueId(), proposer.getUniqueId()).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    messageManager.send(player, result.getMessageKey());
                    messageManager.send(proposer, "relation.accepted-by",
                            "{player}", player.getName());
                } else {
                    messageManager.send(player, result.getMessageKey());
                }
            });
        });
        
        return true;
    }
    
    private boolean handleRemove(Player player, String[] args) {
        if (!player.hasPermission(Perms.RELATION_REMOVE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "relation.remove-usage");
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        java.util.UUID targetUuid;
        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            targetUuid = plugin.getPlayerUuid(targetName);
            if (targetUuid == null) {
                messageManager.send(player, "general.player-not-found");
                return true;
            }
        }
        
        relationManager.removeRelation(player.getUniqueId(), targetUuid).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    messageManager.send(player, result.getMessageKey());
                    if (target != null) {
                        messageManager.send(target, "relation.removed-by",
                                "{player}", player.getName());
                    }
                } else {
                    messageManager.send(player, result.getMessageKey());
                }
            });
        });
        
        return true;
    }
    
    private boolean handleInfo(Player player, String[] args) {
        if (!player.hasPermission(Perms.RELATION)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            messageManager.send(player, "relation.info-usage");
            return true;
        }
        
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        
        java.util.UUID targetUuid;
        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            targetUuid = plugin.getPlayerUuid(targetName);
            if (targetUuid == null) {
                messageManager.send(player, "general.player-not-found");
                return true;
            }
        }
        
        dataManager.getRelation(player.getUniqueId(), targetUuid).thenAccept(optRelation -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (optRelation.isEmpty()) {
                    messageManager.send(player, "relation.not-found");
                    return;
                }
                
                RelationData relation = optRelation.get();
                RelationType type = relationManager.getRelationType(relation.getRelationType());
                String typeName = type != null ? type.getDisplayName() : relation.getRelationType();
                String levelName = intimacyManager.getIntimacyLevelName(relation.getIntimacy());
                
                messageManager.send(player, "relation.info-header", "{player}", targetName);
                messageManager.send(player, "relation.info-type", "{type}", typeName);
                messageManager.send(player, "relation.info-intimacy", 
                        "{intimacy}", String.valueOf(relation.getIntimacy()),
                        "{level}", levelName);
                messageManager.send(player, "relation.info-mutual",
                        "{mutual}", relation.isMutual() ? "是" : "否");
            });
        });
        
        return true;
    }
    
    private boolean handleTypes(Player player) {
        if (!player.hasPermission(Perms.RELATION)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        messageManager.send(player, "relation.types-header");
        for (RelationType type : relationManager.getAllRelationTypes()) {
            String info = messageManager.getRaw("relation.types-entry")
                    .replace("{name}", type.getDisplayName())
                    .replace("{id}", type.getId())
                    .replace("{intimacy}", String.valueOf(type.getRequiredIntimacy()))
                    .replace("{slots}", String.valueOf(type.getMaxSlots()));
            player.sendMessage(messageManager.parseColors(info));
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
            List<String> subCommands = Arrays.asList("list", "propose", "accept", "remove", "info", "types");
            String input = args[0].toLowerCase();
            subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .forEach(completions::add);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            if (subCommand.equals("propose") || subCommand.equals("remove") || subCommand.equals("info") || subCommand.equals("accept")) {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("propose")) {
                String input = args[2].toLowerCase();
                relationManager.getAllRelationTypes().stream()
                        .map(RelationType::getId)
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .forEach(completions::add);
            }
        }
        
        return completions;
    }
}
