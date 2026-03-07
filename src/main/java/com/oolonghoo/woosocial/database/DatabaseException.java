package com.oolonghoo.woosocial.database;

/**
 * 数据库异常类
 * 用于包装数据库操作中的异常
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseException(Throwable cause) {
        super(cause);
    }
    
    /**
     * 创建连接失败异常
     */
    public static DatabaseException connectionFailed(Throwable cause) {
        return new DatabaseException("数据库连接失败", cause);
    }
    
    /**
     * 创建查询失败异常
     */
    public static DatabaseException queryFailed(String query, Throwable cause) {
        return new DatabaseException("数据库查询失败: " + query, cause);
    }
    
    /**
     * 创建更新失败异常
     */
    public static DatabaseException updateFailed(String update, Throwable cause) {
        return new DatabaseException("数据库更新失败: " + update, cause);
    }
    
    /**
     * 创建初始化失败异常
     */
    public static DatabaseException initializationFailed(Throwable cause) {
        return new DatabaseException("数据库初始化失败", cause);
    }
}
