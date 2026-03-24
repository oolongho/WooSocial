package com.oolonghoo.woosocial.module.trade.command;

import com.oolonghoo.woosocial.Perms;
import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.module.trade.database.TradeLogDAO;
import com.oolonghoo.woosocial.module.trade.database.TradeLogDAO.TradeRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 交易历史查询命令
 */
public class TradeHistoryCommand implements CommandExecutor, TabCompleter {
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 10;
    
    private final MessageManager messageManager;
    private final TradeLogDAO tradeLogDAO;
    
    public TradeHistoryCommand(WooSocial plugin) {
        this.messageManager = plugin.getMessageManager();
        this.tradeLogDAO = new TradeLogDAO(plugin, plugin.getDatabaseManager());
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.send(sender, "general.player-only");
            return true;
        }
        
        if (!player.hasPermission(Perms.TRADE_HISTORY)) {
            messageManager.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            // 查看自己的交易历史
            showHistory(player, player.getUniqueId(), 1);
            return true;
        }
        
        if (args.length == 1) {
            if (args[0].matches("\\d+")) {
                // 查看指定页码
                int page = Integer.parseInt(args[0]);
                showHistory(player, player.getUniqueId(), page);
            } else if (player.hasPermission(Perms.TRADE_HISTORY_OTHERS)) {
                // 查看他人的交易历史
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    showHistory(player, target.getUniqueId(), 1);
                } else {
                    messageManager.send(player, "general.player-not-found");
                }
            } else {
                messageManager.send(player, "general.no-permission");
            }
            return true;
        }
        
        if (args.length == 2 && player.hasPermission(Perms.TRADE_HISTORY_OTHERS)) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                try {
                    int page = Integer.parseInt(args[1]);
                    showHistory(player, target.getUniqueId(), page);
                } catch (NumberFormatException e) {
                    messageManager.send(player, "general.invalid-number");
                }
            } else {
                messageManager.send(player, "general.player-not-found");
            }
            return true;
        }
        
        // 显示帮助
        showHelp(player);
        return true;
    }
    
    /**
     * 显示交易历史
     */
    private void showHistory(Player player, UUID targetUuid, int pageNum) {
        if (pageNum < 1) pageNum = 1;
        final int page = pageNum; // 确保是 final
        
        CompletableFuture<Integer> countFuture = tradeLogDAO.getPlayerTradeCount(targetUuid);
        CompletableFuture<List<TradeRecord>> historyFuture = tradeLogDAO.getPlayerTradeHistory(
                targetUuid, PAGE_SIZE, (page - 1) * PAGE_SIZE
        );
        
        CompletableFuture.allOf(countFuture, historyFuture).thenAccept(v -> {
            int total = countFuture.join();
            List<TradeRecord> records = historyFuture.join();
            
            int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
            int currentPage = page;
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            }
            
            // 显示标题
            String targetName = targetUuid.equals(player.getUniqueId()) ? "你的" : Bukkit.getOfflinePlayer(targetUuid).getName() + "的";
            player.sendMessage(Component.text("§e========== §6" + targetName + "交易历史 §e=========="));
            player.sendMessage(Component.text("§7第 §f" + currentPage + "§7/§f" + totalPages + "§7 页，共 §f" + total + "§7 条记录"));
            player.sendMessage(Component.text(""));
            
            if (records.isEmpty()) {
                player.sendMessage(Component.text("§7暂无交易记录").color(NamedTextColor.GRAY));
            } else {
                for (TradeRecord record : records) {
                    player.sendMessage(formatRecord(record, player.getUniqueId()));
                }
            }
            
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("§e================================").color(TextColor.color(255, 255, 85)));
            
            // 分页导航
            if (totalPages > 1) {
                StringBuilder pagination = new StringBuilder();
                if (currentPage > 1) {
                    pagination.append("§e/trade history ").append(currentPage - 1).append(" §7上一页 ");
                }
                if (currentPage < totalPages) {
                    pagination.append("§e/trade history ").append(currentPage + 1).append(" §7下一页");
                }
                player.sendMessage(Component.text(pagination.toString()));
            }
        });
    }
    
    /**
     * 格式化交易记录
     */
    private Component formatRecord(TradeRecord record, UUID viewerUuid) {
        boolean isPlayer1 = record.getPlayer1Uuid().equals(viewerUuid);
        String partnerName = isPlayer1 ? record.getPlayer2Name() : record.getPlayer1Name();
        
        StringBuilder sb = new StringBuilder();
        sb.append("§7[§f").append(dateFormat.format(new Date(record.getTimestamp()))).append("§7] ");
        sb.append("§7与 §f").append(partnerName).append("§7 ");
        
        if (record.isCompleted()) {
            sb.append("§a完成");
        } else {
            sb.append("§c取消");
            if (record.getCancelReason() != null && !record.getCancelReason().isEmpty()) {
                sb.append(" (").append(record.getCancelReason()).append(")");
            }
        }
        
        // 显示收益/损失
        double moneyGained = isPlayer1 ? record.getPlayer2Money() - record.getPlayer1Money() 
                                       : record.getPlayer1Money() - record.getPlayer2Money();
        
        if (moneyGained > 0) {
            sb.append(" §a+").append(String.format("%.2f", moneyGained));
        } else if (moneyGained < 0) {
            sb.append(" §c").append(String.format("%.2f", moneyGained));
        }
        
        return Component.text(sb.toString());
    }
    
    /**
     * 显示帮助
     */
    private void showHelp(Player player) {
        player.sendMessage(Component.text("§e========== §6交易历史帮助 §e=========="));
        player.sendMessage(Component.text("§e/trade history §7- 查看你的交易历史"));
        player.sendMessage(Component.text("§e/trade history [页码] §7- 查看指定页码"));
        player.sendMessage(Component.text("§e/trade history [玩家] §7- 查看他人的交易历史"));
        player.sendMessage(Component.text("§e/trade history [玩家] [页码] §7- 查看他人指定页码"));
        player.sendMessage(Component.text("§e================================"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(Perms.TRADE_HISTORY_OTHERS)) {
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
