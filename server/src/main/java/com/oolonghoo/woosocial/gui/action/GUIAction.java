package com.oolonghoo.woosocial.gui.action;

import org.bukkit.entity.Player;

import java.util.List;

public interface GUIAction {
    
    List<String> getNames();
    
    void execute(Player player, ActionContext context, String params);
}
