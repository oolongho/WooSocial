package com.oolonghoo.woosocial.config;

import com.oolonghoo.woosocial.module.relation.type.MemorialItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemorialItemConfig extends ConfigLoader {
    
    private final Map<String, MemorialItem> items = new HashMap<>();
    
    public MemorialItemConfig(JavaPlugin plugin) {
        super(plugin, "settings/memorial_items.yml");
    }
    
    @Override
    protected void loadValues() {
        super.loadValues();
        items.clear();
        
        ConfigurationSection itemsSection = config.getConfigurationSection("memorial_items");
        if (itemsSection == null) {
            plugin.getLogger().warning("memorial_items.yml 中未找到 'memorial_items' 配置节");
            return;
        }
        
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;
            
            MemorialItem item = new MemorialItem(itemId);
            item.setName(itemSection.getString("name", itemId));
            item.setDescription(itemSection.getString("description", ""));
            
            ConfigurationSection costSection = itemSection.getConfigurationSection("cost");
            if (costSection != null) {
                item.setCostCoins(costSection.getInt("coins", 0));
                item.setCostPoints(costSection.getInt("points", 0));
            }
            
            String iconStr = itemSection.getString("icon", "DIAMOND");
            try {
                item.setIcon(Material.valueOf(iconStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的纪念品图标材质: " + iconStr + "，使用默认值 DIAMOND");
                item.setIcon(Material.DIAMOND);
            }
            
            List<String> lore = itemSection.getStringList("lore");
            item.setLore(lore);
            
            items.put(itemId.toLowerCase(), item);
        }
    }
    
    public MemorialItem getItem(String id) {
        return items.get(id.toLowerCase());
    }
    
    public Collection<MemorialItem> getAllItems() {
        return items.values();
    }
    
    public Set<String> getItemIds() {
        return items.keySet();
    }
    
    public boolean hasItem(String id) {
        return items.containsKey(id.toLowerCase());
    }
    
    public int getItemCount() {
        return items.size();
    }
}
