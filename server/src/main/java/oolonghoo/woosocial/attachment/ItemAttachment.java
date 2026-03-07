package com.oolonghoo.woosocial.attachment;

import com.oolonghoo.woosocial.util.ItemSerializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品附件实现
 * 存储ItemStack物品，领取时添加到玩家背包
 * 
 * @author oolongho
 * @version 1.0.0
 */
public class ItemAttachment implements IAttachment {
    
    private ItemStack itemStack;
    
    /**
     * 默认构造函数
     */
    public ItemAttachment() {
        this.itemStack = null;
    }
    
    /**
     * 创建物品附件
     * 
     * @param itemStack 要存储的物品
     */
    public ItemAttachment(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
    
    @Override
    public boolean use(Player player) {
        if (!isLegal()) {
            return false;
        }
        
        // 检查背包是否有空间
        if (player.getInventory().firstEmpty() == -1) {
            // 背包已满，尝试将物品丢在地上
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack.clone());
        } else {
            // 添加到背包
            player.getInventory().addItem(itemStack.clone());
        }
        
        return true;
    }
    
    @Override
    public String serialize() {
        if (itemStack == null) {
            return null;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("type", getType().getIdentifier());
        json.addProperty("item", ItemSerializer.serialize(itemStack));
        
        return json.toString();
    }
    
    @Override
    public IAttachment deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String itemData = obj.get("item").getAsString();
            
            ItemStack item = ItemSerializer.deserialize(itemData);
            if (item != null) {
                this.itemStack = item;
                return this;
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return null;
    }
    
    @Override
    public AttachmentType getType() {
        return AttachmentType.ITEM;
    }
    
    @Override
    public boolean isLegal() {
        return itemStack != null && !itemStack.getType().isAir() && itemStack.getAmount() > 0;
    }
    
    @Override
    public ItemStack generateIcon() {
        if (itemStack == null) {
            return new ItemStack(Material.BARRIER);
        }
        
        ItemStack icon = itemStack.clone();
        ItemMeta meta = icon.getItemMeta();
        
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (meta.hasLore()) {
                List<net.kyori.adventure.text.Component> existingLore = meta.lore();
                if (existingLore != null) {
                    lore.addAll(existingLore);
                }
            }
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§e点击领取此物品"));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        
        return icon;
    }
    
    @Override
    public String getDescription() {
        if (itemStack == null) {
            return "无物品";
        }
        
        String name = ItemSerializer.getItemDisplayName(itemStack);
        return String.format("§f%s §7x%d", name, itemStack.getAmount());
    }
    
    /**
     * 获取存储的物品
     * 
     * @return ItemStack物品
     */
    public ItemStack getItemStack() {
        return itemStack;
    }
    
    /**
     * 设置存储的物品
     * 
     * @param itemStack 物品
     */
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
