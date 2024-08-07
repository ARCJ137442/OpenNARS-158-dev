package nars.inference;

import nars.entity.*;

/**
 * All truth-value (and desire-value) functions used in inference rules
 * * 📝所有函数均【返回新真值对象】且【不修改所传入参数】
 * * 📌【2024-06-07 13:15:14】所有函数均从public变为「internal」当前包私有
 *
 * * 📝参数可变性标注语法：
 * * * [] ⇒ 传递所有权（深传递，整体只读）
 * * * [m] ⇒ 传递所有权，且可变（深传递，读写）
 * * * [&] ⇒ 传递不可变引用（浅传递，只读）
 * * * [&m] ⇒ 传递可变引用（浅传递，独占可写）
 * * * [R] ⇒ 传递不可变共享引用（共享只读）
 * * * [Rm] ⇒ 传递可变共享引用（共享读写）
 */
final class TruthFunctions extends UtilityFunctions {

    // * 函数式接口 * //

    /**
     * 🆕单真值函数
     */
    @FunctionalInterface
    public interface TruthFSingle {
        Truth call(Truth truth);
    }

    /**
     * 🆕双真值函数
     */
    @FunctionalInterface
    public interface TruthFDouble {
        Truth call(Truth truth1, Truth truth2);
    }

    /**
     * 🆕单真值+依赖度 函数
     */
    @FunctionalInterface
    public interface TruthFAnalytic {
        Truth call(Truth truth, float reliance);
    }

    /* ----- Single argument functions, called in MatchingRules ----- */
    /**
     * {<(*, A, B) --> R>} |- <A --> (/, R, _, B)>
     * 🆕恒等真值函数，用于转换推理
     * * 🎯维护「真值计算」的一致性：所有真值计算均通过真值函数
     *
     * @param v1 [&] Truth value of the premise
     * @return Truth value of the conclusion
     */
    static Truth identity(Truth v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        // * 📝频率=旧频率
        // * 📝信度=旧信度
        return new TruthValue(f1, c1);
    }

    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param v1 [&] Truth value of the premise
     * @return Truth value of the conclusion
     */
    static Truth conversion(Truth v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        // * 📝总频数=频率、信度之合取
        // * 📝频率=1（完全正面之猜测）
        // * 📝信度=总频数转换（保证弱推理）
        float w = and(f1, c1);
        float c = w2c(w);
        return new TruthValue(1, c);
    }

    /* ----- Single argument functions, called in StructuralRules ----- */
    /**
     * {A} |- (--A)
     *
     * @param v1 [&] Truth value of the premise
     * @return Truth value of the conclusion
     */
    static Truth negation(Truth v1) {
        // * 📝频率相反，信度相等
        float f = not(v1.getFrequency());
        float c = v1.getConfidence();
        return new TruthValue(f, c);
    }

    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     *
     * @param v1 [&] Truth value of the premise
     * @return Truth value of the conclusion
     */
    static Truth contraposition(Truth v1) {
        // * 📝频率为零，信度是弱
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = and(not(f1), c1);
        float c = w2c(w);
        return new TruthValue(0, c);
    }

    /* ----- double argument functions, called in MatchingRules ----- */
    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth revision(Truth v1, Truth v2) {
        // * 📝转换为「频数视角」，频数相加，并转换回（频率，信度）二元组
        // * ✅特别兼容「信度为1」的「无穷证据量」情况：覆盖 or 取平均
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final float c1 = v1.getConfidence();
        final float c2 = v2.getConfidence();
        final float f, c;
        final boolean isInf1 = c1 == 1.0;
        final boolean isInf2 = c2 == 1.0;
        // * 1 & 2
        if (isInf1 && isInf2) {
            c = aveAri(c1, c2);
            f = aveAri(f1, f2);
        }
        // * 1
        else if (isInf1) {
            c = c1;
            f = f1;
        }
        // * 2
        else if (isInf2) {
            c = c2;
            f = f2;
        }
        // * _
        else {
            final float w1 = c2w(c1);
            final float w2 = c2w(c2);
            final float w = w1 + w2;
            f = (w1 * f1 + w2 * f2) / w;
            c = w2c(w);
        }
        return new TruthValue(f, c);
    }

