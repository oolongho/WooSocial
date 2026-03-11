package com.oolonghoo.woosocial.gui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * GUI布局辅助类
 * 提供布局解析、槽位计算、分页支持等功能
 * 
 * @author oolongho
 */
public class GUILayout {
    
    private final GUIConfig config;
    private final List<String> layout;
    private final int rows;
    private final int size;
    
    // 缓存字符到槽位的映射
    private final Map<Character, List<Integer>> charSlotCache;
    
    /**
     * 创建布局辅助实例
     * 
     * @param config GUI配置
     */
    public GUILayout(GUIConfig config) {
        this.config = config;
        this.layout = config.getLayout();
        this.rows = layout != null ? layout.size() : 0;
        this.size = rows * 9;
        this.charSlotCache = new HashMap<>();
        
        // 预缓存所有字符的槽位
        if (layout != null) {
            buildCharSlotCache();
        }
    }
    
    /**
     * 构建字符到槽位的缓存
     */
    private void buildCharSlotCache() {
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                charSlotCache.computeIfAbsent(ch, k -> new ArrayList<>())
                        .add(row * 9 + col);
            }
        }
    }
    
    // ==================== 基本信息获取 ====================
    
    /**
     * 获取布局行数
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * 获取布局大小（槽位数）
     */
    public int getSize() {
        return size;
    }
    
    /**
     * 获取原始布局
     */
    public List<String> getLayout() {
        return layout;
    }
    
    /**
     * 检查布局是否有效
     */
    public boolean isValid() {
        if (layout == null || layout.isEmpty()) {
            return false;
        }
        
        // 检查每行长度是否为9
        for (String line : layout) {
            if (line.length() != 9) {
                return false;
            }
        }
        
        // 检查行数是否在1-6之间
        return rows >= 1 && rows <= 6;
    }
    
    // ==================== 槽位计算 ====================
    
    /**
     * 获取指定字符的所有槽位
     * 
     * @param character 字符
     * @return 槽位列表
     */
    public List<Integer> getSlots(char character) {
        return charSlotCache.getOrDefault(character, new ArrayList<>());
    }
    
    /**
     * 获取指定字符的第N个实例的槽位
     * 
     * @param character 字符
     * @param instanceIndex 实例索引（从0开始）
     * @return 槽位，如果不存在返回-1
     */
    public int getSlot(char character, int instanceIndex) {
        List<Integer> slots = getSlots(character);
        if (instanceIndex >= 0 && instanceIndex < slots.size()) {
            return slots.get(instanceIndex);
        }
        return -1;
    }
    
    /**
     * 获取指定字符的出现次数
     */
    public int countChar(char character) {
        return getSlots(character).size();
    }
    
    /**
     * 获取指定槽位的字符
     * 
     * @param slot 槽位
     * @return 字符，如果无效返回空格
     */
    public char getCharAt(int slot) {
        if (slot < 0 || slot >= size) {
            return ' ';
        }
        
        int row = slot / 9;
        int col = slot % 9;
        
        if (row >= layout.size()) {
            return ' ';
        }
        
        String line = layout.get(row);
        if (col >= line.length()) {
            return ' ';
        }
        
        return line.charAt(col);
    }
    
    /**
     * 获取指定行列位置的槽位
     */
    public int getSlot(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= 9) {
            return -1;
        }
        return row * 9 + col;
    }
    
    /**
     * 获取指定槽位的行列位置
     */
    public int[] getRowCol(int slot) {
        if (slot < 0 || slot >= size) {
            return new int[]{-1, -1};
        }
        return new int[]{slot / 9, slot % 9};
    }
    
    // ==================== 区域操作 ====================
    
    /**
     * 获取指定矩形区域内的所有槽位
     * 
     * @param startRow 起始行
     * @param startCol 起始列
     * @param endRow 结束行
     * @param endCol 结束列
     * @return 槽位列表
     */
    public List<Integer> getRectSlots(int startRow, int startCol, int endRow, int endCol) {
        List<Integer> slots = new ArrayList<>();
        
        for (int row = startRow; row <= endRow && row < rows; row++) {
            for (int col = startCol; col <= endCol && col < 9; col++) {
                slots.add(row * 9 + col);
            }
        }
        
        return slots;
    }
    
    /**
     * 获取边框槽位
     */
    public List<Integer> getBorderSlots() {
        List<Integer> borderSlots = new ArrayList<>();
        
        // 顶边和底边
        for (int col = 0; col < 9; col++) {
            borderSlots.add(col); // 顶边
            borderSlots.add((rows - 1) * 9 + col); // 底边
        }
        
        // 左边和右边（排除已添加的角）
        for (int row = 1; row < rows - 1; row++) {
            borderSlots.add(row * 9); // 左边
            borderSlots.add(row * 9 + 8); // 右边
        }
        
        return borderSlots;
    }
    
    /**
     * 获取内容区域槽位（排除边框）
     * 
     * @param borderChar 边框字符
     * @return 内容槽位列表
     */
    public List<Integer> getContentSlots(char borderChar) {
        List<Integer> contentSlots = new ArrayList<>();
        
        for (int slot = 0; slot < size; slot++) {
            if (getCharAt(slot) != borderChar) {
                contentSlots.add(slot);
            }
        }
        
        return contentSlots;
    }
    
    /**
     * 获取内容区域槽位（使用默认边框字符'#'）
     */
    public List<Integer> getContentSlots() {
        return getContentSlots('#');
    }
    
    // ==================== 分页支持 ====================
    
    /**
     * 获取分页内容槽位
     * 
     * @param contentChar 内容字符
     * @param page 页码（从1开始）
     * @param totalItems 总项目数
     * @return 当前页的槽位列表
     */
    public List<Integer> getPageSlots(char contentChar, int page, int totalItems) {
        List<Integer> allSlots = getSlots(contentChar);
        int itemsPerPage = allSlots.size();
        
        if (itemsPerPage == 0) {
            return new ArrayList<>();
        }
        
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        
        List<Integer> pageSlots = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < allSlots.size()) {
                pageSlots.add(allSlots.get(slotIndex));
            }
        }
        
        return pageSlots;
    }
    
    /**
     * 计算总页数
     * 
     * @param contentChar 内容字符
     * @param totalItems 总项目数
     * @return 总页数
     */
    public int getTotalPages(char contentChar, int totalItems) {
        int itemsPerPage = countChar(contentChar);
        if (itemsPerPage == 0) return 1;
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
    
    /**
     * 获取页码起始索引
     */
    public int getPageStartIndex(int page, int itemsPerPage) {
        return (page - 1) * itemsPerPage;
    }
    
    /**
     * 获取页码结束索引
     */
    public int getPageEndIndex(int page, int itemsPerPage, int totalItems) {
        return Math.min(page * itemsPerPage, totalItems);
    }
    
    // ==================== 布局模式 ====================
    
    /**
     * 创建标准6行布局
     */
    public static List<String> createStandardLayout() {
        List<String> layout = new ArrayList<>();
        layout.add("#########");
        layout.add("#1111111#");
        layout.add("#1111111#");
        layout.add("#1111111#");
        layout.add("#1111111#");
        layout.add("#<#####>#");
        return layout;
    }
    
    /**
     * 创建紧凑4行布局
     */
    public static List<String> createCompactLayout() {
        List<String> layout = new ArrayList<>();
        layout.add("#########");
        layout.add("#1111111#");
        layout.add("#1111111#");
        layout.add("#<#####>#");
        return layout;
    }
    
    /**
     * 创建小型3行布局
     */
    public static List<String> createSmallLayout() {
        List<String> layout = new ArrayList<>();
        layout.add("#########");
        layout.add("#1111111#");
        layout.add("#<#####>#");
        return layout;
    }
    
    /**
     * 创建确认对话框布局
     */
    public static List<String> createConfirmLayout() {
        List<String> layout = new ArrayList<>();
        layout.add("#########");
        layout.add("#########");
        layout.add("##<Y#N>##");
        layout.add("#########");
        return layout;
    }
    
    /**
     * 创建信息展示布局
     */
    public static List<String> createInfoLayout() {
        List<String> layout = new ArrayList<>();
        layout.add("#########");
        layout.add("#IIIIIII#");
        layout.add("#IIIIIII#");
        layout.add("#IIIIIII#");
        layout.add("#########");
        layout.add("#<#####>#");
        return layout;
    }
    
    // ==================== 布局验证 ====================
    
    /**
     * 验证布局格式
     * 
     * @return 验证结果，null表示成功，否则返回错误消息
     */
    public String validate() {
        if (layout == null) {
            return "布局为空";
        }
        
        if (layout.isEmpty()) {
            return "布局没有定义任何行";
        }
        
        if (rows < 1 || rows > 6) {
            return "布局行数必须在1-6之间，当前: " + rows;
        }
        
        for (int i = 0; i < layout.size(); i++) {
            String line = layout.get(i);
            if (line.length() != 9) {
                return "第 " + (i + 1) + " 行长度必须为9，当前: " + line.length();
            }
        }
        
        return null;
    }
    
    /**
     * 检查布局是否包含指定字符
     */
    public boolean containsChar(char character) {
        return charSlotCache.containsKey(character);
    }
    
    /**
     * 获取布局中所有使用的字符
     */
    public List<Character> getUsedChars() {
        return new ArrayList<>(charSlotCache.keySet());
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 将槽位列表转换为行列字符串（用于调试）
     */
    public String slotsToString(List<Integer> slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            sb.append(String.format("[%d,%d]", slot / 9, slot % 9));
            if (i < slots.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    
    /**
     * 打印布局（用于调试）
     */
    public void printLayout() {
        System.out.println("=== GUI Layout ===");
        for (int i = 0; i < layout.size(); i++) {
            System.out.println("Row " + i + ": [" + layout.get(i) + "]");
        }
        System.out.println("==================");
    }
    
    @Override
    public String toString() {
        return "GUILayout{" +
                "rows=" + rows +
                ", size=" + size +
                ", valid=" + isValid() +
                '}';
    }
}
