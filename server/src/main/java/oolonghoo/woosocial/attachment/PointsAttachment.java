package com.oolonghoo.woosocial.attachment;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.hook.PlayerPointsHook;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 点券附件实现
 * 存储点券数量，领取时通过PlayerPoints存入玩家账户
 * 
 * @author oolongho
 * @version 1.0.0
 */
public class PointsAttachment implements IAttachment {
    
    private int amount;
    
    /**
     * 默认构造函数
     */
    public PointsAttachment() {
        this.amount = 0;
    }
    
    /**
     * 创建点券附件
     * 
     * @param amount 点券数量
     */
    public PointsAttachment(int amount) {
        this.amount = amount;
    }
    
    @Override
    public boolean use(Player player) {
        if (!isLegal()) {
            return false;
        }
        
        // 获取PlayerPointsHook实例
        PlayerPointsHook pointsHook = getPlayerPointsHook();
        if (pointsHook == null || !pointsHook.isEnabled()) {
            return false;
        }
        
        // 存入点券
        return pointsHook.deposit(player, amount);
    }
    
    @Override
    public String serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getType().getIdentifier());
        json.addProperty("amount", amount);
        
        return json.toString();
    }
    
    @Override
    public IAttachment deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            this.amount = obj.get("amount").getAsInt();
            return this;
        } catch (Exception e) {
            // 解析失败
        }
        
        return null;
    }
    
    @Override
    public AttachmentType getType() {
        return AttachmentType.POINTS;
    }
    
    @Override
    public boolean isLegal() {
        return amount > 0;
    }
    
    @Override
    public ItemStack generateIcon() {
        ItemStack icon = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = icon.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b点券附件");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7数量: §b" + amount + " 点券");
            lore.add("");
            lore.add("§e点击领取");
            meta.setLore(lore);
            
            icon.setItemMeta(meta);
        }
        
        return icon;
    }
    
    @Override
    public String getDescription() {
        return "§b" + amount + " 点券";
    }
    
    /**
     * 获取PlayerPointsHook实例
     * 
     * @return PlayerPointsHook实例，可能为null
     */
    private PlayerPointsHook getPlayerPointsHook() {
        WooSocial plugin = (WooSocial) Bukkit.getPluginManager().getPlugin("WooSocial");
        if (plugin == null) {
            return null;
        }
        
        // 通过模块管理器获取点券Hook
        // 注意：这里需要根据实际项目的Hook管理方式调整
        // 暂时返回null，实际使用时需要从插件获取
        return null;
    }
    
    /**
     * 获取点券数量
     * 
     * @return 点券数量
     */
    public int getAmount() {
        return amount;
    }
    
    /**
     * 设置点券数量
     * 
     * @param amount 点券数量
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
