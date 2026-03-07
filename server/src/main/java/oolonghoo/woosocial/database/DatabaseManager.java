package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * 数据库管理器
 * 使用HikariCP连接池管理数据库连接
 * 支持MySQL和SQLite双存储
 */
public class DatabaseManager {
    
    private final WooSocial plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private String tablePrefix;
    private String databaseType;
    
    public DatabaseManager(WooSocial plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tablePrefix = configManager.getTablePrefix();
        this.databaseType = configManager.getDatabaseType();
    }
    
    /**
     * 初始化数据库连接池
     */
    public void initialize() throws SQLException {
        if (databaseType.equals("mysql")) {
            initializeMySQL();
        } else {
            initializeSQLite();
        }
        
        initializeTables();
    }
    
    /**
     * 初始化MySQL连接池
     */
    private void initializeMySQL() {
        HikariConfig hikariConfig = new HikariConfig();
        
        // 构建JDBC URL
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                configManager.getMySQLHost(),
                configManager.getMySQLPort(),
                configManager.getMySQLDatabase());
        
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(configManager.getMySQLUser());
        hikariConfig.setPassword(configManager.getMySQLPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池配置
        hikariConfig.setMaximumPoolSize(configManager.getMySQLPoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(10000); // 10秒
        hikariConfig.setIdleTimeout(600000); // 10分钟
        hikariConfig.setMaxLifetime(1800000); // 30分钟
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // 连接池名称
        hikariConfig.setPoolName("WooSocial-HikariCP");
        
        // 性能优化配置
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * 初始化SQLite连接池
     */
    private void initializeSQLite() {
        // 确保数据目录存在
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String dbFile = new File(dataFolder, configManager.getSQLiteFile()).getAbsolutePath();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile);
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite连接池配置
        hikariConfig.setMaximumPoolSize(1); // SQLite只支持单连接
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        // 连接池名称
        hikariConfig.setPoolName("WooSocial-SQLite-HikariCP");
        
        // SQLite优化配置
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("foreign_keys", "ON");
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    /**
     * 初始化数据库表结构
     */
    private void initializeTables() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            
            String createPlayersTable = databaseType.equals("mysql") 
                    ? getMySQLPlayersTableSQL() 
                    : getSQLitePlayersTableSQL();
            statement.executeUpdate(createPlayersTable);
            
            String createFriendsTable = databaseType.equals("mysql")
                    ? getMySQLFriendsTableSQL()
                    : getSQLiteFriendsTableSQL();
            statement.executeUpdate(createFriendsTable);
            
            String createRequestsTable = databaseType.equals("mysql")
                    ? getMySQLRequestsTableSQL()
                    : getSQLiteRequestsTableSQL();
            statement.executeUpdate(createRequestsTable);
            
            String createCooldownTable = databaseType.equals("mysql")
                    ? getMySQLCooldownTableSQL()
                    : getSQLiteCooldownTableSQL();
            statement.executeUpdate(createCooldownTable);
            
            String createTeleportSettingsTable = databaseType.equals("mysql")
                    ? getMySQLTeleportSettingsTableSQL()
                    : getSQLiteTeleportSettingsTableSQL();
            statement.executeUpdate(createTeleportSettingsTable);
            
            String createBlockedTable = databaseType.equals("mysql")
                    ? getMySQLBlockedTableSQL()
                    : getSQLiteBlockedTableSQL();
            statement.executeUpdate(createBlockedTable);
        }
    }
    
