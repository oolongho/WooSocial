package com.oolonghoo.woosocial.gui.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI配置基类
 * 负责存储和管理GUI的配置信息，包括标题、布局、图标等
 * 
 * @author oolongho
 */
public class GUIConfig {
    
    private final String name;
    private String title;
    private List<String> layout;
    private Map<String, IconConfig> icons;
    private int size;
    
    // 分页配置
    private boolean enablePagination;
    private int itemsPerPage;
    private String paginationLayout;
    
    // 占位符配置
    private Map<String, String> globalPlaceholders;
    
    public GUIConfig(String name) {
        this.name = name;
        this.icons = new HashMap<>();
        this.size = 54;
        this.enablePagination = false;
        this.itemsPerPage = 28;
        this.globalPlaceholders = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<String> getLayout() {
        return layout;
    }
    
    public void setLayout(List<String> layout) {
        this.layout = layout;
        this.size = layout.size() * 9;
    }
    
    public Map<String, IconConfig> getIcons() {
        return icons;
    }
    
    public void setIcons(Map<String, IconConfig> icons) {
        this.icons = icons;
    }
    
    public IconConfig getIcon(String key) {
        return icons.get(key);
    }
    
    public int getSize() {
        return size;
    }
    
    public int getSlotFromLayout(char character, int instanceIndex) {
        if (layout == null) return -1;
        
        int count = 0;
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == character) {
                    if (count == instanceIndex) {
                        return row * 9 + col;
                    }
                    count++;
                }
            }
        }
        return -1;
    }
    
    public List<Integer> getSlotsForChar(char character) {
        java.util.ArrayList<Integer> slots = new java.util.ArrayList<>();
        if (layout == null) return slots;
        
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == character) {
                    slots.add(row * 9 + col);
                }
            }
        }
        return slots;
    }
    
    public char getCharAtSlot(int slot) {
        if (layout == null) return ' ';
        
        int row = slot / 9;
        int col = slot % 9;
        
        if (row < 0 || row >= layout.size()) return ' ';
        
        String line = layout.get(row);
        if (col < 0 || col >= line.length()) return ' ';
        
        return line.charAt(col);
    }
    
    // ==================== 分页配置 ====================
    
    /**
     * 是否启用分页
     */
    public boolean isPaginationEnabled() {
        return enablePagination;
    }
    
    /**
     * 设置是否启用分页
     */
    public void setEnablePagination(boolean enablePagination) {
        this.enablePagination = enablePagination;
    }
    
    /**
     * 获取每页显示数量
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }
    
    /**
     * 设置每页显示数量
     */
    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }
    
    /**
     * 获取分页布局字符
     */
    public String getPaginationLayout() {
        return paginationLayout;
    }
    
    /**
     * 设置分页布局字符
     */
    public void setPaginationLayout(String paginationLayout) {
        this.paginationLayout = paginationLayout;
    }
    
    /**
     * 计算总页数
     * 
     * @param totalItems 总项目数
     * @return 总页数
     */
    public int calculateTotalPages(int totalItems) {
        if (itemsPerPage <= 0) return 1;
        return Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
    }
    
    /**
     * 获取指定页的起始索引
     * 
     * @param page 页码（从1开始）
     * @return 起始索引
     */
    public int getPageStartIndex(int page) {
        return (page - 1) * itemsPerPage;
    }
    
    /**
     * 获取指定页的结束索引
     * 
     * @param page 页码（从1开始）
     * @param totalItems 总项目数
     * @return 结束索引
     */
    public int getPageEndIndex(int page, int totalItems) {
        return Math.min(page * itemsPerPage, totalItems);
    }
    
    // ==================== 占位符配置 ====================
    
    /**
     * 获取全局占位符
     */
    public Map<String, String> getGlobalPlaceholders() {
        return globalPlaceholders;
    }
    
    /**
     * 设置全局占位符
     */
    public void setGlobalPlaceholders(Map<String, String> globalPlaceholders) {
        this.globalPlaceholders = globalPlaceholders != null ? globalPlaceholders : new HashMap<>();
    }
    
    /**
     * 添加全局占位符
     */
    public void addGlobalPlaceholder(String key, String value) {
        globalPlaceholders.put(key, value);
    }
    
    /**
     * 应用全局占位符到文本
     */
    public String applyGlobalPlaceholders(String text) {
        if (text == null || text.isEmpty()) return "";
        String result = text;
        for (Map.Entry<String, String> entry : globalPlaceholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
    
    // ==================== 布局辅助方法 ====================
    
    /**
     * 获取布局行数
     */
    public int getLayoutRows() {
        return layout != null ? layout.size() : 0;
    }
    
    /**
     * 获取指定字符在布局中的出现次数
     */
    public int countCharInLayout(char character) {
        if (layout == null) return 0;
        
        int count = 0;
        for (String line : layout) {
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == character) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * 检查布局是否有效
     */
    public boolean isLayoutValid() {
        if (layout == null || layout.isEmpty()) return false;
        
        // 检查每行长度是否为9
        for (String line : layout) {
            if (line.length() != 9) return false;
        }
        
        // 检查行数是否在1-6之间
        int rows = layout.size();
        return rows >= 1 && rows <= 6;
    }
    
    /**
     * 获取所有内容槽位（非边框槽位）
     */
    public List<Integer> getContentSlots() {
        List<Integer> contentSlots = new java.util.ArrayList<>();
        if (layout == null) return contentSlots;
        
        // 默认边框字符
        char borderChar = '#';
        
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch != borderChar) {
                    contentSlots.add(row * 9 + col);
                }
            }
        }
        return contentSlots;
    }
    
    /**
     * 验证配置完整性
     */
    public boolean validate() {
        if (title == null || title.isEmpty()) {
            return false;
        }
        if (!isLayoutValid()) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "GUIConfig{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", size=" + size +
                ", enablePagination=" + enablePagination +
                ", itemsPerPage=" + itemsPerPage +
                '}';
    }
}
