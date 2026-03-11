package com.oolonghoo.woosocial.hook;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class PlayerPointsHook implements EconomyHook {
    
    private final WooSocial plugin;
    private Object playerPointsAPI;
    private Method lookMethod;
    private Method takeMethod;
    private Method giveMethod;
    private boolean enabled;
    
    public PlayerPointsHook(WooSocial plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }
    
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }
        
        try {
            Class<?> playerPointsClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            
            Method getAPIMethod = playerPointsClass.getMethod("getAPI");
            playerPointsAPI = getAPIMethod.invoke(playerPoints);
            
            Class<?> apiClass = playerPointsAPI.getClass();
            lookMethod = apiClass.getMethod("look", UUID.class);
            takeMethod = apiClass.getMethod("take", UUID.class, int.class);
            giveMethod = apiClass.getMethod("give", UUID.class, int.class);
            
            enabled = true;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("连接 PlayerPoints 失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && playerPointsAPI != null;
    }
    
    @Override
    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }
    
    @Override
    public double getBalance(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return getBalance(player.getUniqueId());
        }
        return 0;
    }
    
    public int getBalance(UUID uuid) {
        if (!isEnabled()) return 0;
        try {
            return (int) lookMethod.invoke(playerPointsAPI, uuid);
        } catch (Exception e) {
            return 0;
        }
    }
    
    @Override
    public boolean has(Player player, double amount) {
        return has(player.getUniqueId(), (int) amount);
    }
    
    @Override
    public boolean has(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return has(player.getUniqueId(), (int) amount);
        }
        return false;
    }
    
    public boolean has(UUID uuid, int amount) {
        if (!isEnabled()) return false;
        return getBalance(uuid) >= amount;
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        return withdraw(player.getUniqueId(), (int) amount);
    }
    
    @Override
    public boolean withdraw(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return withdraw(player.getUniqueId(), (int) amount);
        }
        return false;
    }
    
    public boolean withdraw(UUID uuid, int amount) {
        if (!isEnabled()) return false;
        try {
            return (boolean) takeMethod.invoke(playerPointsAPI, uuid, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        return deposit(player.getUniqueId(), (int) amount);
    }
    
    @Override
    public boolean deposit(String playerName, double amount) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return deposit(player.getUniqueId(), (int) amount);
        }
        return false;
    }
    
    public boolean deposit(UUID uuid, int amount) {
        if (!isEnabled()) return false;
        try {
            return (boolean) giveMethod.invoke(playerPointsAPI, uuid, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String format(double amount) {
        return String.format("%.0f 点券", amount);
    }
    
    @Override
    public String getName() {
        return "PlayerPoints";
    }
}
