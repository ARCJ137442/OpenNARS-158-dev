package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A compound term whose intension is the intersection of the extensions of its
 * components
 */
public class IntersectionInt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    IntersectionInt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n  The name of the term
     * @param cs Component list
     */
    private IntersectionInt(String n, TermComponents cs) {
        super(n, cs);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Conjunction
     */
    public IntersectionInt clone() {
        return new IntersectionInt(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.INTERSECTION_INT_OPERATOR;
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
