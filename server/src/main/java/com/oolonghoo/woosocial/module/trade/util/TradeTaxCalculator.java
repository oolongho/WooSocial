package com.oolonghoo.woosocial.module.trade.util;

/**
 * 交易税计算器
 * 计算交易手续费
 */
public class TradeTaxCalculator {
    
    private final boolean enabled;
    private final double rate;
    private final double maxTax;
    private final double exemptAmount;
    
    public TradeTaxCalculator(boolean enabled, double rate, double maxTax, double exemptAmount) {
        this.enabled = enabled;
        this.rate = Math.max(0, Math.min(1, rate)); // 限制在 0-1 之间
        this.maxTax = Math.max(0, maxTax);
        this.exemptAmount = Math.max(0, exemptAmount);
    }
    
    /**
     * 计算税额
     * @param amount 交易金额
     * @return 应缴税额
     */
    public double calculateTax(double amount) {
        if (!enabled || amount <= 0) {
            return 0;
        }
        
        // 免税额度
        if (amount <= exemptAmount) {
            return 0;
        }
        
        // 计算应税金额
        double taxableAmount = amount - exemptAmount;
        double tax = taxableAmount * rate;
        
        // 应用最高税额限制
        if (maxTax > 0 && tax > maxTax) {
            return maxTax;
        }
        
        return tax;
    }
    
    /**
     * 计算税后金额
     * @param amount 原始金额
     * @return 税后金额
     */
    public double getAfterTaxAmount(double amount) {
        return amount - calculateTax(amount);
    }
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取税率
     */
    public double getRate() {
        return rate;
    }
    
    /**
     * 获取最高税额
     */
    public double getMaxTax() {
        return maxTax;
    }
    
    /**
     * 获取免税额度
     */
    public double getExemptAmount() {
        return exemptAmount;
    }
}
