/*
 * TruthFunctions.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.inference;

import nars.entity.TruthValue;

/**
 * All truth-value (and desire-value) functions used in inference rules 
 */
public final class TruthFunctions extends UtilityFunctions {
    
    /* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A ==> B>} |- <B ==> A>
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue conversion(TruthValue v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = and(f1, c1);
        float c = w2c(w);
        return new TruthValue(1, c);
    }
    
    /* ----- Single argument functions, called in StructuralRules ----- */

    /**
     * {A} |- (--A)
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue negation(TruthValue v1) {
        float f = 1 - v1.getFrequency();
        float c = v1.getConfidence();
        return new TruthValue(f, c);
    }
    
    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue contraposition(TruthValue v1) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float w = and(1-f1, c1);
        float c = w2c(w);
        return new TruthValue(0, c);
    }
    
    /**
     * {<A ==> B>, A} |- B
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue implying(TruthValue v1) {
        return implying(v1, 1);
    }

    /**
     * {<A ==> B>, A} |- B
     * @param v1 Truth value of the premise
     * @param discount Confidence discount factor for new CompoundTerm
     * @return Truth value of the conclusion
     */
    static TruthValue implying(TruthValue v1, float discount) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float c = and(f1, c1) * discount;
        return new TruthValue(f1, c);
    }

    /**
     * {<A ==> B>, B} |- A
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue implied(TruthValue v1) {
        return implied(v1, 1);
    }

    /**
     * {<A ==> B>, B} |- A
     * @param v1 Truth value of the premise
     * @param discount Confidence discount factor for new CompoundTerm
     * @return Truth value of the conclusion
     */
    static TruthValue implied(TruthValue v1, float discount) {
        float f1 = v1.getFrequency();
        float c1 = v1.getConfidence();
        float c = w2c(c1) * discount;
        return new TruthValue(f1, c);
    }
    
    /**
     * {<A ==> (--, B)>, A} |- B
     * @param v1 Truth value of the premise
     * @return Truth value of the conclusion
     */
    static TruthValue negImply(TruthValue v1) {
        return negation(implying(v1));
    }

    /* ----- double argument functions, called in MatchingRules ----- */
    
    /**
     * {<S ==> P>, <S ==> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue revision(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w1 = c2w(c1);
        float w2 = c2w(c2);
        float w = w1 + w2;
        float f = (w1 * f1 + w2 * f2) / w;
        float c = w2c(w);
        return new TruthValue(f, c);
    }

    /**
     * Revision weighted by time difference
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @param t1 Creation time of the first truth value
     * @param t2 Creation time of the second truth value
     * @param t Target time of the resulting truth value
     * @return Truth value of the conclusion
     */
    static TruthValue temporalRevision(TruthValue v1, TruthValue v2, long t1, long t2, long t) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w1 = c2w(c1);
        float w2 = c2w(c2);
        float d1 = w1 / (Math.abs(t - t1) + 1);
        float d2 = w2 / (Math.abs(t - t2) + 1);
        float f = (d1 * f1 + d2 * f2) / (d1 + d2);
        float c = w2c(w1 + w2);
        return new TruthValue(f, c);
    }

    /* ----- double argument functions, called in SyllogisticRules ----- */
    
    /**
     * {<S ==> M>, <M ==> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue deduction(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = and(c1, c2, f);
        return new TruthValue(f, c);
    }
    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue analogy(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue resemblance(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue abduction(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float w = and(f2, c1, c2);
        float c = w2c(w);
        return new TruthValue(f1, c);
    }
    
    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue induction(TruthValue v1, TruthValue v2) {
        return abduction(v2, v1);
    }
    
    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue exemplification(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue comparison(TruthValue v1, TruthValue v2) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f0 = or(f1, f2);
        float f = (f0 == 0) ? 0 : (and(f1, f2) / f0) ;
        float w = and(f0, c1, c2);
        float c = w2c(w);
        return new TruthValue(f, c);
    }
    
    
    /* ----- desire-value functions, called in SyllogisticRules ----- */
    
    /**
     * A function specially designed for desire value [To be refined]
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireStrong(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireWeak(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireDed(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue desireInd(TruthValue v1, TruthValue v2) {
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
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue union(TruthValue v1, TruthValue v2) {
        return union(v1, v2, 1);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @param discount Confidence discount factor for new CompoundTerm
     * @return Truth value of the conclusion
     */
    static TruthValue union(TruthValue v1, TruthValue v2, float discount) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = or(f1, f2);
        float c = or(and(f1, c1), and(f2, c2)) + and(1 - f1, 1 - f2, c1, c2);
        return new TruthValue(f, c * discount);
    }
    
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue intersection(TruthValue v1, TruthValue v2) {
        return intersection(v1, v2, 1);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @param discount Confidence discount factor for new CompoundTerm
     * @return Truth value of the conclusion
     */
    static TruthValue intersection(TruthValue v1, TruthValue v2, float discount) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        float c1 = v1.getConfidence();
        float c2 = v2.getConfidence();
        float f = and(f1, f2);
        float c = or(and(1 - f1, c1), and(1 - f2, c2)) + and(f1, f2, c1, c2);
        return new TruthValue(f, c * discount);
    }
    
    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S-P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue difference(TruthValue v1, TruthValue v2) {
        TruthValue v0 = negation(v2);
        return intersection(v1, v0, 1);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S-P)>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @param discount Confidence discount factor for new CompoundTerm
     * @return Truth value of the conclusion
     */
    static TruthValue difference(TruthValue v1, TruthValue v2, float discount) {
        TruthValue v0 = negation(v2);
        return intersection(v1, v0, discount);
    }
    
    /**
     * {(||, A, B), (--, B)} |- A
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceDisjunction(TruthValue v1, TruthValue v2) {
        TruthValue v0 = intersection(v1, negation(v2));
        return implying(v0);
    }
    
    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceConjunction(TruthValue v1, TruthValue v2) {
        TruthValue v0 = intersection(negation(v1), v2, 1);
        return negation(implying(v0));
    }
    
    /**
     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue reduceConjunctionNeg(TruthValue v1, TruthValue v2) {
        return reduceConjunction(v1, negation(v2));
    }
    
    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    static TruthValue existAnalogy(TruthValue v1, TruthValue v2) {
        return abduction(v1, v2);
    }
}