    /* ----- double argument functions, called in SyllogisticRules ----- */
    /**
     * {<S ==> M>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth deduction(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率二者合取，信度四者合取
        float f = and(f1, f2);
        float c = and(c1, c2, f);
        return new TruthValue(f, c);
    }

    /**
     * {M, <M ==> P>} |- P
     *
     * @param v1       [&] Truth value of the first premise
     * @param reliance [] Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    static Truth analyticDeduction(Truth v1, float reliance) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        // * 📌对于第二个「分析性前提」使用「依赖度」衡量
        // * 📝频率采用前者，信度合取以前者频率、依赖度，并标明这是「分析性」真值
        float c = and(f1, c1, reliance);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth analogy(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝类比：频率为二者合取，信度为双方信度、第二方频率三者合取
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth resemblance(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝类比：频率为二者合取，信度为「双方频率之析取」与「双方信度之合取」之合取
        float f = and(f1, f2);
        float c = and(c1, c2, or(f1, f2));
        return new TruthValue(f, c);
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth abduction(Truth v1, Truth v2) {
        // * 🚩分析性⇒无意义（信度清零）
        if (v1.getAnalytic() || v2.getAnalytic())
            return new TruthValue(0.5f, 0f);
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝总频数=第二方频率与双方信度之合取
        // * 📝频率=第一方频率
        // * 📝信度=总频数转换（总是弱推理）
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(f1, c);
    }

    /**
     * {M, <P ==> M>} |- P
     *
     * @param v1       [&] Truth value of the first premise
     * @param reliance [] Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    static Truth analyticAbduction(Truth v1, float reliance) {
        // * 🚩分析性⇒无意义（信度清零） | 只能「分析」一次
        if (v1.getAnalytic())
            return new TruthValue(0.5f, 0f);
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        // * 📝总频数=频率与「依赖度」之合取
        // * 📝频率=第一方频率
        // * 📝信度=总频数转换（总是弱推理）
        float w = and(c1, reliance);
        float c = w2c(w);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth induction(Truth v1, Truth v2) {
        // * 📝归纳是倒过来的归因
        return abduction(v2, v1);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth exemplification(Truth v1, Truth v2) {
        // * 🚩分析性⇒无意义（信度清零） | 只能「分析」一次
        if (v1.getAnalytic() || v2.getAnalytic())
            return new TruthValue(0.5f, 0f);
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝总频数=四方值综合
        // * 📝频率=1（无中生有）
        // * 📝信度=总频数转换（总是弱推理）
        float w = and(f1, f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth comparison(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝总频数=「双频之析取」与「双信之合取」之合取
        // * 📝频率=「双频之合取」/「双频之析取」（📌根据函数图像，可以取"(0,0) -> 0"为可去间断点）
        // * 📝信度=总频数转换（总是弱推理）
        float f0 = or(f1, f2);
        float f = (f0 == 0) ? 0 : (and(f1, f2) / f0);
        float w = and(f0, c1, c2);
        float c = w2c(w);
        return new TruthValue(f, c);
    }

    /* ----- desire-value functions, called in SyllogisticRules ----- */
    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth desireStrong(Truth v1, Truth v2) {
        // ? 此函数似乎是用在「目标」上的
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率=双频之合取
        // * 📝信度=双方信度 合取 第二方频率
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth desireWeak(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率=双频之合取
        // * 📝信度=双方信度 合取 第二方频率 合取 单位数目信度（保证弱推理）
        float f = and(f1, f2);
        float c = and(c1, c2, f2, W2C1);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth desireDed(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率=双频之合取
        // * 📝信度=双信之合取
        float f = and(f1, f2);
        float c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth desireInd(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝总频数=第二方频率 合取 双信之合取
        // * 📝频率=第一方频率
        // * 📝信度=总频数转换（保证弱推理）
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(f1, c);
    }

    /* ----- double argument functions, called in CompositionalRules ----- */
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth union(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率=双频之析取
        // * 📝信度=双信之合取
        float f = or(f1, f2);
        float c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth intersection(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        // * 📝频率=双频之合取
        // * 📝信度=双信之合取
        float f = and(f1, f2);
        float c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {(||, A, B), (--, B)} |- A
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth reduceDisjunction(Truth v1, Truth v2) {
        // * 🚩演绎（反向交集，依赖度=1）
        Truth v0 = intersection(v1, negation(v2));
        return analyticDeduction(v0, 1f);
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth reduceConjunction(Truth v1, Truth v2) {
        // * 🚩否定演绎（反向交集（内部取反），依赖度=1）
        Truth v0 = intersection(negation(v1), v2);
        return negation(analyticDeduction(v0, 1f));
    }

    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth reduceConjunctionNeg(Truth v1, Truth v2) {
        // * 🚩消取，但对第二方套否定
        return reduceConjunction(v1, negation(v2));
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param v1 [&] Truth value of the first premise
     * @param v2 [&] Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static Truth anonymousAnalogy(Truth v1, Truth v2) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        // * 📝中间频率=第一方频
        // * 📝中间信度=第一方信度作为「总频数」（弱推理）
        Truth v0 = new TruthValue(f1, w2c(c1));
        // * 🚩再参与「类比」（弱中之弱）
        return analogy(v2, v0);
    }

    /**
     * 🆕函数表
     * * 🎯示例性存储表示「真值函数」的引用（函数指针）
     * * 🚩无需真正创建实例
     */
    static abstract class FunctionTable {
        // * 📌单真值函数
        TruthFSingle identity = TruthFunctions::identity;
        TruthFSingle conversion = TruthFunctions::conversion;
        TruthFSingle negation = TruthFunctions::negation;
        TruthFSingle contraposition = TruthFunctions::contraposition;
        // * 📌双真值函数
        TruthFDouble revision = TruthFunctions::revision;
        TruthFDouble deduction = TruthFunctions::deduction;
        TruthFDouble analogy = TruthFunctions::analogy;
        TruthFDouble resemblance = TruthFunctions::resemblance;
        TruthFDouble abduction = TruthFunctions::abduction;
        TruthFDouble induction = TruthFunctions::induction;
        TruthFDouble exemplification = TruthFunctions::exemplification;
        TruthFDouble desireStrong = TruthFunctions::desireStrong;
        TruthFDouble desireWeak = TruthFunctions::desireWeak;
        TruthFDouble desireDeduction = TruthFunctions::desireDed;
        TruthFDouble desireInduction = TruthFunctions::desireInd;
        TruthFDouble union = TruthFunctions::union;
        TruthFDouble intersection = TruthFunctions::intersection;
        TruthFDouble reduceDisjunction = TruthFunctions::reduceDisjunction;
        TruthFDouble reduceConjunction = TruthFunctions::reduceConjunction;
        TruthFDouble reduceConjunctionNeg = TruthFunctions::reduceConjunctionNeg;
        TruthFDouble anonymousAnalogy = TruthFunctions::anonymousAnalogy;
        // * 📌单真值依赖函数（分析性函数）
        TruthFAnalytic analyticDeduction = TruthFunctions::analyticDeduction;
        TruthFAnalytic analyticAbduction = TruthFunctions::analyticAbduction;
    }
}
