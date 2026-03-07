package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class OpenGUIAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public OpenGUIAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("open_gui", "open");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        String guiName = params.isEmpty() ? context.getString("target_gui") : params;
        if (guiName == null || guiName.isEmpty()) {
            return;
        }
        
        switch (guiName.toLowerCase()) {
            case "social_main" -> {
                new com.oolonghoo.woosocial.gui.SocialMainGUI(plugin, player).open(player);
            }
            case "friend_list" -> {
                new com.oolonghoo.woosocial.gui.FriendListGUI(plugin, player).open(player);
            }
            case "friend_detail" -> {
                UUID friendUuid = context.getFriendUuid();
                String friendName = context.getFriendName();
                if (friendUuid != null) {
                    new com.oolonghoo.woosocial.gui.FriendDetailGUI(plugin, player, friendUuid, 
                            friendName != null ? friendName : friendUuid.toString()).open(player);
                }
            }
            case "friend_requests" -> {
                new com.oolonghoo.woosocial.gui.FriendRequestsGUI(plugin, player).open(player);
            }
            case "social_settings" -> {
                new com.oolonghoo.woosocial.gui.SocialSettingsGUI(plugin, player).open(player);
            }
            case "blocked_list" -> {
                new com.oolonghoo.woosocial.gui.BlockedListGUI(plugin, player).open(player);
            }
        }
    }
}