    /**
     * 获取MySQL玩家表创建SQL
     */
    private String getMySQLPlayersTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "players` (" +
                "`uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`last_name` VARCHAR(16), " +
                "`first_join_time` BIGINT NOT NULL DEFAULT 0, " +
                "`last_online_time` BIGINT NOT NULL DEFAULT 0, " +
                "`notify_online` TINYINT(1) NOT NULL DEFAULT 1, " +
                "`allow_teleport` TINYINT(1) NOT NULL DEFAULT 1, " +
                "`settings` TEXT, " +
                "INDEX `idx_last_name` (`last_name`), " +
                "INDEX `idx_last_online` (`last_online_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    /**
     * 获取SQLite玩家表创建SQL
     */
    private String getSQLitePlayersTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "players` (" +
                "`uuid` TEXT NOT NULL PRIMARY KEY, " +
                "`last_name` TEXT, " +
                "`first_join_time` INTEGER NOT NULL DEFAULT 0, " +
                "`last_online_time` INTEGER NOT NULL DEFAULT 0, " +
                "`notify_online` INTEGER NOT NULL DEFAULT 1, " +
                "`allow_teleport` INTEGER NOT NULL DEFAULT 1, " +
                "`settings` TEXT);";
    }
    
    /**
     * 获取MySQL好友关系表创建SQL
     */
    private String getMySQLFriendsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "friends` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL, " +
                "`friend_uuid` VARCHAR(36) NOT NULL, " +
                "`add_time` BIGINT NOT NULL DEFAULT 0, " +
                "`friend_name` VARCHAR(16), " +
                "`favorite` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`nickname` VARCHAR(32), " +
                "`receive_messages` TINYINT(1) NOT NULL DEFAULT 1, " +
                "UNIQUE KEY `uk_player_friend` (`player_uuid`, `friend_uuid`), " +
                "INDEX `idx_player_uuid` (`player_uuid`), " +
                "INDEX `idx_friend_uuid` (`friend_uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteFriendsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "friends` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL, " +
                "`friend_uuid` TEXT NOT NULL, " +
                "`add_time` INTEGER NOT NULL DEFAULT 0, " +
                "`friend_name` TEXT, " +
                "`favorite` INTEGER NOT NULL DEFAULT 0, " +
                "`nickname` TEXT, " +
                "`receive_messages` INTEGER NOT NULL DEFAULT 1, " +
                "UNIQUE (`player_uuid`, `friend_uuid`));";
    }
    
    /**
     * 获取MySQL好友请求表创建SQL
     */
    private String getMySQLRequestsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "requests` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`sender_uuid` VARCHAR(36) NOT NULL, " +
                "`receiver_uuid` VARCHAR(36) NOT NULL, " +
                "`sender_name` VARCHAR(16), " +
                "`receiver_name` VARCHAR(16), " +
                "`request_time` BIGINT NOT NULL DEFAULT 0, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                "UNIQUE KEY `uk_sender_receiver` (`sender_uuid`, `receiver_uuid`), " +
                "INDEX `idx_sender_uuid` (`sender_uuid`), " +
                "INDEX `idx_receiver_uuid` (`receiver_uuid`), " +
                "INDEX `idx_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    /**
     * 获取SQLite好友请求表创建SQL
     */
    private String getSQLiteRequestsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "requests` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`sender_uuid` TEXT NOT NULL, " +
                "`receiver_uuid` TEXT NOT NULL, " +
                "`sender_name` TEXT, " +
                "`receiver_name` TEXT, " +
                "`request_time` INTEGER NOT NULL DEFAULT 0, " +
                "`status` TEXT NOT NULL DEFAULT 'PENDING', " +
                "UNIQUE (`sender_uuid`, `receiver_uuid`));";
    }
    
    /**
     * 获取MySQL传送冷却表创建SQL
     */
    private String getMySQLCooldownTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "teleport_cooldown` (" +
                "`uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`cooldown_end_time` BIGINT NOT NULL DEFAULT 0, " +
                "`cooldown_type` VARCHAR(20) NOT NULL DEFAULT 'TP_FRIEND', " +
                "INDEX `idx_cooldown_end` (`cooldown_end_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    /**
     * 获取SQLite传送冷却表创建SQL
     */
    private String getSQLiteCooldownTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "teleport_cooldown` (" +
                "`uuid` TEXT NOT NULL PRIMARY KEY, " +
                "`cooldown_end_time` INTEGER NOT NULL DEFAULT 0, " +
                "`cooldown_type` TEXT NOT NULL DEFAULT 'TP_FRIEND');";
    }
    
    private String getMySQLTeleportSettingsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "teleport_settings` (" +
                "`player_uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`allow_friend_teleport` TINYINT(1) NOT NULL DEFAULT 1, " +
                "`allow_stranger_teleport` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`teleport_cooldown` INT NOT NULL DEFAULT 60, " +
                "`teleport_countdown` INT NOT NULL DEFAULT 5, " +
                "`friend_permissions` TEXT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteTeleportSettingsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "teleport_settings` (" +
                "`player_uuid` TEXT NOT NULL PRIMARY KEY, " +
                "`allow_friend_teleport` INTEGER NOT NULL DEFAULT 1, " +
                "`allow_stranger_teleport` INTEGER NOT NULL DEFAULT 0, " +
                "`teleport_cooldown` INTEGER NOT NULL DEFAULT 60, " +
                "`teleport_countdown` INTEGER NOT NULL DEFAULT 5, " +
                "`friend_permissions` TEXT);";
    }
    
    private String getMySQLBlockedTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "blocked` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL, " +
                "`blocked_uuid` VARCHAR(36) NOT NULL, " +
                "`block_time` BIGINT NOT NULL DEFAULT 0, " +
                "UNIQUE KEY `uk_player_blocked` (`player_uuid`, `blocked_uuid`), " +
                "INDEX `idx_player_uuid` (`player_uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteBlockedTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "blocked` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL, " +
                "`blocked_uuid` TEXT NOT NULL, " +
                "`block_time` INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE (`player_uuid`, `blocked_uuid`));";
    }
    
    /**
     * 获取数据库连接
     * @return 数据库连接
     * @throws SQLException 连接异常
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("数据库连接池未初始化或已关闭");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 关闭数据库连接池
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接池已关闭");
        }
    }
    
    /**
     * 获取表前缀
     */
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    /**
     * 获取数据库类型
     */
    public String getDatabaseType() {
        return databaseType;
    }
    
    /**
     * 检查是否使用MySQL数据库
     * 
     * @return 是否为MySQL
     */
    public boolean isMySQL() {
        return "mysql".equalsIgnoreCase(databaseType);
    }
    
    /**
     * 检查数据库连接是否正常
     */
    public boolean isHealthy() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "数据库连接检查失败", e);
            return false;
        }
    }
}
