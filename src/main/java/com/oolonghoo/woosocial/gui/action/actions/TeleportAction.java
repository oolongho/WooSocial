package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TeleportAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public TeleportAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("teleport", "teleport_to_friend", "tp");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        UUID friendUuid = context.getFriendUuid();
        if (friendUuid == null) {
            return;
        }
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(friendUuid);
        if (!offlinePlayer.isOnline()) {
            plugin.getMessageManager().send(player, "teleport.target-not-online");
            return;
        }
        
        Player target = offlinePlayer.getPlayer();
        if (target == null) {
            plugin.getMessageManager().send(player, "teleport.target-not-online");
            return;
        }
        
        player.closeInventory();
        plugin.getModuleManager().getTeleportModule().getTeleportManager().startTeleport(player, target);
    }
}
