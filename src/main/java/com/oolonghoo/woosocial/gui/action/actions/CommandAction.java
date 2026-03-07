package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import com.oolonghoo.woosocial.util.PlaceholderParser;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public CommandAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("command", "cmd");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        
        PlaceholderParser parser = new PlaceholderParser();
        
        if (context.getFriendUuid() != null) {
            parser.set("friend_uuid", context.getFriendUuid().toString());
        }
        if (context.getFriendName() != null) {
            parser.set("friend_name", context.getFriendName());
        }
        parser.set("player", player.getName());
        parser.set("page", String.valueOf(context.getCurrentPage()));
        
        String command = parser.parse(params);
        
        player.performCommand(command);
    }
}
