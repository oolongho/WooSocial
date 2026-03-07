package com.oolonghoo.woosocial.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.SocialMainGUI;
import com.oolonghoo.woosocial.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主命令处理器
 * 处理 /woosocial 命令及其子命令
 *
 * @author oolongho
 * @since 1.0.0
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final WooSocial plugin;
    private final MessageManager messageManager;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public MainCommand(WooSocial plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 无参数时打开社交菜单（玩家）或显示帮助（控制台）
        if (args.length == 0) {
            if (sender instanceof Player) {
                handleMenu((Player) sender);
            } else {
                handleHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
            case "?":
                handleHelp(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "version":
            case "ver":
                handleVersion(sender);
                break;
            default:
                messageManager.send(sender, "general.unknown-command");
                break;
        }

        return true;
    }

    /**
     * 处理社交菜单命令
     * 打开社交总菜单GUI
     *
     * @param player 玩家
     */
    private void handleMenu(Player player) {
        // 检查权限
        if (!player.hasPermission(Perms.GUI_SOCIAL)) {
            messageManager.send(player, "general.no-permission");
            return;
        }

        // 打开社交总菜单GUI
        SocialMainGUI mainGUI = new SocialMainGUI(plugin, player);
        mainGUI.open(player);
    }

    /**
     * 处理帮助命令
     * 显示所有可用命令的帮助信息
     *
     * @param sender 命令发送者
     */
    private void handleHelp(CommandSender sender) {
        // 检查权限
        if (!sender.hasPermission(Perms.HELP)) {
            messageManager.send(sender, "general.no-permission");
            return;
        }

        // 发送帮助信息
        messageManager.sendList(sender, "help.header");

        // 主命令帮助
        messageManager.sendNoPrefix(sender, "help.social");

        messageManager.sendNoPrefix(sender, "help.help");

        // 好友命令帮助
        if (sender.hasPermission(Perms.FRIEND_BASE)) {
            messageManager.sendNoPrefix(sender, "help.friend");
            messageManager.sendNoPrefix(sender, "help.friend-add");
            messageManager.sendNoPrefix(sender, "help.friend-accept");
            messageManager.sendNoPrefix(sender, "help.friend-deny");
            messageManager.sendNoPrefix(sender, "help.friend-remove");
            messageManager.sendNoPrefix(sender, "help.friend-list");
            messageManager.sendNoPrefix(sender, "help.friend-notify");
            messageManager.sendNoPrefix(sender, "help.friend-requests");
            messageManager.sendNoPrefix(sender, "help.friend-block");
            messageManager.sendNoPrefix(sender, "help.friend-unblock");
            messageManager.sendNoPrefix(sender, "help.friend-blocked");
        }

        // 传送命令帮助
        if (sender.hasPermission(Perms.TELEPORT_BASE)) {
            messageManager.sendNoPrefix(sender, "help.tpf");
            messageManager.sendNoPrefix(sender, "help.tpftoggle");
            messageManager.sendNoPrefix(sender, "help.tpfallow");
            messageManager.sendNoPrefix(sender, "help.tpfdeny");
        }

        messageManager.sendList(sender, "help.footer");
    }

    /**
     * 处理重载配置命令
     * 需要 woosocial.admin 权限
     *
     * @param sender 命令发送者
     */
    private void handleReload(CommandSender sender) {
        // 检查权限
        if (!sender.hasPermission(Perms.ADMIN) && !sender.hasPermission(Perms.RELOAD)) {
            messageManager.send(sender, "general.no-permission");
            return;
        }

        // 异步执行重载操作
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 重载插件配置
                plugin.reloadPluginConfig();

                // 返回主线程发送消息
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messageManager.send(sender, "general.reload-success");
                    plugin.getLogger().info("配置已由 " + sender.getName() + " 重新加载");
                });
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messageManager.send(sender, "general.internal-error");
                    plugin.getLogger().severe("重载配置时发生错误: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 处理查看玩家信息命令
     * 显示指定玩家的社交信息
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleInfo(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission(Perms.ADMIN)) {
            messageManager.send(sender, "general.no-permission");
            return;
        }

        // 检查参数
        if (args.length < 2) {
            messageManager.send(sender, "general.invalid-argument", "arg", "玩家名称");
            return;
        }

        String targetName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

        if (targetPlayer == null || (targetPlayer.getName() == null && !targetPlayer.hasPlayedBefore())) {
            messageManager.send(sender, "general.player-not-found", "player", targetName);
            return;
        }

        // 异步获取玩家信息
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String playerName = targetPlayer.getName() != null ? targetPlayer.getName() : targetName;
            String uuid = targetPlayer.getUniqueId().toString();
            boolean isOnline = targetPlayer.isOnline();
            long lastPlayed = targetPlayer.getLastSeen();

            // 获取好友数量
            int friendCount = plugin.getFriendDAO().getFriendCount(targetPlayer.getUniqueId()).join();

            // 返回主线程显示信息
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messageManager.sendNoPrefix(sender, "&e========== 玩家信息 ==========");
                messageManager.sendNoPrefix(sender, "&7玩家名称: &f" + playerName);
                messageManager.sendNoPrefix(sender, "&7UUID: &f" + uuid);
                messageManager.sendNoPrefix(sender, "&7状态: " + (isOnline ? "&a在线" : "&c离线"));
                messageManager.sendNoPrefix(sender, "&7好友数量: &f" + friendCount);

                if (!isOnline && lastPlayed > 0) {
                    long timeAgo = System.currentTimeMillis() - lastPlayed;
                    String timeStr = formatTimeAgo(timeAgo);
                    messageManager.sendNoPrefix(sender, "&7最后在线: &f" + timeStr);
                }

                messageManager.sendNoPrefix(sender, "&e============================");
            });
        });
    }

    /**
     * 处理版本命令
     * 显示插件版本信息
     *
     * @param sender 命令发送者
     */
    private void handleVersion(CommandSender sender) {
        var meta = plugin.getPluginMeta();
        messageManager.sendNoPrefix(sender, "&e========== WooSocial ==========");
        messageManager.sendNoPrefix(sender, "&7版本: &f" + meta.getVersion());
        messageManager.sendNoPrefix(sender, "&7作者: &f" + String.join(", ", meta.getAuthors()));
        messageManager.sendNoPrefix(sender, "&7描述: &f" + meta.getDescription());
        messageManager.sendNoPrefix(sender, "&7语言: &f" + messageManager.getCurrentLanguage());
        
        Map<String, Module> loadedModules = plugin.getModuleManager().getLoadedModules();
        if (!loadedModules.isEmpty()) {
            messageManager.sendNoPrefix(sender, "&7已启用模块: &f" + String.join(", ", loadedModules.keySet()));
        }

        messageManager.sendNoPrefix(sender, "&e===============================");
    }

    /**
     * 格式化时间差
     *
     * @param timeAgo 时间差（毫秒）
     * @return 格式化后的时间字符串
     */
    private String formatTimeAgo(long timeAgo) {
        long seconds = timeAgo / 1000;

        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + " 分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " 小时前";
        } else {
            return (seconds / 86400) + " 天前";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 子命令补全
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "?", "info", "version", "ver"));

            // 只有有权限的玩家才能看到 reload
            if (sender.hasPermission(Perms.ADMIN) || sender.hasPermission(Perms.RELOAD)) {
                subCommands.add("reload");
            }

            // 过滤匹配的子命令
            String input = args[0].toLowerCase();
            completions.addAll(subCommands.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            // info 子命令补全玩家名称
            if (subCommand.equals("info") && sender.hasPermission(Perms.ADMIN)) {
                String input = args[1].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
