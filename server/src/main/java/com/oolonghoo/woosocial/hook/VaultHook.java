package com.oolonghoo.woosocial.hook;

import com.oolonghoo.woosocial.WooSocial;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook implements EconomyHook {
    
    private final WooSocial plugin;
    private Economy economy;
    private boolean enabled;
    
    public VaultHook(WooSocial plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }
    
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到 Vault 插件");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到经济系统提供者");
            return false;
        }
        
        economy = rsp.getProvider();
        enabled = true;
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    @Override
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }
    
    @Override
    public double getBalance(String playerName) {
        if (!isEnabled()) return 0;
        return economy.getBalance(playerName);
    }
    
    @Override
    public boolean has(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }
    
    @Override
    public boolean has(String playerName, double amount) {
        if (!isEnabled()) return false;
        return economy.has(playerName, amount);
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public boolean withdraw(String playerName, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(playerName, amount).transactionSuccess();
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public boolean deposit(String playerName, double amount) {
        if (!isEnabled()) return false;
        return economy.depositPlayer(playerName, amount).transactionSuccess();
    }
    
    @Override
    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        return economy.format(amount);
    }
    
    @Override
    public String getName() {
        return isEnabled() ? economy.getName() : "None";
    }
    
    public Economy getEconomy() {
        return economy;
    }
}
