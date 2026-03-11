package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class FriendAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public FriendAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("add_friend", "remove_friend", "accept_request", "deny_request", "set_nickname");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        String actionType = params.isEmpty() ? context.getString("friend_action") : params;
        UUID friendUuid = context.getFriendUuid();
        UUID playerUuid = player.getUniqueId();
        
        if (actionType == null) return;
        
        switch (actionType.toLowerCase()) {
            case "remove", "remove_friend" -> {
                if (friendUuid != null) {
                    plugin.getModuleManager().getFriendModule().getDataManager()
                            .removeFriend(playerUuid, friendUuid);
                    player.closeInventory();
                }
            }
            case "accept", "accept_request" -> {
                if (friendUuid != null) {
                    plugin.getModuleManager().getFriendModule().getDataManager()
                            .acceptFriendRequest(playerUuid, friendUuid);
                }
            }
            case "deny", "deny_request" -> {
                if (friendUuid != null) {
                    plugin.getModuleManager().getFriendModule().getDataManager()
                            .denyFriendRequest(playerUuid, friendUuid);
                }
            }
            case "nickname", "set_nickname" -> {
                plugin.getMessageManager().send(player, "general.feature-disabled");
            }
        }
    }
}
