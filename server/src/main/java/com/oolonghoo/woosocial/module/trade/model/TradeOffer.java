package com.oolonghoo.woosocial.module.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 交易报价
 * 表示玩家在交易中提供的物品和经济货币
 */
public class TradeOffer {
    
    private final UUID playerUuid;
    private final List<ItemStack> items;
    private double money;
    private int points;
    private long updateTime;
    
    public TradeOffer(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.items = new ArrayList<>();
        this.money = 0;
        this.points = 0;
        this.updateTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public void addItem(ItemStack item) {
        if (item != null) {
            items.add(item.clone());
            updateTime = System.currentTimeMillis();
        }
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            updateTime = System.currentTimeMillis();
        }
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
        return items.stream().mapToInt(ItemStack::getAmount).sum();
    }
}
