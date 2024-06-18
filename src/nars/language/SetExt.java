package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * An extensionally defined set, which contains one or more instances.
 */
public class SetExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    SetExt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     * @param complexity Syntactic complexity of the compound
     */
    private SetExt(String name, TermComponents components, short complexity) {
        super(name, components, complexity);
    }

    /**
     * Clone a SetExt
     *
     * @return A new object, to be casted into a SetExt
     */
    public SetExt clone() {
        return new SetExt(name, this.components.deepClone(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return "" + Symbols.SET_EXT_OPENER;
    }

    /**
     * Check if the compound is communicative.
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
        return makeSetName(Symbols.SET_EXT_OPENER, components, Symbols.SET_EXT_CLOSER);
    }
}
