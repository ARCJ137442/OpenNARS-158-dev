package nars.entity;

import nars.inference.Truth;
import nars.language.Term;

/**
 * A Sentence is an abstract class, mainly containing a Term, a Truth, and
 * a Stamp.
 * <p>
 * It is used as the premises and conclusions of all inference rules.
 * <p>
 * * 📝【2024-05-22 16:45:08】其中所有字段基本为只读——静态数据，可自由复制
 * * 🚩【2024-06-01 15:51:19】现在作为相应接口的初代实现
 */
public class SentenceV1 implements Sentence {

    /**
     * The content of a Sentence is a Term
     */
    private final Term content;
    /**
     * The punctuation also indicates the type of the Sentence: Judgment,
     * Question, or Goal
     */
    private final char punctuation;
    /**
     * The truth value of Judgment
     * TODO: 后续或许考虑「类型分离」（「疑问句」没有真值，那就不要在这儿存储）
     */
    private final TruthValue truth;
    /**
     * Partial record of the derivation path
     */
    private final Stamp stamp;
    /**
     * Whether the sentence can be revised
     */
    private final boolean revisable;

    @Override
    public Term __content() {
        return content;
    }

    @Override
    public char __punctuation() {
        return punctuation;
    }

    @Override
    public TruthValue __truth() {
        return truth;
    }

    @Override
    public long[] __evidentialBase() {
        return this.stamp.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.stamp.__creationTime();
    }

    @Override
    public boolean __revisable() {
        return revisable;
    }

    /**
     * Create a Sentence with the given fields
     *
     * @param content     The Term that forms the content of the sentence
     * @param punctuation The punctuation indicating the type of the sentence
     * @param truth       The truth value of the sentence, null for question
     * @param stamp       The stamp of the sentence indicating its derivation time
     *                    and
     *                    base
     * @param revisable   Whether the sentence can be revised
     */
    public SentenceV1(Term content, char punctuation, Truth truth, Stamp stamp, boolean revisable) {
        this.content = content;
        this.content.renameVariables();
        this.punctuation = punctuation;
        this.truth = TruthValue.from(truth); // ! 📌【2024-06-07 15:37:46】真值可能为空
        this.stamp = stamp;
        this.revisable = revisable;
        if (stamp == null) {
            throw new NullPointerException("Stamp is null!");
        }
        if (this.isQuestion() && this.truth != null) {
            throw new NullPointerException("Questions has truth!");
        }
    }

    /**
     * To check whether two sentences are equal
     *
     * @param that The other sentence
     * @return Whether the two sentences have the same content
     */
    @Override
    public boolean equals(Object that) {
        if (that instanceof Sentence) {
            Sentence t = (Sentence) that;
            return content.equals(t.getContent()) && punctuation == t.getPunctuation() && truth.equals(t.__truth())
                    && this.evidenceEqual(t);
        }
        return false;
    }

    /**
     * To produce the hashcode of a sentence
     *
     * @return A hashcode
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.content != null ? this.content.hashCode() : 0);
        hash = 67 * hash + this.punctuation;
        hash = 67 * hash + (this.truth != null ? this.truth.hashCode() : 0);
        hash = 67 * hash + (this.stamp != null ? this.stamp.hashCode() : 0);
        return hash;
    }

    /**
     * Clone the Sentence
     *
     * @return The clone
     */
    public Sentence cloneSentence() {
        // * ❓这是否意味着：只在「有真值」时，才需要`revisable`——「问题」不用修订
        // * 🚩【2024-05-19 12:44:12】实际上直接合并即可——「问题」并不会用到`revisable`
        return new SentenceV1(
                content.clone(),
                punctuation,
                truth == null ? null : truth.clone(),
                stamp.clone(),
                revisable);
    }

    /**
     * Get a String representation of the sentence
     *
     * @return The String
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(content.toString());
        s.append(punctuation).append(" ");
        if (truth != null) {
            s.append(truth.toString());
        }
        s.append(this.stampToString());
        return s.toString();
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    @Override
    public String toStringBrief() {
        return toKey() + this.stampToString();
    }

    /**
     * 🆕原版没有，此处仅重定向
     */
    @Override
    public String toStringLong() {
        return toString();
    }
}
