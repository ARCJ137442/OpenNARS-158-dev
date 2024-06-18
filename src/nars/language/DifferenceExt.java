package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A compound term whose extension is the difference of the extensions of its
 * components
 */
public class DifferenceExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    DifferenceExt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     */
    private DifferenceExt(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a DifferenceExt
     */
    public DifferenceExt clone() {
        return new DifferenceExt(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.DIFFERENCE_EXT_OPERATOR;
    }
}
