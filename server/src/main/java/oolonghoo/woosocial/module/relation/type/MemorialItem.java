package com.oolonghoo.woosocial.module.relation.type;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class MemorialItem {
    
    private final String id;
    private String name;
    private String description;
    private int costCoins;
    private int costPoints;
    private Material icon;
    private List<String> lore;
    
    public MemorialItem(String id) {
        this.id = id;
        this.name = id;
        this.description = "";
        this.costCoins = 0;
        this.costPoints = 0;
        this.icon = Material.DIAMOND;
        this.lore = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getCostCoins() {
        return costCoins;
    }
    
    public void setCostCoins(int costCoins) {
        this.costCoins = costCoins;
    }
    
    public int getCostPoints() {
        return costPoints;
    }
    
    public void setCostPoints(int costPoints) {
        this.costPoints = costPoints;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }
    
    public boolean isFree() {
        return costCoins == 0 && costPoints == 0;
    }
    
    @Override
    public String toString() {
        return "MemorialItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", costCoins=" + costCoins +
                ", costPoints=" + costPoints +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MemorialItem that = (MemorialItem) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
