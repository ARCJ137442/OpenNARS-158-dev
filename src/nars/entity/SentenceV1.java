package nars.entity;

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

    // struct SentenceV1

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
     * Partial record of the derivation path
     */
    private final Stamp stamp;
    /**
     * Whether the sentence can be revised
     */
    private final boolean revisable;

    // impl SentenceV1

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
    protected SentenceV1(Term content, char punctuation, Stamp stamp, boolean revisable) {
        this.content = content;
        this.content.renameVariables();
        this.punctuation = punctuation;
        this.stamp = stamp;
        this.revisable = revisable;
        if (stamp == null) {
            throw new NullPointerException("Stamp is null!");
        }
    }

    // impl Evidential for SentenceV1

    @Override
    public long[] __evidentialBase() {
        return this.stamp.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.stamp.__creationTime();
    }

    // impl Sentence for SentenceV1

    @Override
    public boolean __revisable() {
        return revisable;
    }

    @Override
    public Term getContent() {
        return content;
    }

    @Override
    public char getPunctuation() {
        return punctuation;
    }

    @Override
    public Sentence sentenceClone() {
        // * ❓这是否意味着：只在「有真值」时，才需要`revisable`——「问题」不用修订
        // * 🚩【2024-05-19 12:44:12】实际上直接合并即可——「问题」并不会用到`revisable`
        return new SentenceV1(
                content.clone(),
                punctuation,
                // truth == null ? null : truth.clone(),
                stamp.clone(),
                revisable);
    }

    // impl ToStringBriefAndLong for SentenceV1

    @Override
    public String toString() {
        return this.sentenceToString();
    }

    @Override
    public String toStringBrief() {
        return this.sentenceToStringBrief();
    }

    @Override
    public String toStringLong() {
        return toString();
    }

    // impl Eq for SentenceV1

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
            return content.equals(t.getContent()) && punctuation == t.getPunctuation()
            // // * 🚩真值判等：需要考虑「没有真值」的情况
            // && (this.hasTruth() == t.hasTruth() && this.truthEquals(t))
                    && this.evidentialEqual(t);
        }
        return false;
    }

    // impl Hash for SentenceV1

    /**
     * To produce the hashcode of a sentence
     * * 🚩【2024-06-08 14:22:55】此处破坏性更新：不再需要「真值」
     *
     * @return A hashcode
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.content != null ? this.content.hashCode() : 0);
        hash = 67 * hash + this.punctuation;
        // hash = 67 * hash + (this.truth != null ? this.truth.hashCode() : 0);
        hash = 67 * hash + (this.stamp != null ? this.stamp.hashCode() : 0);
        return hash;
    }

    @Override
    public String toKey() {
        // ! 不予实现
        throw new UnsupportedOperationException("Unimplemented method 'toKey'");
    }

    @Override
    public String sentenceToString() {
        // ! 不予实现
        throw new UnsupportedOperationException("Unimplemented method 'sentenceToString'");
    }
}
