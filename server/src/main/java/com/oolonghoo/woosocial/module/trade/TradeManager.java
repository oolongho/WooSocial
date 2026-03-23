package com.oolonghoo.woosocial.module.trade;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.model.TradeOffer;
import com.oolonghoo.woosocial.module.trade.model.TradeResult;
import com.oolonghoo.woosocial.module.trade.model.TradeSession;
import com.oolonghoo.woosocial.module.trade.model.TradeState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 交易管理器
 * 管理所有交易会话的创建、执行和取消
 */
public class TradeManager {
    
    private final WooSocial plugin;
    private final TradeConfig config;
    private final MessageManager messageManager;
    
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
    
    public TradeManager(WooSocial plugin, TradeConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.messageManager = plugin.getMessageManager();
    }
    
    /**
     * 创建新的交易会话
     */
    public TradeSession createSession(Player player1, Player player2) {
        UUID player1Uuid = player1.getUniqueId();
        UUID player2Uuid = player2.getUniqueId();
        
        if (isInTrade(player1Uuid) || isInTrade(player2Uuid)) {
            return null;
        }
        
        TradeSession session = new TradeSession(
            player1Uuid, player1.getName(),
            player2Uuid, player2.getName()
        );
        
        activeSessions.put(player1Uuid, session);
        activeSessions.put(player2Uuid, session);
        
        session.setState(TradeState.PENDING);
        
        player1.playSound(player1.getLocation(), config.getSoundTradeStart(), 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), config.getSoundTradeStart(), 1.0f, 1.0f);
        
