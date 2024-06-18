package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A compound term whose extension is the intersection of the extensions of its
 * components
 */
public class IntersectionExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    IntersectionExt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     */
    private IntersectionExt(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a IntersectionExt
     */
    public IntersectionExt clone() {
        return new IntersectionExt(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.INTERSECTION_EXT_OPERATOR;
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
