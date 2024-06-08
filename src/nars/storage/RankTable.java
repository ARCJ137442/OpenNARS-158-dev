package nars.storage;

/**
 * 🆕排行表 抽象类型
 * * 🎯按照一个抽象的「排行函数」确定内部元素的位置
 * * 🎯用于「概念」的「信念表」
 */
public interface RankTable<E> extends Iterable<E> {
    /** 表内已有元素数量 */
    public int size();

    /** 表内最大元素数量（容量） */
    public int getCapacity();

    /**
     * 【核心】排行函数
     */
    public float rank(E element);

    /** 【内部】获取指定位置的元素 */
    public E __get(int index);

    /** 【内部】在某处插入元素 */
    public void __insert(int index, E newElement);

    /** 【内部】在某处插入元素（末尾） */
    public void __insert(E newElement);

    /** 🆕内部弹出（末尾元素） */
    public E __pop();

    public default int rankIndexToAdd(E element) {
        // * 🚩按排行计算排行应处在的位置
        final float rankNew = this.rank(element); // for the new isBelief
        int iToAdd = 0;
        for (; iToAdd < this.size(); iToAdd++) {
            // * 🚩获取待比较的排行
            final E existed = this.__get(iToAdd);
            final float rankExisted = this.rank(existed);
            // * 🚩总体顺序：从大到小（一旦比当前的大，那就在前边插入）
            if (rankNew >= rankExisted) {
                // * 🚩检查是否兼容
                if (isCompatibleToAdd(element, existed))
                    // * 🚩标记待插入的位置
                    return iToAdd;
                else
                    // * 🚩不兼容
                    return -1;
            }
        }
        // * 🚩一直到末尾
        return iToAdd;
    }

    public default boolean isCompatibleToAdd(E newElement, E existedElement) {
        return true;
    }

    /**
     * 加入元素
     * * 🚩成功加入⇒返回null/旧元素；加入失败⇒返回待加入的元素
     */
    public default E add(E newElement) {
        // * 🚩按排行计算元素应处在的位置
        final int iToAdd = this.rankIndexToAdd(newElement);
        final int tableSize = this.size();

        // * 🚩将新元素插入到「排行表」的索引i位置（可以是末尾）
        if (iToAdd < 0)
            // * 🚩添加失败
            return newElement;
        if (iToAdd == tableSize)
            // * 🚩插入到末尾
            if (tableSize == this.getCapacity())
                // * 🚩超出容量⇒添加失败
                return newElement;
            else
                this.__insert(newElement);
        else
            // * 🚩插入到中间
            this.__insert(iToAdd, newElement);

        // * 🚩排行表溢出 | 📌一次只增加一个
        final int newSize = this.size();
        if (newSize > this.getCapacity()) {
            // * 🚩缩减容量到限定的容量
            if (newSize - this.getCapacity() > 1)
                throw new AssertionError("【2024-06-08 10:07:31】断言：一次只会添加一个，并且容量不会突然变化");
            // * 🚩从末尾移除，返回移除后的元素
            return this.__pop();
        }

        // * 🚩最终添加成功，且没有排行被移除
        return null;
    }

}
