package nars.storage;

import java.util.*;

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
public abstract class Bag<E extends Item> {

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
    private static final float LOAD_FACTOR = Parameters.LOAD_FACTOR; //
    /**
     * shared DISTRIBUTOR that produce the probability distribution
     */
    private static final Distributor DISTRIBUTOR = new Distributor(TOTAL_LEVEL); //
    /**
     * mapping from key to item
     */
    private HashMap<String, E> nameTable;
    /**
     * array of lists of items, for items on different level
     */
    private ArrayList<LinkedList<E>> itemTable;
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
     * reference to memory
     */
    protected final Memory memory;

    private BagObserver<E> bagObserver = new NullBagObserver<>();

    /**
     * The display level; initialized at lowest
     */
    private int showLevel = THRESHOLD;

    /**
     * constructor, called from subclasses
     *
     * @param memory The reference to memory
     */
    protected Bag(Memory memory) {
        this.memory = memory;
        capacity = capacity();
        init();
    }

    public void init() {
        itemTable = new ArrayList<>(TOTAL_LEVEL);
        for (int i = 0; i < TOTAL_LEVEL; i++) {
            itemTable.add(new LinkedList<E>());
        }
        nameTable = new HashMap<>((int) (capacity / LOAD_FACTOR), LOAD_FACTOR);
        currentLevel = TOTAL_LEVEL - 1;
        levelIndex = capacity % TOTAL_LEVEL; // so that different bags start at different point
        mass = 0;
        currentCounter = 0;
    }

    /**
     * To get the capacity of the concrete subclass
     *
     * @return Bag capacity, in number of Items allowed
     */
    protected abstract int capacity();

    /**
     * Get the item decay rate, which differs in difference subclass, and can be
     * changed in run time by the user, so not a constant.
     *
     * @return The number of times for a decay factor to be fully applied
     */
    protected abstract int forgetRate();

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    public int size() {
        return nameTable.size();
    }

    /**
     * Get the average priority of Items
     *
     * @return The average priority of Items in the bag
     */
    public float averagePriority() {
        if (size() == 0) {
            return 0.01f;
        }
        float f = (float) mass / (size() * TOTAL_LEVEL);
        if (f > 1) {
            return 1.0f;
        }
        return f;
    }

    /**
     * Check if an item is in the bag
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    public boolean contains(E it) {
        return nameTable.containsValue(it);
    }

    /**
     * 🆕获取一个Key是否在一个袋内
     *
     * @param it
     * @return
     */
    public boolean has(String key) {
        return nameTable.containsKey(key);
    }

    /**
     * Get an Item by key
     *
     * @param key The key of the Item
     * @return The Item with the given key
     */
    public E get(String key) {
        return nameTable.get(key);
    }

    /**
     * 检查要放入的Item是否合法
     * * 🚩非空检查：`null`⇒NPE报错
     * * 🚩已有检查：已有⇒重复置入⇒报错
     *
     * @param in
     */
    public void validateIn(E in) {
        if (in == null)
            throw new NullPointerException("尝试放进null");
        if (this.contains(in))
            throw new IllegalArgumentException("尝试放进重复的项 " + in);
    }

    /**
     * 检查要放出的Item是否合法
     * // * 🚩非空检查：`null`⇒NPE报错 | `null`在此属正常情况
     * * 🚩已有检查：已有⇒虚空放出⇒报错
     *
     * @param in
     */
    public E validateOut(E out) {
        // if (out == null)
        // throw new NullPointerException("尝试放出null");
        if (!this.has(out.getKey()))
            throw new IllegalArgumentException("尝试放出没有的项" + out);
        return out;
    }

    /**
     * Add a new Item into the Bag
     *
     * @param newItem The new Item
     * @return Whether the new Item is added into the Bag
     */
    public boolean putIn(E newItem) {
        // TODO: 过程笔记注释
        validateIn(newItem);
        String newKey = newItem.getKey();
        E oldItem = nameTable.put(newKey, newItem);
        if (oldItem != null) { // merge duplications
            outOfBase(oldItem);
            newItem.mergeBudget(oldItem);
        }
        E overflowItem = intoBase(newItem); // put the (new or merged) item into itemTable
        if (overflowItem != null) { // remove overflow
            String overflowKey = overflowItem.getKey();
            nameTable.remove(overflowKey);
            return (overflowItem != newItem);
        } else {
            return true;
        }
    }

    /**
     * Put an item back into the itemTable
     * <p>
     * The only place where the forgetting rate is applied
     *
     * @param oldItem The Item to put back
     * @return Whether the new Item is added into the Bag
     */
    public boolean putBack(E oldItem) {
        // TODO: 过程笔记注释
        validateIn(oldItem);
        forget(oldItem);
        return putIn(oldItem);
    }

