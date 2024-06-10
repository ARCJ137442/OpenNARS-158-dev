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

    /**
     * Check whether two sentences can be used in revision
     * * 📝【2024-05-19 13:09:40】这里的`s1`、`s2`必定是「判断」类型
     * * 🚩只有两个「判断句」才有可能「被用于修正」
     *
     * @param newBelief  The first sentence
     * @param baseBelief The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisable(Judgement newBelief, Judgement baseBelief) {
        // * 🚩如果两个「判断句」的「内容」相同，并且新的「判断句」是可（参与）修正的，那么第二个「判断句」可以修正第一个「判断句」
        final boolean contentEq = newBelief.getContent().equals(baseBelief.getContent());
        final boolean baseRevisable = newBelief.getRevisable();
        return contentEq && baseRevisable;
    }

    /**
     * 🆕作为一个「新信念」与「基础信念」是否可参与修正
     *
     * @param baseBelief
     * @return
     */
    public default boolean revisable(Judgement baseBelief) {
        return Judgement.revisable(this, baseBelief);
    }

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
