/*
 * Judgment.java
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
package nars.entity;

import nars.inference.TemporalRules;
import nars.io.Symbols;
import nars.language.Term;
import nars.main.Parameters;

/**
 * A Judgment is an piece of new knowledge to be absorbed.
 */
public class Judgment extends Sentence {

    /**
     * Constructor
     * @param term The content
     * @param punc The punctuation
     * @param s The tense
     * @param t The truth value
     * @param b The stamp
     */
    public Judgment(Term term, char punc, TemporalRules.Relation s, TruthValue t, Stamp b) {
        content = term;
        punctuation = punc;
        tense = s;
        truth = t;
        stamp = b;
    }

    /**
     * Construct a Judgment to indicate an operation just executed
     * @param g The goal that trigger the execution
     */
    public Judgment(Goal g) {
        content = g.cloneContent();
        punctuation = Symbols.JUDGMENT_MARK;
        tense = TemporalRules.Relation.BEFORE;
        truth = new TruthValue(1.0f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);
        stamp = new Stamp();
    }

    /**
     * Check whether the judgment is equivalent to another one
     * <p>
     * The two may have different keys
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    boolean equivalentTo(Judgment that) {
        assert content.equals(that.getContent());
        return (truth.equals(that.getTruth()) && stamp.equals(that.getStamp())); 
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     * @param problem A goal or question
     * @return The quality of the judgment as the solution
     */
    public float solutionQuality(Sentence problem) {
        if (problem instanceof Goal) {
            return truth.getExpectation();
        } else if (problem.getContent().isConstant()) {   // "yes/no" question
            return truth.getConfidence();
        } else {                                    // "what" question or goal
            return truth.getExpectation() / content.getComplexity();
        }
    }
}

