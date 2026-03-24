package com.oolonghoo.woosocial.module.trade.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 交易统计数据
 * 记录服务器交易统计信息
 */
public class TradeStatistics {
    
    private final AtomicLong totalTrades = new AtomicLong(0);
    private final AtomicLong totalMoneyTraded = new AtomicLong(0); // 以分为单位存储，避免精度丢失
    private final AtomicLong totalPointsTraded = new AtomicLong(0);
    private final AtomicLong cancelledTrades = new AtomicLong(0);
    private final AtomicLong todayTrades = new AtomicLong(0);
    
    private final Map<String, AtomicLong> itemTradeCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> playerTradeCounts = new ConcurrentHashMap<>();
    
    private long lastResetTime = System.currentTimeMillis();
    
    /**
     * 记录完成的交易
     * @param money 金额（元，支持小数）
     * @param points 点数
     * @param itemCount 物品数量
     */
    public void recordTrade(double money, int points, int itemCount) {
        totalTrades.incrementAndGet();
        todayTrades.incrementAndGet();
        
        if (money > 0) {
            // 将元转换为分存储，避免精度丢失
            long moneyInCents = Math.round(money * 100);
            totalMoneyTraded.addAndGet(moneyInCents);
        }
        
        if (points > 0) {
            totalPointsTraded.addAndGet(points);
        }
    }
    
    /**
     * 记录取消的交易
     */
    public void recordCancelled() {
        cancelledTrades.incrementAndGet();
    }
    
    /**
     * 记录物品交易
     */
    public void recordItemTrade(String materialName) {
        itemTradeCounts.computeIfAbsent(materialName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 记录玩家交易
     */
    public void recordPlayerTrade(String playerName) {
        playerTradeCounts.computeIfAbsent(playerName.toLowerCase(), k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 重置每日统计
     */
    public void resetDaily() {
        todayTrades.set(0);
        lastResetTime = System.currentTimeMillis();
    }
    
    // Getters
    public long getTotalTrades() { return totalTrades.get(); }
    
    /**
     * 获取总交易金额（分）
     * @return 总金额（分），如需转换为元请除以 100.0
     */
    public long getTotalMoneyTraded() { return totalMoneyTraded.get(); }
    
    /**
     * 获取总交易金额（元）
     * @return 总金额（元）
     */
    public double getTotalMoneyTradedInYuan() { 
        return totalMoneyTraded.get() / 100.0; 
    }
    
    public long getTotalPointsTraded() { return totalPointsTraded.get(); }
    public long getCancelledTrades() { return cancelledTrades.get(); }
    public long getTodayTrades() { return todayTrades.get(); }
    public long getLastResetTime() { return lastResetTime; }
    
    /**
     * 获取交易成功率
     */
    public double getSuccessRate() {
        long total = totalTrades.get() + cancelledTrades.get();
        if (total == 0) return 0;
        return (double) totalTrades.get() / total * 100;
    }
    
    /**
     * 获取热门物品
     */
    public Map<String, Long> getTopItems(int limit) {
        return itemTradeCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }
    
    /**
     * 获取活跃交易者
     */
    public Map<String, Long> getTopTraders(int limit) {
        return playerTradeCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }
}
