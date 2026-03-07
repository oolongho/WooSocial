package com.oolonghoo.woosocial.module.relation.type;

import org.bukkit.Material;

public class RelationType {
    
    private final String id;
    private String displayName;
    private String description;
    private int requiredIntimacy;
    private int maxSlots;
    private int priority;
    private Material icon;
    private boolean isDefault;
    private boolean isSpecial;
    private String requireItem;
    private boolean requireMutual;
    
    public RelationType(String id) {
        this.id = id;
        this.displayName = id;
        this.description = "";
        this.requiredIntimacy = 0;
        this.maxSlots = 10;
        this.priority = 1;
        this.icon = Material.PLAYER_HEAD;
        this.isDefault = false;
        this.isSpecial = false;
        this.requireItem = null;
        this.requireMutual = false;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getRequiredIntimacy() {
        return requiredIntimacy;
    }
    
    public void setRequiredIntimacy(int requiredIntimacy) {
        this.requiredIntimacy = requiredIntimacy;
    }
    
    public int getMaxSlots() {
        return maxSlots;
    }
    
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public void setIcon(Material icon) {
        this.icon = icon;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
    
    public boolean isSpecial() {
        return isSpecial;
    }
    
    public void setSpecial(boolean special) {
        isSpecial = special;
    }
    
    public String getRequireItem() {
        return requireItem;
    }
    
    public void setRequireItem(String requireItem) {
        this.requireItem = requireItem;
    }
    
    public boolean isRequireMutual() {
        return requireMutual;
    }
    
    public void setRequireMutual(boolean requireMutual) {
        this.requireMutual = requireMutual;
    }
    
    public boolean requiresItem() {
        return requireItem != null && !requireItem.isEmpty();
    }
    
    @Override
    public String toString() {
        return "RelationType{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", requiredIntimacy=" + requiredIntimacy +
                ", maxSlots=" + maxSlots +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationType that = (RelationType) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
