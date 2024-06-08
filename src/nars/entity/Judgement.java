package nars.entity;

import nars.inference.Truth;

/**
 * 🆕判断句：「判断」类型
 */
public interface Judgement extends Sentence, Truth {
    // @Override
    // public default char getPunctuation() {
    // return JUDGMENT_MARK;
    // }

    @Override
    default boolean isJudgment() {
        return true;
    }

    @Override
    default Judgement asJudgement() {
        return this;
    }

    /**
     * Check whether the judgment is equivalent to another one
     * <p>
     * The two may have different keys
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    public static boolean isBeliefEquivalent(Judgement self, Judgement that) {
        return (
        // * 🚩真值相等
        self.truthEquals(that)
                // * 🚩时间戳相等（证据集相同）
                && self.evidentialEqual(that));
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    @Override
    public default String toKey() {
        final StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        s.append(this.truthToStringBrief());
        return s.toString();
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    @Override
    public default String sentenceToString() {
        StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        s.append(this.truthToString());
        s.append(this.stampToString());
        return s.toString();
    }
}
