package nars.entity;

import nars.inference.Budget;
import nars.io.ToStringBriefAndLong;

/**
 * An item is an object that can be put into a Bag,
 * to participate in the resource competition of the system.
 * <p>
 * It has a key and a budget. Cannot be cloned
 */
public interface Item extends Budget {

    /**
     * * 📝【2024-06-01 20:35:41】回答{@link Token}中的考虑——困难重重：
     * * 虽然设计上确实能在最后通过复合对象「BagItem<Task>」等解耦存储，
     * * 但实际上仍然解决不了「随时更新预算值」的耦合
     * * ⚠️亦即：不能完全将「推理机制」和「存储控制机制」在代码上隔离开来——二者
     * * ❌即便能通过「钩子调用」让各处「预算更新」得到call，这也有很大耦合度
     * * 💭乃至不如最开始的「抽象接口」好使
     */
    public static final class BagItem<T> implements Item {
        private final T value;
        private final String key;
        private final BudgetValue budget;

        public BagItem(T value, String key, BudgetValue budget) {
            this.value = value;
            this.key = key;
            this.budget = budget;
        }

        public T getValue() {
            return this.value;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Budget getBudget() {
            return this.budget;
        }

        @Override
        public ShortFloat __priority() {
            return budget.priority;
        }

        @Override
        public ShortFloat __durability() {
            return budget.durability;
        }

        @Override
        public ShortFloat __quality() {
            return budget.quality;
        }
    }

    /**
     * 🆕一个基于「复合」而非「继承」的{@link Item}默认实现
     * * 🚩使用`final`强制使用复合手段（而非继承）
     */
    public static final class Token implements Item, ToStringBriefAndLong {

        /**
         * The key of the Item, unique in a Bag
         * * ❓后续可以放入「袋」中，使用「Key → Item(T, Budget)」的结构将「预算值」完全合并入「袋」中
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

        /** 🆕用于「预算值」的字符串呈现 */
        public BudgetValue getBudgetValue() {
            return budget;
        }

        @Override
        public ShortFloat __priority() {
            return budget.priority;
        }

        @Override
        public ShortFloat __durability() {
            return budget.durability;
        }

        @Override
        public ShortFloat __quality() {
            return budget.quality;
        }

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
            // * 🚩动态检查可空性
            if (key == null)
                throw new IllegalArgumentException("key cannot be null");
            if (budget == null)
                throw new IllegalArgumentException("budget cannot be null");
            this.key = key;
            this.budget = new BudgetValue(budget); // clone, not assignment
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Budget getBudget() {
            return budget;
        }

        @Override
        public String toString() {
            return getBudget() + " " + getKey();
        }

        @Override
        public String toStringBrief() {
            return budget.toStringBrief() + " " + getKey();
        }

        /**
         * 🆕原版没有，此处仅重定向
         */
        @Override
        public String toStringLong() {
            return toString();
        }
    }

    /**
     * Get the current key
     *
     * @return Current key value
     */
    public String getKey();

    /**
     * Get BudgetValue
     *
     * @return Current BudgetValue
     */
    public Budget getBudget();

    public default ShortFloat __priority() {
        return getBudget().__priority();
    }

    public default ShortFloat __durability() {
        return getBudget().__durability();
    }

    public default ShortFloat __quality() {
        return getBudget().__quality();
    }

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
}
