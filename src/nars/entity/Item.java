package nars.entity;

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 * <p>
 * It has a key and a budget. Cannot be cloned
 */
public abstract class Item {

    /**
     * The key of the Item, unique in a Bag
     * * ❓TODO: 后续可以放入「袋」中，使用「Key → Item(T, Budget)」的结构将「预算值」完全合并入「袋」中
     *
     * * ️📝可空性：可空 | 仅「词项链模板」
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    protected final String key;
    /**
     * The budget of the Item, consisting of 3 numbers
     * * 📝仅用于各预算值函数，以及在「袋」中的选取（优先级）
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：始终具所有权
     */
    protected final BudgetValue budget;

    /**
     * Constructor with default budget
     *
     * @param key The key value
     */
    protected Item(String key) {
        this.key = key;
        this.budget = new BudgetValue();
    }

    /**
     * Constructor with initial budget
     *
     * @param key    The key value
     * @param budget The initial budget
     */
    protected Item(String key, BudgetValue budget) {
        this.key = key;
        this.budget = new BudgetValue(budget); // clone, not assignment
    }

    /**
     * Get the current key
     *
     * @return Current key value
     */
    public String getKey() {
        return key;
    }

    /**
     * Get BudgetValue
     *
     * @return Current BudgetValue
     */
    public BudgetValue getBudget() {
        return budget;
    }

    /**
     * Get priority value
     *
     * @return Current priority value
     */
    public float getPriority() {
        return budget.getPriority();
    }

    /**
     * Set priority value
     *
     * @param v Set a new priority value
     */
    public void setPriority(float v) {
        budget.setPriority(v);
    }

    /**
     * Increase priority value
     *
     * @param v The amount of increase
     */
    public void incPriority(float v) {
        budget.incPriority(v);
    }

    /**
     * Decrease priority value
     *
     * @param v The amount of decrease
     */
    public void decPriority(float v) {
        budget.decPriority(v);
    }

    /**
     * Get durability value
     *
     * @return Current durability value
     */
    public float getDurability() {
        return budget.getDurability();
    }

    /**
     * Set durability value
     *
     * @param v The new durability value
     */
    public void setDurability(float v) {
        budget.setDurability(v);
    }

    /**
     * Increase durability value
     *
     * @param v The amount of increase
     */
    public void incDurability(float v) {
        budget.incDurability(v);
    }

    /**
     * Decrease durability value
     *
     * @param v The amount of decrease
     */
    public void decDurability(float v) {
        budget.decDurability(v);
    }

    /**
     * Get quality value
     *
     * @return The quality value
     */
    public float getTotalQuality() {
        return budget.getQuality();
    }

    /**
     * Set quality value
     *
     * @param v The new quality value
     */
    public void setQuality(float v) {
        budget.setQuality(v);
    }

    /**
     * Merge with another Item with identical key
     *
     * @param that The Item to be merged
     */
    public void merge(Item that) {
        budget.merge(that.getBudget());
    }

    /**
     * Return a String representation of the Item
     *
     * @return The String representation of the full content
     */
    @Override
    public String toString() {
        return budget + " " + key;
    }

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    public String toStringBrief() {
        return budget.toStringBrief() + " " + key;
    }

    public String toStringLong() {
        return toString();
    }

}
