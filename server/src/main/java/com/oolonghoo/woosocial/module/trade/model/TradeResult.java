package com.oolonghoo.woosocial.module.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 交易结果模型
 * 表示交易完成后的结果信息
 *
 * @author oolongho
 * @since 1.0.0
 */
public class TradeResult {

    /**
     * 交易是否成功
     */
    private final boolean success;

    /**
     * 结果消息
     */
    private final String message;

    /**
     * 玩家1收到的物品（来自玩家2）
     */
    private final List<ItemStack> items1;

    /**
     * 玩家2收到的物品（来自玩家1）
     */
    private final List<ItemStack> items2;

    /**
     * 创建交易结果
     *
     * @param success 交易是否成功
     * @param message 结果消息
     * @param items1  玩家1收到的物品
     * @param items2  玩家2收到的物品
     */
    public TradeResult(boolean success, String message, List<ItemStack> items1, List<ItemStack> items2) {
        this.success = success;
        this.message = message;
        this.items1 = items1 != null ? new ArrayList<>(items1) : new ArrayList<>();
        this.items2 = items2 != null ? new ArrayList<>(items2) : new ArrayList<>();
    }

    /**
     * 创建成功的交易结果
     *
     * @param message 成功消息
     * @param items1  玩家1收到的物品
     * @param items2  玩家2收到的物品
     * @return 交易结果
     */
    public static TradeResult success(String message, List<ItemStack> items1, List<ItemStack> items2) {
        return new TradeResult(true, message, items1, items2);
    }

    /**
     * 创建失败的交易结果
     *
     * @param message 失败消息
     * @return 交易结果
     */
    public static TradeResult failure(String message) {
        return new TradeResult(false, message, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<ItemStack> getItems1() {
        return Collections.unmodifiableList(items1);
    }

    public List<ItemStack> getItems2() {
        return Collections.unmodifiableList(items2);
    }

    /**
     * 检查玩家1是否有物品
     *
     * @return 是否有物品
     */
    public boolean hasItems1() {
        return !items1.isEmpty();
    }

    /**
     * 检查玩家2是否有物品
     *
     * @return 是否有物品
     */
    public boolean hasItems2() {
        return !items2.isEmpty();
    }

    @Override
    public String toString() {
        return "TradeResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", items1Count=" + items1.size() +
                ", items2Count=" + items2.size() +
                '}';
    }
}
