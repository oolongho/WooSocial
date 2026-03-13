package com.oolonghoo.woosocial.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class ItemSerializer {
    
    private static final int MAX_ITEM_SIZE = 65535;
    
    // 允许反序列化的 Bukkit 类白名单
    private static final Set<String> ALLOWED_CLASSES = Set.of(
        "org.bukkit.inventory.ItemStack",
        "org.bukkit.Material",
        "org.bukkit.inventory.meta.ItemMeta",
        "org.bukkit.inventory.meta.BookMeta",
        "org.bukkit.inventory.meta.EnchantmentStorageMeta",
        "org.bukkit.inventory.meta.FireworkEffectMeta",
        "org.bukkit.inventory.meta.FireworkMeta",
        "org.bukkit.inventory.meta.LeatherArmorMeta",
        "org.bukkit.inventory.meta.MapMeta",
        "org.bukkit.inventory.meta.PotionMeta",
        "org.bukkit.inventory.meta.Repairable",
        "org.bukkit.inventory.meta.SkullMeta",
        "org.bukkit.inventory.meta.BannerMeta",
        "org.bukkit.inventory.meta.BlockStateMeta",
        "org.bukkit.inventory.meta.ColorableArmorMeta",
        "org.bukkit.inventory.meta.Damageable",
        "org.bukkit.inventory.meta.KnowledgeBookMeta",
        "org.bukkit.inventory.meta.CrossbowMeta",
        "org.bukkit.inventory.meta.SuspiciousStewMeta",
        "org.bukkit.inventory.meta.TropicalFishBucketMeta",
        "org.bukkit.inventory.meta.AxolotlBucketMeta",
        "org.bukkit.inventory.meta.GlowItemFrameMeta",
        "org.bukkit.Color",
        "org.bukkit.FireworkEffect",
        "org.bukkit.FireworkEffect.Type",
        "org.bukkit.potion.PotionEffect",
        "org.bukkit.potion.PotionEffectType",
        "org.bukkit.potion.PotionData",
        "org.bukkit.potion.PotionType",
        "org.bukkit.block.BlockState",
        "org.bukkit.block.Banner",
        "org.bukkit.block.Barrel",
        "org.bukkit.block.Beacon",
        "org.bukkit.block.Bell",
        "org.bukkit.block.BlastFurnace",
        "org.bukkit.block.BrewingStand",
        "org.bukkit.block.Campfire",
        "org.bukkit.block.Chest",
        "org.bukkit.block.CreatureSpawner",
        "org.bukkit.block.DaylightDetector",
        "org.bukkit.block.Dispenser",
        "org.bukkit.block.Dropper",
        "org.bukkit.block.EnchantingTable",
        "org.bukkit.block.EnderChest",
        "org.bukkit.block.Furnace",
        "org.bukkit.block.Hopper",
        "org.bukkit.block.Jukebox",
        "org.bukkit.block.Lectern",
        "org.bukkit.block.Lootable",
        "org.bukkit.block.ShulkerBox",
        "org.bukkit.block.Sign",
        "org.bukkit.block.Smoker",
        "org.bukkit.block.StructureSpawnMarker",
        "org.bukkit.configuration.serialization.ConfigurationSerializable",
        "org.bukkit.util.Vector",
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.List",
        "java.util.Map",
        "java.lang.String"
    );
    
    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        ByteArrayOutputStream bos = null;
        BukkitObjectOutputStream oos = null;
        
        try {
            bos = new ByteArrayOutputStream();
            oos = new BukkitObjectOutputStream(bos);
            oos.writeObject(item);
            oos.flush();
            
            byte[] bytes = bos.toByteArray();
            String encoded = Base64.getEncoder().encodeToString(bytes);
            
            if (encoded.length() > MAX_ITEM_SIZE) {
                Bukkit.getLogger().warning(() -> "[WooSocial] 物品数据过大：" + encoded.length() + " bytes");
            }
            
            return encoded;
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, () -> "[WooSocial] 物品序列化失败：" + e.getMessage());
            return null;
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    Bukkit.getLogger().log(Level.FINE, () -> "[WooSocial] 关闭输出流失败：" + e.getMessage());
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    Bukkit.getLogger().log(Level.FINE, () -> "[WooSocial] 关闭字节流失败：" + e.getMessage());
                }
            }
        }
    }
    
    public static ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            
            if (bytes.length > MAX_ITEM_SIZE) {
                Bukkit.getLogger().warning(() -> "[WooSocial] 物品数据过大：" + bytes.length + " bytes");
                return null;
            }
            
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 SafeBukkitObjectInputStream ois = new SafeBukkitObjectInputStream(bis)) {
                ItemStack item = (ItemStack) ois.readObject();
                return item;
            }
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.WARNING, () -> "[WooSocial] 物品反序列化失败：" + e.getMessage());
            return null;
        }
    }
    
    /**
     * 安全的 BukkitObjectInputStream，限制可反序列化的类
     */
    private static class SafeBukkitObjectInputStream extends BukkitObjectInputStream {
        
        public SafeBukkitObjectInputStream(ByteArrayInputStream in) throws IOException {
            super(in);
        }
        
        @Override
        protected Class<?> resolveClass(java.io.ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String className = desc.getName();
            if (!ALLOWED_CLASSES.contains(className)) {
                throw new ClassNotFoundException("不允许反序列化的类：" + className);
            }
            return super.resolveClass(desc);
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
