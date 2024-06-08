package nars.storage;

import nars.entity.Judgement;
import nars.inference.BudgetFunctions;
import nars.main.Parameters;

public class BeliefTable extends ArrayRankTable<Judgement> {

    public BeliefTable() {
        // * 🚩默认使用「超参数」中的长度
        super(Parameters.MAXIMUM_BELIEF_LENGTH);
    }

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
