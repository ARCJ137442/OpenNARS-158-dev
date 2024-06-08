package nars.storage;

import java.util.ArrayList;
import java.util.Iterator;

public class ArrayBuffer<T> implements Buffer<T> {

    private final int capacity;
    private final ArrayList<T> inner;

    public ArrayBuffer(int capacity) {
        this.capacity = capacity;
        this.inner = new ArrayList<T>(capacity);
    }

    @Override
    public Iterator<T> iterator() {
        return this.inner.iterator();
    }

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
