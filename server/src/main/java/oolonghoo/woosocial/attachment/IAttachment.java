package com.oolonghoo.woosocial.attachment;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 附件接口
 * 定义邮件附件的基本行为，支持多种附件类型的扩展
 * 
 * @author oolongho
 * @version 1.0.0
 */
public interface IAttachment {
    
    /**
     * 玩家领取附件
     * 将附件内容发放给指定玩家
     * 
     * @param player 领取附件的玩家
     * @return 是否领取成功
     */
    boolean use(Player player);
    
    /**
     * 将附件序列化为JSON字符串
     * 用于数据库存储
     * 
     * @return JSON格式的序列化字符串
     */
    String serialize();
    
    /**
     * 从JSON字符串反序列化附件
     * 
     * @param json JSON格式的字符串
     * @return 反序列化后的附件对象，失败返回null
     */
    IAttachment deserialize(String json);
    
    /**
     * 获取附件类型
     * 
     * @return 附件类型枚举
     */
    AttachmentType getType();
    
    /**
     * 检查附件是否合法有效
     * 用于验证附件数据是否正确
     * 
     * @return 附件是否合法
     */
    boolean isLegal();
    
    /**
     * 生成附件的展示图标
     * 用于在GUI中显示附件信息
     * 
     * @return 展示用的ItemStack图标
     */
    ItemStack generateIcon();
    
    /**
     * 获取附件的描述文本
     * 用于显示附件详情
     * 
     * @return 描述文本
     */
    String getDescription();
}
