package com.oolonghoo.woosocial.module.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TradeOffer {
    
    private final UUID playerUuid;
    private final List<ItemStack> items;
    private volatile double money;
    private volatile int points;
    private volatile long updateTime;
    
    public TradeOffer(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.items = new CopyOnWriteArrayList<>();
        this.money = 0;
        this.points = 0;
        this.updateTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }
    
    public void addItem(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            items.add(item.clone());
            updateTime = System.currentTimeMillis();
        }
    }
    
    public void addItem(ItemStack item, int slot) {
        if (item != null && !item.getType().isAir()) {
            while (items.size() <= slot) {
                items.add(null);
            }
            items.set(slot, item.clone());
            updateTime = System.currentTimeMillis();
        }
    }
    
    public ItemStack removeItem(int slot) {
        if (slot >= 0 && slot < items.size()) {
            ItemStack removed = items.remove(slot);
            updateTime = System.currentTimeMillis();
            return removed;
        }
        return null;
    }
    
    public void setItem(int index, ItemStack item) {
        if (index >= 0 && index < items.size()) {
            if (item == null || item.getType().isAir()) {
                items.remove(index);
            } else {
                items.set(index, item.clone());
            }
            updateTime = System.currentTimeMillis();
        }
    }
    
    public void clearItems() {
        items.clear();
        updateTime = System.currentTimeMillis();
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public boolean hasItems() {
        return !items.isEmpty();
    }
    
    public double getMoney() {
        return money;
    }
    
    public void setMoney(double money) {
        this.money = Math.max(0, money);
        updateTime = System.currentTimeMillis();
    }
    
    public boolean hasMoney() {
        return money > 0;
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = Math.max(0, points);
        updateTime = System.currentTimeMillis();
    }
    
    public boolean hasPoints() {
        return points > 0;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public boolean isEmpty() {
        return items.isEmpty() && money <= 0 && points <= 0;
    }
    
    public boolean hasContent() {
        return !isEmpty();
    }
    
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("player_uuid", playerUuid.toString());
        data.put("money", money);
        data.put("points", points);
        data.put("update_time", updateTime);
        return data;
    }
    
    public int getTotalItemAmount() {
        int total = 0;
        for (ItemStack item : items) {
            if (item != null) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
