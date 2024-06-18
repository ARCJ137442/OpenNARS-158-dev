package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * An intensionally defined set, which contains one or more instances defining
 * the Term.
 */
public class SetInt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    SetInt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * constructor with full values, called by clone
     *
     * @param n  The name of the term
     * @param cs Component list
     * @param i  Syntactic complexity of the compound
     */
    private SetInt(String n, TermComponents cs, short i) {
        super(n, cs, i);
    }

    /**
     * Clone a SetInt
     *
     * @return A new object, to be casted into a SetInt
     */
    public SetInt clone() {
        return new SetInt(name, this.components.deepClone(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return "" + Symbols.SET_INT_OPENER;
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

    /**
     * Make a String representation of the set, override the default.
     *
     * @return true for commutative
     */
    @Override
    public String makeName() {
        return makeSetName(Symbols.SET_INT_OPENER, components, Symbols.SET_INT_CLOSER);
    }
}
