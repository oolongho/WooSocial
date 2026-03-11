package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * 数据库操作辅助类
 * 提供简化的异步数据库操作方法
 */
public class DatabaseHelper {
    
    private final WooSocial plugin;
    private final DatabaseManager databaseManager;
    
    public DatabaseHelper(WooSocial plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * 异步执行查询操作
     * @param sql SQL语句
     * @param params 参数
     * @param mapper 结果映射函数
     * @return CompletableFuture
     */
    public <T> CompletableFuture<T> queryAsync(String sql, Object[] params, 
                                                Function<ResultSet, T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // 设置参数
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        statement.setObject(i + 1, params[i]);
                    }
                }
                
                // 执行查询
                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapper.apply(resultSet);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "数据库查询失败: " + sql, e);
                throw new DatabaseException("查询失败", e);
            }
        });
    }
    
    /**
     * 异步执行更新操作
     * @param sql SQL语句
     * @param params 参数
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> updateAsync(String sql, Object[] params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // 设置参数
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        statement.setObject(i + 1, params[i]);
                    }
                }
                
                // 执行更新
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "数据库更新失败: " + sql, e);
                return false;
            }
        });
    }
    
    /**
     * 异步执行批量更新操作
     * @param sql SQL语句
     * @param paramsList 参数列表
     * @return CompletableFuture<Integer> 影响的行数
     */
    public CompletableFuture<Integer> batchUpdateAsync(String sql, Object[][] paramsList) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                // 添加批量操作
                for (Object[] params : paramsList) {
                    if (params != null) {
                        for (int i = 0; i < params.length; i++) {
                            statement.setObject(i + 1, params[i]);
                        }
                    }
                    statement.addBatch();
                }
                
                // 执行批量更新
                int[] results = statement.executeBatch();
                int totalAffected = 0;
                for (int result : results) {
                    totalAffected += result;
                }
                
                return totalAffected;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "批量更新失败: " + sql, e);
                return 0;
            }
        });
    }
    
    /**
     * 异步执行事务操作
     * @param transaction 事务操作
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> executeTransactionAsync(Consumer<Connection> transaction) {
        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            try {
                connection = databaseManager.getConnection();
                connection.setAutoCommit(false);
                
                // 执行事务操作
                transaction.accept(connection);
                
                // 提交事务
                connection.commit();
                return true;
            } catch (SQLException e) {
                // 回滚事务
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackEx) {
                        plugin.getLogger().log(Level.SEVERE, "事务回滚失败", rollbackEx);
                    }
                }
                plugin.getLogger().log(Level.SEVERE, "事务执行失败", e);
                return false;
            } finally {
                if (connection != null) {
                    try {
                        connection.setAutoCommit(true);
                        connection.close();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "关闭连接失败", e);
                    }
                }
            }
        });
    }
    
    /**
     * 在主线程执行回调
     * @param future 异步操作结果
     * @param callback 回调函数
     */
    public <T> void thenOnMainThread(CompletableFuture<T> future, Consumer<T> callback) {
        future.thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }
    
    /**
     * 异常处理
     * @param future 异步操作结果
     * @param errorHandler 错误处理器
     */
    public <T> void handleException(CompletableFuture<T> future, Consumer<Throwable> errorHandler) {
        future.exceptionally(throwable -> {
            errorHandler.accept(throwable);
            return null;
        });
    }
}
