package com.oolonghoo.woosocial.attachment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 附件序列化器
 * 负责附件列表与JSON字符串之间的序列化和反序列化
 * 支持扩展新的附件类型
 * 
 * @author oolongho
 * @version 1.0.0
 */
public class AttachmentSerializer {
    
    /**
     * 附件类型注册表
     * 存储类型标识符与附件类的映射关系
     */
    private static final Map<String, Class<? extends IAttachment>> attachmentRegistry = new HashMap<>();
    
    static {
        // 注册内置附件类型
        registerAttachmentType(AttachmentType.ITEM, ItemAttachment.class);
        registerAttachmentType(AttachmentType.MONEY, MoneyAttachment.class);
        registerAttachmentType(AttachmentType.POINTS, PointsAttachment.class);
    }
    
    /**
     * 注册新的附件类型
     * 允许外部扩展注册自定义附件类型
     * 
     * @param type 附件类型枚举
     * @param attachmentClass 附件实现类
     */
    public static void registerAttachmentType(AttachmentType type, Class<? extends IAttachment> attachmentClass) {
        if (type != null && attachmentClass != null) {
            attachmentRegistry.put(type.getIdentifier(), attachmentClass);
            Bukkit.getLogger().info("[WooSocial] 注册附件类型: " + type.getIdentifier() + " -> " + attachmentClass.getSimpleName());
        }
    }
    
    /**
     * 注册新的附件类型（使用字符串标识符）
     * 允许外部扩展注册自定义附件类型
     * 
     * @param identifier 类型标识符
     * @param attachmentClass 附件实现类
     */
    public static void registerAttachmentType(String identifier, Class<? extends IAttachment> attachmentClass) {
        if (identifier != null && !identifier.isEmpty() && attachmentClass != null) {
            attachmentRegistry.put(identifier.toLowerCase(), attachmentClass);
            Bukkit.getLogger().info("[WooSocial] 注册附件类型: " + identifier + " -> " + attachmentClass.getSimpleName());
        }
    }
    
    /**
     * 将附件列表序列化为JSON字符串
     * 
     * @param attachments 附件列表
     * @return JSON字符串，失败返回null
     */
    public static String serializeList(List<IAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        
        JsonArray jsonArray = new JsonArray();
        
        for (IAttachment attachment : attachments) {
            if (attachment != null && attachment.isLegal()) {
                String json = attachment.serialize();
                if (json != null) {
                    jsonArray.add(json);
                }
            }
        }
        
        return jsonArray.size() > 0 ? jsonArray.toString() : null;
    }
    
    /**
     * 从JSON字符串反序列化附件列表
     * 
     * @param json JSON字符串
     * @return 附件列表，失败返回空列表
     */
    public static List<IAttachment> deserializeList(String json) {
        List<IAttachment> attachments = new ArrayList<>();
        
        if (json == null || json.isEmpty()) {
            return attachments;
        }
        
        try {
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            
            for (JsonElement element : jsonArray) {
                try {
                    String attachmentJson = element.getAsString();
                    IAttachment attachment = deserializeSingle(attachmentJson);
                    if (attachment != null && attachment.isLegal()) {
                        attachments.add(attachment);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.WARNING, "[WooSocial] 反序列化单个附件失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[WooSocial] 解析附件列表JSON失败: " + e.getMessage());
        }
        
        return attachments;
    }
    
    /**
     * 序列化单个附件
     * 
     * @param attachment 附件对象
     * @return JSON字符串，失败返回null
     */
    public static String serializeSingle(IAttachment attachment) {
        if (attachment == null || !attachment.isLegal()) {
            return null;
        }
        
        return attachment.serialize();
    }
    
    /**
     * 反序列化单个附件
     * 
     * @param json JSON字符串
     * @return 附件对象，失败返回null
     */
    public static IAttachment deserializeSingle(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            // 解析JSON获取类型标识符
            com.google.gson.JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
            String typeIdentifier = jsonObj.get("type").getAsString();
            
            // 根据类型获取对应的附件类
            Class<? extends IAttachment> attachmentClass = attachmentRegistry.get(typeIdentifier.toLowerCase());
            if (attachmentClass == null) {
                Bukkit.getLogger().warning("[WooSocial] 未知的附件类型: " + typeIdentifier);
                return null;
            }
            
            // 创建实例并反序列化
            IAttachment attachment = attachmentClass.getDeclaredConstructor().newInstance();
            return attachment.deserialize(json);
            
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[WooSocial] 反序列化附件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查附件类型是否已注册
     * 
     * @param identifier 类型标识符
     * @return 是否已注册
     */
    public static boolean isTypeRegistered(String identifier) {
        return identifier != null && attachmentRegistry.containsKey(identifier.toLowerCase());
    }
    
    /**
     * 获取所有已注册的附件类型标识符
     * 
     * @return 类型标识符集合
     */
    public static java.util.Set<String> getRegisteredTypes() {
        return new java.util.HashSet<>(attachmentRegistry.keySet());
    }
}
