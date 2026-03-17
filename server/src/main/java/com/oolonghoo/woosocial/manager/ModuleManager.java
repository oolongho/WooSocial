package com.oolonghoo.woosocial.manager;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.ConfigLoader;
import com.oolonghoo.woosocial.module.Module;
import com.oolonghoo.woosocial.module.friend.FriendModule;
import com.oolonghoo.woosocial.module.mail.MailModule;
import com.oolonghoo.woosocial.module.teleport.TeleportModule;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 模块管理器
 * 负责管理所有功能模块的生命周期
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class ModuleManager {
    
    private final WooSocial plugin;
    private final Map<String, Supplier<Module>> moduleFactories = new LinkedHashMap<>();
    private final Map<String, Module> loadedModules = new HashMap<>();
    private ConfigLoader modulesLoader;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public ModuleManager(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化模块管理器
     */
    public void initialize() {
        this.modulesLoader = new ConfigLoader(plugin, "modules.yml");
        this.modulesLoader.initialize();
    }
    
    /**
     * 注册模块工厂
     * 
     * @param name 模块名称
     * @param factory 模块工厂
     */
    public void registerModule(String name, Supplier<Module> factory) {
        moduleFactories.put(name.toLowerCase(), factory);
    }
    
    /**
     * 加载所有启用的模块
     */
    public void loadEnabledModules() {
        if (modulesLoader == null) {
            plugin.getLogger().warning("模块配置加载器未初始化");
            return;
        }
        
        FileConfiguration modulesConfig = modulesLoader.getConfig();
        
        loadModule("friend");
        
        for (Map.Entry<String, Supplier<Module>> entry : moduleFactories.entrySet()) {
            String moduleName = entry.getKey();
            
            if ("friend".equals(moduleName)) {
                continue;
            }
            
            boolean enabled = modulesConfig.getBoolean("modules." + moduleName + ".enabled", true);
            
            if (enabled) {
                loadModule(moduleName);
            }
        }
    }
    
    /**
     * 加载指定模块
     * 
     * @param name 模块名称
     */
    public void loadModule(String name) {
        final String moduleName = name.toLowerCase();
        
        if (loadedModules.containsKey(moduleName)) {
            plugin.getLogger().warning(() -> "模块 " + moduleName + " 已经加载");
            return;
        }
        
        Supplier<Module> factory = moduleFactories.get(moduleName);
        if (factory == null) {
            plugin.getLogger().warning(() -> "未知模块：" + moduleName);
            return;
        }
        
        try {
            Module module = factory.get();
            module.onEnable();
            loadedModules.put(moduleName, module);
        } catch (Exception e) {
            plugin.getLogger().severe(() -> "加载模块 " + moduleName + " 时发生错误：" + e.getMessage());
        }
    }
    
    /**
     * 卸载指定模块
     * 
     * @param name 模块名称
     */
    public void unloadModule(String name) {
        final String moduleName = name.toLowerCase();
        
        Module module = loadedModules.remove(moduleName);
        if (module != null) {
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.getLogger().severe(() -> "卸载模块 " + moduleName + " 时发生错误：" + e.getMessage());
            }
        }
    }
    
    /**
     * 重载指定模块
     * 
     * @param name 模块名称
     */
    public void reloadModule(String name) {
        name = name.toLowerCase();
        
        if (loadedModules.containsKey(name)) {
            unloadModule(name);
        }
        
        FileConfiguration modulesConfig = modulesLoader.getConfig();
        boolean enabled = modulesConfig.getBoolean("modules." + name + ".enabled", true);
        
        if (enabled) {
            loadModule(name);
        }
    }
    
    /**
     * 重载所有模块
     */
    public void reloadAllModules() {
        // 重载配置文件
        if (modulesLoader != null) {
            modulesLoader.reload();
        }
        
        // 重载所有模块
        for (String moduleName : moduleFactories.keySet()) {
            reloadModule(moduleName);
        }
    }
    
    /**
     * 禁用所有模块
     */
    public void disableAllModules() {
        if (!loadedModules.isEmpty()) {
            for (Map.Entry<String, Module> entry : loadedModules.entrySet()) {
                try {
                    entry.getValue().onDisable();
                } catch (Exception e) {
                    plugin.getLogger().severe(() -> "禁用模块 " + entry.getKey() + " 时发生错误：" + e.getMessage());
                }
            }
            loadedModules.clear();
        }
    }
    
    /**
     * 保存所有模块数据
     */
    public void saveAllModules() {
        for (Module module : loadedModules.values()) {
            try {
                module.saveAll();
            } catch (Exception e) {
                plugin.getLogger().severe(() -> "保存模块 " + module.getName() + " 数据时发生错误：" + e.getMessage());
            }
        }
    }
    
    /**
     * 获取指定模块
     * 
     * @param name 模块名称
     * @return 模块实例，如果未加载则返回null
     */
    public Module getModule(String name) {
        return loadedModules.get(name.toLowerCase());
    }
    
    /**
     * 获取指定类型的模块
     * 
     * @param name 模块名称
     * @param moduleClass 模块类型
     * @return 模块实例，如果未加载或类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name, Class<T> moduleClass) {
        Module module = loadedModules.get(name.toLowerCase());
        if (module != null && moduleClass.isInstance(module)) {
            return (T) module;
        }
        return null;
    }
    
    /**
     * 检查模块是否已加载
     * 
     * @param name 模块名称
     * @return 是否已加载
     */
    public boolean isModuleLoaded(String name) {
        return loadedModules.containsKey(name.toLowerCase());
    }
    
    /**
     * 获取所有已加载的模块
     * 
     * @return 模块映射
     */
    public Map<String, Module> getLoadedModules() {
        return new HashMap<>(loadedModules);
    }
    
    /**
     * 获取模块配置加载器
     * 
     * @return 配置加载器
     */
    public ConfigLoader getModulesLoader() {
        return modulesLoader;
    }
    
    /**
     * 获取好友模块
     * 
     * @return 好友模块实例，如果未加载则返回null
     */
    public FriendModule getFriendModule() {
        return getModule("friend", FriendModule.class);
    }
    
    /**
     * 获取传送模块
     * 
     * @return 传送模块实例，如果未加载则返回null
     */
    public TeleportModule getTeleportModule() {
        return getModule("teleport", TeleportModule.class);
    }
    
    /**
     * 获取邮箱模块
     * 
     * @return 邮箱模块实例，如果未加载则返回null
     */
    public MailModule getMailModule() {
        return getModule("mail", MailModule.class);
    }
    
    /**
     * 获取关系模块
     * 
     * @return 关系模块实例，如果未加载则返回null
     */
    public com.oolonghoo.woosocial.module.relation.RelationModule getRelationModule() {
        return getModule("relation", com.oolonghoo.woosocial.module.relation.RelationModule.class);
    }
    
    /**
     * 获取展示柜模块
     * 
     * @return 展示柜模块实例，如果未加载则返回null
     */
    public com.oolonghoo.woosocial.module.showcase.ShowcaseModule getShowcaseModule() {
        return getModule("showcase", com.oolonghoo.woosocial.module.showcase.ShowcaseModule.class);
    }
}
