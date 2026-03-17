package com.oolonghoo.woosocial.module.showcase;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.ShowcaseDAO;
import com.oolonghoo.woosocial.database.ShowcaseLikeCooldownDAO;
import com.oolonghoo.woosocial.model.ShowcaseData;
import com.oolonghoo.woosocial.model.ShowcaseLikeCooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShowcaseManager {
    
    private final WooSocial plugin;
    private final ShowcaseDAO showcaseDAO;
    private final ShowcaseLikeCooldownDAO cooldownDAO;
    private final Map<UUID, ShowcaseData> showcaseCache;
    private final Map<UUID, ShowcaseLikeCooldown> cooldownCache;
    
    public ShowcaseManager(WooSocial plugin, ShowcaseDAO showcaseDAO, ShowcaseLikeCooldownDAO cooldownDAO) {
        this.plugin = plugin;
        this.showcaseDAO = showcaseDAO;
        this.cooldownDAO = cooldownDAO;
        this.showcaseCache = new ConcurrentHashMap<>();
        this.cooldownCache = new ConcurrentHashMap<>();
    }
    
    public ShowcaseData getShowcase(UUID ownerUuid) {
        return showcaseCache.computeIfAbsent(ownerUuid, uuid -> {
            ShowcaseData data = showcaseDAO.loadShowcaseSync(uuid);
            if (data == null) {
                data = new ShowcaseData(uuid);
            }
            return data;
        });
    }
    
    public ShowcaseData getShowcase(UUID ownerUuid, String ownerName) {
        ShowcaseData data = getShowcase(ownerUuid);
        if (data.getOwnerName() == null || data.getOwnerName().isEmpty()) {
            data.setOwnerName(ownerName);
        }
        return data;
    }
    
    public void saveShowcase(Player player) {
        ShowcaseData data = getShowcase(player.getUniqueId(), player.getName());
        data.setLastUpdated(System.currentTimeMillis());
        showcaseDAO.saveShowcase(data);
    }
    
    public void saveShowcase(UUID ownerUuid) {
        ShowcaseData data = showcaseCache.get(ownerUuid);
        if (data != null) {
            showcaseDAO.saveShowcase(data);
        }
    }
    
    public void addItem(Player player, ItemStack item, int slot) {
        ShowcaseData data = getShowcase(player.getUniqueId(), player.getName());
        data.setItem(slot, item.clone());
        showcaseDAO.saveShowcase(data);
    }
    
    public void removeItem(Player player, int slot) {
        ShowcaseData data = getShowcase(player.getUniqueId(), player.getName());
        data.setItem(slot, null);
        showcaseDAO.saveShowcase(data);
    }
    
    public void clearShowcase(Player player) {
        ShowcaseData data = getShowcase(player.getUniqueId(), player.getName());
        data.clearItems();
        showcaseDAO.saveShowcase(data);
    }
    
    public void toggleLike(Player liker, UUID targetUuid) {
        if (!canLike(liker.getUniqueId(), targetUuid)) {
            int remainingSeconds = getRemainingCooldownSeconds(liker.getUniqueId());
            if (remainingSeconds > 0) {
                plugin.getMessageManager().send(liker, "showcase.like-cooldown", "seconds", String.valueOf(remainingSeconds));
            } else {
                int dailyRemaining = getDailyRemainingLikes(liker.getUniqueId());
                if (dailyRemaining <= 0) {
                    plugin.getMessageManager().send(liker, "showcase.like-daily-limit", "remaining", "0");
                } else {
                    int playerDailyRemaining = getPlayerDailyRemainingLikes(liker.getUniqueId(), targetUuid);
                    if (playerDailyRemaining <= 0) {
                        plugin.getMessageManager().send(liker, "showcase.like-player-limit");
                    }
                }
            }
            return;
        }
        
        ShowcaseData targetData = getShowcase(targetUuid);
        
        if (hasLiked(liker.getUniqueId(), targetUuid)) {
            showcaseDAO.removeLike(liker.getUniqueId(), targetUuid).thenAccept(success -> {
                if (success) {
                    targetData.decrementLikes();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getMessageManager().send(liker, "showcase.like-removed");
                    });
                }
            });
        } else {
            showcaseDAO.addLike(liker.getUniqueId(), targetUuid).thenAccept(success -> {
                if (success) {
                    targetData.incrementLikes();
                    recordLike(liker.getUniqueId(), targetUuid);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getMessageManager().send(liker, "showcase.like-added");
                    });
                }
            });
        }
    }
    
    public boolean hasLiked(UUID likerUuid, UUID targetUuid) {
        return showcaseDAO.hasLikedSync(likerUuid, targetUuid);
    }
    
    public boolean canLike(UUID playerUuid, UUID targetUuid) {
        ShowcaseLikeCooldown cooldown = cooldownCache.computeIfAbsent(playerUuid, uuid -> {
            return cooldownDAO.getCooldownSync(uuid);
        });
        
        if (cooldown.shouldResetDaily()) {
            cooldown.resetDailyData();
            cooldownDAO.updateCooldownSync(cooldown);
        }
        
        int cooldownSeconds = plugin.getConfig().getInt("showcase.like.cooldown", 30);
        int dailyLimit = plugin.getConfig().getInt("showcase.like.daily-limit", 10);
        int perPlayerDailyLimit = plugin.getConfig().getInt("showcase.like.per-player-daily-limit", 1);
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastLike = currentTime - cooldown.getLastLikeTime();
        
        return timeSinceLastLike >= cooldownSeconds * 1000L
                && cooldown.getDailyCount() < dailyLimit
                && cooldown.getPlayerDailyLikeCount(targetUuid) < perPlayerDailyLimit;
    }
    
    public void recordLike(UUID playerUuid, UUID targetUuid) {
        ShowcaseLikeCooldown cooldown = cooldownCache.computeIfAbsent(playerUuid, uuid -> {
            return cooldownDAO.getCooldownSync(uuid);
        });
        
        if (cooldown.shouldResetDaily()) {
            cooldown.resetDailyData();
        }
        
        cooldown.setLastLikeTime(System.currentTimeMillis());
        cooldown.incrementDailyCount();
        cooldown.incrementPlayerDailyLike(targetUuid);
        
        cooldownDAO.updateCooldownSync(cooldown);
    }
    
    public int getRemainingCooldownSeconds(UUID playerUuid) {
        ShowcaseLikeCooldown cooldown = cooldownCache.getOrDefault(playerUuid, cooldownDAO.getCooldownSync(playerUuid));
        
        int cooldownSeconds = plugin.getConfig().getInt("showcase.like.cooldown", 30);
        long currentTime = System.currentTimeMillis();
        long timeSinceLastLike = currentTime - cooldown.getLastLikeTime();
        
        if (timeSinceLastLike >= cooldownSeconds * 1000L) {
            return 0;
        }
        
        return (int) ((cooldownSeconds * 1000L - timeSinceLastLike) / 1000L);
    }
    
    public int getDailyRemainingLikes(UUID playerUuid) {
        ShowcaseLikeCooldown cooldown = cooldownCache.getOrDefault(playerUuid, cooldownDAO.getCooldownSync(playerUuid));
        
        if (cooldown.shouldResetDaily()) {
            cooldown.resetDailyData();
        }
        
        int dailyLimit = plugin.getConfig().getInt("showcase.like.daily-limit", 10);
        return Math.max(0, dailyLimit - cooldown.getDailyCount());
    }
    
    public int getPlayerDailyRemainingLikes(UUID playerUuid, UUID targetUuid) {
        ShowcaseLikeCooldown cooldown = cooldownCache.getOrDefault(playerUuid, cooldownDAO.getCooldownSync(playerUuid));
        
        if (cooldown.shouldResetDaily()) {
            cooldown.resetDailyData();
        }
        
        int perPlayerDailyLimit = plugin.getConfig().getInt("showcase.like.per-player-daily-limit", 1);
        return Math.max(0, perPlayerDailyLimit - cooldown.getPlayerDailyLikeCount(targetUuid));
    }
    
    public void saveAll() {
        for (Map.Entry<UUID, ShowcaseData> entry : showcaseCache.entrySet()) {
            showcaseDAO.saveShowcase(entry.getValue());
        }
    }
    
    public void clearCache(UUID ownerUuid) {
        showcaseCache.remove(ownerUuid);
    }
    
    public void clearAllCache() {
        showcaseCache.clear();
    }
}
