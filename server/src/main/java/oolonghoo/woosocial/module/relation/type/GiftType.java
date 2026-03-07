package com.oolonghoo.woosocial.module.relation.type;

import org.bukkit.Material;

public class GiftType {
    
    private final String id;
    private String name;
    private String description;
    private int intimacy;
    private int costCoins;
    private int costPoints;
    private int dailyLimit;
    private Material icon;
    
    public GiftType(String id) {
        this.id = id;
        this.name = id;
        this.description = "";
        this.intimacy = 1;
        this.costCoins = 0;
        this.costPoints = 0;
        this.dailyLimit = 0;
        this.icon = Material.GOLD_INGOT;
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
    
    public int getIntimacy() {
        return intimacy;
    }
    
    public void setIntimacy(int intimacy) {
        this.intimacy = intimacy;
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
    
    public int getDailyLimit() {
        return dailyLimit;
    }
    
    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    public boolean isFree() {
        return costCoins == 0 && costPoints == 0;
    }
    
    public boolean hasDailyLimit() {
        return dailyLimit > 0;
    }
    
    @Override
    public String toString() {
        return "GiftType{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", intimacy=" + intimacy +
                ", costCoins=" + costCoins +
                ", costPoints=" + costPoints +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GiftType that = (GiftType) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
