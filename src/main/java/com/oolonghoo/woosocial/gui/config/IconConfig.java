package com.oolonghoo.woosocial.gui.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconConfig {
    
    private String type;
    private String material;
    private String skullOwner;
    private String itemModel;
    private int customModelData;
    private String name;
    private List<String> lore;
    private int amount;
    private boolean glow;
    private Map<String, List<String>> actions;
    
    public IconConfig() {
        this.lore = new ArrayList<>();
        this.actions = new HashMap<>();
        this.amount = 1;
        this.glow = false;
        this.customModelData = -1;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isSpecialType() {
        return type != null && !type.isEmpty();
    }
    
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    public String getSkullOwner() {
        return skullOwner;
    }
    
    public void setSkullOwner(String skullOwner) {
        this.skullOwner = skullOwner;
    }
    
    public String getItemModel() {
        return itemModel;
    }
    
    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }
    
    public int getCustomModelData() {
        return customModelData;
    }
    
    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public boolean isGlow() {
        return glow;
    }
    
    public void setGlow(boolean glow) {
        this.glow = glow;
    }
    
    public Map<String, List<String>> getActions() {
        return actions;
    }
    
    public void setActions(Map<String, List<String>> actions) {
        this.actions = actions != null ? actions : new HashMap<>();
    }
    
    public List<String> getLeftActions() {
        return actions.getOrDefault("left", new ArrayList<>());
    }
    
    public List<String> getRightActions() {
        return actions.getOrDefault("right", new ArrayList<>());
    }
    
    public boolean hasActions() {
        return !actions.isEmpty();
    }
    
    public static IconConfig fromMap(Map<String, Object> map) {
        IconConfig config = new IconConfig();
        
        if (map.containsKey("type")) {
            config.setType((String) map.get("type"));
        }
        
        if (map.containsKey("material")) {
            String mat = (String) map.get("material");
            config.setMaterial(mat);
            
            if (mat != null && mat.contains("{model-data=")) {
                int start = mat.indexOf("{model-data=") + 12;
                int end = mat.indexOf("}", start);
                if (end > start) {
                    try {
                        config.setCustomModelData(Integer.parseInt(mat.substring(start, end)));
                        config.setMaterial(mat.substring(0, mat.indexOf("{")));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        
        if (map.containsKey("skull_owner")) {
            config.setSkullOwner((String) map.get("skull_owner"));
        }
        
        if (map.containsKey("item_model")) {
            config.setItemModel((String) map.get("item_model"));
        }
        
        if (map.containsKey("custom_model_data")) {
            config.setCustomModelData(((Number) map.get("custom_model_data")).intValue());
        }
        
        if (map.containsKey("name")) {
            config.setName((String) map.get("name"));
        }
        
        if (map.containsKey("lore")) {
            @SuppressWarnings("unchecked")
            List<String> loreList = (List<String>) map.get("lore");
            config.setLore(loreList);
        }
        
        if (map.containsKey("amount")) {
            config.setAmount(((Number) map.get("amount")).intValue());
        }
        
        if (map.containsKey("glow")) {
            config.setGlow((Boolean) map.get("glow"));
        }
        
        if (map.containsKey("actions")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionsMap = (Map<String, Object>) map.get("actions");
            Map<String, List<String>> parsedActions = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : actionsMap.entrySet()) {
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> actionList = (List<String>) entry.getValue();
                    parsedActions.put(entry.getKey(), actionList);
                }
            }
            config.setActions(parsedActions);
        }
        
        return config;
    }
}
