package com.oolonghoo.woosocial.gui.action;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.actions.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionParser {
    
    private final WooSocial plugin;
    private final Map<String, GUIAction> actions;
    
    public ActionParser(WooSocial plugin) {
        this.plugin = plugin;
        this.actions = new HashMap<>();
        registerDefaultActions();
    }
    
    private void registerDefaultActions() {
        registerAction(new OpenGUIAction(plugin));
        registerAction(new CloseAction());
        registerAction(new BackAction(plugin));
        registerAction(new PageAction());
        registerAction(new TeleportAction(plugin));
        registerAction(new ToggleAction(plugin));
        registerAction(new FriendAction(plugin));
        registerAction(new BlockAction(plugin));
        registerAction(new SoundAction(plugin));
        registerAction(new MessageAction(plugin));
        registerAction(new CommandAction(plugin));
    }
    
    public void registerAction(GUIAction action) {
        for (String name : action.getNames()) {
            actions.put(name.toLowerCase(), action);
        }
    }
    
    public void executeActions(List<String> actionList, Player player, ActionContext context) {
        if (actionList == null || actionList.isEmpty()) {
            return;
        }
        
        for (String actionStr : actionList) {
            executeAction(actionStr, player, context);
        }
    }
    
    public void executeAction(String actionStr, Player player, ActionContext context) {
        if (actionStr == null || actionStr.isEmpty()) {
            return;
        }
        
        String[] parts = actionStr.split(":", 2);
        String actionName = parts[0].toLowerCase().trim();
        String params = parts.length > 1 ? parts[1].trim() : "";
        
        GUIAction action = actions.get(actionName);
        if (action != null) {
            try {
                action.execute(player, context, params);
            } catch (Exception e) {
                plugin.getLogger().warning("[GUI] 执行Action失败: " + actionName + " - " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("[GUI] 未知的Action: " + actionName);
        }
    }
    
    public boolean hasAction(String name) {
        return actions.containsKey(name.toLowerCase());
    }
}
