package nars.language;

import java.util.ArrayList;

import nars.io.Symbols;

/**
 * A Statement about a Similarity relation.
 */
public class Similarity extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    Similarity(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n  The name of the term
     * @param cs Component list
     */
    private Similarity(String n, TermComponents cs) {
        super(n, cs);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Similarity
     */
    @Override
    public Similarity clone() {
        return new Similarity(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.SIMILARITY_RELATION;
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
