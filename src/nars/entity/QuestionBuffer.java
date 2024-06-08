package nars.entity;

import java.util.ArrayList;
import java.util.Iterator;

import nars.main.Parameters;
import nars.storage.Buffer;

public class QuestionBuffer implements Buffer<Task> {

    private final int capacity;
    private final ArrayList<Task> inner;

    public QuestionBuffer() {
        // * 🚩最大问题长度
        this(Parameters.MAXIMUM_QUESTIONS_LENGTH);
    }

    public QuestionBuffer(int capacity) {
        this.capacity = capacity;
        this.inner = new ArrayList<Task>(capacity);
    }

    @Override
    public Iterator<Task> iterator() {
        return this.inner.iterator();
    }

    @Override
    public void __push(Task element) {
        this.inner.add(element);
    }

    @Override
    public Task __pop() {
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
