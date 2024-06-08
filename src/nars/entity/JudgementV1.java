package nars.entity;

import static nars.io.Symbols.JUDGMENT_MARK;

import nars.inference.Truth;
import nars.language.Term;

/**
 * 🆕判断句 初代实现
 */
public class JudgementV1 implements Judgement {

    // struct JudgementV1

    /** 🆕内部存储的「语句」实现 */
    private final SentenceV1 inner;

    /**
     * The truth value of Judgment
     */
    private final TruthValue truth;

    // impl JudgementV1

    public JudgementV1(Term content, Truth truth, Stamp stamp, boolean revisable) {
        if (truth == null)
            throw new IllegalArgumentException("truth can't be null");
        this.inner = new SentenceV1(content, JUDGMENT_MARK, stamp, revisable);
        this.truth = TruthValue.from(truth);
    }

    /** 复制构造函数 */
    protected JudgementV1(JudgementV1 j) {
        this.inner = (SentenceV1) j.inner.sentenceClone();
        this.truth = j.truth.clone();
    }

    // impl Truth for JudgementV1

    @Override
    public ShortFloat __frequency() {
        return this.truth.__frequency();
    }

    @Override
    public ShortFloat __confidence() {
        return this.truth.__confidence();
    }

    @Override
    public boolean __isAnalytic() {
        return this.truth.__isAnalytic();
    }

    @Override
    public Truth truthClone() {
        return this.truth.clone();
    }

    // impl Hash for SentenceV1

    /** 🆕对「判断」加入「真值」 */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.truth.hashCode();
        hash = 67 * hash + this.inner.hashCode();
        return hash;
    }

    // impl Clone for JudgementV1
    @Override
    public Judgement clone() {
        return new JudgementV1(this);
    }

    // impl Eq for JudgementV1

    @Override
    public boolean equals(Object that) {
        if (that == null)
            return false;
        if (that instanceof JudgementV1)
            return
            // * 🚩内部相等
            ((JudgementV1) that).inner.equals(this.inner)
                    // * 🚩真值相等
                    && ((JudgementV1) that).truthEquals(this);
        // * 🚩否则
        return false;
    }

    // impl ToStringBriefAndLong for JudgementV1

    @Override
    public String toStringBrief() {
        return this.sentenceToStringBrief();
    }

    @Override
    public String toStringLong() {
        return this.sentenceToStringLong();
    }

    // impl Sentence for JudgementV1

    @Override
    public boolean __revisable() {
        return this.inner.__revisable();
    }

    @Override
    public Sentence sentenceClone() {
        return new JudgementV1(this);
    }

    @Override
    public Term getContent() {
        return this.inner.getContent();
    }

    @Override
    public char getPunctuation() {
        return this.inner.getPunctuation();
    }

    // impl Evidential for JudgementV1

    @Override
    public long[] __evidentialBase() {
        return this.inner.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.inner.__creationTime();
    }

    // impl Judgement for JudgementV1

    @Override
    public Sentence sentenceCloneWithSamePunctuation(Term content,
            final Term newContent,
            final Truth newTruth,
            final Stamp newStamp,
            final boolean revisable) {
        return new JudgementV1(content, newTruth, newStamp, revisable);
    }
}
