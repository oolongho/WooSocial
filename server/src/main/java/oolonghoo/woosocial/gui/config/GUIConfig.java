package com.oolonghoo.woosocial.gui.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIConfig {
    
    private final String name;
    private String title;
    private List<String> layout;
    private Map<String, IconConfig> icons;
    private int size;
    
    public GUIConfig(String name) {
        this.name = name;
        this.icons = new HashMap<>();
        this.size = 54;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<String> getLayout() {
        return layout;
    }
    
    public void setLayout(List<String> layout) {
        this.layout = layout;
        this.size = layout.size() * 9;
    }
    
    public Map<String, IconConfig> getIcons() {
        return icons;
    }
    
    public void setIcons(Map<String, IconConfig> icons) {
        this.icons = icons;
    }
    
    public IconConfig getIcon(String key) {
        return icons.get(key);
    }
    
    public int getSize() {
        return size;
    }
    
    public int getSlotFromLayout(char character, int instanceIndex) {
        if (layout == null) return -1;
        
        int count = 0;
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == character) {
                    if (count == instanceIndex) {
                        return row * 9 + col;
                    }
                    count++;
                }
            }
        }
        return -1;
    }
    
    public List<Integer> getSlotsForChar(char character) {
        java.util.ArrayList<Integer> slots = new java.util.ArrayList<>();
        if (layout == null) return slots;
        
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == character) {
                    slots.add(row * 9 + col);
                }
            }
        }
        return slots;
    }
    
    public char getCharAtSlot(int slot) {
        if (layout == null) return ' ';
        
        int row = slot / 9;
        int col = slot % 9;
        
        if (row < 0 || row >= layout.size()) return ' ';
        
        String line = layout.get(row);
        if (col < 0 || col >= line.length()) return ' ';
        
        return line.charAt(col);
    }
}
