package com.oolonghoo.woosocial.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.ActionParser;
import com.oolonghoo.woosocial.gui.config.GUIConfig;
import com.oolonghoo.woosocial.gui.config.IconConfig;
import com.oolonghoo.woosocial.util.PlaceholderParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class BaseGUI implements InventoryHolder {
    
    protected final WooSocial plugin;
    protected final MessageManager messageManager;
    protected final GUIConfig guiConfig;
    protected final ActionParser actionParser;
    protected final PlaceholderParser placeholderParser;
    protected final String guiName;
    protected final Player viewer;
    protected Inventory inventory;
    protected int currentPage = 1;
    protected int totalPages = 1;
    
    private final Map<Integer, IconConfig> slotIconMap = new HashMap<>();
    private final Map<Integer, Map<String, Object>> slotDataContext = new HashMap<>();
    
    protected static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    protected static final int BACK_SLOT = 0;
    protected static final int PREV_PAGE_SLOT = 45;
    protected static final int NEXT_PAGE_SLOT = 53;
    
    public BaseGUI(WooSocial plugin, Player viewer, String guiName) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.viewer = viewer;
        this.guiName = guiName;
        this.guiConfig = plugin.getGuiConfigManager().getConfig(guiName);
        this.actionParser = plugin.getActionParser();
        this.placeholderParser = new PlaceholderParser();
        
        createInventory();
    }
    
    private void createInventory() {
        if (guiConfig != null && guiConfig.getTitle() != null) {
            String title = placeholderParser.parse(guiConfig.getTitle());
            this.inventory = Bukkit.createInventory(this, 54, 
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(title));
        } else {
            this.inventory = Bukkit.createInventory(this, 54, Component.text("GUI"));
        }
    }
    
    protected void renderFromConfig() {
        if (guiConfig == null) {
            plugin.getLogger().warning("[GUI] 配置未找到: " + guiName);
            return;
        }
        
        slotIconMap.clear();
        slotDataContext.clear();
        
        setupPlaceholders();
        
        for (int row = 0; row < guiConfig.getLayout().size(); row++) {
            String line = guiConfig.getLayout().get(row);
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                int slot = row * 9 + col;
                
                IconConfig iconConfig = guiConfig.getIcon(String.valueOf(ch));
                if (iconConfig != null) {
                    if (!iconConfig.isSpecialType()) {
                        ItemStack item = createItemFromConfig(iconConfig);
                        inventory.setItem(slot, item);
                        slotIconMap.put(slot, iconConfig);
                    }
                }
            }
        }
    }
    
    protected void renderDynamicContent(char layoutChar, List<DynamicContent> contents) {
        List<Integer> slots = guiConfig.getSlotsForChar(layoutChar);
        IconConfig template = guiConfig.getIcon(String.valueOf(layoutChar));
        
        if (template == null) return;
        
        int startIndex = (currentPage - 1) * slots.size();
        int endIndex = Math.min(startIndex + slots.size(), contents.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= slots.size()) break;
            
            int slot = slots.get(slotIndex);
            DynamicContent content = contents.get(i);
            
            PlaceholderParser contentParser = new PlaceholderParser();
            setupContentPlaceholders(contentParser, content);
            
            ItemStack item = createItemFromConfig(template, contentParser);
            inventory.setItem(slot, item);
            slotIconMap.put(slot, template);
            
            Map<String, Object> context = new HashMap<>();
            context.put("content", content);
            slotDataContext.put(slot, context);
        }
    }
    
    protected abstract void setupPlaceholders();
    
    protected void setupContentPlaceholders(PlaceholderParser parser, DynamicContent content) {
    }
    
    protected ItemStack createItemFromConfig(IconConfig config) {
        return createItemFromConfig(config, placeholderParser);
    }
    
    protected ItemStack createItemFromConfig(IconConfig config, PlaceholderParser parser) {
        String materialStr = parser.parse(config.getMaterial());
        Material material = parseMaterial(materialStr);
        
        ItemStack item = new ItemStack(material, config.getAmount());
        ItemMeta meta = item.getItemMeta();
        
        if (config.getName() != null) {
            meta.displayName(parser.parseToComponent(config.getName()));
        }
        
        if (!config.getLore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : config.getLore()) {
                lore.add(parser.parseToComponent(line));
            }
            meta.lore(lore);
        }
        
        if (material == Material.PLAYER_HEAD && config.getSkullOwner() != null) {
            String owner = parser.parse(config.getSkullOwner());
            if (owner != null && !owner.isEmpty()) {
                ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            }
        }
        
        if (config.getCustomModelData() > 0) {
            meta.setCustomModelData(config.getCustomModelData());
        }
        
        if (config.isGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private Material parseMaterial(String materialStr) {
        if (materialStr == null || materialStr.isEmpty()) {
            return Material.STONE;
        }
        
        if (materialStr.toLowerCase().startsWith("head:")) {
            return Material.PLAYER_HEAD;
        }
        
        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
    
    public void handleClick(int slot, Player player, int clickType) {
        IconConfig config = slotIconMap.get(slot);
        if (config == null || !config.hasActions()) return;
        
        String clickKey = switch (clickType) {
            case 1 -> "right";
            case 2 -> "shift_left";
            default -> "left";
        };
        
        List<String> actions = config.getActions().getOrDefault(clickKey, config.getLeftActions());
        
        ActionContext context = ActionContext.create()
                .guiName(guiName)
                .friendUuid(getFriendUuidFromSlot(slot))
                .friendName(getFriendNameFromSlot(slot))
                .page(currentPage, totalPages);
        
        actionParser.executeActions(actions, player, context);
    }
    
    private UUID getFriendUuidFromSlot(int slot) {
        Map<String, Object> data = slotDataContext.get(slot);
        if (data != null && data.get("content") instanceof DynamicContent) {
            return ((DynamicContent) data.get("content")).getUuid();
        }
        return null;
    }
    
    private String getFriendNameFromSlot(int slot) {
        Map<String, Object> data = slotDataContext.get(slot);
        if (data != null && data.get("content") instanceof DynamicContent) {
            return ((DynamicContent) data.get("content")).getName();
        }
        return null;
    }
    
    protected void setPlaceholder(String key, Object value) {
        placeholderParser.set(key, value);
    }
    
    protected void setCondition(String key, boolean value) {
        placeholderParser.setCondition(key, value);
    }
    
    public abstract void refresh();
    
    public void open(Player player) {
        player.openInventory(inventory);
    }
    
    protected void fillBorder(int size) {
        ItemStack borderItem = createBorderItem();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
        }
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, borderItem);
        }
        for (int i = 0; i < size; i += 9) {
            inventory.setItem(i, borderItem);
            inventory.setItem(i + 8, borderItem);
        }
    }
    
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }
    
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }
    
    protected void setupNavigation() {
        if (totalPages > 1) {
            if (currentPage > 1) {
                ItemStack prevButton = new ItemStack(Material.SPECTRAL_ARROW);
                ItemMeta prevMeta = prevButton.getItemMeta();
                prevMeta.displayName(Component.text("上一页", NamedTextColor.GREEN));
                prevButton.setItemMeta(prevMeta);
                inventory.setItem(PREV_PAGE_SLOT, prevButton);
            }
            if (currentPage < totalPages) {
                ItemStack nextButton = new ItemStack(Material.SPECTRAL_ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                nextMeta.displayName(Component.text("下一页", NamedTextColor.GREEN));
                nextButton.setItemMeta(nextMeta);
                inventory.setItem(NEXT_PAGE_SLOT, nextButton);
            }
        }
    }
    
    protected void handlePagination(int clickType, Player player) {
        if (clickType == 1) {
            if (currentPage < totalPages) {
                currentPage++;
                refresh();
            }
        } else {
            if (currentPage > 1) {
                currentPage--;
                refresh();
            }
        }
    }
    
    protected int calculateTotalPages(int totalItems, int itemsPerPage) {
        return Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
    }
    
    protected int getPageStartIndex(int page) {
        return (page - 1) * CONTENT_SLOTS.length;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getViewer() {
        return viewer;
    }
    
    public String getGuiName() {
        return guiName;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int pages) {
        this.totalPages = pages;
    }
    
    public static class DynamicContent {
        private final UUID uuid;
        private final String name;
        private final Map<String, Object> data = new HashMap<>();
        
        public DynamicContent(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getName() {
            return name;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public DynamicContent set(String key, Object value) {
            data.put(key, value);
            return this;
        }
    }
}
