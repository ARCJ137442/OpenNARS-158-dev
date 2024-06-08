package nars.entity;

import nars.inference.Truth;
import nars.language.Term;
import static nars.io.Symbols.*;

/**
 * A Sentence is an abstract class, mainly containing a Term, a Truth, and
 * a Stamp.
 * <p>
 * It is used as the premises and conclusions of all inference rules.
 * <p>
 * * 📝【2024-05-22 16:45:08】其中所有字段基本为只读——静态数据，可自由复制
 * * 🚩【2024-06-01 15:51:19】现在作为相应接口的初代实现
 * * 🚩【2024-06-08 23:11:43】现在基于「复合」作为其它类型「共同字段」的持有者，挪出为{@link SentenceInner}
 * * 🚩【2024-06-08 23:20:55】目前此类仅提供全局静态方法，用于在全局角度（定义完「判断初代实现」「问题初代实现」后）做公共方法
 * * 📌【2024-06-08 23:21:11】亦可看作一个枚举类，下辖「判断」「问题」两个变种
 */
public abstract class SentenceV1 implements Sentence {

    // impl SentenceV1

    /**
     * 🆕通过词项、标点、真值、时间戳、可修正 构造 语句
     * * 🚩根据「标点」分发到各具体类型
     * * 💭应该挑出到「语句」之外，但暂且放置于此
     * * 🎯自「导出结论」和「输入构造」被使用
     *
     * @param newContent
     * @param punctuation
     * @param newTruth
     * @param newStamp
     * @param revisable
     * @return
     */
    public static Sentence newSentenceFromPunctuation(
            final Term newContent,
            final char punctuation,
            final Truth newTruth,
            final Stamp newStamp,
            final boolean revisable) {
        switch (punctuation) {
            case JUDGMENT_MARK:
                return new JudgementV1(newContent, newTruth, newStamp, revisable);
            case QUESTION_MARK:
                return new QuestionV1(newContent, newStamp, revisable);
            default:
                throw new IllegalArgumentException("unknown punctuation: " + punctuation);
        }
    }

    /** 🆕呈现用字符串显示方案 */
    @Override
    public String toString() {
        return this.sentenceToString();
    }

    // ! 🚩【2024-06-08 23:30:24】经实验，用法上并不需要判等

    @Override
    public boolean equals(Object that) {
        throw new Error("【2024-06-08 23:30:49】语句类型并不需要判等");
    }

    // ! 🚩【2024-06-08 23:30:24】经实验，只会生成key再加入散列表；因此无需参与散列化

    // // impl Hash for SentenceV1

    @Override
    public int hashCode() {
        throw new Error("【2024-06-08 23:36:49】语句类型并不需要散列化");
    }

    // // impl Eq for SentenceV1

    // public static boolean eq(Sentence a, Sentence b) {
    // if (a instanceof JudgementV1 && b instanceof JudgementV1)
    // // * 🚩都是「判断」⇒使用「判断」的判等方法
    // return ((JudgementV1) a).eq((JudgementV1) b);
    // else if (a instanceof QuestionV1 && b instanceof QuestionV1)
    // // * 🚩都是「问题」⇒使用「问题」的判等方法
    // return ((QuestionV1) a).eq((QuestionV1) b);
    // else
    // // * 🚩其它情况⇒不等
    // return false;
    // }
}
