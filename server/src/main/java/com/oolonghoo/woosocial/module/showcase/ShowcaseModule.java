package com.oolonghoo.woosocial.module.showcase;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.database.ShowcaseDAO;
import com.oolonghoo.woosocial.database.ShowcaseLikeCooldownDAO;
import com.oolonghoo.woosocial.listener.ShowcaseListener;
import com.oolonghoo.woosocial.module.Module;

public class ShowcaseModule extends Module {
    
    private ShowcaseDAO showcaseDAO;
    private ShowcaseManager showcaseManager;
    private ShowcaseListener showcaseListener;
    
    public ShowcaseModule(WooSocial plugin) {
        super(plugin, "Showcase");
    }
    
    @Override
    public void onEnable() {
        showcaseDAO = new ShowcaseDAO(plugin, plugin.getDatabaseManager());
        ShowcaseLikeCooldownDAO cooldownDAO = new ShowcaseLikeCooldownDAO(plugin, plugin.getDatabaseManager());
        showcaseManager = new ShowcaseManager(plugin, showcaseDAO, cooldownDAO);
        
        showcaseListener = new ShowcaseListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(showcaseListener, plugin);
        
        log("展示柜模块已启用");
    }
    
    @Override
    public void onDisable() {
        if (showcaseManager != null) {
            showcaseManager.saveAll();
        }
        log("展示柜模块已禁用");
    }
    
    @Override
    public void onReload() {
        log("展示柜模块已重载");
    }
    
    @Override
    public void saveAll() {
        if (showcaseManager != null) {
            showcaseManager.saveAll();
        }
    }
    
    public ShowcaseDAO getShowcaseDAO() {
        return showcaseDAO;
    }
    
    public ShowcaseManager getShowcaseManager() {
        return showcaseManager;
    }
}
