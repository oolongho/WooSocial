package com.oolonghoo.woosocial.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.ShowcaseViewGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ShowcaseListener implements Listener {
    
    private final WooSocial plugin;
    
    public ShowcaseListener(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }
        
        Player player = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        
        if (!player.isSneaking()) {
            return;
        }
        
        event.setCancelled(true);
        
        if (player.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        
        ShowcaseViewGUI gui = new ShowcaseViewGUI(plugin, player, target.getUniqueId(), target.getName());
        gui.open(player);
    }
}
