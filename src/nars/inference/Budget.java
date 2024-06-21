package nars.inference;

import nars.entity.ShortFloat;
import nars.io.Symbols;
import nars.main.Parameters;

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
     * 🆕从其它预算值处拷贝值
     * * 🚩拷贝优先级、耐久度与质量
     *
     * @param &m-this
     * @param that    [&]
     */
    public default void copyBudgetFrom(final Budget that) {
        this.setPriority(that.getPriority());
        this.setDurability(that.getDurability());
        this.setQuality(that.getQuality());
    }

    /**
     * Merge one BudgetValue into another
     *
     * @param that The other Budget
     */
    public default void mergeBudget(Budget that) {
        final Budget newBudget = BudgetFunctions.merge(this, that);
        this.copyBudgetFrom(newBudget);
    }

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    public default float budgetSummary() {
        return UtilityFunctions.aveGeo(__priority().getValue(), __durability().getValue(), __quality().getValue());
    }

    /**
     * Whether the budget should get any processing at all
     * <p>
     * to be revised to depend on how busy the system is
     *
     * @return The decision on whether to process the Item
     */
    public default boolean budgetAboveThreshold() {
        // * 🚩现在自动用「系统参数」重定向
        return aboveThreshold(Parameters.BUDGET_THRESHOLD);
    }

    public default boolean aboveThreshold(float threshold) {
        // * 🚩就是普通的「大于阈值」
        return budgetSummary() >= threshold;
    }

    /** The character that marks the two ends of a budget value */
    static final char MARK = Symbols.BUDGET_VALUE_MARK;
    /** The character that separates the factors in a budget value */
    static final char SEPARATOR = Symbols.VALUE_SEPARATOR;

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    public default String budgetToString() {
        final StringBuilder b = new StringBuilder();
        b.append(MARK);
        b.append(__priority().toString());
        b.append(SEPARATOR);
        b.append(__durability().toString());
        b.append(SEPARATOR);
        b.append(__quality().toString());
        b.append(MARK);
        return b.toString();
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    public default String budgetToStringBrief() {
        final StringBuilder b = new StringBuilder();
        b.append(MARK);
        b.append(__priority().toStringBrief());
        b.append(SEPARATOR);
        b.append(__durability().toStringBrief());
        b.append(SEPARATOR);
        b.append(__quality().toStringBrief());
        b.append(MARK);
        return b.toString();
    }
}
