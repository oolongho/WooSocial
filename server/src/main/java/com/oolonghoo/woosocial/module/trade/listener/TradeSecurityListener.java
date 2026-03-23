package com.oolonghoo.woosocial.module.trade.listener;

import com.oolonghoo.woosocial.module.trade.TradeConfig;
import com.oolonghoo.woosocial.module.trade.TradeManager;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.module.trade.model.TradeState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeSecurityListener implements Listener {
    
    private final TradeManager tradeManager;
    private final TradeConfig config;
    
    private final Map<UUID, Location> initialLocations = new ConcurrentHashMap<>();
    
    public TradeSecurityListener(TradeManager tradeManager, TradeConfig config) {
        this.tradeManager = tradeManager;
        this.config = config;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!config.isCancelOnDamage()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        TradeSession session = tradeManager.getSession(player.getUniqueId());
        
        if (session != null && session.getState() != TradeState.CANCELLED && session.getState() != TradeState.COMPLETED) {
            tradeManager.cancelTrade(player.getUniqueId(), "玩家受到伤害");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isCancelOnMove()) return;
        
        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player.getUniqueId());
        
        if (session == null || session.getState() == TradeState.CANCELLED || session.getState() == TradeState.COMPLETED) {
            return;
        }
        
        if (event.getFrom().getX() == event.getTo().getX() &&
            event.getFrom().getY() == event.getTo().getY() &&
            event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        
        double distance = event.getFrom().distanceSquared(event.getTo());
        if (distance > config.getMoveThreshold() * config.getMoveThreshold()) {
            tradeManager.cancelTrade(player.getUniqueId(), "玩家移动");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player.getUniqueId());
        
        if (session != null && session.getState() != TradeState.CANCELLED && session.getState() != TradeState.COMPLETED) {
            tradeManager.cancelTrade(player.getUniqueId(), "玩家传送");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        TradeSession session = tradeManager.getSession(playerUuid);
        if (session != null) {
            tradeManager.cancelTrade(playerUuid, "玩家离线");
        }
        
        initialLocations.remove(playerUuid);
    }
    
    public void recordInitialLocation(UUID playerUuid, Location location) {
        initialLocations.put(playerUuid, location.clone());
    }
    
    public void clearInitialLocation(UUID playerUuid) {
        initialLocations.remove(playerUuid);
    }
    
    public boolean hasPlayerMoved(UUID playerUuid, Location currentLocation) {
        Location initial = initialLocations.get(playerUuid);
        if (initial == null) return false;
        
        if (!initial.getWorld().equals(currentLocation.getWorld())) {
            return true;
        }
        
        double distanceSquared = initial.distanceSquared(currentLocation);
        return distanceSquared > config.getMoveThreshold() * config.getMoveThreshold();
    }
}
