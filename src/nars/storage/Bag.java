package nars.storage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.Item;
import nars.inference.BudgetFunctions;
import nars.io.ToStringBriefAndLong;
import nars.main.Parameters;

/**
 * A Bag is a storage with a constant capacity and maintains an internal
 * priority distribution for retrieval.
 * <p>
 * Each entity in a bag must extend Item, which has a BudgetValue and a key.
 * <p>
 * A name table is used to merge duplicate items that have the same key.
 * <p>
 * The bag space is divided by a threshold, above which is mainly time
 * management, and below, space management. Differences: (1) level selection vs.
 * item selection, (2) decay rate
 *
 * @param <E> The type of the Item in the Bag
 */
public final class Bag<E extends Item> {
    // struct Bag<E: Item>
    /**
     * priority levels
     */
    private static final int TOTAL_LEVEL = Parameters.BAG_LEVEL;
    /**
     * firing threshold
     */
    private static final int THRESHOLD = Parameters.BAG_THRESHOLD;
    /**
     * relative threshold, only calculate once
     */
    private static final float RELATIVE_THRESHOLD = (float) THRESHOLD / (float) TOTAL_LEVEL;
    /**
     * hashtable load factor
     */
    private static final float LOAD_FACTOR = Parameters.LOAD_FACTOR;
    /**
     * shared DISTRIBUTOR that produce the probability distribution
     */
    private static final Distributor DISTRIBUTOR = new Distributor(TOTAL_LEVEL);
    /**
     * mapping from key to item
     */
    private final HashMap<String, E> nameTable;
    /**
     * array of lists of items, for items on different level
     */
    private final ArrayList<LinkedList<E>> itemTable;
    /**
     * defined in different bags
     */
    private final int capacity;
    /**
     * current sum of occupied level
     */
    private int mass;
    /**
     * index to get next level, kept in individual objects
     */
    private int levelIndex;
    /**
     * current take out level
     */
    private int currentLevel;
    /**
     * maximum number of items to be taken out at current level
     */
    private int currentCounter;

    /**
     * The item decay rate, which differs in difference subclass,
     * and can be changed in run time by the user, so not a constant.
     *
     * @return The number of times for a decay factor to be fully applied
     */
    private final AtomicInteger forgetRate;

    private BagObserver<E> observer = new BagObserver.NullObserver<>();

    /**
     * The display level; initialized at lowest
     */
    private int showLevel = THRESHOLD;

    // impl<E> Bag<E>

    /**
     * constructor, called from subclasses
     *
     * @param forgetRate the priority decay rate
     * @param capacity   the capacity of the bag
     */
    public Bag(AtomicInteger forgetRate, int capacity) {
        // * 📜默认就是「旧的并入新的」
        this(forgetRate, capacity, (oldValue, newValue) -> MergeOrder.OldToNew);
    }

    /**
     * 除了以上参数外，还附加「并入顺序决定函数」
     *
     * @param forgetRate  the priority decay rate
     * @param capacity    the capacity of the bag
     * @param mergeOrderF the merge order function
     */
    public Bag(AtomicInteger forgetRate, int capacity, MergeOrderF<E> mergeOrderF) {
        this.capacity = capacity;
        this.forgetRate = forgetRate;
        this.itemTable = new ArrayList<>(TOTAL_LEVEL);
        this.nameTable = new HashMap<>((int) (capacity / LOAD_FACTOR), LOAD_FACTOR);
        this.mergeOrderF = mergeOrderF;
        init();
    }

    public void init() {
        this.itemTable.clear();
        for (int i = 0; i < TOTAL_LEVEL; i++) {
            this.itemTable.add(new LinkedList<E>());
        }
        this.nameTable.clear();
        this.currentLevel = TOTAL_LEVEL - 1;
        this.levelIndex = this.capacity % TOTAL_LEVEL; // so that different bags start at different point
        this.mass = 0;
        this.currentCounter = 0;
    }

    /**
     * To get the capacity of the concrete subclass
     *
     * @return Bag capacity, in number of Items allowed
     */
    public final int capacity() {
        return capacity;
    }

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    public final int size() {
        return this.nameTable.size();
    }

