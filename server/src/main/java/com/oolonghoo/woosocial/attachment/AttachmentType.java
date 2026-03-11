package com.oolonghoo.woosocial.attachment;

/**
 * 附件类型枚举
 * 定义所有支持的附件类型，便于扩展
 * 
 * @author oolongho
 * @version 1.0.0
 */
public enum AttachmentType {
    
    /**
     * 物品附件
     * 存储Minecraft物品
     */
    ITEM("item", "物品"),
    
    /**
     * 金币附件
     * 通过Vault经济系统发放金币
     */
    MONEY("money", "金币"),
    
    /**
     * 点券附件
     * 通过PlayerPoints插件发放点券
     */
    POINTS("points", "点券");
    
    private final String identifier;
    private final String displayName;
    
    AttachmentType(String identifier, String displayName) {
        this.identifier = identifier;
        this.displayName = displayName;
    }
    
    /**
     * 获取类型标识符
     * 用于JSON序列化时识别类型
     * 
     * @return 类型标识符字符串
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * 获取显示名称
     * 用于在GUI和消息中展示
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据标识符获取对应的附件类型
     * 
     * @param identifier 类型标识符
     * @return 对应的附件类型，未找到则返回null
     */
    public static AttachmentType fromIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        
        for (AttachmentType type : values()) {
            if (type.identifier.equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        
        return null;
    }
}
