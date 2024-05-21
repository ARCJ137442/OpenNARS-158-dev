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
    public Similarity(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private Similarity(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Similarity
     */
    @Override
    public Similarity clone() {
        return new Similarity(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
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
