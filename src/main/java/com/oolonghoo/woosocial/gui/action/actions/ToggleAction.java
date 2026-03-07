package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ToggleAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public ToggleAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("toggle_teleport", "toggle_notify_online", "toggle_favorite", 
                "toggle_friend_teleport", "toggle");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        String toggleType = params.isEmpty() ? context.getString("toggle_type") : params;
        UUID friendUuid = context.getFriendUuid();
        UUID playerUuid = player.getUniqueId();
        
        if (toggleType == null) return;
        
        switch (toggleType.toLowerCase()) {
            case "teleport", "toggle_teleport" -> {
                if (friendUuid != null) {
                    boolean current = plugin.getModuleManager().getTeleportModule().getDataManager()
                            .isAllowTeleport(playerUuid, friendUuid);
                    plugin.getModuleManager().getTeleportModule().getDataManager()
                            .setAllowTeleport(playerUuid, friendUuid, !current);
                }
            }
            case "notify_online", "toggle_notify_online" -> {
                if (friendUuid != null) {
                    boolean current = plugin.getModuleManager().getFriendModule().getDataManager()
                            .isNotifyOnlineForFriend(playerUuid, friendUuid);
                    plugin.getModuleManager().getFriendModule().getDataManager()
                            .setNotifyOnlineForFriend(playerUuid, friendUuid, !current);
                }
            }
            case "favorite", "toggle_favorite" -> {
                if (friendUuid != null) {
                    boolean current = plugin.getModuleManager().getFriendModule().getDataManager()
                            .isFavorite(playerUuid, friendUuid);
                    plugin.getModuleManager().getFriendModule().getDataManager()
                            .setFavorite(playerUuid, friendUuid, !current);
                }
            }
            case "friend_teleport", "toggle_friend_teleport" -> {
                plugin.getModuleManager().getTeleportModule().getDataManager()
                        .toggleFriendTeleport(playerUuid);
            }
        }
    }
}
