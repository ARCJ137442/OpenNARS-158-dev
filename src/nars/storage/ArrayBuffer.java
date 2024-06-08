package nars.storage;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 🆕使用「变长数组」实现的「缓冲区」类型
 */
public class ArrayBuffer<T> implements Buffer<T> {

    // struct ArrayBuffer<T>

    /** 内部数组 */
    private final ArrayList<T> inner;
    /** 缓冲区容量 */
    private final int capacity;

    // impl<T> Buffer<T>

    /** 构造函数 */
    public ArrayBuffer(int capacity) {
        this.capacity = capacity;
        this.inner = new ArrayList<T>(capacity);
    }

    // impl<T> Iterator<T> for ArrayBuffer<T>

    @Override
    public Iterator<T> iterator() {
        return this.inner.iterator();
    }

    // impl<T> Buffer<T> for ArrayBuffer<T>

    @Override
    public void __push(T element) {
        this.inner.add(element);
    }

    @Override
    public T __pop() {
        // * 🚩【2024-06-08 17:23:10】此处从「概念」类中迁移而来，移除首个元素（最老的元素）
        return this.inner.remove(0);
    }

    @Override
    public int size() {
        return this.inner.size();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }
}
