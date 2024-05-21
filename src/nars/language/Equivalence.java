package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A Statement about an Equivalence relation.
 */
public class Equivalence extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param components The component list of the term
     */
    public Equivalence(ArrayList<Term> components) {
        super(components);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n          The name of the term
     * @param components Component list
     * @param constant   Whether the statement contains open variable
     * @param complexity Syntactic complexity of the compound
     */
    protected Equivalence(String n, ArrayList<Term> components, boolean constant, short complexity) {
        super(n, components, constant, complexity);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Equivalence clone() {
        return new Equivalence(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.EQUIVALENCE_RELATION;
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
