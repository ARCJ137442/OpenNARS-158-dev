package nars.inference;

import nars.entity.*;

/**
 * All truth-value (and desire-value) functions used in inference rules
 * * 🚩【2024-05-30 09:21:34】此处不加`final`：逻辑最简
 * * 📝所有函数均【返回新真值对象】且【不修改所传入参数】
 * TODO: 过程笔记注释
 */
public final class TruthFunctions extends UtilityFunctions {

    /* ----- Single argument functions, called in MatchingRules ----- */
    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static TruthValue conversion(TruthValue v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = and(f1, c1);
        float c = w2c(w);
        return new TruthValue(1, c);
    }

    /* ----- Single argument functions, called in StructuralRules ----- */
    /**
     * {A} |- (--A)
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static TruthValue negation(TruthValue v1) {
        // * 📝频率相反，信度相等
        float f = not(v1.getFrequency());
        float c = v1.getConfidence();
        return new TruthValue(f, c);
    }

    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     *
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static TruthValue contraposition(TruthValue v1) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue revision(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue deduction(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, f);
        return new TruthValue(f, c);
    }

    /**
     * {M, <M ==> P>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    public static TruthValue deduction(TruthValue v1, float reliance) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float c = and(f1, c1, reliance);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue analogy(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue resemblance(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, or(f1, f2));
        return new TruthValue(f, c);
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue abduction(TruthValue v1, TruthValue v2) {
        if (v1.getAnalytic() || v2.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(f1, c);
    }

    /**
     * {M, <P ==> M>} |- P
     *
     * @param v1       Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return Truth value of the conclusion
     */
    public static TruthValue abduction(TruthValue v1, float reliance) {
        if (v1.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = and(c1, reliance);
        float c = w2c(w);
        return new TruthValue(f1, c, true);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue induction(TruthValue v1, TruthValue v2) {
        return abduction(v2, v1);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue exemplification(TruthValue v1, TruthValue v2) {
        if (v1.getAnalytic() || v2.getAnalytic()) {
            return new TruthValue(0.5f, 0f);
        }
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = and(f1, f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(1, c);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue comparison(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue desireStrong(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue desireWeak(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, f2, w2c(1.0f));
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue desireDed(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue desireInd(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(f1, c);
    }

    /* ----- double argument functions, called in CompositionalRules ----- */
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue union(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = or(f1, f2);
        float c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue intersection(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
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
    public static TruthValue reduceDisjunction(TruthValue v1, TruthValue v2) {
        TruthValue v0 = intersection(v1, negation(v2));
        return deduction(v0, 1f);
    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue reduceConjunction(TruthValue v1, TruthValue v2) {
        TruthValue v0 = intersection(negation(v1), v2);
        return negation(deduction(v0, 1f));
    }

    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue reduceConjunctionNeg(TruthValue v1, TruthValue v2) {
        return reduceConjunction(v1, negation(v2));
    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static TruthValue anonymousAnalogy(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        TruthValue v0 = new TruthValue(f1, w2c(c1));
        return analogy(v2, v0);
    }
}
