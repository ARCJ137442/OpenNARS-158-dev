package nars.entity;

import nars.inference.Truth;
import nars.io.Symbols;
import nars.io.ToStringBriefAndLong;
import nars.language.Term;

/**
 * A Sentence is an abstract class, mainly containing a Term, a TruthValue, and
 * a Stamp.
 * <p>
 * It is used as the premises and conclusions of all inference rules.
 * <p>
 * * 🚩作为一个接口，仅对其中的字段做抽象要求（实现者只要求在这些方法里返回字段或其它表达式）
 * * 🚩所有「字段类接口方法」均【以双下划线开头】并【不带public】
 */
public interface Sentence extends ToStringBriefAndLong, Truth, Evidential {

    // 所有抽象字段

    // * 🚩【2024-06-07 15:17:47】仍然保留，语句可能没有「真值」
    // TruthValue __truth();
    /** 🆕专用于判断「是否有真值」 */
    public boolean hasTruth();

    // * ✅【2024-06-08 11:36:18】成功删除：通过`stampToString`成功解耦

    boolean __revisable();

    /**
     * 🆕复制其中的「语句」成分
     * * 🎯为了不让方法实现冲突而构建（复制出一个「纯粹的」语句对象）
     * * ⚠️可能没有
     */
    public Sentence sentenceClone();

    /**
     * Get the content of the sentence
     *
     * @return The content Term
     */
    public Term getContent();

    /**
     * Get the punctuation of the sentence
     *
     * @return The character '.' or '?'
     */
    public char getPunctuation();

    /**
     * Clone the content of the sentence
     *
     * @return A clone of the content Term
     */
    public default Term cloneContent() {
        return this.getContent().clone();
    }

    // ! 🚩【2024-06-07 15:40:21】现在将「语句」本身作为「真值」，或者是【能作为真值使用】的对象

    // ! 🚩【2024-06-08 11:22:16】现在将「语句」本身作为「时间戳」，或者是【能作为时间戳使用】的对象

    /**
     * Distinguish Judgment from Goal ("instanceof Judgment" doesn't work)
     *
     * @return Whether the object is a Judgment
     */
    public default boolean isJudgment() {
        return (this.getPunctuation() == Symbols.JUDGMENT_MARK);
    }

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     *
     * @return Whether the object is a Question
     */
    public default boolean isQuestion() {
        return (this.getPunctuation() == Symbols.QUESTION_MARK);
    }

    public default boolean containQueryVar() {
        return (this.getContent().getName().indexOf(Symbols.VAR_QUERY) >= 0);
    }

    public default boolean getRevisable() {
        return __revisable();
    }

    /**
     * Check whether the judgment is equivalent to another one
     * <p>
     * The two may have different keys
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    public static boolean isBeliefEquivalent(Sentence self, Sentence that) {
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
    public default String toKey() {
        final StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        if (this.hasTruth()) {
            s.append(this.truthToStringBrief());
        }
        return s.toString();
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    public default String sentenceToString() {
        StringBuilder s = new StringBuilder();
        s.append(this.getContent().toString());
        s.append(this.getPunctuation()).append(" ");
        if (this.hasTruth()) {
            s.append(this.truthToString());
        }
        s.append(this.stampToString());
        return s.toString();
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    public default String sentenceToStringBrief() {
        return toKey() + this.stampToString();
    }
}
