package com.oolonghoo.woosocial.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;

public class ItemSerializer {
    
    private static final int MAX_ITEM_SIZE = 65535;
    
    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos);
            oos.writeObject(item);
            oos.flush();
            oos.close();
            
            byte[] bytes = bos.toByteArray();
            String encoded = Base64.getEncoder().encodeToString(bytes);
            
            if (encoded.length() > MAX_ITEM_SIZE) {
                Bukkit.getLogger().warning("[WooSocial] 物品数据过大: " + encoded.length() + " bytes");
            }
            
            return encoded;
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "[WooSocial] 物品序列化失败: " + e.getMessage());
            return null;
        }
    }
    
    public static ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bis);
            ItemStack item = (ItemStack) ois.readObject();
            ois.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.WARNING, "[WooSocial] 物品反序列化失败: " + e.getMessage());
            return null;
        }
    }
    
    public static boolean isValidItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }
    
    public static String getItemDisplayName(ItemStack item) {
        if (item == null) return "未知物品";
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        return item.getType().name().replace("_", " ").toLowerCase();
    }
    
    public static int estimateSize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        String serialized = serialize(item);
        return serialized != null ? serialized.length() : 0;
    }
}
