package com.oolonghoo.woosocial.attachment;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.hook.VaultHook;
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
 * 金币附件实现
 * 存储金币金额，领取时通过Vault存入玩家账户
 * 
 * @author oolongho
 * @version 1.0.0
 */
public class MoneyAttachment implements IAttachment {
    
    private double amount;
    
    /**
     * 默认构造函数
     */
    public MoneyAttachment() {
        this.amount = 0;
    }
    
    /**
     * 创建金币附件
     * 
     * @param amount 金币数量
     */
    public MoneyAttachment(double amount) {
        this.amount = amount;
    }
    
    @Override
    public boolean use(Player player) {
        if (!isLegal()) {
            return false;
        }
        
        // 获取VaultHook实例
        VaultHook vaultHook = getVaultHook();
        if (vaultHook == null || !vaultHook.isEnabled()) {
            return false;
        }
        
        // 存入金币
        return vaultHook.deposit(player, amount);
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
            this.amount = obj.get("amount").getAsDouble();
            return this;
        } catch (Exception e) {
            // 解析失败
        }
        
        return null;
    }
    
    @Override
    public AttachmentType getType() {
        return AttachmentType.MONEY;
    }
    
    @Override
    public boolean isLegal() {
        return amount > 0;
    }
    
    @Override
    public ItemStack generateIcon() {
        ItemStack icon = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = icon.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6金币附件");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7金额: §e" + formatAmount());
            lore.add("");
            lore.add("§e点击领取");
            meta.setLore(lore);
            
            icon.setItemMeta(meta);
        }
        
        return icon;
    }
    
    @Override
    public String getDescription() {
        return "§6" + formatAmount();
    }
    
    /**
     * 格式化金额显示
     * 
     * @return 格式化后的金额字符串
     */
    private String formatAmount() {
        VaultHook vaultHook = getVaultHook();
        if (vaultHook != null && vaultHook.isEnabled()) {
            return vaultHook.format(amount);
        }
        return String.format("%.2f 金币", amount);
    }
    
    /**
     * 获取VaultHook实例
     * 
     * @return VaultHook实例，可能为null
     */
    private VaultHook getVaultHook() {
        WooSocial plugin = (WooSocial) Bukkit.getPluginManager().getPlugin("WooSocial");
        if (plugin == null) {
            return null;
        }
        
        // 通过模块管理器获取经济Hook
        // 注意：这里需要根据实际项目的Hook管理方式调整
        // 暂时返回null，实际使用时需要从插件获取
        return null;
    }
    
    /**
     * 获取金币数量
     * 
     * @return 金币数量
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * 设置金币数量
     * 
     * @param amount 金币数量
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
