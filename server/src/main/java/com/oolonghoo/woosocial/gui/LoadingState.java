package com.oolonghoo.woosocial.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载状态管理器
 * 用于管理玩家在GUI操作中的加载状态，防止重复操作
 * 
 * <p>使用场景：
 * <ul>
 *   <li>打开GUI时的数据加载</li>
 *   <li>翻页时的数据加载</li>
 *   <li>领取附件时的处理状态</li>
 *   <li>其他需要防止重复操作的异步操作</li>
 * </ul>
 * 
 * @author oolongho
 */
public class LoadingState {
    
    /**
     * 玩家加载状态映射
     * Key: 玩家UUID
     * Value: 是否处于加载状态
     */
    private final Map<UUID, Boolean> loadingStates = new ConcurrentHashMap<>();
    
    /**
     * 设置玩家的加载状态
     * 
     * @param playerId 玩家UUID
     * @param loading 是否处于加载状态
     */
    public void setLoading(UUID playerId, boolean loading) {
        if (loading) {
            loadingStates.put(playerId, true);
        } else {
            loadingStates.remove(playerId);
        }
    }
    
    /**
     * 检查玩家是否处于加载状态
     * 
     * @param playerId 玩家UUID
     * @return 如果玩家正在加载中返回true，否则返回false
     */
    public boolean isLoading(UUID playerId) {
        return loadingStates.containsKey(playerId);
    }
    
    /**
     * 在加载状态下执行操作
     * 自动管理加载状态的设置和清除
     * 
     * <p>使用示例：
     * <pre>{@code
     * loadingState.withLoading(player.getUniqueId(), () -> {
     *     // 执行异步操作
     *     loadDataAsync(player);
     * });
     * }</pre>
     * 
     * @param playerId 玩家UUID
     * @param action 要执行的操作
     * @return 如果成功开始执行返回true，如果已经在加载中返回false
     */
    public boolean withLoading(UUID playerId, Runnable action) {
        // 检查是否已经在加载中
        if (isLoading(playerId)) {
            return false;
        }
        
        // 设置加载状态
        setLoading(playerId, true);
        
        try {
            // 执行操作
            action.run();
            return true;
        } catch (Exception e) {
            // 发生异常时清除加载状态
            setLoading(playerId, false);
            throw e;
        }
    }
    
    /**
     * 清除玩家的加载状态
     * 用于强制清除加载状态（如发生错误或玩家退出）
     * 
     * @param playerId 玩家UUID
     */
    public void clearLoading(UUID playerId) {
        loadingStates.remove(playerId);
    }
    
    /**
     * 清除所有加载状态
     * 用于插件禁用时清理资源
     */
    public void clearAll() {
        loadingStates.clear();
    }
    
    /**
     * 获取当前处于加载状态的玩家数量
     * 用于监控和调试
     * 
     * @return 加载中的玩家数量
     */
    public int getLoadingCount() {
        return loadingStates.size();
    }
    
    /**
     * 检查是否有任何玩家处于加载状态
     * 
     * @return 如果有玩家正在加载返回true
     */
    public boolean hasAnyLoading() {
        return !loadingStates.isEmpty();
    }
}
