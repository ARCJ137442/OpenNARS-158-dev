package nars.entity;

import static nars.io.Symbols.QUESTION_MARK;

import nars.inference.Truth;
import nars.language.Term;

/**
 * 🆕疑问句 初代实现
 */
public class QuestionV1 implements Question {

    // struct QuestionV1

    /** 🆕内部存储的「语句」实现 */
    private final SentenceV1 inner;

    // impl QuestionV1

    public QuestionV1(Term content, Stamp stamp, boolean revisable) {
        this.inner = new SentenceV1(content, QUESTION_MARK, stamp, revisable);
    }

    /** 复制构造函数 */
    protected QuestionV1(QuestionV1 j) {
        this.inner = (SentenceV1) j.inner.sentenceClone();
    }

    // impl Question for QuestionV1

    // impl Hash for SentenceV1

    /** 🆕对「问题」直接取0 */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + 0;
        hash = 67 * hash + this.inner.hashCode();
        return hash;
    }

    // impl Clone for QuestionV1
    @Override
    public Question clone() {
        return new QuestionV1(this);
    }

    // impl Eq for QuestionV1

    @Override
    public boolean equals(Object that) {
        if (that == null)
            return false;
        if (that instanceof QuestionV1)
            return
            // * 🚩内部相等
            ((QuestionV1) that).inner.equals(this.inner);
        // * 🚩否则
        return false;
    }

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
        return this.inner.getPunctuation();
    }

    // impl Evidential for QuestionV1

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
        return new QuestionV1(content, newStamp, revisable);
    }
}
