package com.oolonghoo.woosocial.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * WooSocial 事件基类
 * <p>
 * 所有 WooSocial 插件的事件都继承此类，提供统一的 HandlerList 管理
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public abstract class WooSocialEvent extends Event {
    
    /**
     * 事件处理器列表
     * 使用静态 HandlerList 模式，确保每个事件类型有独立的处理器列表
     */
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 是否异步执行
     */
    private final boolean async;
    
    /**
     * 构造函数，默认为同步事件
     */
    public WooSocialEvent() {
        this(false);
    }
    
    /**
     * 构造函数
     *
     * @param async 是否异步执行
     */
    public WooSocialEvent(boolean async) {
        super(async);
        this.async = async;
    }
    
    /**
     * 获取事件处理器列表
     *
     * @return HandlerList 实例
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    /**
     * 获取事件处理器列表（静态方法）
     * Bukkit API 需要此方法来注册事件处理器
     *
     * @return HandlerList 实例
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    /**
     * 检查事件是否异步执行
     *
     * @return true 如果事件异步执行，否则 false
     */
    public boolean isAsync() {
        return async;
    }
}
