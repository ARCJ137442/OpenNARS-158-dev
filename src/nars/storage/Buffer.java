package nars.storage;

/**
 * 🆕新的 缓冲区 抽象类型
 * * 📌本质上是一个先进先出队列
 * * 🚩抽象的「添加元素」「弹出元素」
 */
public interface Buffer<E> extends Iterable<E> {

    /** 【内部】添加元素（到队尾） */
    void __push(E element);

    /** 【内部】弹出元素（队首） */
    E __pop();

    /** 【内部】获取已有元素数量 */
    public int size();

    /** 【内部】获取容量 */
    public int capacity();

    /**
     * 添加元素（到队尾）
     * * 🚩先添加元素到队尾，再弹出队首元素
     */
    public default E add(E newElement) {
        // * 🚩添加元素到队尾
        this.__push(newElement);
        // * 🚩缓冲区机制 | 📝断言：只在变动时处理
        if (size() > this.capacity()) {
            return this.__pop(); // FIFO
        }
        return null;
    }
}
