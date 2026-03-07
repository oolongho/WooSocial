package com.oolonghoo.woosocial.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.friend.FriendDataManager;
import com.oolonghoo.woosocial.module.friend.FriendModule;
import com.oolonghoo.woosocial.module.teleport.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 传送命令处理器
 * 处理 /tpf 命令 - 传送到好友
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportCommand implements CommandExecutor, TabCompleter {
    
    private final WooSocial plugin;
    private final TeleportManager teleportManager;
    private final MessageManager messageManager;
    private FriendDataManager friendDataManager;
    
    public TeleportCommand(WooSocial plugin, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.teleportManager = teleportManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    private FriendDataManager getFriendDataManager() {
        if (friendDataManager == null) {
            FriendModule friendModule = (FriendModule) plugin.getModuleManager().getModule("friend");
            if (friendModule != null) {
                friendDataManager = friendModule.getDataManager();
            }
        }
        return friendDataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            handleHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("help") || subCommand.equals("?")) {
            handleHelp(player);
        } else {
            handleTeleportTo(player, args);
        }
        
        return true;
    }
    
    private void handleTeleportTo(Player player, String[] args) {
        if (!player.hasPermission(Perms.TELEPORT_TO)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        String targetName = args[0];
        
        if (targetName.equalsIgnoreCase(player.getName())) {
            messageManager.send(player, "teleport.cannot-teleport-self");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            messageManager.send(player, "teleport.target-not-online");
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        
        FriendDataManager fdm = getFriendDataManager();
        if (fdm == null || !fdm.isFriend(playerUuid, targetUuid)) {
            messageManager.send(player, "teleport.target-not-friend");
            return;
        }
        
        teleportManager.startTeleport(player, target);
    }
    
    private void handleHelp(Player player) {
        if (!player.hasPermission(Perms.HELP)) {
            messageManager.send(player, "general.no-permission");
            return;
        }
        
        messageManager.sendList(player, "help.header");
        messageManager.sendNoPrefix(player, "help.tpf");
        messageManager.sendList(player, "help.footer");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        FriendDataManager fdm = getFriendDataManager();
        
        if (args.length == 1) {
            completions.add("help");
            
            if (fdm != null) {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !p.equals(player))
                        .filter(p -> fdm.isFriend(player.getUniqueId(), p.getUniqueId()))
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .forEach(completions::add);
            }
        }
        
        return completions;
    }
}
