package nars.inference;

import nars.entity.ShortFloat;
import nars.main_nogui.Parameters;

/**
 * 🆕标记【可预算的】对象
 * * ✅可以获取预算值
 * * ✅可以修改预算值（设置、增减…）
 * * 🚩【2024-06-07 13:42:23】目前为了方便，直接要求「三元组」作为内部字段
 */
public interface Budget {

    /** 🆕内部的「优先级」属性 */
    ShortFloat __priority();

    /** 🆕内部的「耐久度」属性 */
    ShortFloat __durability();

    /** 🆕内部的「质量」属性 */
    ShortFloat __quality();

    /**
     * Get priority value
     *
     * @return The current priority
     */
    public default float getPriority() {
        return __priority().getValue();
    }

    /**
     * Change priority value
     *
     * @param v The new priority
     */
    public default void setPriority(float v) {
        __priority().setValue(v);
    }

    /**
     * Increase priority value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    public default void incPriority(float v) {
        __priority().setValue(UtilityFunctions.or(__priority().getValue(), v));
    }

    /**
     * Decrease priority value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    public default void decPriority(float v) {
        __priority().setValue(UtilityFunctions.and(__priority().getValue(), v));
    }

    /**
     * Get durability value
     *
     * @return The current durability
     */
    public default float getDurability() {
        return __durability().getValue();
    }

    /**
     * Change durability value
     *
     * @param v The new durability
     */
    public default void setDurability(float v) {
        __durability().setValue(v);
    }

    /**
     * Increase durability value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    public default void incDurability(float v) {
        __durability().setValue(UtilityFunctions.or(__durability().getValue(), v));
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    public default void decDurability(float v) {
        __durability().setValue(UtilityFunctions.and(__durability().getValue(), v));
    }

    /**
     * Get quality value
     *
     * @return The current quality
     */
    public default float getQuality() {
        return __quality().getValue();
    }

    /**
     * Change quality value
     *
     * @param v The new quality
     */
    public default void setQuality(float v) {
        __quality().setValue(v);
    }

    /**
     * Increase quality value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    public default void incQuality(float v) {
        __quality().setValue(UtilityFunctions.or(__quality().getValue(), v));
    }

    /**
     * Decrease quality value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    public default void decQuality(float v) {
        __quality().setValue(UtilityFunctions.and(__quality().getValue(), v));
    }

    /**
     * Merge one BudgetValue into another
     *
     * @param that The other Budget
     */
    public default void merge(Budget that) {
        BudgetFunctions.merge(this, that);
    }

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    public default float summary() {
        return UtilityFunctions.aveGeo(__priority().getValue(), __durability().getValue(), __quality().getValue());
    }

    /**
     * Whether the budget should get any processing at all
     * <p>
     * to be revised to depend on how busy the system is
     *
     * @return The decision on whether to process the Item
     */
    public default boolean aboveThreshold() {
        // * 🚩现在自动用「系统参数」重定向
        return aboveThreshold(Parameters.BUDGET_THRESHOLD);
    }

    public default boolean aboveThreshold(float threshold) {
        // * 🚩就是普通的「大于阈值」
        return summary() >= threshold;
    }
}
