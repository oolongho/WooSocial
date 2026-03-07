package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class BlockAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public BlockAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("block_player", "unblock_player", "block", "unblock");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        String actionType = params.isEmpty() ? context.getString("block_action") : params;
        UUID targetUuid = context.getTargetUuid() != null ? context.getTargetUuid() : context.getFriendUuid();
        UUID playerUuid = player.getUniqueId();
        
        if (targetUuid == null) return;
        
        var dataManager = plugin.getModuleManager().getFriendModule().getDataManager();
        
        switch (actionType.toLowerCase()) {
            case "block", "block_player" -> {
                dataManager.blockPlayer(playerUuid, targetUuid);
            }
            case "unblock", "unblock_player" -> {
                dataManager.unblockPlayer(playerUuid, targetUuid);
            }
        }
    }
}
