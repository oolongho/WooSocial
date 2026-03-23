package com.oolonghoo.woosocial.module.trade.config;

import com.oolonghoo.woosocial.WooSocial;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易 GUI 配置
 * 提供完全可自定义的 GUI 配置支持
 */
public class TradeGUIConfig {
    
    private static final String LEGACY_COLOR_CHAR = "§";
    
    private final WooSocial plugin;
    private FileConfiguration config;
    
    private String titleTemplate = "&8交易 - &e{player}";
    private int size = 54;
    
    private List<Integer> myItemSlots = new ArrayList<>();
    private List<Integer> otherItemSlots = new ArrayList<>();
    private int readyButtonSlot = 47;
    private int cancelButtonSlot = 49;
    private int moneyButtonSlot = 45;
    
    private ItemStack readyButton;
    private ItemStack cancelButton;
    private ItemStack moneyButton;
    private Map<Integer, ItemStack> decorations = new HashMap<>();
    
    public TradeGUIConfig(WooSocial plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection == null) {
            useDefaults();
            return;
        }
        
        // 加载基础配置
        titleTemplate = guiSection.getString("title", titleTemplate);
        size = guiSection.getInt("size", 54);
        
        // 加载布局配置
        ConfigurationSection layoutSection = guiSection.getConfigurationSection("layout");
        if (layoutSection != null) {
            myItemSlots = layoutSection.getIntegerList("my-items");
            otherItemSlots = layoutSection.getIntegerList("other-items");
            readyButtonSlot = layoutSection.getInt("ready-button", readyButtonSlot);
            cancelButtonSlot = layoutSection.getInt("cancel-button", cancelButtonSlot);
            moneyButtonSlot = layoutSection.getInt("money-button", moneyButtonSlot);
        }
        
        // 如果配置为空，使用默认值
        if (myItemSlots.isEmpty()) {
            myItemSlots = List.of(0,1,2,3,4,9,10,11,12,13,18,19,20,21,22,27,28,29,30,31);
        }
        if (otherItemSlots.isEmpty()) {
            otherItemSlots = List.of(5,6,7,8,14,15,16,17,23,24,25,26,32,33,34,35,36,37,38,39);
        }
        
        // 加载物品配置
        loadItems(guiSection);
    }
    
    /**
     * 使用默认配置
     */
    private void useDefaults() {
        myItemSlots = List.of(0,1,2,3,4,9,10,11,12,13,18,19,20,21,22,27,28,29,30,31);
        otherItemSlots = List.of(5,6,7,8,14,15,16,17,23,24,25,26,32,33,34,35,36,37,38,39);
        createDefaultButtons();
    }
    
    /**
     * 加载物品配置
     */
    private void loadItems(ConfigurationSection guiSection) {
        ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");
        if (itemsSection == null) {
            createDefaultButtons();
            return;
        }
        
        // 加载确认按钮
        readyButton = loadItemStack(itemsSection, "ready-button", 
                Material.LIME_DYE, "&a确认交易", List.of("&7点击确认交易"));
        
        // 加载取消按钮
        cancelButton = loadItemStack(itemsSection, "cancel-button",
                Material.RED_DYE, "&c取消交易", List.of("&7点击取消交易"));
        
        // 加载金币按钮
        moneyButton = loadItemStack(itemsSection, "money-button",
                Material.GOLD_INGOT, "&e放入金币", List.of("&7点击输入金币数量"));
        
        // 加载装饰物品
        loadDecorations(itemsSection);
    }
    
    /**
     * 创建默认按钮
     */
    private void createDefaultButtons() {
        readyButton = createItem(Material.LIME_DYE, "&a确认交易", List.of("&7点击确认交易"));
        cancelButton = createItem(Material.RED_DYE, "&c取消交易", List.of("&7点击取消交易"));
        moneyButton = createItem(Material.GOLD_INGOT, "&e放入金币", List.of("&7点击输入金币数量"));
    }
    
    /**
     * 加载装饰物品
     */
    private void loadDecorations(ConfigurationSection itemsSection) {
        decorations.clear();
        
        ConfigurationSection decorationSection = itemsSection.getConfigurationSection("decoration");
        if (decorationSection == null) {
            // 默认装饰
            List<Integer> defaultSlots = List.of(46, 48, 50, 51, 52, 53);
            ItemStack decoration = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int slot : defaultSlots) {
                decorations.put(slot, decoration);
            }
            return;
        }
        
        Material material = Material.valueOf(decorationSection.getString("material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        String name = decorationSection.getString("name", " ");
        List<String> lore = decorationSection.getStringList("lore");
        List<Integer> slots = decorationSection.getIntegerList("slots");
        
        ItemStack decoration = createItem(material, name, lore);
        for (int slot : slots) {
            decorations.put(slot, decoration);
        }
    }
    
    /**
     * 从配置加载物品
     */
    private ItemStack loadItemStack(ConfigurationSection section, String path, 
                                   Material defaultMaterial, String defaultName, List<String> defaultLore) {
        ConfigurationSection itemSection = section.getConfigurationSection(path);
        if (itemSection == null) {
            return createItem(defaultMaterial, defaultName, defaultLore);
        }
        
        String materialStr = itemSection.getString("material", defaultMaterial.name());
        Material material = Material.valueOf(materialStr.toUpperCase());
        String name = itemSection.getString("name", defaultName);
        List<String> lore = itemSection.getStringList("lore");
        if (lore.isEmpty()) {
            lore = defaultLore;
        }
        
        return createItem(material, name, lore);
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(parseComponent(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(parseComponent(line));
                }
                meta.lore(loreComponents);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 解析组件文本
     */
    private Component parseComponent(String text) {
        try {
            return Component.text(text.replace('&', '§'));
        } catch (Exception e) {
            return Component.text(text);
        }
    }
    
    /**
     * 创建物品（简化版）
     */
    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, List.of());
    }
    
    /**
     * 获取带玩家名称的标题
     */
    public String getTitle(String playerName) {
        return titleTemplate.replace("{player}", playerName);
    }
    
    // Getters
    
    public String getTitleTemplate() {
        return titleTemplate;
    }
    
    public int getSize() {
        return size;
    }
    
    public List<Integer> getMyItemSlots() {
        return myItemSlots;
    }
    
    public List<Integer> getOtherItemSlots() {
        return otherItemSlots;
    }
    
    public int getReadyButtonSlot() {
        return readyButtonSlot;
    }
    
    public int getCancelButtonSlot() {
        return cancelButtonSlot;
    }
    
    public int getMoneyButtonSlot() {
        return moneyButtonSlot;
    }
    
    public ItemStack getReadyButton() {
        return readyButton.clone();
    }
    
    public ItemStack getCancelButton() {
        return cancelButton.clone();
    }
    
    public ItemStack getMoneyButton() {
        return moneyButton.clone();
    }
    
    public Map<Integer, ItemStack> getDecorations() {
        Map<Integer, ItemStack> cloned = new HashMap<>();
        decorations.forEach((slot, item) -> cloned.put(slot, item.clone()));
        return cloned;
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        plugin.reloadConfig();
        loadConfig();
    }
}
