package nars.language;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 🆕定长高不可变性数组
 * * 📌创建后长度固定（无法增删元素）
 * * 📌大部分情况下无法替换元素（可变性留给子类定义，默认不可修改）
 * * 📝不可使用「代理模式」或「内建数组」：
 * * * 将导致「长期稳定性」结果改变，且NAL测试6.20、6.21不通过
 */
public abstract class FixedSizeArray<T> extends ArrayList<T> {

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

    public final ArrayList<T> asList() {
        return this.toArrayList();
    }
}

/** 🆕错误类型：尝试修改不可变对象 */
final class SizeViolationError extends Error {

    /** 🆕错误消息 */
    public static final String MESSAGE = "This ArrayList has fixed length and it's size cant' be changed.";

    public SizeViolationError() {
        super(MESSAGE);
    }
}