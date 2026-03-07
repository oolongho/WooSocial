package com.oolonghoo.woosocial.config;

import com.oolonghoo.woosocial.module.relation.type.GiftType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GiftConfig extends ConfigLoader {
    
    private final Map<String, GiftType> gifts = new HashMap<>();
    
    public GiftConfig(JavaPlugin plugin) {
        super(plugin, "gifts.yml");
    }
    
    @Override
    protected void loadValues() {
        super.loadValues();
        gifts.clear();
        
        ConfigurationSection giftsSection = config.getConfigurationSection("gifts");
        if (giftsSection == null) {
            plugin.getLogger().warning("gifts.yml 中未找到 'gifts' 配置节");
            return;
        }
        
        for (String giftId : giftsSection.getKeys(false)) {
            ConfigurationSection giftSection = giftsSection.getConfigurationSection(giftId);
            if (giftSection == null) continue;
            
            GiftType gift = new GiftType(giftId);
            gift.setName(giftSection.getString("name", giftId));
            gift.setDescription(giftSection.getString("description", ""));
            gift.setIntimacy(giftSection.getInt("intimacy", 1));
            gift.setDailyLimit(giftSection.getInt("daily-limit", 0));
            
            ConfigurationSection costSection = giftSection.getConfigurationSection("cost");
            if (costSection != null) {
                gift.setCostCoins(costSection.getInt("coins", 0));
                gift.setCostPoints(costSection.getInt("points", 0));
            }
            
            String iconStr = giftSection.getString("icon", "GOLD_INGOT");
            try {
                gift.setIcon(Material.valueOf(iconStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的礼品图标材质: " + iconStr + "，使用默认值 GOLD_INGOT");
                gift.setIcon(Material.GOLD_INGOT);
            }
            
            gifts.put(giftId.toLowerCase(), gift);
        }
        
        plugin.getLogger().info("已加载 " + gifts.size() + " 种礼品配置");
    }
    
    public GiftType getGift(String id) {
        return gifts.get(id.toLowerCase());
    }
    
    public Collection<GiftType> getAllGifts() {
        return gifts.values();
    }
    
    public Set<String> getGiftIds() {
        return gifts.keySet();
    }
    
    public boolean hasGift(String id) {
        return gifts.containsKey(id.toLowerCase());
    }
    
    public int getGiftCount() {
        return gifts.size();
    }
}
