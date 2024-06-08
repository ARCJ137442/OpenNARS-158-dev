package nars.storage;

import java.util.ArrayList;
import java.util.Iterator;

import nars.entity.Judgement;
import nars.inference.BudgetFunctions;
import nars.main.Parameters;

public class BeliefTable implements RankTable<Judgement> {
    private final ArrayList<Judgement> inner;
    private int capacity;

    public BeliefTable() {
        // * 🚩默认使用「超参数」中的长度
        this(Parameters.MAXIMUM_BELIEF_LENGTH);
    }

    public BeliefTable(int capacity) {
        this.capacity = capacity;
        this.inner = new ArrayList<Judgement>(capacity);
    }

    // impl Iterator<Judgement> for BeliefTable

    @Override
    public Iterator<Judgement> iterator() {
        return this.inner.iterator();
    }

    // impl Buffer<Judgement> for BeliefTable

    @Override
    public int size() {
        return this.inner.size();
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public Judgement __get(int index) {
        return this.inner.get(index);
    }

    @Override
    public void __insert(int index, Judgement newElement) {
        this.inner.add(index, newElement);
    }

    @Override
    public void __insert(Judgement newElement) {
        this.inner.add(newElement);
    }

    @Override
    public Judgement __pop() {
        return this.inner.remove(this.inner.size() - 1);
    }

    /** 🆕提取出的「计算排行」函数 */
    @Override
    public float rank(Judgement belief) {
        return BudgetFunctions.rankBelief(belief);
    }

    @Override
    public boolean isCompatibleToAdd(Judgement newBelief, Judgement existedBelief) {
        // * 🚩断言内容、标点相等
        final boolean sameContentAndPunctuation = newBelief.getContent().equals(existedBelief.getContent())
                && newBelief.getPunctuation() == existedBelief.getPunctuation();
        if (!sameContentAndPunctuation)
            throw new AssertionError("判断等价的前提不成立：需要「内容」和「标点」相同");
        // * 🚩若内容完全等价⇒不予理睬（添加失败）
        if (Judgement.isBeliefEquivalent(newBelief, existedBelief)) {
            return false; // * 🚩标记为「不予添加」
        }
        // * 🚩否则就是兼容的
        return true;
    }
}
