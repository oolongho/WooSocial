package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.WooSocial;
import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Registry;

import java.util.List;

public class SoundAction implements GUIAction {
    
    private final WooSocial plugin;
    
    public SoundAction(WooSocial plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> getNames() {
        return List.of("sound", "play_sound");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        if (params.isEmpty()) {
            params = "entity_mule_chest";
        }
        
        try {
            NamespacedKey key = NamespacedKey.minecraft(params.toLowerCase());
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } else {
                plugin.getLogger().warning("[GUI] 未知的音效: " + params);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[GUI] 未知的音效: " + params);
        }
    }
}