    /**
     * 🆕获取是否为空
     *
     * @return
     */
    public final boolean isEmpty() {
        return this.nameTable.isEmpty();
    }

    /**
     * Get the average priority of Items
     * * 📝【2024-06-09 23:56:10】目前仅在「概念」的「平均词项链优先级」中用到
     *
     * @return The average priority of Items in the bag
     */
    public final float averagePriority() {
        // * 🚩没内容⇒默认0.01
        if (size() == 0)
            return 0.01f;
        // * 🚩有内容⇒所有「占据的层级」除以「层级总数」（所有优先级的平均值）
        final float f = (float) mass / (size() * TOTAL_LEVEL);
        // * 🚩和1取最小值
        return Math.min(f, 1.0f);
    }

    /**
     * Check if an item is in the bag
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    public final boolean contains(E it) {
        return nameTable.containsValue(it);
    }

    /**
     * 🆕获取一个Key是否在一个袋内
     *
     * @param key The key of the Item
     * @return Whether the Item with the given key is in the Bag
     */
    public final boolean has(String key) {
        return nameTable.containsKey(key);
    }

    /**
     * Get an Item by key
     *
     * @param key The key of the Item
     * @return The Item with the given key
     */
    public final E get(String key) {
        return nameTable.get(key);
    }

    /**
     * 检查要放入的Item是否合法
     * * 🚩非空检查：`null`⇒NPE报错
     * * 🚩已有检查：已有⇒重复置入⇒报错
     *
     * @param in
     */
    private final void validateIn(E in) {
        if (in == null)
            throw new AssertionError("尝试放进null");
        if (this.contains(in))
            throw new AssertionError("尝试放进重复的项 " + in);
    }

    /**
     * 检查要放出的Item是否合法
     * // * 🚩非空检查：`null`⇒NPE报错 | `null`在此属正常情况
     * * 🚩已有检查：已有⇒虚空放出⇒报错
     *
     * @param in
     */
    private final E validateOut(E out) {
        // if (out == null)
        // throw new AssertionError("尝试放出null");
        if (!this.has(out.getKey()))
            throw new AssertionError("尝试放出没有的项" + out);
        return out;
    }

    public static enum MergeOrder {
        /**
         * 从「将移出的Item」合并到「新进入的Item」
         * * 📌修改「新进入的Item」
         */
        OldToNew,
        /**
         * 从「新进入的Item」合并到「将移出的Item」
         * * 📌修改「将移出的Item」
         */
        NewToOld
    }

    /** 🆕决定「预算合并顺序」的函数指针类型 */
    @FunctionalInterface
    public static interface MergeOrderF<E> {
        MergeOrder call(E oldValue, E newValue);
    }

    /** 🆕决定「预算合并顺序」的函数指针 */
    private final MergeOrderF<E> mergeOrderF;

    /**
     * Add a new Item into the Bag
     *
     * @param newItem The new Item
     * @return Whether the new Item is added into the Bag
     */
    public final boolean putIn(E newItem) {
        // * 🚩预先检查
        validateIn(newItem);
        // * 🚩新物品的键
        final String newKey = newItem.getKey();
        // * 🚩置入名称表
        final E oldItem = nameTable.put(newKey, newItem);
        // * 🚩检查并处理「同名」情况
        if (oldItem != null) { // merge duplications
            // * 🚩重复的键
            this.outOfBase(oldItem);
            // * 🚩按照计算出的「合并顺序」合并预算值
            // newItem.mergeBudget(oldItem);
            switch (this.mergeOrderF.call(oldItem, newItem)) {
                case OldToNew:
                    newItem.mergeBudget(oldItem);
                    break;
                case NewToOld: // * 📝原先统一是相当于这个
                    oldItem.mergeBudget(newItem);
                    break;
            }
        }
        // * 🚩置入层级表
        final E overflowItem = this.intoBase(newItem); // put the (new or merged) item into itemTable
        // * 🚩检查并处理「溢出」情况
        if (overflowItem != null) { // remove overflow
            // * 🚩对应移除「名称表」的元素
            final String overflowKey = overflowItem.getKey();
            nameTable.remove(overflowKey);
            // * 🚩移出的是新增元素⇒添加失败
            return (overflowItem != newItem);
        }
        // * 🚩添加成功
        return true;
    }

