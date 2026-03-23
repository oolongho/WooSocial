package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 交易模块配置
 */
public class TradeConfig {
    
    private final WooSocial plugin;
    private final File configFile;
    private FileConfiguration config;
    
    private int requestExpireTime;
    private int countdownSeconds;
    private int faceToFaceDistance;
    private boolean requirePermission;
    
    private boolean cancelOnDamage;
    private boolean cancelOnMove;
    private double moveThreshold;
    private boolean revokeReadyOnChange;
    
    private boolean blacklistEnabled;
    private final Set<Material> blacklistedMaterials;
    
    private boolean vaultEnabled;
    private boolean playerPointsEnabled;
    
    private Sound soundRequestSend;
    private Sound soundRequestReceive;
    private Sound soundTradeStart;
    private Sound soundTradeComplete;
    private Sound soundTradeCancel;
    private Sound soundCountdownTick;
    private Sound soundBlockedItem;
    
    public TradeConfig(WooSocial plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "settings/trade.yml");
        this.blacklistedMaterials = new HashSet<>();
    }
    
    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("settings/trade.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        requestExpireTime = config.getInt("request-expire-time", 60);
        countdownSeconds = config.getInt("countdown-seconds", 3);
        faceToFaceDistance = config.getInt("face-to-face-distance", 50);
        requirePermission = config.getBoolean("require-permission", false);
        
        cancelOnDamage = config.getBoolean("security.cancel-on-damage", true);
        cancelOnMove = config.getBoolean("security.cancel-on-move", false);
        moveThreshold = config.getDouble("security.move-threshold", 1.5);
        revokeReadyOnChange = config.getBoolean("security.revoke-ready-on-change", true);
        
        loadBlacklist();
        
        vaultEnabled = config.getBoolean("economy.vault.enabled", true);
        playerPointsEnabled = config.getBoolean("economy.playerpoints.enabled", true);
        
        loadSounds();
    }
    
    private void loadBlacklist() {
        blacklistedMaterials.clear();
        blacklistEnabled = config.getBoolean("blacklist.enabled", true);
        
        if (!blacklistEnabled) return;
        
        List<String> items = config.getStringList("blacklist.items");
        if (items != null) {
            for (String itemStr : items) {
                try {
                    Material material = Material.valueOf(itemStr.toUpperCase());
                    blacklistedMaterials.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Trade] 无效的黑名单物品: " + itemStr);
                }
            }
        }
    }
    
    private void loadSounds() {
        soundRequestSend = parseSound(config.getString("sounds.request-send", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        soundRequestReceive = parseSound(config.getString("sounds.request-receive", "BLOCK_NOTE_BLOCK_PLING"));
        soundTradeStart = parseSound(config.getString("sounds.trade-start", "ENTITY_PLAYER_LEVELUP"));
        soundTradeComplete = parseSound(config.getString("sounds.trade-complete", "ENTITY_PLAYER_LEVELUP"));
        soundTradeCancel = parseSound(config.getString("sounds.trade-cancel", "ENTITY_ITEM_BREAK"));
        soundCountdownTick = parseSound(config.getString("sounds.countdown-tick", "BLOCK_NOTE_BLOCK_PLING"));
        soundBlockedItem = parseSound(config.getString("sounds.blocked-item", "ENTITY_ITEM_BREAK"));
    }
    
    private Sound parseSound(String soundStr) {
        try {
            return Sound.valueOf(soundStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }
    
    public int getRequestExpireTime() {
        return requestExpireTime;
    }
    
    public int getCountdownSeconds() {
        return countdownSeconds;
    }
    
    public int getFaceToFaceDistance() {
        return faceToFaceDistance;
    }
    
    public boolean isRequirePermission() {
        return requirePermission;
    }
    
    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }
    
    public boolean isCancelOnMove() {
        return cancelOnMove;
    }
    
    public double getMoveThreshold() {
        return moveThreshold;
    }
    
    public boolean isRevokeReadyOnChange() {
        return revokeReadyOnChange;
    }
    
    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }
    
    public boolean isBlacklisted(Material material) {
        return blacklistEnabled && blacklistedMaterials.contains(material);
    }
    
    public Set<Material> getBlacklistedMaterials() {
        return blacklistedMaterials;
    }
    
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean isPlayerPointsEnabled() {
        return playerPointsEnabled;
    }
    
    public Sound getSoundRequestSend() {
        return soundRequestSend;
    }
    
    public Sound getSoundRequestReceive() {
        return soundRequestReceive;
    }
    
    public Sound getSoundTradeStart() {
        return soundTradeStart;
    }
    
    public Sound getSoundTradeComplete() {
        return soundTradeComplete;
    }
    
    public Sound getSoundTradeCancel() {
        return soundTradeCancel;
    }
    
    public Sound getSoundCountdownTick() {
        return soundCountdownTick;
    }
    
    public Sound getSoundBlockedItem() {
        return soundBlockedItem;
    }
}
