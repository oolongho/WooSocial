package com.oolonghoo.woosocial.manager;

import com.oolonghoo.woosocial.gui.BaseGUI;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI 管理器
 * 管理玩家的 GUI 实例，处理玩家离线时的 GUI 清理
 * 
 * @author oolongho
 * @since 1.0.0
 */
public class GUIManager {
    
    private final Map<UUID, BaseGUI> playerGUIs = new ConcurrentHashMap<>();
    
    /**
     * 注册玩家的 GUI
     * 
     * @param player 玩家
     * @param gui GUI 实例
     */
    public void registerGUI(Player player, BaseGUI gui) {
        if (player == null || gui == null) {
            return;
        }
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 获取玩家的 GUI
     * 
     * @param playerUuid 玩家 UUID
     * @return GUI 实例，如果不存在则返回 null
     */
    public BaseGUI getGUI(UUID playerUuid) {
        return playerGUIs.get(playerUuid);
    }
    
    /**
     * 移除玩家的 GUI
     * 
     * @param playerUuid 玩家 UUID
     * @return 被移除的 GUI 实例
     */
    public BaseGUI removeGUI(UUID playerUuid) {
        BaseGUI gui = playerGUIs.remove(playerUuid);
        if (gui != null) {
            cleanupGUI(gui);
        }
        return gui;
    }
    
    /**
     * 清理 GUI 资源
     * 
     * @param gui 要清理的 GUI
     */
    private void cleanupGUI(BaseGUI gui) {
        if (gui == null) {
            return;
        }
        
        // 清空 inventory
        if (gui.getInventory() != null) {
            gui.getInventory().clear();
        }
    }
    
    /**
     * 当玩家离线时清理 GUI
     * 
     * @param playerUuid 玩家 UUID
     */
    public void onPlayerQuit(UUID playerUuid) {
        removeGUI(playerUuid);
    }
    
    /**
     * 检查玩家是否有打开的 GUI
     * 
     * @param playerUuid 玩家 UUID
     * @return 是否有打开的 GUI
     */
    public boolean hasOpenGUI(UUID playerUuid) {
        return playerGUIs.containsKey(playerUuid);
    }
}
