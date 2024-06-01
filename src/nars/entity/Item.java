package nars.entity;

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 * <p>
 * It has a key and a budget. Cannot be cloned
 */
public interface Item {

    /**
     * 🆕一个基于「复合」而非「继承」的{@link Item}默认实现
     * * 🚩使用`final`强制使用复合手段（而非继承）
     */
    public static final class Token implements Item {

        /**
         * The key of the Item, unique in a Bag
         * * ❓TODO: 后续可以放入「袋」中，使用「Key → Item(T, Budget)」的结构将「预算值」完全合并入「袋」中
         *
         * * ️📝可空性：可空 | 仅「词项链模板」
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final String key;
        /**
         * The budget of the Item, consisting of 3 numbers
         * * 📝仅用于各预算值函数，以及在「袋」中的选取（优先级）
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：始终具所有权
         */
        private final BudgetValue budget;

        /**
         * Constructor with default budget
         *
         * @param key The key value
         */
        public Token(final String key) {
            this.key = key;
            this.budget = new BudgetValue();
        }

        /**
         * Constructor with initial budget
         *
         * @param key    The key value
         * @param budget The initial budget
         */
        public Token(final String key, final BudgetValue budget) {
            this.key = key;
            this.budget = new BudgetValue(budget); // clone, not assignment
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public BudgetValue getBudget() {
            return budget;
        }

        @Override
        public String toString() {
            return getBudget() + " " + getKey();
        }
    }

    /**
     * Get the current key
     *
     * @return Current key value
     */
    public String getKey(); /*
                             * {
                             * return key;
                             * }
                             */

    /**
     * Get BudgetValue
     *
     * @return Current BudgetValue
     */
    public BudgetValue getBudget(); /*
                                     * {
                                     * return budget;
                                     * }
                                     */

    /**
     * Get priority value
     *
     * @return Current priority value
     */
    default public float getPriority() {
        return getBudget().getPriority();
    }

    /**
     * Set priority value
     *
     * @param v Set a new priority value
     */
    default public void setPriority(float v) {
        getBudget().setPriority(v);
    }

    /**
     * Increase priority value
     *
     * @param v The amount of increase
     */
    default public void incPriority(float v) {
        getBudget().incPriority(v);
    }

    /**
     * Decrease priority value
     *
     * @param v The amount of decrease
     */
    default public void decPriority(float v) {
        getBudget().decPriority(v);
    }

    /**
     * Get durability value
     *
     * @return Current durability value
     */
    default public float getDurability() {
        return getBudget().getDurability();
    }

    /**
     * Set durability value
     *
     * @param v The new durability value
     */
    default public void setDurability(float v) {
        getBudget().setDurability(v);
    }

    /**
     * Increase durability value
     *
     * @param v The amount of increase
     */
    default public void incDurability(float v) {
        getBudget().incDurability(v);
    }

    /**
     * Decrease durability value
     *
     * @param v The amount of decrease
     */
    default public void decDurability(float v) {
        getBudget().decDurability(v);
    }

    /**
     * Get quality value
     *
     * @return The quality value
     */
    default public float getQuality() {
        return getBudget().getQuality();
    }

    /**
     * Set quality value
     *
     * @param v The new quality value
     */
    default public void setQuality(float v) {
        getBudget().setQuality(v);
    }

    /**
     * Merge with another Item with identical key
     *
     * @param that The Item to be merged
     */
    default public void merge(Item that) {
        getBudget().merge(that.getBudget());
    }

    /**
     * Return a String representation of the Item
     * 
     * ! ❌无法覆盖{@link Object#toString}：A default method cannot override a method from
     * java.lang.Object Java(67109915)
     *
     * @return The String representation of the full content
     */
    public String toString();

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    public default String toStringBrief() {
        return getBudget().toStringBrief() + " " + getKey();
    }

    public default String toStringLong() {
        return toString();
    }

}
