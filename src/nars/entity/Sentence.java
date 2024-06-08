package nars.entity;

import nars.io.Symbols;
import nars.io.ToStringBriefAndLong;
import nars.language.Term;
import nars.language.Variable;

/**
 * A Sentence is an abstract class, mainly containing a Term, a TruthValue, and
 * a Stamp.
 * <p>
 * It is used as the premises and conclusions of all inference rules.
 * <p>
 * * 🚩作为一个接口，仅对其中的字段做抽象要求（实现者只要求在这些方法里返回字段或其它表达式）
 * * 🚩所有「字段类接口方法」均【以双下划线开头】并【不带public】
 */
public interface Sentence extends ToStringBriefAndLong, Evidential {

    // 所有抽象字段

    // * ✅【2024-06-08 13:23:11】成功删除：通过「真值格式化」「真值相等」成功解耦

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
        return this.getPunctuation() == Symbols.QUESTION_MARK;
    }

    public default boolean containQueryVar() {
        return Variable.containVarQ(this.getContent());
    }

    public default boolean getRevisable() {
        return __revisable();
    }

    /**
     * Get a String representation of the sentence for key of Task and TaskLink
     *
     * @return The String
     */
    public String toKey();

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    public String sentenceToString();

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    public default String sentenceToStringBrief() {
        return toKey() + this.stampToString();
    }

    /**
     * 🆕原版没有，此处仅重定向
     */
    public default String sentenceToStringLong() {
        return this.sentenceToString();
    }

    /**
     * 🆕用于「新任务建立」
     *
     * @return
     */
    public Sentence withSamePunctuation(Term content) {
        // TODO: 【2024-06-08 15:11:31】最后开发断点
    }
}
