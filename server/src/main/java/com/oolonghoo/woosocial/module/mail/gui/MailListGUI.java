package com.oolonghoo.woosocial.module.mail.gui;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.BaseGUI;
import com.oolonghoo.woosocial.gui.FriendSelectGUI;
import com.oolonghoo.woosocial.gui.LoadingState;
import com.oolonghoo.woosocial.gui.SocialMainGUI;
import com.oolonghoo.woosocial.model.MailData;
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
    
    private final List<MailData> mailList;
    private final int currentMailPage;
    private final int totalMailPages;
    private final LoadingState loadingState;
    
    private static final int SEND_MAIL_SLOT = 8;
    private static final int CLAIM_ALL_SLOT = 47;
    private static final int DELETE_READ_SLOT = 51;
    
    public MailListGUI(WooSocial plugin, Player viewer, List<MailData> mails, int currentPage, int totalPages, LoadingState loadingState) {
        super(plugin, viewer, "mail_list");
        this.mailList = mails;
        this.currentMailPage = currentPage;
        this.totalMailPages = totalPages;
        this.loadingState = loadingState;
        
        setupItems();
    }
    
    @Override
    protected void setupPlaceholders() {
        placeholderParser.set("page", String.valueOf(currentMailPage));
        placeholderParser.set("total_pages", String.valueOf(totalMailPages));
    }
    
    private void setupItems() {
        fillBorder(54);
        
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(SEND_MAIL_SLOT, createSendMailButton());
        
        int startIndex = (currentMailPage - 1) * 36;
        int slot = 10;
        
        for (int i = startIndex; i < Math.min(startIndex + 36, mailList.size()); i++) {
            MailData mail = mailList.get(i);
            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) {
                slot += 2;
            }
            inventory.setItem(slot, createMailItem(mail));
            slot++;
        }
        
        if (currentMailPage > 1) {
            inventory.setItem(PREV_PAGE_SLOT, createPrevPageButton());
        }
        
        if (currentMailPage < totalMailPages) {
            inventory.setItem(NEXT_PAGE_SLOT, createNextPageButton());
        }
        
        inventory.setItem(CLAIM_ALL_SLOT, createClaimAllButton());
        inventory.setItem(DELETE_READ_SLOT, createDeleteReadButton());
    }
    
    private ItemStack createMailItem(MailData mail) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer sender = Bukkit.getOfflinePlayer(mail.getSenderUuid());
        meta.setOwningPlayer(sender);
        
        String senderName = mail.getSenderName() != null ? mail.getSenderName() : "未知";
        
        List<Component> lore = new ArrayList<>();
        
        if (!mail.isRead()) {
            meta.displayName(messageManager.getComponent("mail.unread-prefix")
                    .append(Component.text(senderName, NamedTextColor.GOLD)));
            lore.add(messageManager.getComponent("mail.status-unread"));
        } else {
            meta.displayName(Component.text(senderName, NamedTextColor.GRAY));
            lore.add(messageManager.getComponent("mail.status-read"));
        }
        
        lore.add(Component.empty());
        
        String sendTime = DATE_FORMAT.format(new Date(mail.getSendTime()));
        lore.add(messageManager.getComponent("mail.lore-send-time")
                .append(Component.text(sendTime, NamedTextColor.YELLOW)));
        
        if (mail.hasAttachments()) {
            lore.add(Component.text("附件: ", NamedTextColor.GRAY)
                    .append(Component.text("有", NamedTextColor.GREEN)));
        }
        
        if (mail.isClaimed()) {
            lore.add(messageManager.getComponent("mail.lore-status")
                    .append(messageManager.getComponent("mail.status-claimed")));
        } else if (mail.getItemData() != null && !mail.getItemData().isEmpty()) {
            lore.add(messageManager.getComponent("mail.lore-status")
                    .append(messageManager.getComponent("mail.status-unclaimed")));
        }
        
        lore.add(Component.empty());
        lore.add(messageManager.getComponent("mail.click-to-view"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createSendMailButton() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.send-mail"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("发送邮件给其他玩家", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击发送", NamedTextColor.AQUA));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createClaimAllButton() {
        int unclaimedCount = 0;
        for (MailData mail : mailList) {
            if (!mail.isClaimed() && mail.getItemData() != null && !mail.getItemData().isEmpty()) {
                unclaimedCount++;
            }
        }
        
        ItemStack item;
        if (unclaimedCount > 0) {
            item = new ItemStack(Material.HOPPER_MINECART);
        } else {
            item = new ItemStack(Material.MINECART);
        }
        
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.claim-all"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("领取所有未领取的附件", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("可领取: ", NamedTextColor.GRAY)
                .append(Component.text(unclaimedCount, NamedTextColor.YELLOW)));
        
        if (unclaimedCount > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("点击领取", NamedTextColor.AQUA));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createDeleteReadButton() {
        int readCount = 0;
        for (MailData mail : mailList) {
            if (mail.isRead() && (mail.isClaimed() || mail.getItemData() == null || mail.getItemData().isEmpty())) {
                readCount++;
            }
        }
        
        ItemStack item = new ItemStack(Material.BUCKET);
        var meta = item.getItemMeta();
        meta.displayName(messageManager.getComponent("mail.delete-read"));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("删除所有已读且已领取的邮件", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("可删除: ", NamedTextColor.GRAY)
                .append(Component.text(readCount, NamedTextColor.YELLOW)));
        
        if (readCount > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("Shift+点击删除", NamedTextColor.RED));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    protected ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("返回", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("返回社交菜单", NamedTextColor.GRAY));
        meta.lore(lore);
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
        if (loadingState.isLoading(player.getUniqueId())) {
            messageManager.send(player, "mail.loading");
            return;
        }
        
        if (slot == BACK_SLOT) {
            new SocialMainGUI(plugin, player).open(player);
            return;
        }
        
        if (slot == SEND_MAIL_SLOT) {
            new FriendSelectGUI(plugin, player, FriendSelectGUI.SelectMode.SEND_MAIL, (p, friend) -> {
                new SendMailGUI(plugin, p, friend.getFriendUuid(), friend.getFriendName(), loadingState).open(p);
            }).open(player);
            return;
        }
        
        if (slot == CLAIM_ALL_SLOT) {
            handleClaimAll(player);
            return;
        }
        
        if (slot == DELETE_READ_SLOT) {
            if (clickType == 1) {
                handleDeleteRead(player);
            }
            return;
        }
        
        if (slot == PREV_PAGE_SLOT && currentMailPage > 1) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, currentMailPage - 1);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT && currentMailPage < totalMailPages) {
            plugin.getModuleManager().getMailModule().getMailManager().openMailListGUI(player, currentMailPage + 1);
            return;
        }
        
        int mailIndex = getMailIndexFromSlot(slot);
        if (mailIndex >= 0 && mailIndex < mailList.size()) {
            MailData mail = mailList.get(mailIndex);
            plugin.getModuleManager().getMailModule().getMailManager().openMailDetailGUI(player, mail.getId());
        }
    }
    
    private int getMailIndexFromSlot(int slot) {
        int row = (slot - 9) / 9;
        int col = (slot - 9) % 9;
        
        if (row < 0 || row >= 4 || col < 1 || col > 7) {
            return -1;
        }
        
        int itemsInRow = col - 1;
        int itemsBeforeRow = row * 7;
        
        return (currentMailPage - 1) * 36 + itemsBeforeRow + itemsInRow;
    }
    
    private void handleClaimAll(Player player) {
        loadingState.setLoading(player.getUniqueId(), true);
        player.closeInventory();
        
        plugin.getModuleManager().getMailModule().getMailManager()
                .claimAllMails(player)
                .thenAccept(count -> {
                    loadingState.clearLoading(player.getUniqueId());
                    
                    if (count > 0) {
                        messageManager.send(player, "mail.claim-all-success", "count", String.valueOf(count));
                    } else {
                        messageManager.send(player, "mail.no-unclaimed");
                    }
                });
    }
    
    private void handleDeleteRead(Player player) {
        loadingState.setLoading(player.getUniqueId(), true);
        player.closeInventory();
        
        plugin.getModuleManager().getMailModule().getMailManager()
                .deleteReadMails(player)
                .thenAccept(count -> {
                    loadingState.clearLoading(player.getUniqueId());
                    
                    if (count > 0) {
                        messageManager.send(player, "mail.delete-read-success", "count", String.valueOf(count));
                    } else {
                        messageManager.send(player, "mail.no-read-to-delete");
                    }
                });
    }
}