    public void forget(E oldItem) {
        BudgetFunctions.forget(oldItem, forgetRate(), RELATIVE_THRESHOLD);
    }

    /**
     * Choose an Item according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected Item
     */
    public E takeOut() {
        // TODO: 过程笔记注释
        if (nameTable.isEmpty()) { // empty bag
            return null;
        }
        if (emptyLevel(currentLevel) || (currentCounter == 0)) { // done with the current level
            currentLevel = DISTRIBUTOR.pick(levelIndex);
            levelIndex = DISTRIBUTOR.next(levelIndex);
            while (emptyLevel(currentLevel)) { // look for a non-empty level
                currentLevel = DISTRIBUTOR.pick(levelIndex);
                levelIndex = DISTRIBUTOR.next(levelIndex);
            }
            if (currentLevel < THRESHOLD) { // for dormant levels, take one item
                currentCounter = 1;
            } else { // for active levels, take all current items
                currentCounter = itemTable.get(currentLevel).size();
            }
        }
        E selected = takeOutFirst(currentLevel); // take out the first item in the level
        currentCounter--;
        validateOut(selected);
        nameTable.remove(selected.getKey());
        refresh();
        return selected;
    }

    /**
     * Pick an item by key, then remove it from the bag
     *
     * @param key The given key
     * @return The Item with the key
     */
    public E pickOut(String key) {
        // TODO: 过程笔记注释
        E picked = nameTable.get(key);
        if (picked != null) {
            outOfBase(picked);
            validateOut(picked);
            nameTable.remove(key);
        }
        return picked;
    }

    /**
     * Check whether a level is empty
     *
     * @param n The level index
     * @return Whether that level is empty
     */
    protected boolean emptyLevel(int n) {
        return (itemTable.get(n).isEmpty());
    }

    /**
     * Decide the put-in level according to priority
     *
     * @param item The Item to put in
     * @return The put-in level
     */
    private int getLevel(E item) {
        // TODO: 过程笔记注释
        float fl = item.getPriority() * TOTAL_LEVEL;
        int level = (int) Math.ceil(fl) - 1;
        return (level < 0) ? 0 : level; // cannot be -1
    }

    /**
     * Insert an item into the itemTable, and return the overflow
     *
     * @param newItem The Item to put in
     * @return The overflow Item
     */
    private E intoBase(E newItem) {
        // TODO: 过程笔记注释
        E oldItem = null;
        int inLevel = getLevel(newItem);
        if (size() > capacity) { // the bag is full
            int outLevel = 0;
            while (emptyLevel(outLevel)) {
                outLevel++;
            }
            if (outLevel > inLevel) { // ignore the item and exit
                return newItem;
            } else { // remove an old item in the lowest non-empty level
                oldItem = takeOutFirst(outLevel);
            }
        }
        itemTable.get(inLevel).add(newItem); // FIFO
        mass += (inLevel + 1); // increase total mass
        refresh(); // refresh the window
        return oldItem; // TODO return null is a bad smell
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @param level The current level
     * @return The first Item
     */
    private E takeOutFirst(int level) {
        // TODO: 过程笔记注释
        E selected = itemTable.get(level).getFirst();
        itemTable.get(level).removeFirst();
        mass -= (level + 1);
        refresh();
        return selected;
    }

    /**
     * Remove an item from itemTable, then adjust mass
     *
     * @param oldItem The Item to be removed
     */
    protected void outOfBase(E oldItem) {
        // TODO: 过程笔记注释
        int level = getLevel(oldItem);
        itemTable.get(level).remove(oldItem);
        mass -= (level + 1);
        refresh();
    }

    /**
     * To start displaying the Bag in a BagWindow; {@link nars.gui.BagWindow}
     * implements interface {@link BagObserver};
     *
     * @param bagObserver BagObserver to set
     * @param title       The title of the window
     */
    public void addBagObserver(BagObserver<E> bagObserver, String title) {
        this.bagObserver = bagObserver;
        bagObserver.post(toString());
        bagObserver.setTitle(title);
        bagObserver.setBag(this);
    }

    /**
     * Resume display
     */
    public void play() {
        bagObserver.post(toString());
    }

    /**
     * Stop display
     */
    public void stop() {
        bagObserver.stop();
    }

    /**
     * Refresh display
     */
    public void refresh() {
        if (bagObserver != null && !(bagObserver instanceof NullBagObserver)) {
            bagObserver.refresh(toString());
        }
    }

    /**
     * Collect Bag content into a String for display
     *
     * @return A String representation of the content
     */
    @Override
    public String toString() {
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
     * TODO refactor : paste from preceding method
     */
    public String toStringLong() {
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
    String showSizes() {
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
    public void setShowLevel(int showLevel) {
        this.showLevel = showLevel;
    }
}
