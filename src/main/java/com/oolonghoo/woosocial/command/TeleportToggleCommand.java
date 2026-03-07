package com.oolonghoo.woosocial.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.teleport.TeleportDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 传送权限切换命令处理器
 * 处理 /tpftoggle 和 /tpftoggle <玩家> 命令
 *
 * @author oolongho
 * @since 1.0.0
 */
public class TeleportToggleCommand implements CommandExecutor, TabCompleter {
    
    private final TeleportDataManager dataManager;
    private final MessageManager messageManager;
    
    public TeleportToggleCommand(WooSocial plugin, TeleportDataManager dataManager) {
        this.dataManager = dataManager;
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission(Perms.TELEPORT_TOGGLE)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        if (args.length == 0) {
            boolean newState = dataManager.toggleFriendTeleport(playerUuid);
            
            if (newState) {
                messageManager.send(player, "teleport.toggle-teleport-on");
            } else {
                messageManager.send(player, "teleport.toggle-teleport-off");
            }
        } else {
            String targetName = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
            
            if (targetPlayer.getUniqueId().equals(playerUuid)) {
                messageManager.send(player, "teleport.cannot-toggle-self");
                return true;
            }
            
            UUID targetUuid = targetPlayer.getUniqueId();
            boolean currentState = dataManager.isAllowTeleport(playerUuid, targetUuid);
            
            dataManager.setAllowTeleport(playerUuid, targetUuid, !currentState)
                    .thenAccept(success -> {
                        if (success) {
                            if (!currentState) {
                                messageManager.send(player, "teleport.allow-teleport", "player", targetName);
                            } else {
                                messageManager.send(player, "teleport.deny-teleport", "player", targetName);
                            }
                        } else {
                            messageManager.send(player, "general.internal-error");
                        }
                    });
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
