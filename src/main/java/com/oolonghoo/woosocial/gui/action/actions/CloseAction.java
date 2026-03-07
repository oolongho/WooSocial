package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;

public class CloseAction implements GUIAction {
    
    @Override
    public List<String> getNames() {
        return List.of("close");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        player.closeInventory();
    }
}
