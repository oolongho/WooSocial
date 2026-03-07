package com.oolonghoo.woosocial.hook;

import com.oolonghoo.wooeco.api.WooEcoAPI;
import com.oolonghoo.wooeco.manager.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WooEcoHook implements EconomyHook {
    
    private boolean enabled;
    
    public WooEcoHook() {
        this.enabled = false;
    }
    
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("WooEco") == null) {
            return false;
        }
        
        if (!WooEcoAPI.isLoaded()) {
            return false;
        }
        
        enabled = true;
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && WooEcoAPI.isLoaded();
    }
    
    @Override
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return WooEcoAPI.getBalance(player.getUniqueId());
    }
    
    @Override
    public double getBalance(String playerName) {
        if (!isEnabled()) return 0;
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return WooEcoAPI.getBalance(player.getUniqueId());
        }
        return 0;
    }
    
    @Override
    public boolean has(Player player, double amount) {
        if (!isEnabled()) return false;
        return WooEcoAPI.has(player.getUniqueId(), amount);
    }
    
    @Override
    public boolean has(String playerName, double amount) {
        if (!isEnabled()) return false;
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return WooEcoAPI.has(player.getUniqueId(), amount);
        }
        return false;
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        EconomyManager.EconomyResult result = WooEcoAPI.withdraw(player.getUniqueId(), amount);
        return result.isSuccess();
    }
    
    @Override
    public boolean withdraw(String playerName, double amount) {
        if (!isEnabled()) return false;
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            EconomyManager.EconomyResult result = WooEcoAPI.withdraw(player.getUniqueId(), amount);
            return result.isSuccess();
        }
        return false;
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) return false;
        EconomyManager.EconomyResult result = WooEcoAPI.deposit(player.getUniqueId(), amount);
        return result.isSuccess();
    }
    
    @Override
    public boolean deposit(String playerName, double amount) {
        if (!isEnabled()) return false;
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            EconomyManager.EconomyResult result = WooEcoAPI.deposit(player.getUniqueId(), amount);
            return result.isSuccess();
        }
        return false;
    }
    
    @Override
    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        return WooEcoAPI.format(amount);
    }
    
    @Override
    public String getName() {
        return isEnabled() ? "WooEco" : "None";
    }
    
    public boolean hasAccount(UUID uuid) {
        if (!isEnabled()) return false;
        return WooEcoAPI.hasAccount(uuid);
    }
    
    public double getBalance(UUID uuid) {
        if (!isEnabled()) return 0;
        return WooEcoAPI.getBalance(uuid);
    }
    
    public boolean has(UUID uuid, double amount) {
        if (!isEnabled()) return false;
        return WooEcoAPI.has(uuid, amount);
    }
    
    public boolean withdraw(UUID uuid, double amount) {
        if (!isEnabled()) return false;
        EconomyManager.EconomyResult result = WooEcoAPI.withdraw(uuid, amount);
        return result.isSuccess();
    }
    
    public boolean deposit(UUID uuid, double amount) {
        if (!isEnabled()) return false;
        EconomyManager.EconomyResult result = WooEcoAPI.deposit(uuid, amount);
        return result.isSuccess();
    }
}
