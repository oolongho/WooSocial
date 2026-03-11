package com.oolonghoo.woosocial.config;

import com.oolonghoo.woosocial.module.relation.type.RelationType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RelationTypeConfig extends ConfigLoader {
    
    private final Map<String, RelationType> types = new HashMap<>();
    private RelationType defaultType;
    
    public RelationTypeConfig(JavaPlugin plugin) {
        super(plugin, "settings/relation_types.yml");
    }
    
    @Override
    protected void loadValues() {
        super.loadValues();
        types.clear();
        defaultType = null;
        
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null) {
            plugin.getLogger().warning("relation_types.yml 中未找到 'types' 配置节");
            return;
        }
        
        for (String typeId : typesSection.getKeys(false)) {
            ConfigurationSection typeSection = typesSection.getConfigurationSection(typeId);
            if (typeSection == null) continue;
            
            RelationType type = new RelationType(typeId);
            type.setDisplayName(typeSection.getString("display-name", typeId));
            type.setDescription(typeSection.getString("description", ""));
            type.setRequiredIntimacy(typeSection.getInt("required-intimacy", 0));
            type.setMaxSlots(typeSection.getInt("max-slots", 10));
            type.setPriority(typeSection.getInt("priority", 1));
            type.setDefault(typeSection.getBoolean("is-default", false));
            type.setSpecial(typeSection.getBoolean("is-special", false));
            type.setRequireMutual(typeSection.getBoolean("require-mutual", false));
            
            String requireItem = typeSection.getString("require-item");
            if (requireItem != null && !requireItem.isEmpty() && !requireItem.equals("null")) {
                type.setRequireItem(requireItem.toLowerCase());
            }
            
            String iconStr = typeSection.getString("icon", "PLAYER_HEAD");
            try {
                type.setIcon(Material.valueOf(iconStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的关系类型图标材质: " + iconStr + "，使用默认值 PLAYER_HEAD");
                type.setIcon(Material.PLAYER_HEAD);
            }
            
            types.put(typeId.toLowerCase(), type);
            
            if (type.isDefault()) {
                defaultType = type;
            }
        }
        
        if (defaultType == null && !types.isEmpty()) {
            defaultType = types.values().iterator().next();
            plugin.getLogger().warning("未设置默认关系类型，使用: " + defaultType.getId());
        }
    }
    
    public RelationType getType(String id) {
        return types.get(id.toLowerCase());
    }
    
    public Collection<RelationType> getAllTypes() {
        return types.values();
    }
    
    public Set<String> getTypeIds() {
        return types.keySet();
    }
    
    public boolean hasType(String id) {
        return types.containsKey(id.toLowerCase());
    }
    
    public int getTypeCount() {
        return types.size();
    }
    
    public RelationType getDefaultType() {
        return defaultType;
    }
    
    public Optional<RelationType> getTypeByItem(String itemId) {
        return types.values().stream()
                .filter(t -> itemId != null && itemId.equalsIgnoreCase(t.getRequireItem()))
                .findFirst();
    }
}
