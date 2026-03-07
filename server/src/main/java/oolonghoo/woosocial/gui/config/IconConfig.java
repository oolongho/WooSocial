package com.oolonghoo.woosocial.gui.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * GUI图标配置类
 * 定义图标的材质、名称、描述、自定义模型数据等属性
 * 支持条件显示和动态占位符
 * 
 * @author oolongho
 */
public class IconConfig {
    
    private String type;
    private String material;
    private String skullOwner;
    private String itemModel;
    private int customModelData;
    private String name;
    private List<String> lore;
    private int amount;
    private boolean glow;
    private Map<String, List<String>> actions;
    
    // 条件显示配置
    private List<String> showConditions;
    private List<String> hideConditions;
    
    // 动态配置
    private Map<String, String> dynamicMaterials;
    private Map<String, String> dynamicNames;
    private Map<String, List<String>> dynamicLore;
    
    // 优先级（用于动态图标排序）
    private int priority;
    
    public IconConfig() {
        this.lore = new ArrayList<>();
        this.actions = new HashMap<>();
        this.amount = 1;
        this.glow = false;
        this.customModelData = -1;
        this.showConditions = new ArrayList<>();
        this.hideConditions = new ArrayList<>();
        this.dynamicMaterials = new HashMap<>();
        this.dynamicNames = new HashMap<>();
        this.dynamicLore = new HashMap<>();
        this.priority = 0;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isSpecialType() {
        return type != null && !type.isEmpty();
    }
    
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    public String getSkullOwner() {
        return skullOwner;
    }
    
    public void setSkullOwner(String skullOwner) {
        this.skullOwner = skullOwner;
    }
    
    public String getItemModel() {
        return itemModel;
    }
    
    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }
    
    public int getCustomModelData() {
        return customModelData;
    }
    
    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public boolean isGlow() {
        return glow;
    }
    
    public void setGlow(boolean glow) {
        this.glow = glow;
    }
    
    public Map<String, List<String>> getActions() {
        return actions;
    }
    
    public void setActions(Map<String, List<String>> actions) {
        this.actions = actions != null ? actions : new HashMap<>();
    }
    
    public List<String> getLeftActions() {
        return actions.getOrDefault("left", new ArrayList<>());
    }
    
    public List<String> getRightActions() {
        return actions.getOrDefault("right", new ArrayList<>());
    }
    
    public boolean hasActions() {
        return !actions.isEmpty();
    }
    
