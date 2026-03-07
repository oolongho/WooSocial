package com.oolonghoo.woosocial.module;

import com.oolonghoo.woosocial.WooSocial;

/**
 * 模块基类
 * 所有功能模块都需要继承此类
 * 
 * @author oolongho
 * @since 1.0.0
 */
public abstract class Module {
    
    protected final WooSocial plugin;
    protected final String name;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param name 模块名称
     */
    public Module(WooSocial plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }
    
    /**
     * 模块启用时调用
     * 在此方法中初始化模块所需资源
     */
    public abstract void onEnable();
    
    /**
     * 模块禁用时调用
     * 在此方法中清理模块资源
     */
    public abstract void onDisable();
    
    /**
     * 模块重载时调用
     * 在此方法中重新加载模块配置
     */
    public abstract void onReload();
    
    /**
     * 保存所有数据
     * 子类可以重写此方法以实现数据保存逻辑
     */
    public void saveAll() {
        // 默认空实现，子类可以重写
    }
    
    /**
     * 获取模块名称
     * 
     * @return 模块名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 检查模块是否已启用
     * 子类可以重写此方法以提供启用状态
     * 
     * @return 是否已启用
     */
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * 记录信息日志
     * 
     * @param message 日志消息
     */
    protected void log(String message) {
        plugin.getLogger().info("[" + name + "] " + message);
    }
    
    /**
     * 记录警告日志
     * 
     * @param message 警告消息
     */
    protected void logWarning(String message) {
        plugin.getLogger().warning("[" + name + "] " + message);
    }
    
    /**
     * 记录严重错误日志
     * 
     * @param message 错误消息
     */
    protected void logSevere(String message) {
        plugin.getLogger().severe("[" + name + "] " + message);
    }
}
