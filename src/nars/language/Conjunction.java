package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * Conjunction of statements
 */
public class Conjunction extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    Conjunction(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether the term is a constant
     * @param i   Syntactic complexity of the compound
     */
    private Conjunction(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Conjunction clone() {
        final ArrayList<Term> cs = ArrayUtils.cloneList(components);
        return new Conjunction(name, cs, this.isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.CONJUNCTION_OPERATOR;
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
