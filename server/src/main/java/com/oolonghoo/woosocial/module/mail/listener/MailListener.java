package com.oolonghoo.woosocial.module.mail.listener;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.mail.MailDataManager;
import com.oolonghoo.woosocial.module.mail.MailManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MailListener implements Listener {
    
    private final WooSocial plugin;
    private final MailDataManager dataManager;
    @SuppressWarnings("unused")
    private final MailManager mailManager;
    
    public MailListener(WooSocial plugin, MailDataManager dataManager, MailManager mailManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.mailManager = mailManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dataManager.onPlayerJoin(player);
        
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            int unread = dataManager.getUnreadCount(player.getUniqueId());
            if (unread > 0) {
                plugin.getMessageManager().send(player, "mail.unread-notify", "count", String.valueOf(unread));
            }
        }, 40L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataManager.onPlayerQuit(player);
    }
}
