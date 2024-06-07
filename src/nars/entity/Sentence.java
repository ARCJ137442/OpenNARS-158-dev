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
public interface Sentence extends ToStringBriefAndLong, Truth {

    // 所有抽象字段
    Term __content();

    char __punctuation();

    TruthValue __truth(); // * 🚩【2024-06-07 15:17:47】仍然保留，要用于「生成密钥」

    Stamp __stamp();

    boolean __revisable();

    @Override
    default ShortFloat __frequency() {
        return this.__truth().__frequency();
    }

    @Override
    default ShortFloat __confidence() {
        return this.__truth().__confidence();
    }

    @Override
    default boolean __isAnalytic() {
        return this.__truth().__isAnalytic();
    }

    /**
     * 🆕复制其中的「语句」成分
     * * 🎯为了不让方法实现冲突而构建
     * * ⚠️可能没有
     */
    public Sentence cloneSentence();

    /**
     * Get the content of the sentence
     *
     * @return The content Term
     */
    public default Term getContent() {
        return __content();
    }

    /**
     * Get the punctuation of the sentence
     *
     * @return The character '.' or '?'
     */
    public default char getPunctuation() {
        return __punctuation();
    }

    /**
     * Clone the content of the sentence
     *
     * @return A clone of the content Term
     */
    public default Term cloneContent() {
        return __content().clone();
    }

    // ! 🚩【2024-06-07 15:40:21】现在将「语句」本身作为「真值」，或者是【能作为真值使用】的对象

    /**
     * Get the stamp of the sentence
     *
     * @return The stamp
     */
    public default Stamp getStamp() {
        return __stamp();
    }

    /**
     * Distinguish Judgment from Goal ("instanceof Judgment" doesn't work)
     *
     * @return Whether the object is a Judgment
     */
    public default boolean isJudgment() {
        return (__punctuation() == Symbols.JUDGMENT_MARK);
    }

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     *
     * @return Whether the object is a Question
     */
    public default boolean isQuestion() {
        return (__punctuation() == Symbols.QUESTION_MARK);
    }

    public default boolean containQueryVar() {
        return (__content().getName().indexOf(Symbols.VAR_QUERY) >= 0);
    }

    public default boolean getRevisable() {
        return __revisable();
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    public default String toKey() {
        final StringBuilder s = new StringBuilder();
        s.append(__content().toString());
        s.append(__punctuation()).append(" ");
        if (__truth() != null) {
            s.append(__truth().toStringBrief());
        }
        return s.toString();
    }
}
