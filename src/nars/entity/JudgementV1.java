package nars.entity;

import static nars.io.Symbols.JUDGMENT_MARK;

import nars.inference.Truth;
import nars.language.Term;

/**
 * 🆕判断句 初代实现
 */
public class JudgementV1 extends SentenceV1 implements Judgement {

    // struct JudgementV1

    /**
     * 🆕内部存储的「语句」实现
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final SentenceInner inner;

    /**
     * The truth value of Judgment
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final TruthValue truth;

    // impl JudgementV1

    public JudgementV1(Term content, Truth truth, Stamp stamp, boolean revisable) {
        if (truth == null)
            throw new IllegalArgumentException("truth can't be null");
        this.inner = new SentenceInner(content, stamp, revisable);
        this.truth = TruthValue.from(truth);
    }

    /** 复制构造函数 */
    protected JudgementV1(JudgementV1 j) {
        this.inner = j.inner.clone();
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

    // ! 🚩【2024-06-08 23:30:24】经实验，只会生成key再加入散列表；因此无需参与散列化

    // // impl Hash for SentenceV1

    // /** 🆕对「判断」加入「真值」 */
    // @Override
    // public int hashCode() {
    // int hash = 5;
    // hash = 67 * hash + this.getPunctuation(); // ! 不要在SentenceV1中调用，可能会调用超类方法导致报错
    // hash = 67 * hash + this.truth.hashCode();
    // hash = 67 * hash + this.inner.hashCode();
    // return hash;
    // }

    // impl Clone for JudgementV1
    @Override
    public Judgement clone() {
        return new JudgementV1(this);
    }

    // ! 🚩【2024-06-08 23:30:24】经实验，用法上并不需要判等

    // // impl Eq for JudgementV1

    // public boolean eq(JudgementV1 that) {
    // if (that == null)
    // return false;
    // return
    // // * 🚩内部相等（词项，时间戳）
    // ((JudgementV1) that).inner.equals(this.inner)
    // // * 🚩标点相等
    // && ((JudgementV1) that).getPunctuation() == this.getPunctuation()
    // // * 🚩真值相等
    // && ((JudgementV1) that).truthEquals(this);
    // }

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
        return JUDGMENT_MARK;
    }

    @Override
    public Sentence sentenceCloneWithSamePunctuation(Term content,
            final Term newContent,
            final Truth newTruth,
            final Stamp newStamp,
            final boolean revisable) {
        return new JudgementV1(content, newTruth, newStamp, revisable);
    }

    // impl Evidential for JudgementV1

    @Override
    public long[] __evidentialBase() {
        return this.inner.stamp().__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.inner.stamp().__creationTime();
    }

    // impl Judgement for JudgementV1
}
