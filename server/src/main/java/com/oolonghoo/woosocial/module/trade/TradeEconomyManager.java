package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.hook.EconomyHook;
import com.oolonghoo.woosocial.hook.PlayerPointsHook;
import com.oolonghoo.woosocial.hook.VaultHook;
import org.bukkit.entity.Player;

/**
 * 交易经济管理器
 * 管理金币和点券的交易操作
 */
public class TradeEconomyManager {
    
    private final WooSocial plugin;
    private VaultHook vaultHook;
    private PlayerPointsHook playerPointsHook;
    
    public TradeEconomyManager(WooSocial plugin) {
        this.plugin = plugin;
        initializeHooks();
    }
    
    private void initializeHooks() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                vaultHook = new VaultHook(plugin);
                plugin.getLogger().info("[Trade] Vault 集成已启用");
            } catch (Exception e) {
                plugin.getLogger().warning("[Trade] Vault 集成失败: " + e.getMessage());
            }
        }
        
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            try {
                playerPointsHook = new PlayerPointsHook(plugin);
                plugin.getLogger().info("[Trade] PlayerPoints 集成已启用");
            } catch (Exception e) {
                plugin.getLogger().warning("[Trade] PlayerPoints 集成失败: " + e.getMessage());
            }
        }
    }
    
    public boolean hasVault() {
        return vaultHook != null && vaultHook.isEnabled();
    }
    
    public boolean hasPlayerPoints() {
        return playerPointsHook != null && playerPointsHook.isEnabled();
    }
    
    public boolean hasMoney(Player player, double amount) {
        if (!hasVault()) return false;
        return vaultHook.has(player, amount);
    }
    
    public boolean hasPoints(Player player, int amount) {
        if (!hasPlayerPoints()) return false;
        return playerPointsHook.has(player, amount);
    }
    
    public double getBalance(Player player) {
        if (!hasVault()) return 0;
        return vaultHook.getBalance(player);
    }
    
    public int getPoints(Player player) {
        if (!hasPlayerPoints()) return 0;
        return (int) playerPointsHook.getBalance(player);
    }
    
    public boolean withdrawMoney(Player player, double amount) {
        if (!hasVault()) return false;
        return vaultHook.withdraw(player, amount);
    }
    
    public boolean depositMoney(Player player, double amount) {
        if (!hasVault()) return false;
        return vaultHook.deposit(player, amount);
    }
    
    public boolean withdrawPoints(Player player, int amount) {
        if (!hasPlayerPoints()) return false;
        return playerPointsHook.withdraw(player, amount);
    }
    
    public boolean depositPoints(Player player, int amount) {
        if (!hasPlayerPoints()) return false;
        return playerPointsHook.deposit(player, amount);
    }
    
    public String formatMoney(double amount) {
        if (!hasVault()) return String.format("%.2f", amount);
        return vaultHook.format(amount);
    }
}