    /**
     * Put an item back into the itemTable
     * <p>
     * The only place where the forgetting rate is applied
     *
     * @param oldItem The Item to put back
     * @return Whether the new Item is added into the Bag
     */
    public final boolean putBack(E oldItem) {
        // * 🚩检查
        this.validateIn(oldItem);
        // * 🚩在「放入」前进行一次「遗忘」
        this.forget(oldItem);
        // * 🚩继续「放入」
        return this.putIn(oldItem);
    }

    /** 以一定函数修改某个Item的优先级 */
    public final void forget(E oldItem) {
        final float newPriority = BudgetFunctions.forget(oldItem, this.forgetRate.get(), RELATIVE_THRESHOLD);
        oldItem.setPriority(newPriority);
    }

    /**
     * Choose an Item according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected Item (or null)
     */
    public final E takeOut() {
        // * 🚩空袋⇒返回空
        if (this.isEmpty()) // empty bag
            return null;
        // * 🚩切换随机索引 | 当前层级为空/多次取到某层的值 ⇒ 更新
        if (this.emptyLevel(currentLevel) || currentCounter == 0) { // done with the current level
            // * 🚩伪随机循环到第一个「非空层级」
            this.currentLevel = DISTRIBUTOR.pick(this.levelIndex);
            this.levelIndex = DISTRIBUTOR.next(this.levelIndex);
            while (this.emptyLevel(this.currentLevel)) { // look for a non-empty level
                this.currentLevel = DISTRIBUTOR.pick(levelIndex);
                this.levelIndex = DISTRIBUTOR.next(levelIndex);
            }
            // * 🚩最后更新计数器
            this.currentCounter = (this.currentLevel < THRESHOLD)
                    // for dormant levels, take one item
                    ? 1
                    // for active levels, take all current items
                    : this.itemTable.get(this.currentLevel).size();
        }
        // * 🚩拿取物品
        final E selected = this.takeOutFirst(currentLevel); // take out the first item in the level
        this.validateOut(selected);
        this.nameTable.remove(selected.getKey());
        // * 🚩更新计数器、显示呈现
        this.currentCounter--;
        this.refresh();
        // * 🚩返回被选中者
        return selected;
    }

    /**
     * Pick an item by key, then remove it from the bag
     *
     * @param key The given key
     * @return The Item with the key (or null)
     */
    public final E pickOut(String key) {
        // * 🚩从「名称表」中拿出一个物品
        if (this.nameTable.containsKey(key)) {
            // * 🚩真的拿出物品
            final E picked = this.nameTable.remove(key);
            this.outOfBase(picked);
            return picked;
        }
        return null;
    }

    /**
     * Check whether a level is empty
     *
     * @param n The level index
     * @return Whether that level is empty
     */
    private final boolean emptyLevel(int n) {
        return (itemTable.get(n).isEmpty());
    }

    /**
     * Decide the put-in level according to priority
     *
     * @param item The Item to put in
     * @return The put-in level
     */
    private final int getLevel(E item) {
        // * 🚩优先级×总层级
        float fl = item.getPriority() * TOTAL_LEVEL;
        // * 🚩舍入 | 💫其中的机制稍许令人困惑
        int level = (int) Math.ceil(fl) - 1;
        return Math.max(level, 0); // cannot be -1
    }

