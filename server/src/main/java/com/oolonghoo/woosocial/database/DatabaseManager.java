package com.oolonghoo.woosocial.database;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.manager.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private final String tablePrefix;
    private final String databaseType;
    
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
        
        int maxPoolSize = Math.max(2, Math.min(configManager.getPoolMaximumPoolSize(), 50));
        int minimumIdle = Math.max(1, Math.min(configManager.getPoolMinimumIdle(), maxPoolSize));
        long connectionTimeout = Math.max(5000, Math.min(configManager.getPoolConnectionTimeout(), 60000));
        long idleTimeout = Math.max(10000, Math.min(configManager.getPoolIdleTimeout(), 600000));
        long maxLifetime = Math.max(30000, Math.min(configManager.getPoolMaxLifetime(), 1800000));
        
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                configManager.getMySQLHost(),
                configManager.getMySQLPort(),
                configManager.getMySQLDatabase());
        
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(configManager.getMySQLUser());
        hikariConfig.setPassword(configManager.getMySQLPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        hikariConfig.setPoolName("WooSocial-HikariCP");
        
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
        
        // SQLite连接池配置（SQLite只支持单连接，但保持配置一致性）
        hikariConfig.setMaximumPoolSize(1); // SQLite只支持单连接
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(configManager.getPoolConnectionTimeout());
        hikariConfig.setIdleTimeout(configManager.getPoolIdleTimeout());
        hikariConfig.setMaxLifetime(configManager.getPoolMaxLifetime());
        
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
            
            String createMailsTable = databaseType.equals("mysql")
                    ? getMySQLMailsTableSQL()
                    : getSQLiteMailsTableSQL();
            statement.executeUpdate(createMailsTable);
            
            String createRelationsTable = databaseType.equals("mysql")
                    ? getMySQLRelationsTableSQL()
                    : getSQLiteRelationsTableSQL();
            statement.executeUpdate(createRelationsTable);
            
            String createGiftsTable = databaseType.equals("mysql")
                    ? getMySQLGiftsTableSQL()
                    : getSQLiteGiftsTableSQL();
            statement.executeUpdate(createGiftsTable);
            
            String createDailyGiftsTable = databaseType.equals("mysql")
                    ? getMySQLDailyGiftsTableSQL()
                    : getSQLiteDailyGiftsTableSQL();
            statement.executeUpdate(createDailyGiftsTable);
            
            String createGlobalDailyGiftsTable = databaseType.equals("mysql")
                    ? getMySQLGlobalDailyGiftsTableSQL()
                    : getSQLiteGlobalDailyGiftsTableSQL();
            statement.executeUpdate(createGlobalDailyGiftsTable);
            
            String createScheduledMailsTable = databaseType.equals("mysql")
                    ? getMySQLScheduledMailsTableSQL()
                    : getSQLiteScheduledMailsTableSQL();
            statement.executeUpdate(createScheduledMailsTable);
            
            String createShowcaseTable = databaseType.equals("mysql")
                    ? getMySQLShowcaseTableSQL()
                    : getSQLiteShowcaseTableSQL();
            statement.executeUpdate(createShowcaseTable);
            
            String createShowcaseLikesTable = databaseType.equals("mysql")
                    ? getMySQLShowcaseLikesTableSQL()
                    : getSQLiteShowcaseLikesTableSQL();
            statement.executeUpdate(createShowcaseLikesTable);
            
            String createShowcaseLikeCooldownTable = databaseType.equals("mysql")
                    ? getMySQLShowcaseLikeCooldownTableSQL()
                    : getSQLiteShowcaseLikeCooldownTableSQL();
            statement.executeUpdate(createShowcaseLikeCooldownTable);
            
            // 执行数据库迁移（复用同一个连接）
            migrateMailsTable(connection);
            migrateGiftsTable(connection);
        }
    }
    
    /**
     * 迁移礼物表结构
     * 为现有礼物表添加 sender_name 和 receiver_name 字段
     * @param connection 数据库连接（复用外层连接避免SQLite死锁）
     */
    private void migrateGiftsTable(Connection connection) {
        try {
            // 检查并添加 sender_name 字段
            if (!columnExists(connection, tablePrefix + "gifts", "sender_name")) {
                executeAlterTable(connection, 
                        "ALTER TABLE `" + tablePrefix + "gifts` ADD COLUMN `sender_name` VARCHAR(64)");
                plugin.getLogger().info("[数据库迁移] 成功添加 gifts.sender_name 字段");
            }
            
            // 检查并添加 receiver_name 字段
            if (!columnExists(connection, tablePrefix + "gifts", "receiver_name")) {
                executeAlterTable(connection, 
                        "ALTER TABLE `" + tablePrefix + "gifts` ADD COLUMN `receiver_name` VARCHAR(64)");
                plugin.getLogger().info("[数据库迁移] 成功添加 gifts.receiver_name 字段");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(() -> "[数据库迁移] gifts表迁移失败: " + e.getMessage());
        }
    }
    
    /**
     * 迁移邮件表结构
     * 为现有邮件表添加新字段
     * @param connection 数据库连接（复用外层连接避免SQLite死锁）
     */
    private void migrateMailsTable(Connection connection) {
        try {
            // 检查并添加 attachments 字段
            if (!columnExists(connection, tablePrefix + "mails", "attachments")) {
                executeAlterTable(connection, 
                    databaseType.equals("mysql") 
                        ? "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `attachments` TEXT"
                        : "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `attachments` TEXT");
                plugin.getLogger().info("[数据库迁移] 已添加 attachments 字段到邮件表");
            }
            
            // 检查并添加 is_system 字段
            if (!columnExists(connection, tablePrefix + "mails", "is_system")) {
                executeAlterTable(connection,
                    databaseType.equals("mysql")
                        ? "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `is_system` TINYINT(1) NOT NULL DEFAULT 0"
                        : "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `is_system` INTEGER NOT NULL DEFAULT 0");
                plugin.getLogger().info("[数据库迁移] 已添加 is_system 字段到邮件表");
            }
            
            // 检查并添加 scheduled_time 字段
            if (!columnExists(connection, tablePrefix + "mails", "scheduled_time")) {
                executeAlterTable(connection,
                    databaseType.equals("mysql")
                        ? "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `scheduled_time` BIGINT NOT NULL DEFAULT 0"
                        : "ALTER TABLE `" + tablePrefix + "mails` ADD COLUMN `scheduled_time` INTEGER NOT NULL DEFAULT 0");
                plugin.getLogger().info("[数据库迁移] 已添加 scheduled_time 字段到邮件表");
            }
            
            // 为MySQL添加索引
            if (databaseType.equals("mysql")) {
                addIndexIfNotExists(connection, tablePrefix + "mails", "idx_scheduled_time", "scheduled_time");
                addIndexIfNotExists(connection, tablePrefix + "mails", "idx_is_system", "is_system");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[数据库迁移] 邮件表迁移失败，可能字段已存在", e);
        }
    }
    
    /**
     * 检查列是否存在
     */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        if (databaseType.equals("mysql")) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                stmt.setString(1, tableName);
                stmt.setString(2, columnName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } else {
            // SQLite
            try (PreparedStatement stmt = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 执行表结构修改
     */
    private void executeAlterTable(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    /**
     * 添加索引（如果不存在）
     */
    private void addIndexIfNotExists(Connection connection, String tableName, String indexName, String columnName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?")) {
            stmt.setString(1, tableName);
            stmt.setString(2, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (Statement alterStmt = connection.createStatement()) {
                        alterStmt.executeUpdate("ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (`" + columnName + "`)");
                        plugin.getLogger().info(() -> "[数据库迁移] 已添加索引 " + indexName + " 到邮件表");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[数据库迁移] 添加索引失败: " + indexName, e);
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
                "`notify_online` TINYINT(1) NOT NULL DEFAULT 1, " +
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
                "`notify_online` INTEGER NOT NULL DEFAULT 1, " +
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
    
    private String getMySQLMailsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "mails` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`sender_uuid` VARCHAR(36) NOT NULL, " +
                "`sender_name` VARCHAR(16), " +
                "`receiver_uuid` VARCHAR(36) NOT NULL, " +
                "`receiver_name` VARCHAR(16), " +
                "`item_data` TEXT, " + // 保留用于向后兼容
                "`attachments` TEXT, " + // 新的附件列表字段
                "`send_time` BIGINT NOT NULL DEFAULT 0, " +
                "`expire_time` BIGINT NOT NULL DEFAULT 0, " +
                "`is_read` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`is_claimed` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`is_bulk` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`bulk_id` VARCHAR(36), " +
                "`is_system` TINYINT(1) NOT NULL DEFAULT 0, " + // 系统邮件标识
                "`scheduled_time` BIGINT NOT NULL DEFAULT 0, " + // 定时发送时间戳
                "INDEX `idx_receiver_uuid` (`receiver_uuid`), " +
                "INDEX `idx_sender_uuid` (`sender_uuid`), " +
                "INDEX `idx_expire_time` (`expire_time`), " +
                "INDEX `idx_scheduled_time` (`scheduled_time`), " +
                "INDEX `idx_is_system` (`is_system`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteMailsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "mails` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`sender_uuid` TEXT NOT NULL, " +
                "`sender_name` TEXT, " +
                "`receiver_uuid` TEXT NOT NULL, " +
                "`receiver_name` TEXT, " +
                "`item_data` TEXT, " + // 保留用于向后兼容
                "`attachments` TEXT, " + // 新的附件列表字段
                "`send_time` INTEGER NOT NULL DEFAULT 0, " +
                "`expire_time` INTEGER NOT NULL DEFAULT 0, " +
                "`is_read` INTEGER NOT NULL DEFAULT 0, " +
                "`is_claimed` INTEGER NOT NULL DEFAULT 0, " +
                "`is_bulk` INTEGER NOT NULL DEFAULT 0, " +
                "`bulk_id` TEXT, " +
                "`is_system` INTEGER NOT NULL DEFAULT 0, " + // 系统邮件标识
                "`scheduled_time` INTEGER NOT NULL DEFAULT 0" + // 定时发送时间戳
                ");";
    }
    
    private String getMySQLRelationsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "relations` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL, " +
                "`friend_uuid` VARCHAR(36) NOT NULL, " +
                "`relation_type` VARCHAR(32) NOT NULL DEFAULT 'friend', " +
                "`intimacy` INT NOT NULL DEFAULT 0, " +
                "`create_time` BIGINT NOT NULL DEFAULT 0, " +
                "`update_time` BIGINT NOT NULL DEFAULT 0, " +
                "`is_mutual` TINYINT(1) NOT NULL DEFAULT 0, " +
                "`proposal_time` BIGINT NOT NULL DEFAULT 0, " +
                "UNIQUE KEY `uk_player_friend` (`player_uuid`, `friend_uuid`), " +
                "INDEX `idx_player_uuid` (`player_uuid`), " +
                "INDEX `idx_friend_uuid` (`friend_uuid`), " +
                "INDEX `idx_relation_type` (`relation_type`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteRelationsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "relations` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL, " +
                "`friend_uuid` TEXT NOT NULL, " +
                "`relation_type` TEXT NOT NULL DEFAULT 'friend', " +
                "`intimacy` INTEGER NOT NULL DEFAULT 0, " +
                "`create_time` INTEGER NOT NULL DEFAULT 0, " +
                "`update_time` INTEGER NOT NULL DEFAULT 0, " +
                "`is_mutual` INTEGER NOT NULL DEFAULT 0, " +
                "`proposal_time` INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE (`player_uuid`, `friend_uuid`));";
    }
    
    private String getMySQLGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "gifts` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`sender_uuid` VARCHAR(36) NOT NULL, " +
                "`receiver_uuid` VARCHAR(36) NOT NULL, " +
                "`gift_id` VARCHAR(32) NOT NULL, " +
                "`gift_amount` INT NOT NULL DEFAULT 1, " +
                "`intimacy_gained` INT NOT NULL DEFAULT 0, " +
                "`send_time` BIGINT NOT NULL DEFAULT 0, " +
                "`sender_name` VARCHAR(64), " +
                "`receiver_name` VARCHAR(64), " +
                "INDEX `idx_sender_uuid` (`sender_uuid`), " +
                "INDEX `idx_receiver_uuid` (`receiver_uuid`), " +
                "INDEX `idx_send_time` (`send_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "gifts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`sender_uuid` TEXT NOT NULL, " +
                "`receiver_uuid` TEXT NOT NULL, " +
                "`gift_id` TEXT NOT NULL, " +
                "`gift_amount` INTEGER NOT NULL DEFAULT 1, " +
                "`intimacy_gained` INTEGER NOT NULL DEFAULT 0, " +
                "`send_time` INTEGER NOT NULL DEFAULT 0, " +
                "`sender_name` TEXT, " +
                "`receiver_name` TEXT);";
    }
    
    private String getMySQLDailyGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "daily_gifts` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL, " +
                "`target_uuid` VARCHAR(36) NOT NULL, " +
                "`date` DATE NOT NULL, " +
                "`coins_sent` INT NOT NULL DEFAULT 0, " +
                "`gifts_sent` TEXT, " +
                "UNIQUE KEY `uk_player_target_date` (`player_uuid`, `target_uuid`, `date`), " +
                "INDEX `idx_player_uuid` (`player_uuid`), " +
                "INDEX `idx_date` (`date`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteDailyGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "daily_gifts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL, " +
                "`target_uuid` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`coins_sent` INTEGER NOT NULL DEFAULT 0, " +
                "`gifts_sent` TEXT, " +
                "UNIQUE (`player_uuid`, `target_uuid`, `date`));";
    }
    
    private String getMySQLGlobalDailyGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "global_daily_gifts` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL, " +
                "`date` DATE NOT NULL, " +
                "`gifts_sent` TEXT, " +
                "UNIQUE KEY `uk_player_date` (`player_uuid`, `date`), " +
                "INDEX `idx_player_uuid` (`player_uuid`), " +
                "INDEX `idx_date` (`date`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteGlobalDailyGiftsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "global_daily_gifts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`gifts_sent` TEXT, " +
                "UNIQUE (`player_uuid`, `date`));";
    }
    
    /**
     * 获取MySQL定时邮件表创建SQL
     */
    private String getMySQLScheduledMailsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "scheduled_mails` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`sender_uuid` VARCHAR(36) NOT NULL, " +
                "`sender_name` VARCHAR(16), " +
                "`receiver_uuids` TEXT, " +
                "`receiver_names` TEXT, " +
                "`attachments` TEXT, " +
                "`scheduled_time` BIGINT NOT NULL DEFAULT 0, " +
                "`create_time` BIGINT NOT NULL DEFAULT 0, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                "INDEX `idx_sender_uuid` (`sender_uuid`), " +
                "INDEX `idx_scheduled_time` (`scheduled_time`), " +
                "INDEX `idx_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    /**
     * 获取SQLite定时邮件表创建SQL
     */
    private String getSQLiteScheduledMailsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "scheduled_mails` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`sender_uuid` TEXT NOT NULL, " +
                "`sender_name` TEXT, " +
                "`receiver_uuids` TEXT, " +
                "`receiver_names` TEXT, " +
                "`attachments` TEXT, " +
                "`scheduled_time` INTEGER NOT NULL DEFAULT 0, " +
                "`create_time` INTEGER NOT NULL DEFAULT 0, " +
                "`status` TEXT NOT NULL DEFAULT 'PENDING');";
    }
    
    private String getMySQLShowcaseTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`owner_uuid` VARCHAR(36) NOT NULL UNIQUE, " +
                "`owner_name` VARCHAR(64), " +
                "`items` TEXT, " +
                "`likes` INT NOT NULL DEFAULT 0, " +
                "`last_updated` BIGINT NOT NULL DEFAULT 0, " +
                "INDEX `idx_owner_uuid` (`owner_uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteShowcaseTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`owner_uuid` TEXT NOT NULL UNIQUE, " +
                "`owner_name` TEXT, " +
                "`items` TEXT, " +
                "`likes` INTEGER NOT NULL DEFAULT 0, " +
                "`last_updated` INTEGER NOT NULL DEFAULT 0);";
    }
    
    private String getMySQLShowcaseLikesTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase_likes` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`liker_uuid` VARCHAR(36) NOT NULL, " +
                "`target_uuid` VARCHAR(36) NOT NULL, " +
                "`like_time` BIGINT NOT NULL DEFAULT 0, " +
                "UNIQUE KEY `uk_liker_target` (`liker_uuid`, `target_uuid`), " +
                "INDEX `idx_liker_uuid` (`liker_uuid`), " +
                "INDEX `idx_target_uuid` (`target_uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteShowcaseLikesTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase_likes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`liker_uuid` TEXT NOT NULL, " +
                "`target_uuid` TEXT NOT NULL, " +
                "`like_time` INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE (`liker_uuid`, `target_uuid`));";
    }
    
    private String getMySQLShowcaseLikeCooldownTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase_like_cooldown` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`player_uuid` VARCHAR(36) NOT NULL UNIQUE, " +
                "`last_like_time` BIGINT NOT NULL DEFAULT 0, " +
                "`daily_count` INT NOT NULL DEFAULT 0, " +
                "`daily_reset_time` BIGINT NOT NULL DEFAULT 0, " +
                "INDEX `idx_player_uuid` (`player_uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    }
    
    private String getSQLiteShowcaseLikeCooldownTableSQL() {
        return "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "showcase_like_cooldown` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`player_uuid` TEXT NOT NULL UNIQUE, " +
                "`last_like_time` INTEGER NOT NULL DEFAULT 0, " +
                "`daily_count` INTEGER NOT NULL DEFAULT 0, " +
                "`daily_reset_time` INTEGER NOT NULL DEFAULT 0);";
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
