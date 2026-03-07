package com.oolonghoo.woosocial.module.mail.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.config.MessageManager;
import com.oolonghoo.woosocial.gui.BaseGUI;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailListGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private final List<MailData> mails;
    private final int currentPage;
    private final int totalPages;
    
    public MailListGUI(WooSocial plugin, Player viewer, List<MailData> mails, int currentPage, int totalPages) {
        super(plugin, viewer, "mail_list");
        this.mails = mails;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        placeholderParser.set("page", String.valueOf(currentPage));
        placeholderParser.set("total_pages", String.valueOf(totalPages));
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createMailBackButton());
        
        int startIndex = (currentPage - 1) * 45;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + 45, mails.size()); i++) {
            MailData mail = mails.get(i);
            inventory.setItem(slot, createMailItem(mail));
            slot++;
        }
        
        if (currentPage > 1) {
            inventory.setItem(PREV_PAGE_SLOT, createPrevPageButton());
        }
        
        if (currentPage < totalPages) {
            inventory.setItem(NEXT_PAGE_SLOT, createNextPageButton());
        }
    }
    
    private ItemStack createMailItem(MailData mail) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer sender = Bukkit.getOfflinePlayer(mail.getSenderUuid());
        meta.setOwningPlayer(sender);
        
        String senderName = mail.getSenderName() != null ? mail.getSenderName() : "未知";
        if (!mail.isRead()) {
            meta.displayName(messageManager.getComponent("mail.unread-prefix")
                    .append(Component.text(senderName, NamedTextColor.GOLD)));
        } else {
            meta.displayName(Component.text(senderName, NamedTextColor.GRAY));
        }
        
        List<Component> lore = new ArrayList<>();
        
        String sendTime = DATE_FORMAT.format(new Date(mail.getSendTime()));
        lore.add(messageManager.getComponent("mail.lore-send-time")
                .append(Component.text(sendTime, NamedTextColor.YELLOW)));
        
        if (mail.isClaimed()) {
            lore.add(messageManager.getComponent("mail.lore-status")
                    .append(messageManager.getComponent("mail.status-claimed")));
        } else {
            lore.add(messageManager.getComponent("mail.lore-status")
                    .append(messageManager.getComponent("mail.status-unclaimed")));
        }
        
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("mail.click-to-view"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    protected ItemStack createMailBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        item.setItemMeta(meta);
        return item;
    }
    
    protected ItemStack createPrevPageButton() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.prev-page"));
        item.setItemMeta(meta);
        return item;
    }
    
    protected ItemStack createNextPageButton() {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("gui.next-page"));
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        setupItems();
    }
    
    @Override
    public void handleClick(int slot, Player player, int clickType) {
        if (slot == BACK_SLOT) {
            player.closeInventory();
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentPage > 1) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, currentPage - 1);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, currentPage + 1);
            return;
        }
        
        int mailIndex = (currentPage - 1) * 45 + (slot - 9);
        if (mailIndex >= 0 && mailIndex < mails.size()) {
            MailData mail = mails.get(mailIndex);
            plugin.getModuleManager().getMailModule().getMailManager().openMailDetailGUI(player, mail.getId());
        }
    }
}
