package com.oolonghoo.woosocial.hook;

import org.bukkit.entity.Player;

public interface EconomyHook {
    
    boolean isEnabled();
    
    double getBalance(Player player);
    
    double getBalance(String playerName);
    
    boolean has(Player player, double amount);
    
    boolean has(String playerName, double amount);
    
    boolean withdraw(Player player, double amount);
    
    boolean withdraw(String playerName, double amount);
    
    boolean deposit(Player player, double amount);
    
    boolean deposit(String playerName, double amount);
    
    String format(double amount);
    
    String getName();
}
