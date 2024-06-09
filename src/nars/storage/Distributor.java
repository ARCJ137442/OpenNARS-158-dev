package nars.storage;

import java.util.Arrays;

/**
 * A pseudo-random number generator, used in Bag.
 */
class Distributor {

    /** Shuffled sequence of index numbers */
    private final int[] order;
    /** Capacity of the array */
    private final int capacity;
    /** 🆕Cached "next" for order */
    private final int[] next;

    /**
     * For any number N < range, there is N+1 copies of it in the array, distributed
     * as evenly as possible
     *
     * @param range Range of valid numbers
     */
    public Distributor(int range) {
        // * 🚩计算并分布列
        this.order = createOrderFromRange(range);
        // * 🚩计算总容量（就是「分布列」的长度）
        this.capacity = order.length;
        // * 🚩预缓存「下一索引」数组
        this.next = createNextFromCapacity(this.capacity);
    }

    /**
     * 🆕根据「范围」计算「分布列」
     *
     * @param range
     * @param capacity
     * @return
     */
    private static final int[] createOrderFromRange(final int range) {
        // * 🚩计算总容量（就是「分布列」的长度）
        final int capacity = (range * (range + 1)) / 2;
        // * 🚩初始化分布列
        final int[] order = new int[capacity];
        // * 🚩全部初始化为-1（后续要用到）
        Arrays.fill(order, -1);
        // * 🚩正式开始计算
        // * 📌【2024-06-09 23:11:27】约简index的初值 capacity -> 0 | 实际上「偏移取余」后跟0无区别
        int index = 0;
        // * 🚩在range~1（两端含）遍历
        for (int rank = range; rank > 0; rank--) {
            // * 🚩索引跳转的基位移 | 📝用于下方线性同余
            final int baseOffset = capacity / rank;
            // * 🚩重复rank次数
            for (int time = 0; time < rank; time++) {
                // * 🚩跳转索引 | 📝线性同余
                index += baseOffset;
                index %= capacity;
                // * 🚩向后找到第一个未初始化的地方
                while (order[index] >= 0) {
                    index += 1;
                    index %= capacity;
                }
                // * 🚩设置值
                order[index] = rank - 1;
            }
        }
        // * 🚩返回
        return order;
    }

    /**
     * 🆕根据已有「容量」创建「下一索引」数组
     *
     * @param capacity 容量
     * @return 「下一索引」数组
     */
    private static final int[] createNextFromCapacity(final int capacity) {
        // * 🚩创建新数组
        final int[] next = new int[capacity];
        for (int index = 0; index < capacity; index++) {
            next[index] = index + 1;
        }
        next[capacity - 1] = 0;
        return next;
    }

    /**
     * Get the next number according to the given index
     *
     * @param index The current index
     * @return the random value
     */
    public int pick(int index) {
        return this.order[index];
    }

    /**
     * Advance the index
     *
     * @param index The current index
     * @return the next index
     */
    public int next(int index) {
        return this.next[index];
    }
}
