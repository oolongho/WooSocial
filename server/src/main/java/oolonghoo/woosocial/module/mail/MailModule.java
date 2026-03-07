package com.oolonghoo.woosocial.module.mail;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.module.Module;
import com.oolonghoo.woosocial.module.mail.command.MailCommand;
import com.oolonghoo.woosocial.module.mail.gui.MailDetailGUI;
import com.oolonghoo.woosocial.module.mail.gui.MailListGUI;
import com.oolonghoo.woosocial.module.mail.listener.MailListener;
import org.bukkit.Bukkit;

public class MailModule extends Module {
    
    private MailDataManager dataManager;
    private MailManager mailManager;
    private MailCommand mailCommand;
    private MailListener mailListener;
    
    public MailModule(WooSocial plugin) {
        super(plugin, "mail");
    }
    
    @Override
    public void onEnable() {
        dataManager = new MailDataManager(plugin);
        dataManager.initialize();
        
        mailManager = new MailManager(plugin, dataManager);
        
        mailCommand = new MailCommand(plugin, dataManager, mailManager);
        try {
            plugin.getCommand("mail").setExecutor(mailCommand);
            plugin.getCommand("mail").setTabCompleter(mailCommand);
        } catch (Exception e) {
            logWarning("命令处理器注册失败: " + e.getMessage());
        }
        
        mailListener = new MailListener(plugin, dataManager, mailManager);
        plugin.getServer().getPluginManager().registerEvents(mailListener, plugin);
        
        startCleanupTask();
        
        log("邮箱模块已启用");
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        if (mailListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(mailListener);
        }
        
        log("邮箱模块已关闭");
    }
    
    @Override
    public void onReload() {
        saveAll();
    }
    
    @Override
    public void saveAll() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
    }
    
    private void startCleanupTask() {
        long interval = 20L * 60 * 60;
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            dataManager.cleanExpiredMails();
        }, interval, interval);
    }
    
    public MailDataManager getDataManager() {
        return dataManager;
    }
    
    public MailManager getMailManager() {
        return mailManager;
    }
}
