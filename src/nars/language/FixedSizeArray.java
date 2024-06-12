package nars.language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 🆕定长高不可变性数组
 * * 📌创建后长度固定（无法增删元素）
 * * 📌大部分情况下无法替换元素（可变性留给子类定义，默认不可修改）
 */
public abstract class FixedSizeArray<T> extends ArrayList<T> {

    /** 🆕错误类型：尝试修改不可变对象 */
    public static final class SizeViolationError extends Error {

        /** 🆕错误消息 */
        public static final String MESSAGE = "This ArrayList has fixed length and it's size cant' be changed.";

        public SizeViolationError() {
            super(MESSAGE);
        }
    }

    public FixedSizeArray(ArrayList<T> list) {
        super(list);
    }

    @Override
    public final boolean add(T t) {
        throw new SizeViolationError();
    }

    @Override
    public final void add(int index, T element) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean addAll(Collection<? extends T> c) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean addAll(int index, Collection<? extends T> c) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean remove(Object o) {
        throw new SizeViolationError();
    }

    @Override
    public final T remove(int index) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean removeAll(Collection<?> c) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean removeIf(Predicate<? super T> filter) {
        throw new SizeViolationError();
    }

    @Override
    final protected void removeRange(int fromIndex, int toIndex) {
        throw new SizeViolationError();
    }

    @Override
    public final boolean retainAll(Collection<?> c) {
        throw new SizeViolationError();
    }

    @Override
    public final T set(int index, T element) {
        throw new SizeViolationError();
    }

    @Override
    public final void replaceAll(UnaryOperator<T> operator) {
        throw new SizeViolationError();
    }

    /** 转换回原始数组 */
    public final ArrayList<T> toArrayList() {
        return new ArrayList<>(this);
    }

    /**
     * 特殊：留给子类的可设置窗口
     */
    protected final T __set(int index, T element) {
        return super.set(index, element);
    }
}