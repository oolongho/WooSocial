package com.oolonghoo.woosocial.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShowcaseData {
    
    private final UUID ownerUuid;
    private String ownerName;
    private List<ItemStack> items;
    private int likes;
    private long lastUpdated;
    
    public ShowcaseData(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        this.items = new ArrayList<>();
        this.likes = 0;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public ShowcaseData(UUID ownerUuid, String ownerName) {
        this(ownerUuid);
        this.ownerName = ownerName;
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public void setItems(List<ItemStack> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
    
    public ItemStack getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }
    
    public void setItem(int index, ItemStack item) {
        while (items.size() <= index) {
            items.add(null);
        }
        items.set(index, item);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void addItem(ItemStack item) {
        items.add(item);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    public void clearItems() {
        items.clear();
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public int getItemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }
    
    public int getLikes() {
        return likes;
    }
    
    public void setLikes(int likes) {
        this.likes = likes;
    }
    
    public void incrementLikes() {
        this.likes++;
    }
    
    public void decrementLikes() {
        if (this.likes > 0) {
            this.likes--;
        }
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