    public static IconConfig fromMap(Map<String, Object> map) {
        IconConfig config = new IconConfig();
        
        if (map.containsKey("type")) {
            config.setType((String) map.get("type"));
        }
        
        if (map.containsKey("material")) {
            String mat = (String) map.get("material");
            config.setMaterial(mat);
            
            if (mat != null && mat.contains("{model-data=")) {
                int start = mat.indexOf("{model-data=") + 12;
                int end = mat.indexOf("}", start);
                if (end > start) {
                    try {
                        config.setCustomModelData(Integer.parseInt(mat.substring(start, end)));
                        config.setMaterial(mat.substring(0, mat.indexOf("{")));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        
        if (map.containsKey("skull_owner")) {
            config.setSkullOwner((String) map.get("skull_owner"));
        }
        
        if (map.containsKey("item_model")) {
            config.setItemModel((String) map.get("item_model"));
        }
        
        if (map.containsKey("custom_model_data")) {
            config.setCustomModelData(((Number) map.get("custom_model_data")).intValue());
        }
        
        if (map.containsKey("name")) {
            config.setName((String) map.get("name"));
        }
        
        if (map.containsKey("lore")) {
            @SuppressWarnings("unchecked")
            List<String> loreList = (List<String>) map.get("lore");
            config.setLore(loreList);
        }
        
        if (map.containsKey("amount")) {
            config.setAmount(((Number) map.get("amount")).intValue());
        }
        
        if (map.containsKey("glow")) {
            config.setGlow((Boolean) map.get("glow"));
        }
        
        if (map.containsKey("actions")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> actionsMap = (Map<String, Object>) map.get("actions");
            Map<String, List<String>> parsedActions = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : actionsMap.entrySet()) {
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> actionList = (List<String>) entry.getValue();
                    parsedActions.put(entry.getKey(), actionList);
                }
            }
            config.setActions(parsedActions);
        }
        
        // 解析条件显示配置
        if (map.containsKey("show_if")) {
            @SuppressWarnings("unchecked")
            List<String> conditions = (List<String>) map.get("show_if");
            config.setShowConditions(conditions);
        }
        
        if (map.containsKey("hide_if")) {
            @SuppressWarnings("unchecked")
            List<String> conditions = (List<String>) map.get("hide_if");
            config.setHideConditions(conditions);
        }
        
        // 解析优先级
        if (map.containsKey("priority")) {
            config.setPriority(((Number) map.get("priority")).intValue());
        }
        
        return config;
    }
    
    // ==================== 条件显示方法 ====================
    
    /**
     * 获取显示条件列表
     */
    public List<String> getShowConditions() {
        return showConditions;
    }
    
    /**
     * 设置显示条件列表
     */
    public void setShowConditions(List<String> showConditions) {
        this.showConditions = showConditions != null ? showConditions : new ArrayList<>();
    }
    
    /**
     * 获取隐藏条件列表
     */
    public List<String> getHideConditions() {
        return hideConditions;
    }
    
    /**
     * 设置隐藏条件列表
     */
    public void setHideConditions(List<String> hideConditions) {
        this.hideConditions = hideConditions != null ? hideConditions : new ArrayList<>();
    }
    
    /**
     * 添加显示条件
     */
    public void addShowCondition(String condition) {
        showConditions.add(condition);
    }
    
    /**
     * 添加隐藏条件
     */
    public void addHideCondition(String condition) {
        hideConditions.add(condition);
    }
    
    /**
     * 检查是否应该显示此图标
     * 
     * @param conditionEvaluator 条件评估函数，接收条件字符串，返回布尔值
     * @return 是否应该显示
     */
    public boolean shouldShow(Function<String, Boolean> conditionEvaluator) {
        // 如果有隐藏条件满足，则不显示
        for (String condition : hideConditions) {
            if (conditionEvaluator.apply(condition)) {
                return false;
            }
        }
        
        // 如果没有显示条件，默认显示
        if (showConditions.isEmpty()) {
            return true;
        }
        
        // 检查是否有任意显示条件满足
        for (String condition : showConditions) {
            if (conditionEvaluator.apply(condition)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有条件配置
     */
    public boolean hasConditions() {
        return !showConditions.isEmpty() || !hideConditions.isEmpty();
    }
    
    // ==================== 动态配置方法 ====================
    
    /**
     * 获取动态材质映射
     */
    public Map<String, String> getDynamicMaterials() {
        return dynamicMaterials;
    }
    
    /**
     * 设置动态材质映射
     */
    public void setDynamicMaterials(Map<String, String> dynamicMaterials) {
        this.dynamicMaterials = dynamicMaterials != null ? dynamicMaterials : new HashMap<>();
    }
    
    /**
     * 添加动态材质
     */
    public void addDynamicMaterial(String condition, String material) {
        dynamicMaterials.put(condition, material);
    }
    
    /**
     * 获取动态名称映射
     */
    public Map<String, String> getDynamicNames() {
        return dynamicNames;
    }
    
    /**
     * 设置动态名称映射
     */
    public void setDynamicNames(Map<String, String> dynamicNames) {
        this.dynamicNames = dynamicNames != null ? dynamicNames : new HashMap<>();
    }
    
    /**
     * 添加动态名称
     */
    public void addDynamicName(String condition, String name) {
        dynamicNames.put(condition, name);
    }
    
    /**
     * 获取动态描述映射
     */
    public Map<String, List<String>> getDynamicLore() {
        return dynamicLore;
    }
    
    /**
     * 设置动态描述映射
     */
    public void setDynamicLore(Map<String, List<String>> dynamicLore) {
        this.dynamicLore = dynamicLore != null ? dynamicLore : new HashMap<>();
    }
    
    /**
     * 添加动态描述
     */
    public void addDynamicLore(String condition, List<String> lore) {
        dynamicLore.put(condition, lore);
    }
    
    /**
     * 根据条件获取材质
     */
    public String getMaterialForCondition(Function<String, Boolean> conditionEvaluator) {
        for (Map.Entry<String, String> entry : dynamicMaterials.entrySet()) {
            if (conditionEvaluator.apply(entry.getKey())) {
                return entry.getValue();
            }
        }
        return material;
    }
    
    /**
     * 根据条件获取名称
     */
    public String getNameForCondition(Function<String, Boolean> conditionEvaluator) {
        for (Map.Entry<String, String> entry : dynamicNames.entrySet()) {
            if (conditionEvaluator.apply(entry.getKey())) {
                return entry.getValue();
            }
        }
        return name;
    }
    
    /**
     * 根据条件获取描述
     */
    public List<String> getLoreForCondition(Function<String, Boolean> conditionEvaluator) {
        for (Map.Entry<String, List<String>> entry : dynamicLore.entrySet()) {
            if (conditionEvaluator.apply(entry.getKey())) {
                return entry.getValue();
            }
        }
        return lore;
    }
    
    // ==================== 优先级方法 ====================
    
    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 设置优先级
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取Shift+左键点击动作
     */
    public List<String> getShiftLeftActions() {
        return actions.getOrDefault("shift_left", new ArrayList<>());
    }
    
    /**
     * 获取Shift+右键点击动作
     */
    public List<String> getShiftRightActions() {
        return actions.getOrDefault("shift_right", new ArrayList<>());
    }
    
    /**
     * 获取中间键点击动作
     */
    public List<String> getMiddleActions() {
        return actions.getOrDefault("middle", new ArrayList<>());
    }
    
    /**
     * 创建图标配置的副本
     */
    public IconConfig copy() {
        IconConfig copy = new IconConfig();
        copy.type = this.type;
        copy.material = this.material;
        copy.skullOwner = this.skullOwner;
        copy.itemModel = this.itemModel;
        copy.customModelData = this.customModelData;
        copy.name = this.name;
        copy.lore = new ArrayList<>(this.lore);
        copy.amount = this.amount;
        copy.glow = this.glow;
        copy.actions = new HashMap<>(this.actions);
        copy.showConditions = new ArrayList<>(this.showConditions);
        copy.hideConditions = new ArrayList<>(this.hideConditions);
        copy.dynamicMaterials = new HashMap<>(this.dynamicMaterials);
        copy.dynamicNames = new HashMap<>(this.dynamicNames);
        copy.dynamicLore = new HashMap<>(this.dynamicLore);
        copy.priority = this.priority;
        return copy;
    }
    
    @Override
    public String toString() {
        return "IconConfig{" +
                "material='" + material + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", priority=" + priority +
                '}';
    }
}
