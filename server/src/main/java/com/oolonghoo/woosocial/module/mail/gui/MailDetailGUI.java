package com.oolonghoo.woosocial.module.mail.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.BaseGUI;
import com.oolonghoo.woosocial.gui.LoadingState;
import com.oolonghoo.woosocial.model.MailData;
import com.oolonghoo.woosocial.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailDetailGUI extends BaseGUI {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private final MailData mail;
    private final LoadingState loadingState;
    private final com.oolonghoo.woosocial.module.mail.MailDataManager mailDataManager;
    
    private static final int MAIL_DETAIL_BACK_SLOT = 0;
    private static final int SENDER_HEAD_SLOT = 4;
    private static final int ITEM_DISPLAY_SLOT = 22;
    private static final int CLAIM_SLOT = 47;
    private static final int DELETE_SLOT = 51;
    private static final int REPLY_SLOT = 48;
    
    public MailDetailGUI(WooSocial plugin, Player viewer, MailData mail, LoadingState loadingState) {
        super(plugin, viewer, "mail_detail");
        this.mail = mail;
        this.loadingState = loadingState;
        this.mailDataManager = plugin.getModuleManager().getMailModule().getDataManager();
        
        if (!mail.isRead()) {
            mailDataManager.markAsRead(viewer.getUniqueId(), mail.getId());
        }
        
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
        
        inventory.setItem(MAIL_DETAIL_BACK_SLOT, createMailBackButton());
        inventory.setItem(SENDER_HEAD_SLOT, createSenderInfoItem());
        inventory.setItem(ITEM_DISPLAY_SLOT, createItemDisplay());
        inventory.setItem(CLAIM_SLOT, createClaimButton());
        inventory.setItem(DELETE_SLOT, createDeleteButton());
        inventory.setItem(REPLY_SLOT, createReplyButton());
    }
    
    private ItemStack createSenderInfoItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        String senderName = mail.getSenderName() != null ? mail.getSenderName() : "未知";
        OfflinePlayer sender = Bukkit.getOfflinePlayer(mail.getSenderUuid());
        meta.setOwningPlayer(sender);
        
        meta.displayName(Component.text("发件人: ", NamedTextColor.GRAY)
                .append(Component.text(senderName, NamedTextColor.GREEN)));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        String sendTime = DATE_FORMAT.format(new Date(mail.getSendTime()));
        lore.add(Component.text("发送时间: ", NamedTextColor.GRAY)
                .append(Component.text(sendTime, NamedTextColor.YELLOW)));
        
        lore.add(Component.empty());
        
        if (mail.getItemData() != null && !mail.getItemData().isEmpty()) {
            ItemStack mailItem = ItemSerializer.deserialize(mail.getItemData());
            if (mailItem != null && mailItem.getType() != Material.AIR) {
                String itemName = ItemSerializer.getItemDisplayName(mailItem);
                lore.add(Component.text("附件: ", NamedTextColor.GRAY)
                        .append(Component.text(itemName, NamedTextColor.GOLD)));
                
                if (mail.isClaimed()) {
                    lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                            .append(Component.text("已领取", NamedTextColor.GREEN)));
                } else {
                    lore.add(Component.text("状态: ", NamedTextColor.GRAY)
                            .append(Component.text("未领取", NamedTextColor.YELLOW)));
                }
            }
        } else {
            lore.add(Component.text("无附件", NamedTextColor.GRAY));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createItemDisplay() {
        if (mail.getItemData() == null || mail.getItemData().isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            var meta = item.getItemMeta();
            meta.displayName(Component.text("无附件", NamedTextColor.GRAY));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("这封邮件没有附件", NamedTextColor.GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }
        
        ItemStack item = ItemSerializer.deserialize(mail.getItemData());
        
        if (item == null || item.getType() == Material.AIR) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            var meta = emptyItem.getItemMeta();
            meta.displayName(Component.text("物品数据损坏", NamedTextColor.RED));
            emptyItem.setItemMeta(meta);
            return emptyItem;
        }
        
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        List<Component> originalLore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        List<Component> newLore = new ArrayList<>();
        
        newLore.add(Component.empty());
        newLore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        newLore.add(Component.empty());
        
        if (mail.isClaimed()) {
            newLore.add(Component.text("✓ 已领取", NamedTextColor.GREEN));
        } else {
            newLore.add(Component.text("○ 未领取", NamedTextColor.YELLOW));
            newLore.add(Component.text("点击下方按钮领取", NamedTextColor.AQUA));
        }
        
        newLore.addAll(originalLore);
        meta.lore(newLore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
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
        if (mail.getItemData() == null || mail.getItemData().isEmpty()) {
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            var meta = item.getItemMeta();
            meta.displayName(Component.text("无附件可领取", NamedTextColor.GRAY));
            item.setItemMeta(meta);
            return item;
        }
        
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
        if (!mail.isClaimed() && mail.getItemData() != null && !mail.getItemData().isEmpty()) {
            lore.add(Component.text("警告: 附件未领取！", NamedTextColor.RED));
            lore.add(Component.text("删除后将丢失附件", NamedTextColor.RED));
            lore.add(Component.empty());
        }
        lore.add(messageManager.getComponent("mail.shift-click-delete"));
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createReplyButton() {
        if (mail.getSenderUuid() == null) {
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            var meta = item.getItemMeta();
            meta.displayName(Component.text("无法回复", NamedTextColor.GRAY));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("发件人信息未知", NamedTextColor.GRAY));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }
        
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("回复", NamedTextColor.AQUA));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("发送邮件给 ", NamedTextColor.GRAY)
                .append(Component.text(mail.getSenderName(), NamedTextColor.GREEN)));
        lore.add(Component.empty());
        lore.add(Component.text("点击回复", NamedTextColor.YELLOW));
        
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
        if (loadingState.isLoading(player.getUniqueId())) {
            messageManager.send(player, "mail.processing");
            return;
        }
        
        if (slot == MAIL_DETAIL_BACK_SLOT) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, 1);
            return;
        }
        
        if (slot == CLAIM_SLOT) {
            if (!mail.isClaimed() && mail.getItemData() != null && !mail.getItemData().isEmpty()) {
                loadingState.setLoading(player.getUniqueId(), true);
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
        
        if (slot == REPLY_SLOT) {
            if (mail.getSenderUuid() != null) {
                player.closeInventory();
                String senderName = mail.getSenderName() != null ? mail.getSenderName() : "未知";
                plugin.getModuleManager().getMailModule().getMailManager()
                        .openSendMailGUI(player, mail.getSenderUuid(), senderName);
            }
        }
    }
}