        return session;
    }
    
    /**
     * 获取玩家的交易会话
     */
    public TradeSession getSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }
    
    /**
     * 检查玩家是否在交易中
     */
    public boolean isInTrade(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }
    
    /**
     * 取消交易
     */
    public void cancelTrade(UUID playerUuid, String reason) {
        TradeSession session = activeSessions.get(playerUuid);
        if (session == null) return;
        
        cancelTrade(session, reason);
    }
    
    /**
     * 取消交易
     */
    public void cancelTrade(TradeSession session, String reason) {
        if (session.getState() == TradeState.CANCELLED || session.getState() == TradeState.COMPLETED) {
            return;
        }
        
        session.setState(TradeState.CANCELLED);
        
        stopCountdown(session);
        
        returnItems(session);
        
        Player player1 = Bukkit.getPlayer(session.getPlayer1Uuid());
        Player player2 = Bukkit.getPlayer(session.getPlayer2Uuid());
        
        if (player1 != null) {
            player1.playSound(player1.getLocation(), config.getSoundTradeCancel(), 1.0f, 1.0f);
            messageManager.send(player1, "trade.cancelled", "reason", reason);
            player1.closeInventory();
        }
        
        if (player2 != null) {
            player2.playSound(player2.getLocation(), config.getSoundTradeCancel(), 1.0f, 1.0f);
            messageManager.send(player2, "trade.cancelled", "reason", reason);
            player2.closeInventory();
        }
        
        activeSessions.remove(session.getPlayer1Uuid());
        activeSessions.remove(session.getPlayer2Uuid());
    }
    
    /**
     * 取消所有交易
     */
    public void cancelAllTrades() {
        for (TradeSession session : new HashSet<>(activeSessions.values())) {
            cancelTrade(session, "插件关闭");
        }
    }
    
    /**
     * 切换准备状态
     */
    public void toggleReady(UUID playerUuid) {
        TradeSession session = activeSessions.get(playerUuid);
        if (session == null || session.getState() != TradeState.PENDING) {
            return;
        }
        
        boolean currentState = session.isReady(playerUuid);
        session.setReady(playerUuid, !currentState);
        
        if (session.isBothReady()) {
            startCountdown(session);
        } else {
            stopCountdown(session);
        }
    }
    
    /**
     * 开始倒计时
     */
    private void startCountdown(TradeSession session) {
        stopCountdown(session);
        
        session.setState(TradeState.COUNTDOWN);
        int seconds = config.getCountdownSeconds();
        session.setCountdownEndTime(System.currentTimeMillis() + (seconds * 1000L));
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (session.getState() == TradeState.COUNTDOWN && session.isBothReady()) {
                executeTrade(session);
            }
        }, seconds * 20L);
        
        countdownTasks.put(session.getSessionId(), task);
        
        Player player1 = Bukkit.getPlayer(session.getPlayer1Uuid());
        Player player2 = Bukkit.getPlayer(session.getPlayer2Uuid());
        
        for (int i = seconds; i > 0; i--) {
            final int count = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (session.getState() != TradeState.COUNTDOWN) return;
                
                if (player1 != null && player1.isOnline()) {
                    player1.playSound(player1.getLocation(), config.getSoundCountdownTick(), 1.0f, 1.0f);
                    messageManager.send(player1, "trade.countdown", "seconds", String.valueOf(count));
                }
                if (player2 != null && player2.isOnline()) {
                    player2.playSound(player2.getLocation(), config.getSoundCountdownTick(), 1.0f, 1.0f);
                    messageManager.send(player2, "trade.countdown", "seconds", String.valueOf(count));
                }
            }, (seconds - i) * 20L);
        }
    }
    
    /**
     * 停止倒计时
     */
    private void stopCountdown(TradeSession session) {
        BukkitTask task = countdownTasks.remove(session.getSessionId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * 执行交易
     */
    private void executeTrade(TradeSession session) {
        session.setState(TradeState.COMPLETING);
        
        TradeOffer offer1 = session.getOffer1();
        TradeOffer offer2 = session.getOffer2();
        
        Player player1 = Bukkit.getPlayer(session.getPlayer1Uuid());
        Player player2 = Bukkit.getPlayer(session.getPlayer2Uuid());
        
        if (player1 == null || player2 == null || !player1.isOnline() || !player2.isOnline()) {
            cancelTrade(session, "对方已离线");
            return;
        }
        
        if (!validateTrade(session, player1, player2)) {
            cancelTrade(session, "交易验证失败");
            return;
        }
        
        boolean success = processTrade(session, player1, player2);
        
        if (success) {
            session.setState(TradeState.COMPLETED);
            
            player1.playSound(player1.getLocation(), config.getSoundTradeComplete(), 1.0f, 1.0f);
            player2.playSound(player2.getLocation(), config.getSoundTradeComplete(), 1.0f, 1.0f);
            
            messageManager.send(player1, "trade.completed", "player", session.getPlayer2Name());
            messageManager.send(player2, "trade.completed", "player", session.getPlayer1Name());
            
            player1.closeInventory();
            player2.closeInventory();
        } else {
            cancelTrade(session, "交易处理失败");
        }
        
        activeSessions.remove(session.getPlayer1Uuid());
        activeSessions.remove(session.getPlayer2Uuid());
    }
    
    /**
     * 验证交易
     */
    private boolean validateTrade(TradeSession session, Player player1, Player player2) {
        TradeOffer offer1 = session.getOffer1();
        TradeOffer offer2 = session.getOffer2();
        
        if (offer1.getMoney() > 0 || offer2.getMoney() > 0) {
            if (config.isVaultEnabled()) {
                if (!plugin.getVaultHook().has(player1, offer1.getMoney())) {
                    messageManager.send(player1, "trade.not-enough-money");
                    return false;
                }
                if (!plugin.getVaultHook().has(player2, offer2.getMoney())) {
                    messageManager.send(player2, "trade.not-enough-money");
                    return false;
                }
            }
        }
        
        if (offer1.getPoints() > 0 || offer2.getPoints() > 0) {
            if (config.isPlayerPointsEnabled()) {
                if (!plugin.getPlayerPointsHook().has(player1, offer1.getPoints())) {
                    messageManager.send(player1, "trade.not-enough-points");
                    return false;
                }
                if (!plugin.getPlayerPointsHook().has(player2, offer2.getPoints())) {
                    messageManager.send(player2, "trade.not-enough-points");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 处理交易
     */
    private boolean processTrade(TradeSession session, Player player1, Player player2) {
        TradeOffer offer1 = session.getOffer1();
        TradeOffer offer2 = session.getOffer2();
        
        try {
            for (ItemStack item : offer2.getItems()) {
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> leftover = player1.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player1.getWorld().dropItem(player1.getLocation(), drop);
                    }
                }
            }
            
            for (ItemStack item : offer1.getItems()) {
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> leftover = player2.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player2.getWorld().dropItem(player2.getLocation(), drop);
                    }
                }
            }
            
            if (config.isVaultEnabled()) {
                if (offer1.getMoney() > 0) {
                    plugin.getVaultHook().withdraw(player1, offer1.getMoney());
                    plugin.getVaultHook().deposit(player2, offer1.getMoney());
                }
                if (offer2.getMoney() > 0) {
                    plugin.getVaultHook().withdraw(player2, offer2.getMoney());
                    plugin.getVaultHook().deposit(player1, offer2.getMoney());
                }
            }
            
            if (config.isPlayerPointsEnabled()) {
                if (offer1.getPoints() > 0) {
                    plugin.getPlayerPointsHook().withdraw(player1, offer1.getPoints());
                    plugin.getPlayerPointsHook().deposit(player2, offer1.getPoints());
                }
                if (offer2.getPoints() > 0) {
                    plugin.getPlayerPointsHook().withdraw(player2, offer2.getPoints());
                    plugin.getPlayerPointsHook().deposit(player1, offer2.getPoints());
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[Trade] 交易处理异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 归还物品
     */
    private void returnItems(TradeSession session) {
        Player player1 = Bukkit.getPlayer(session.getPlayer1Uuid());
        Player player2 = Bukkit.getPlayer(session.getPlayer2Uuid());
        
        TradeOffer offer1 = session.getOffer1();
        TradeOffer offer2 = session.getOffer2();
        
        if (player1 != null && player1.isOnline()) {
            for (ItemStack item : offer1.getItems()) {
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> leftover = player1.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player1.getWorld().dropItem(player1.getLocation(), drop);
                    }
                }
            }
        }
        
        if (player2 != null && player2.isOnline()) {
            for (ItemStack item : offer2.getItems()) {
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> leftover = player2.getInventory().addItem(item);
                    for (ItemStack drop : leftover.values()) {
                        player2.getWorld().dropItem(player2.getLocation(), drop);
                    }
                }
            }
        }
    }
    
    /**
     * 处理物品放入
     */
    public boolean handleItemPut(UUID playerUuid, ItemStack item, int slot) {
        if (config.isBlacklisted(item.getType())) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                player.playSound(player.getLocation(), config.getSoundBlockedItem(), 1.0f, 1.0f);
                messageManager.send(player, "trade.item-blocked");
            }
            return false;
        }
        
        TradeSession session = activeSessions.get(playerUuid);
        if (session == null) return false;
        
        TradeOffer offer = session.getOffer(playerUuid);
        if (offer == null) return false;
        
        offer.addItem(item, slot);
        
        if (config.isRevokeReadyOnChange()) {
            session.resetReadyStates();
        }
        
        return true;
    }
    
    /**
     * 处理物品取出
     */
    public ItemStack handleItemTake(UUID playerUuid, int slot) {
        TradeSession session = activeSessions.get(playerUuid);
        if (session == null) return null;
        
        TradeOffer offer = session.getOffer(playerUuid);
        if (offer == null) return null;
        
        ItemStack item = offer.removeItem(slot);
        
        if (config.isRevokeReadyOnChange() && item != null) {
            session.resetReadyStates();
        }
        
        return item;
    }
    
    /**
     * 处理GUI切换（防止复制漏洞）
     */
    public void acknowledgeGuiSwitch(Player player) {
        TradeSession session = activeSessions.get(player.getUniqueId());
        if (session != null && session.getState() == TradeState.COUNTDOWN) {
            session.setReady(player.getUniqueId(), false);
            stopCountdown(session);
            session.setState(TradeState.PENDING);
        }
    }
}
