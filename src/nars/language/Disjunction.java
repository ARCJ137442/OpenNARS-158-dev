package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A disjunction of Statements.
 */
public class Disjunction extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    Disjunction(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     */
    private Disjunction(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Disjunction clone() {
        return new Disjunction(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.DISJUNCTION_OPERATOR;
    }

    /**
     * Disjunction is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
