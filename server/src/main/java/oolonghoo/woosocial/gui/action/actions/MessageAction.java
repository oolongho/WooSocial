package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public MessageAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("message", "send_message", "msg");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        if (params.isEmpty()) {
            return;
        }
        
        plugin.getMessageManager().send(player, params);
    }
}
