package nars.entity;

import nars.inference.*;
import nars.io.Symbols;

/**
 * A triple of priority (current), durability (decay), and quality (long-term
 * average).
 */
public class BudgetValue implements Cloneable, Budget {

    /** The character that marks the two ends of a budget value */
    private static final char MARK = Symbols.BUDGET_VALUE_MARK;
    /** The character that separates the factors in a budget value */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
    /** The relative share of time resource to be allocated */
    protected final ShortFloat priority;
    /**
     * The percent of priority to be kept in a constant period; All priority
     * values “decay” over time, though at different rates. Each item is given a
     * “durability” factor in (0, 1) to specify the percentage of priority level
     * left after each reevaluation
     */
    protected final ShortFloat durability;
    /** The overall (context-independent) evaluation */
    protected final ShortFloat quality;

    @Override
    public ShortFloat __priority() {
        return this.priority;
    }

    @Override
    public ShortFloat __durability() {
        return this.durability;
    }

    @Override
    public ShortFloat __quality() {
        return this.quality;
    }

    /**
     * Default constructor
     */
    public BudgetValue() {
        priority = new ShortFloat(0.01f);
        durability = new ShortFloat(0.01f);
        quality = new ShortFloat(0.01f);
    }

    /**
     * Constructor with initialization
     *
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    public BudgetValue(float p, float d, float q) {
        priority = new ShortFloat(p);
        durability = new ShortFloat(d);
        quality = new ShortFloat(q);
    }

    /**
     * Cloning constructor
     *
     * @param v Budget value to be cloned
     */
    public BudgetValue(Budget v) {
        priority = new ShortFloat(v.getPriority());
        durability = new ShortFloat(v.getDurability());
        quality = new ShortFloat(v.getQuality());
    }

    /**
     * Cloning method
     */
    @Override
    public BudgetValue clone() {
        return new BudgetValue(this.getPriority(), this.getDurability(), this.getQuality());
    }

    /**
     * 🆕根据值而非引用判等
     */
    @Override
    public boolean equals(Object that) {
        return (that instanceof BudgetValue
                && this.getPriority() == ((BudgetValue) that).getPriority()
                && this.getDurability() == ((BudgetValue) that).getDurability()
                && this.getQuality() == ((BudgetValue) that).getQuality());
    }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return MARK + priority.toString() + SEPARATOR + durability.toString() + SEPARATOR + quality.toString() + MARK;
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    public String toStringBrief() {
        return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR
                + quality.toStringBrief() + MARK;
    }
}
