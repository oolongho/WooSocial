package com.oolonghoo.woosocial.module.trade.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 物品快照
 * 记录物品的完整信息用于验证
 */
public class ItemSnapshot implements ConfigurationSerializable {
    
    private final Material material;
    private final int amount;
    private final int durability;
    private final Map<String, Object> nbtData;
    private final long timestamp;
    
    public ItemSnapshot(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            this.material = Material.AIR;
            this.amount = 0;
            this.durability = 0;
            this.nbtData = new HashMap<>();
        } else {
            this.material = item.getType();
            this.amount = item.getAmount();
            this.durability = item.getDurability();
            this.nbtData = serializeNBT(item);
        }
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 序列化物品 NBT 数据
     */
    private Map<String, Object> serializeNBT(ItemStack item) {
        Map<String, Object> data = new HashMap<>();
        
        // 序列化物品元数据
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                data.put("display_name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                data.put("lore", meta.getLore());
            }
            if (meta.hasEnchants()) {
                data.put("enchants", meta.getEnchants());
            }
            if (meta.isUnbreakable()) {
                data.put("unbreakable", true);
            }
        }
        
        return data;
    }
    
    /**
     * 验证物品是否匹配快照
     */
    public boolean matches(ItemStack item) {
        if (item == null) {
            return material == Material.AIR;
        }
        
        // 检查基础信息
        if (item.getType() != material) {
            return false;
        }
        
        if (item.getAmount() != amount) {
            return false;
        }
        
        if (item.getDurability() != durability) {
            return false;
        }
        
        // 检查 NBT 数据
        return matchesNBT(item);
    }
    
    /**
     * 验证 NBT 数据
     */
    private boolean matchesNBT(ItemStack item) {
        if (!item.hasItemMeta() && nbtData.isEmpty()) {
            return true;
        }
        
        if (!item.hasItemMeta()) {
            return nbtData.isEmpty();
        }
        
        var meta = item.getItemMeta();
        
        // 检查显示名称
        if (nbtData.containsKey("display_name")) {
            if (!meta.hasDisplayName()) {
                return false;
            }
            if (!meta.getDisplayName().equals(nbtData.get("display_name"))) {
                return false;
            }
        }
        
        // 检查 Lore
        if (nbtData.containsKey("lore")) {
            if (!meta.hasLore()) {
                return false;
            }
            var expectedLore = nbtData.get("lore");
            var actualLore = meta.getLore();
            if (!Objects.equals(expectedLore, actualLore)) {
                return false;
            }
        }
        
        // 检查附魔
        if (nbtData.containsKey("enchants")) {
            if (!meta.hasEnchants()) {
                return false;
            }
            var expectedEnchants = nbtData.get("enchants");
            var actualEnchants = meta.getEnchants();
            if (!Objects.equals(expectedEnchants, actualEnchants)) {
                return false;
            }
        }
        
        return true;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public int getDurability() {
        return durability;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isEmpty() {
        return material == Material.AIR;
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("material", material.name());
        data.put("amount", amount);
        data.put("durability", durability);
        data.put("nbt_data", nbtData);
        data.put("timestamp", timestamp);
        return data;
    }
    
    /**
     * 从序列化数据创建快照
     */
    public static ItemSnapshot deserialize(Map<String, Object> data) {
        Material material = Material.valueOf((String) data.get("material"));
        int amount = (Integer) data.get("amount");
        int durability = ((Number) data.get("durability")).intValue();
        Map<String, Object> nbtData = (Map<String, Object>) data.get("nbt_data");
        long timestamp = ((Number) data.get("timestamp")).longValue();
        
        ItemSnapshot snapshot = new ItemSnapshot(new ItemStack(material, amount));
        // 这里应该恢复 NBT 数据，但 Bukkit API 限制，暂时简化处理
        return snapshot;
    }
    
    @Override
    public String toString() {
        return "ItemSnapshot{" +
                "material=" + material +
                ", amount=" + amount +
                ", durability=" + durability +
                ", nbt_size=" + nbtData.size() +
                '}';
    }
}
