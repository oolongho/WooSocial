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

public class MailDetailGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private final MailData mail;
    
    private static final int BACK_SLOT = 0;
    private static final int ITEM_SLOT = 4;
    private static final int CLAIM_SLOT = 47;
    private static final int DELETE_SLOT = 48;
    
    public MailDetailGUI(WooSocial plugin, Player viewer, MailData mail) {
        super(plugin, viewer, "mail_detail");
        this.mail = mail;
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        placeholderParser.set("sender_name", mail.getSenderName() != null ? mail.getSenderName() : "未知");
        placeholderParser.set("send_time", DATE_FORMAT.format(new Date(mail.getSendTime())));
        placeholderParser.set("is_claimed", String.valueOf(mail.isClaimed()));
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createMailBackButton());
        inventory.setItem(ITEM_SLOT, createItemDisplay());
        inventory.setItem(CLAIM_SLOT, createClaimButton());
        inventory.setItem(DELETE_SLOT, createDeleteButton());
    }
    
    private ItemStack createItemDisplay() {
        ItemStack item = ItemSerializer.deserialize(mail.getItemData());
        
        if (item == null) {
            return new ItemStack(Material.BARRIER);
        }
        
        return item.clone();
    }
    
    protected ItemStack createMailBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回邮件列表", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createClaimButton() {
        if (mail.isClaimed()) {
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            var meta = item.getItemMeta();
            meta.displayName(messageManager.getComponent("mail.already-claimed"));
            item.setItemMeta(meta);
            return item;
        }
        
        ItemStack item = new ItemStack(Material.LIME_DYE);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.claim-item"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("mail.click-to-claim"));
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createDeleteButton() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.delete-mail"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(messageManager.getComponent("mail.shift-click-delete"));
        meta.lore(lore);
        
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
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, 1);
            return;
        }
        
        if (slot == CLAIM_SLOT) {
            if (!mail.isClaimed()) {
                player.closeInventory();
                plugin.getModuleManager().getMailModule().getMailManager().claimMail(player, mail.getId());
            }
            return;
        }
        
        if (slot == DELETE_SLOT) {
            if (clickType == 1) {
                player.closeInventory();
                plugin.getModuleManager().getMailModule().getMailManager().forceDeleteMail(player, mail.getId());
            }
            return;
        }
    }
}
