package com.oolonghoo.woosocial.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程安全的 LRU (Least Recently Used) 缓存实现
 * 继承 LinkedHashMap 并重写 removeEldestEntry 方法实现自动淘汰最久未使用的条目
 * 
 * <p>LRU 缓存特点：
 * <ul>
 *   <li>当缓存达到最大容量时，自动移除最久未访问的条目</li>
 *   <li>每次 get 操作会将访问的条目移到链表尾部（最近使用）</li>
 *   <li>每次 put 操作会将新条目添加到链表尾部</li>
 * </ul>
 * 
 * @param <K> 键的类型
 * @param <V> 值的类型
 * @author oolongho
 * @version 1.0.0
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 缓存的最大容量
     */
    private final int maxCapacity;
    
    /**
     * 缓存命中次数统计
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    
    /**
     * 缓存未命中次数统计
     */
    private final AtomicLong missCount = new AtomicLong(0);
    
    /**
     * 用于保证线程安全的锁对象
     */
    private final Object lock = new Object();
    
    /**
     * 创建一个指定最大容量的 LRU 缓存
     * 
     * @param maxCapacity 缓存的最大容量，必须大于 0
     * @throws IllegalArgumentException 如果 maxCapacity 小于等于 0
     */
    public LRUCache(int maxCapacity) {
        // 设置初始容量为 maxCapacity + 1，负载因子 0.75，accessOrder = true
        // accessOrder = true 表示按照访问顺序排序，这是实现 LRU 的关键
        super(maxCapacity + 1, 0.75f, true);
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity must be greater than 0");
        }
        this.maxCapacity = maxCapacity;
    }
    
    /**
     * 重写此方法以实现 LRU 淘汰策略
     * 当 Map 中的条目数量超过最大容量时，移除最久未使用的条目（链表头部）
     * 
     * @param eldest 最久未访问的条目
     * @return 如果返回 true，则移除 eldest 条目；返回 false 则不移除
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }
    
    /**
     * 获取缓存中指定键对应的值
     * 线程安全，并记录缓存命中/未命中统计
     * 
     * @param key 键
     * @return 值，如果不存在则返回 null
     */
    @Override
    public V get(Object key) {
        synchronized (lock) {
            V value = super.get(key);
            if (value != null) {
                hitCount.incrementAndGet();
                return value;
            }
            missCount.incrementAndGet();
            return null;
        }
    }
    
    /**
     * 将指定键值对放入缓存
     * 线程安全，如果键已存在则更新值并移到链表尾部
     * 
     * @param key 键
     * @param value 值
     * @return 之前关联的值，如果不存在则返回 null
     */
    @Override
    public V put(K key, V value) {
        synchronized (lock) {
            return super.put(key, value);
        }
    }
    
    /**
     * 如果指定键不存在，则将其放入缓存
     * 线程安全
     * 
     * @param key 键
     * @param value 值
     * @return 如果键不存在，返回 null；如果键已存在，返回已存在的值
     */
    @Override
    public V putIfAbsent(K key, V value) {
        synchronized (lock) {
            return super.putIfAbsent(key, value);
        }
    }
    
    /**
     * 从缓存中移除指定键及其对应的值
     * 线程安全
     * 
     * @param key 键
     * @return 被移除的值，如果不存在则返回 null
     */
    @Override
    public V remove(Object key) {
        synchronized (lock) {
            return super.remove(key);
        }
    }
    
    /**
     * 清空缓存
     * 线程安全，同时重置命中率统计
     */
    @Override
    public void clear() {
        synchronized (lock) {
            super.clear();
            hitCount.set(0);
            missCount.set(0);
        }
    }
    
    /**
     * 获取缓存中条目的数量
     * 线程安全
     * 
     * @return 缓存中的条目数量
     */
    @Override
    public int size() {
        synchronized (lock) {
            return super.size();
        }
    }
    
    /**
     * 检查缓存是否为空
     * 线程安全
     * 
     * @return 如果缓存为空返回 true，否则返回 false
     */
    @Override
    public boolean isEmpty() {
        synchronized (lock) {
            return super.isEmpty();
        }
    }
    
    /**
     * 检查缓存是否包含指定的键
     * 线程安全
     * 
     * @param key 键
     * @return 如果包含返回 true，否则返回 false
     */
    @Override
    public boolean containsKey(Object key) {
        synchronized (lock) {
            return super.containsKey(key);
        }
    }
    
    /**
     * 检查缓存是否包含指定的值
     * 线程安全
     * 
     * @param value 值
     * @return 如果包含返回 true，否则返回 false
     */
    @Override
    public boolean containsValue(Object value) {
        synchronized (lock) {
            return super.containsValue(value);
        }
    }
    
    /**
     * 获取缓存的最大容量
     * 
     * @return 最大容量
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    /**
     * 获取缓存命中次数
     * 
     * @return 命中次数
     */
    public long getHitCount() {
        return hitCount.get();
    }
    
    /**
     * 获取缓存未命中次数
     * 
     * @return 未命中次数
     */
    public long getMissCount() {
        return missCount.get();
    }
    
    /**
     * 获取缓存命中率
     * 
     * @return 命中率（0.0 到 1.0 之间），如果总访问次数为 0 则返回 0.0
     */
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / total;
    }
    
    /**
     * 重置命中率统计
     */
    public void resetStatistics() {
        hitCount.set(0);
        missCount.set(0);
    }
    
    /**
     * 获取缓存统计信息字符串
     * 
     * @return 统计信息字符串，包含大小、最大容量、命中率等
     */
    public String getStatistics() {
        return String.format("LRUCache[size=%d, maxCapacity=%d, hits=%d, misses=%d, hitRate=%.2f%%]",
                size(), maxCapacity, getHitCount(), getMissCount(), getHitRate() * 100);
    }
    
    @Override
    public String toString() {
        synchronized (lock) {
            return "LRUCache{" +
                    "maxCapacity=" + maxCapacity +
                    ", size=" + super.size() +
                    ", hitRate=" + String.format("%.2f%%", getHitRate() * 100) +
                    '}';
        }
    }
}