    /**
     * Insert an item into the itemTable, and return the overflow
     *
     * @param newItem The Item to put in
     * @return The overflow Item (may be null)
     */
    private final E intoBase(E newItem) {
        E oldItem = null;
        // * 🚩获取新物品要被放到的层级
        final int inLevel = this.getLevel(newItem);
        // * 🚩容量已满，准备放出物品
        if (this.size() > capacity) { // the bag is full
            // * 🚩查看第一个非空层级
            int outLevel = 0;
            while (this.emptyLevel(outLevel)) {
                outLevel++;
            }
            // * 🚩非空层级高于新物品⇒弹出新物品 | 始终弹出层级最低的物品（放进去就最低⇒拒绝置入）
            if (outLevel > inLevel) // ignore the item and exit
                return newItem;
            // * 🚩从对应层级拿出旧的物品（先进先出）
            else // remove an old item in the lowest non-empty level
                oldItem = this.takeOutFirst(outLevel);
        }
        // * 🚩加入层级
        this.itemTable.get(inLevel).add(newItem); // FIFO
        // * 🚩更新状态变量
        this.mass += inLevel + 1; // increase total mass
        // * 🚩刷新显示呈现
        this.refresh(); // refresh the window
        // * 🚩返回「溢出的旧物品」
        return oldItem; // TODo return null is a bad smell
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @param level The current level
     * @return The first Item
     */
    private final E takeOutFirst(int level) {
        // * 🚩尝试在指定层级中取出一个元素 | 📝同义重构：获取第一个=移除第一个（后的返回值）
        final E selected = this.itemTable.get(level).removeFirst();
        // * 🚩更新自身的「质量」值
        this.mass -= level + 1;
        // * 🚩刷新显示呈现
        this.refresh();
        return selected;
    }

    /**
     * Remove an item from itemTable, then adjust mass
     *
     * @param oldItem The Item to be removed
     */
    private final void outOfBase(E oldItem) {
        // * 🚩从「层级表」中移除对应物品
        final int level = this.getLevel(oldItem);
        this.itemTable.get(level).remove(oldItem);
        // * 🚩更新自身的「质量」值
        this.mass -= level + 1;
        // * 🚩刷新显示呈现
        this.refresh();
    }

    /**
     * To start displaying the Bag in a BagWindow; {@link nars.gui.BagWindow}
     * implements interface {@link BagObserver};
     *
     * @param bagObserver BagObserver to set
     * @param title       The title of the window
     */
    public final void addBagObserver(BagObserver<E> bagObserver, String title) {
        this.observer = bagObserver;
        this.observer.post(toString());
        this.observer.setTitle(title);
        this.observer.setBag(this);
    }

    /**
     * Resume display
     */
    public final void play() {
        this.observer.post(toString());
    }

    /**
     * Stop display
     */
    public final void stop() {
        this.observer.stop();
    }

    /**
     * Refresh display
     */
    public final void refresh() {
        if (this.observer != null && !(this.observer instanceof BagObserver.NullObserver)) {
            this.observer.refresh(toString());
        }
    }

    /**
     * Collect Bag content into a String for display
     *
     * @return A String representation of the content
     */
    @Override
    public final String toString() {
        StringBuffer buf = new StringBuffer(" ");
        for (int i = TOTAL_LEVEL; i >= showLevel; i--) {
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- Level ").append(i).append(":\n ");
                for (int j = 0; j < itemTable.get(i - 1).size(); j++) {
                    final Item item = itemTable.get(i - 1).get(j);
                    if (item instanceof ToStringBriefAndLong)
                        buf = buf.append(((ToStringBriefAndLong) item).toStringBrief()).append("\n ");
                }
            }
        }
        return buf.toString();
    }

    /**
     * TODo refactor : paste from preceding method
     */
    public final String toStringLong() {
        StringBuffer buf = new StringBuffer(" BAG " + getClass().getSimpleName());
        buf.append(" ").append(showSizes());
        for (int i = TOTAL_LEVEL; i >= showLevel; i--) {
            if (!emptyLevel(i - 1)) {
                buf = buf.append("\n --- LEVEL ").append(i).append(":\n ");
                for (int j = 0; j < itemTable.get(i - 1).size(); j++) {
                    final Item item = itemTable.get(i - 1).get(j);
                    if (item instanceof ToStringBriefAndLong)
                        buf = buf.append(((ToStringBriefAndLong) item).toStringLong()).append("\n ");
                }
            }
        }
        buf.append(">>>> end of Bag").append(getClass().getSimpleName());
        return buf.toString();
    }

    /**
     * show item Table Sizes
     */
    final String showSizes() {
        StringBuilder buf = new StringBuilder(" ");
        int levels = 0;
        for (LinkedList<E> items : itemTable) {
            if ((items != null) && !items.isEmpty()) {
                levels++;
                buf.append(items.size()).append(" ");
            }
        }
        return "Levels: " + Integer.toString(levels) + ", sizes: " + buf;
    }

    /**
     * set Show Level
     */
    public final void setShowLevel(int showLevel) {
        this.showLevel = showLevel;
    }
}
