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
     * @param name       The name of the term
     * @param components Component list
     *
     * @param complexity Syntactic complexity of the compound
     */
    private Conjunction(String name, TermComponents components, short complexity) {
        super(name, components, complexity);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Conjunction clone() {
        return new Conjunction(name, this.components.deepClone(), complexity);
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
