package nars.entity;

import static nars.io.Symbols.QUESTION_MARK;

import nars.inference.Truth;
import nars.language.Term;

/**
 * 🆕疑问句 初代实现
 */
public class QuestionV1 extends SentenceV1 implements Question {

    // struct QuestionV1

    /**
     * 🆕内部存储的「语句」实现
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final SentenceInner inner;

    // impl QuestionV1

    public QuestionV1(Term content, Stamp stamp, boolean revisable) {
        this.inner = new SentenceInner(content, stamp, revisable);
    }

    /** 复制构造函数 */
    protected QuestionV1(QuestionV1 j) {
        this.inner = j.inner.clone();
    }

    // impl Question for QuestionV1

    // ! 🚩【2024-06-08 23:30:24】经实验，只会生成key再加入散列表；因此无需参与散列化

    // // impl Hash for SentenceV1

    // /** 🆕对「问题」直接取0 */
    // @Override
    // public int hashCode() {
    // int hash = 5;
    // hash = 67 * hash + this.getPunctuation(); // ! 不要在SentenceV1中调用，可能会调用超类方法导致报错
    // hash = 67 * hash + 0;
    // hash = 67 * hash + this.inner.hashCode();
    // return hash;
    // }

    // impl Clone for QuestionV1

    @Override
    public Question clone() {
        return new QuestionV1(this);
    }

    // ! 🚩【2024-06-08 23:30:24】经实验，用法上并不需要判等

    // // impl Eq for QuestionV1

    // public boolean eq(QuestionV1 that) {
    // if (that == null)
    // return false;
    // return
    // // * 🚩内部相等
    // ((QuestionV1) that).inner.equals(this.inner)
    // // * 🚩标点相等
    // && ((QuestionV1) that).getPunctuation() == this.getPunctuation();
    // }

    // impl ToStringBriefAndLong for QuestionV1

    @Override
    public String toStringBrief() {
        return this.sentenceToStringBrief();
    }

    @Override
    public String toStringLong() {
        return this.sentenceToStringLong();
    }

    // impl Sentence for QuestionV1

    @Override
    public boolean __revisable() {
        return this.inner.__revisable();
    }

    @Override
    public Sentence sentenceClone() {
        return new QuestionV1(this);
    }

    @Override
    public Term getContent() {
        return this.inner.getContent();
    }

    @Override
    public char getPunctuation() {
        return QUESTION_MARK;
    }

    @Override
    public Sentence sentenceCloneWithSamePunctuation(Term content,
            final Term newContent,
            final Truth newTruth,
            final Stamp newStamp,
            final boolean revisable) {
        return new QuestionV1(content, newStamp, revisable);
    }

    // impl Evidential for QuestionV1

    @Override
    public long[] __evidentialBase() {
        return this.inner.stamp().__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.inner.stamp().__creationTime();
    }
}
