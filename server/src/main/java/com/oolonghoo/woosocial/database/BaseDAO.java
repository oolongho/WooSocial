package com.oolonghoo.woosocial.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 通用 DAO 基类
 * 提供基础的 CRUD 操作方法
 * 
 * @param <T> 实体类型
 * @author oolongho
 * @since 1.0.0
 */
public abstract class BaseDAO<T> {
    
    protected final DatabaseManager databaseManager;
    protected final String tablePrefix;
    
    public BaseDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.tablePrefix = databaseManager.getTablePrefix();
    }
    
    /**
     * 执行插入操作
     */
    protected CompletableFuture<Integer> executeInsert(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                
                setParameters(statement, params);
                return statement.executeUpdate();
            } catch (SQLException e) {
                logError("执行插入操作失败", e);
                return 0;
            }
        });
    }
    
    /**
     * 执行更新操作
     */
    protected CompletableFuture<Boolean> executeUpdate(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                setParameters(statement, params);
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                logError("执行更新操作失败", e);
                return false;
            }
        });
    }
    
    /**
     * 执行删除操作
     */
    protected CompletableFuture<Boolean> executeDelete(String sql, Object... params) {
        return executeUpdate(sql, params);
    }
    
    /**
     * 执行查询，返回单个结果
     */
    protected <R> CompletableFuture<Optional<R>> executeQueryOne(String sql, RowMapper<R> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                setParameters(statement, params);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(mapper.mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                logError("执行查询操作失败", e);
            }
            return Optional.empty();
        });
    }
    
    /**
     * 执行查询，返回结果列表
     */
    protected <R> CompletableFuture<List<R>> executeQueryList(String sql, RowMapper<R> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            List<R> results = new ArrayList<>();
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                setParameters(statement, params);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        R result = mapper.mapRow(rs);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                }
            } catch (SQLException e) {
                logError("执行查询操作失败", e);
            }
            return results;
        });
    }
    
    /**
     * 执行批量插入操作
     */
    protected CompletableFuture<Integer> executeBatchInsert(String sql, List<Object[]> paramsList) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                for (Object[] params : paramsList) {
                    setParameters(statement, params);
                    statement.addBatch();
                }
                
                int[] results = statement.executeBatch();
                int totalAffected = 0;
                for (int result : results) {
                    if (result > 0) {
                        totalAffected += result;
                    }
                }
                return totalAffected;
            } catch (SQLException e) {
                logError("执行批量插入操作失败", e);
                return 0;
            }
        });
    }
    
    /**
     * 设置预编译语句参数
     */
    protected void setParameters(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null || params.length == 0) {
            return;
        }
        
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int index = i + 1;
            
            if (param == null) {
                statement.setNull(index, java.sql.Types.NULL);
            } else if (param instanceof String s) {
                statement.setString(index, s);
            } else if (param instanceof Integer intVal) {
                statement.setInt(index, intVal);
            } else if (param instanceof Long l) {
                statement.setLong(index, l);
            } else if (param instanceof Boolean b) {
                statement.setBoolean(index, b);
            } else if (param instanceof Double d) {
                statement.setDouble(index, d);
            } else if (param instanceof Float f) {
                statement.setFloat(index, f);
            } else if (param instanceof Timestamp t) {
                statement.setTimestamp(index, t);
            } else {
                statement.setObject(index, param);
            }
        }
    }
    
    /**
     * 记录错误日志
     */
    protected void logError(String message, SQLException e) {
        java.util.logging.Logger.getLogger(getClass().getName())
                .log(Level.SEVERE, message + ": " + e.getMessage(), e);
    }
    
    /**
     * 行映射函数接口
     */
    @FunctionalInterface
    public interface RowMapper<R> {
        R mapRow(ResultSet rs) throws SQLException;
    }
}
