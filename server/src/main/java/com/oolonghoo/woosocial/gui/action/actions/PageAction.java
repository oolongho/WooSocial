package com.oolonghoo.woosocial.gui.action.actions;

import com.oolonghoo.woosocial.gui.action.ActionContext;
import com.oolonghoo.woosocial.gui.action.GUIAction;
import org.bukkit.entity.Player;

import java.util.List;

public class PageAction implements GUIAction {
    
    @Override
    public List<String> getNames() {
        return List.of("next_page", "prev_page", "page");
    }
    
    @Override
    public void execute(Player player, ActionContext context, String params) {
        int currentPage = context.getCurrentPage();
        int totalPages = context.getTotalPages();
        
        int newPage = switch (params.toLowerCase()) {
            case "next", "next_page" -> Math.min(currentPage + 1, totalPages);
            case "prev", "prev_page" -> Math.max(currentPage - 1, 1);
            default -> {
                try {
                    yield Integer.parseInt(params);
                } catch (NumberFormatException e) {
                    yield currentPage;
                }
            }
        };
        
        context.set("new_page", newPage);
    }
}
