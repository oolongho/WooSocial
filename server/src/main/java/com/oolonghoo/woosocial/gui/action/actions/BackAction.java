package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;

public class BackAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public BackAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("back");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        String currentGui = context.getCurrentGuiName();
        if (currentGui == null) {
            player.closeInventory();
            return;
        }
        
        switch (currentGui) {
            case "friend_list", "friend_requests", "social_settings", "blocked_list" -> {
                new com.oolonghoo.woosocial.gui.SocialMainGUI(plugin, player).open(player);
            }
            case "friend_detail" -> {
                new com.oolonghoo.woosocial.gui.FriendListGUI(plugin, player).open(player);
            }
            default -> player.closeInventory();
        }
    }
}
