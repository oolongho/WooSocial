package com.oolonghoo.woosocial.attachment;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.hook.EconomyHook;
import com.oolonghoo.woosocial.module.relation.RelationManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MoneyAttachment implements IAttachment {
    
    private double amount;
    
    public MoneyAttachment() {
        this.amount = 0;
    }
    
    public MoneyAttachment(double amount) {
        this.amount = amount;
    }
    
    @Override
    public boolean use(Player player) {
        if (!isLegal()) {
            return false;
        }
        
        EconomyHook economyHook = getEconomyHook();
        if (economyHook == null || !economyHook.isEnabled()) {
            return false;
        }
        
        return economyHook.deposit(player, amount);
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
            meta.displayName(net.kyori.adventure.text.Component.text("§6金币附件"));
            
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§7金额: §e" + formatAmount()));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§e点击领取"));
            meta.lore(lore);
            
            icon.setItemMeta(meta);
        }
        
        return icon;
    }
    
    @Override
    public String getDescription() {
        return "§6" + formatAmount();
    }
    
    private String formatAmount() {
        EconomyHook economyHook = getEconomyHook();
        if (economyHook != null && economyHook.isEnabled()) {
            return economyHook.format(amount);
        }
        return String.format("%.2f 金币", amount);
    }
    
    private EconomyHook getEconomyHook() {
        WooSocial plugin = (WooSocial) Bukkit.getPluginManager().getPlugin("WooSocial");
        if (plugin == null) {
            return null;
        }
        
        RelationManager relationManager = plugin.getRelationModule().getRelationManager();
        if (relationManager == null) {
            return null;
        }
        
        return relationManager.getPrimaryEconomyHook();
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
