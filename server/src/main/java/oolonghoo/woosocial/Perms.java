package com.oolonghoo.woosocial;

/**
 * 权限节点常量类
 * 定义插件所有权限节点
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class Perms {
    
    // ==================== 管理员权限 ====================
    
    /**
     * 管理员权限（所有权限）
     */
    public static final String ADMIN = "woosocial.admin";
    
    /**
     * 重载配置权限
     */
    public static final String RELOAD = "woosocial.reload";
    
    /**
     * 查看帮助权限
     */
    public static final String HELP = "woosocial.help";
    
    // ==================== 好友系统权限 ====================
    
    /**
     * 好友命令基础权限
     */
    public static final String FRIEND_BASE = "woosocial.friend";
    
    /**
     * 查看好友列表权限
     */
    public static final String FRIEND_LIST = "woosocial.friend.list";
    
    /**
     * 添加好友权限
     */
    public static final String FRIEND_ADD = "woosocial.friend.add";
    
    /**
     * 接受好友请求权限
     */
    public static final String FRIEND_ACCEPT = "woosocial.friend.accept";
    
    /**
     * 拒绝好友请求权限
     */
    public static final String FRIEND_DENY = "woosocial.friend.deny";
    
    /**
     * 删除好友权限
     */
    public static final String FRIEND_REMOVE = "woosocial.friend.remove";
    
    /**
     * 查看好友请求权限
     */
    public static final String FRIEND_REQUESTS = "woosocial.friend.requests";
    
    /**
     * 设置上线提醒权限
     */
    public static final String FRIEND_NOTIFY = "woosocial.friend.notify";
    
    /**
     * 增加好友数量上限权限
     */
    public static final String FRIEND_LIMIT_BYPASS = "woosocial.friend.limit.bypass";
    
    // ==================== 屏蔽系统权限 ====================
    
    /**
     * 屏蔽命令基础权限
     */
    public static final String BLOCK_BASE = "woosocial.block";
    
    /**
     * 屏蔽玩家权限
     */
    public static final String BLOCK_ADD = "woosocial.block.add";
    
    /**
     * 取消屏蔽权限
     */
    public static final String BLOCK_REMOVE = "woosocial.block.remove";
    
    /**
     * 查看屏蔽列表权限
     */
    public static final String BLOCK_LIST = "woosocial.block.list";
    
    // ==================== 传送系统权限 ====================
    
    /**
     * 传送命令基础权限
     */
    public static final String TELEPORT_BASE = "woosocial.teleport";
    
    /**
     * 传送到好友权限
     */
    public static final String TELEPORT_TO = "woosocial.teleport.to";
    
    /**
     * 设置传送权限权限
     */
    public static final String TELEPORT_TOGGLE = "woosocial.teleport.toggle";
    
    /**
     * 绕过传送冷却权限
     */
    public static final String TELEPORT_COOLDOWN_BYPASS = "woosocial.teleport.cooldown.bypass";
    
    /**
     * 绕过传送倒计时权限
     */
    public static final String TELEPORT_COUNTDOWN_BYPASS = "woosocial.teleport.countdown.bypass";
    
    // ==================== GUI权限 ====================
    
    /**
     * 打开社交菜单权限
     */
    public static final String GUI_SOCIAL = "woosocial.gui";
    
    /**
     * 打开好友GUI权限
     */
    public static final String GUI_FRIEND = "woosocial.gui.friend";
    
    // ==================== 私有构造函数 ====================
    
    private Perms() {
        // 工具类，不允许实例化
    }
}
