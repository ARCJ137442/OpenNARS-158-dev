package nars.storage;

import java.util.ArrayList;
import java.util.Iterator;

// TODO: 字段可空性、可变性、所有权标记

/**
 * 🆕使用「变长数组」实现的「排行表」类型
 * * 📌抽象：需要指定「排行」与「判断是否兼容」两个抽象方法（函数指针）
 */
public final class ArrayRankTable<T> implements RankTable<T> {

    // struct ArrayRankTable<T>

    /** 内部数组 */
    private final ArrayList<T> inner;
    /** 排行表容量 */
    private int capacity;

    @FunctionalInterface
    public interface RankFunction<T> {
        float call(T element);
    }

    @FunctionalInterface
    public interface CompatibleFunction<T> {
        boolean call(T newElement, T existedElement);
    }

    /** 🆕提取出的「计算排行」函数（函数指针） */
    private final RankFunction<T> rankF;

    @Override
    public float rank(T element) {
        return this.rankF.call(element);
    }

    /** 🆕提取出的「计算是否可兼容以添加」（函数指针） */
    private final CompatibleFunction<T> isCompatibleToAddF;

    @Override
    public boolean isCompatibleToAdd(T newElement, T existedElement) {
        return this.isCompatibleToAddF.call(newElement, existedElement);
    }

    // impl<T> ArrayRankTable<T>

    /** 构造函数 */
    public ArrayRankTable(
            final int capacity,
            final RankFunction<T> rank,
            final CompatibleFunction<T> isCompatibleToAdd) {
        this.capacity = capacity;
        this.inner = new ArrayList<T>(capacity);
        this.rankF = rank;
        this.isCompatibleToAddF = isCompatibleToAdd;
    }

    // impl<T> Iterator<T> for ArrayRankTable<T>

    @Override
    public Iterator<T> iterator() {
        return this.inner.iterator();
    }

    // impl<T> RankTable<T> for ArrayRankTable<T>

    @Override
    public int size() {
        return this.inner.size();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public T __get(int index) {
        return this.inner.get(index);
    }

    @Override
    public void __insert(int index, T newElement) {
        this.inner.add(index, newElement);
    }

    @Override
    public void __insert(T newElement) {
        this.inner.add(newElement);
    }

    @Override
    public T __pop() {
        return this.inner.remove(this.inner.size() - 1);
    }
}
