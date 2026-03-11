package com.oolonghoo.woosocial.module.relation.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.relation.RelationDataManager;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class RelationListener implements Listener {
    
    private final WooSocial plugin;
    private final RelationDataManager dataManager;
    private final RelationManager relationManager;
    
    public RelationListener(WooSocial plugin, RelationDataManager dataManager, RelationManager relationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.relationManager = relationManager;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        dataManager.clearCache(playerUuid);
    }
}
