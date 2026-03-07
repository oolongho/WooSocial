package com.oolonghoo.woosocial.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderParser {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{([^}]+)\\s*\\?\\s*([^:]+)\\s*:\\s*([^}]+)\\}");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    
    private final MiniMessage miniMessage;
    private final Map<String, Object> placeholders;
    private final Map<String, Boolean> conditions;
    
    public PlaceholderParser() {
        this.miniMessage = MiniMessage.miniMessage();
        this.placeholders = new HashMap<>();
        this.conditions = new HashMap<>();
    }
    
    public PlaceholderParser set(String key, Object value) {
        placeholders.put(key, value);
        return this;
    }
    
    public PlaceholderParser setCondition(String key, boolean value) {
        conditions.put(key, value);
        return this;
    }
    
    public String parse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String result = text;
        
        Matcher conditionalMatcher = CONDITIONAL_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (conditionalMatcher.find()) {
            String condition = conditionalMatcher.group(1).trim();
            String trueValue = conditionalMatcher.group(2).trim();
            String falseValue = conditionalMatcher.group(3).trim();
            
            boolean conditionResult = conditions.getOrDefault(condition, false);
            String replacement = conditionResult ? trueValue : falseValue;
            conditionalMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        conditionalMatcher.appendTail(sb);
        result = sb.toString();
        
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (placeholderMatcher.find()) {
            String key = placeholderMatcher.group(1);
            Object value = placeholders.get(key);
            String replacement = value != null ? String.valueOf(value) : "";
            placeholderMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        placeholderMatcher.appendTail(sb);
        result = sb.toString();
        
        return result;
    }
    
    public Component parseToComponent(String text) {
        String parsed = parse(text);
        parsed = convertLegacyColors(parsed);
        return miniMessage.deserialize(parsed);
    }
    
    private String convertLegacyColors(String text) {
        Matcher matcher = LEGACY_COLOR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            char colorCode = matcher.group(1).charAt(0);
            String miniMessageColor = legacyToMiniMessage(colorCode);
            matcher.appendReplacement(sb, miniMessageColor);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    private String legacyToMiniMessage(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
    
    public static String parseStatic(String text, String... replacements) {
        if (text == null || replacements.length == 0) {
            return text != null ? text : "";
        }
        
        String result = text;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            if (replacements[i] != null && replacements[i + 1] != null) {
                result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return result;
    }
    
    public void clear() {
        placeholders.clear();
        conditions.clear();
    }
}
