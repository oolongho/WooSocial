package com.oolonghoo.woosocial.util;

/**
 * 分页工具类
 * 提供通用的分页计算方法
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class PaginationUtil {
    
    /**
     * 计算总页数
     * 
     * @param totalItems 总项目数
     * @param itemsPerPage 每页项目数
     * @return 总页数
     */
    public static int calculateTotalPages(int totalItems, int itemsPerPage) {
        if (totalItems == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
    
    /**
     * 计算起始索引
     * 
     * @param page 当前页码（从 1 开始）
     * @param itemsPerPage 每页项目数
     * @return 起始索引
     */
    public static int calculateStartIndex(int page, int itemsPerPage) {
        return (page - 1) * itemsPerPage;
    }
    
    /**
     * 计算结束索引
     * 
     * @param startIndex 起始索引
     * @param itemsPerPage 每页项目数
     * @param totalItems 总项目数
     * @return 结束索引（不包含）
     */
    public static int calculateEndIndex(int startIndex, int itemsPerPage, int totalItems) {
        return Math.min(startIndex + itemsPerPage, totalItems);
    }
    
    /**
     * 验证并修正页码
     * 
     * @param currentPage 当前页码
     * @param totalPages 总页数
     * @return 修正后的页码
     */
    public static int validatePage(int currentPage, int totalPages) {
        if (currentPage < 1) {
            return 1;
        }
        if (currentPage > totalPages) {
            return Math.max(1, totalPages);
        }
        return currentPage;
    }
    
    /**
     * 计算分页信息
     */
    public static class PageInfo {
        private final int currentPage;
        private final int totalPages;
        private final int startIndex;
        private final int endIndex;
        private final int itemsPerPage;
        private final int totalItems;
        
        public PageInfo(int currentPage, int totalPages, int startIndex, int endIndex, 
                       int itemsPerPage, int totalItems) {
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.itemsPerPage = itemsPerPage;
            this.totalItems = totalItems;
        }
        
        public int getCurrentPage() { return currentPage; }
        public int getTotalPages() { return totalPages; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public int getItemsPerPage() { return itemsPerPage; }
        public int getTotalItems() { return totalItems; }
        
        public boolean hasNextPage() { return currentPage < totalPages; }
        public boolean hasPreviousPage() { return currentPage > 1; }
        public int getNextPage() { return Math.min(currentPage + 1, totalPages); }
        public int getPreviousPage() { return Math.max(currentPage - 1, 1); }
    }
    
    /**
     * 创建分页信息
     * 
     * @param currentPage 当前页码
     * @param totalItems 总项目数
     * @param itemsPerPage 每页项目数
     * @return 分页信息
     */
    public static PageInfo createPageInfo(int currentPage, int totalItems, int itemsPerPage) {
        int totalPages = calculateTotalPages(totalItems, itemsPerPage);
        int validatedPage = validatePage(currentPage, totalPages);
        int startIndex = calculateStartIndex(validatedPage, itemsPerPage);
        int endIndex = calculateEndIndex(startIndex, itemsPerPage, totalItems);
        
        return new PageInfo(validatedPage, totalPages, startIndex, endIndex, itemsPerPage, totalItems);
    }
}
