package nars.entity;

import nars.inference.Truth;
import nars.inference.VariableInference;
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
        return false;
    }

    /**
     * 🆕作为一个判断句使用
     * * 🚩是判断句⇒判断句对象，否⇒报错
     */
    public default Judgement asJudgement() {
        throw new Error("不是判断句");
    }

    /**
     * Distinguish Question from Quest ("instanceof Question" doesn't work)
     *
     * @return Whether the object is a Question
     */
    public default boolean isQuestion() {
        return false;
    }

    /**
     * 🆕作为一个判断句使用
     * * 🚩是判断句⇒判断句对象，否⇒报错
     */
    public default Question asQuestion() {
        throw new Error("不是疑问句");
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
     * * 🚩使用最大并集（可设置为空）建立同标点新语句
     * * 📄判断⇒判断，问题⇒问题
     *
     * @return
     */
    public Sentence sentenceCloneWithSamePunctuation(Term content,
            final Term newContent,
            final Truth newTruth,
            final Stamp newStamp,
            final boolean revisable);

    /** 🆕一个用于「复用共有字段」的内部对象 */
    public static final class SentenceInner {

        // struct SentenceInner

        /**
         * The content of a Sentence is a Term
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final Term content;
        /**
         * Partial record of the derivation path
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final Stamp stamp;
        /**
         * Whether the sentence can be revised
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final boolean revisable;

        // impl SentenceInner

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
        protected SentenceInner(Term content, Stamp stamp, boolean revisable) {
            if (content == null)
                throw new AssertionError("【2024-06-15 12:56:36】不能用空词项构造语句！");
            if (stamp == null)
                throw new AssertionError("Stamp is null!");

            this.content = content;
            VariableInference.renameVariables(this.content);
            // ! ❌【2024-06-15 12:58:08】局部同义，全局不同义
            // * * 💭不是内容不一致，是因为其它地方可变性要修改此中词项（暂且）
            // * * 📄如：变量统一
            // final Term newC = VariableInference.renameVariables2New(content);
            // if (!this.content.equals(newC))
            // throw new Error();
            // this.content = VariableInference.renameVariables2New(content);
            this.stamp = stamp;
            this.revisable = revisable;
        }

        // impl Evidential for SentenceInner

        public Stamp stamp() {
            return this.stamp;
        }

        // impl Sentence for SentenceInner

        public boolean __revisable() {
            return revisable;
        }

        public Term getContent() {
            return content;
        }

        @Override
        public SentenceInner clone() {
            // * ❓这是否意味着：只在「有真值」时，才需要`revisable`——「问题」不用修订
            // * 🚩【2024-05-19 12:44:12】实际上直接合并即可——「问题」并不会用到`revisable`
            return new SentenceInner(
                    content.clone(),
                    // punctuation,
                    // truth == null ? null : truth.clone(),
                    stamp.clone(),
                    revisable);
        }

        // ! 🚩【2024-06-08 23:30:24】经实验，用法上并不需要判等

        // // impl Eq for SentenceInner

        // /**
        // * To check whether two sentences are equal
        // *
        // * @param that The other sentence
        // * @return Whether the two sentences have the same content
        // */
        // public boolean eq(SentenceInner that) {
        // if (that instanceof SentenceInner) {
        // final SentenceInner t = (SentenceInner) that;
        // return (
        // // * 🚩内容相等
        // content.equals(t.getContent())
        // // * 🚩时间戳相等（证据基内容相同）
        // && this.stamp().equals(t.stamp()));
        // }
        // return false;
        // }
        // }

        // ! 🚩【2024-06-08 23:30:24】经实验，只会生成key再加入散列表；因此无需参与散列化

        // // impl Hash for SentenceInner

        // /**
        // * To produce the hashcode of a sentence
        // * * 🚩【2024-06-08 14:22:55】此处破坏性更新：不再需要「真值」
        // *
        // * @return A hashcode
        // */
        // @Override
        // public int hashCode() {
        // int hash = 5;
        // hash = 67 * hash + (this.content != null ? this.content.hashCode() : 0);
        // // ! 不要在此调用「获取标点」，可能会调用超类方法导致报错
        // // hash = 67 * hash + (this.truth != null ? this.truth.hashCode() : 0);
        // hash = 67 * hash + (this.stamp != null ? this.stamp.hashCode() : 0);
        // return hash;
        // }
    }
}
