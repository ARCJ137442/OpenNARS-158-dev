package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A Statement about an Inheritance relation.
 */
public class Implication extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    Implication(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether it is a constant term
     * @param i   Syntactic complexity of the compound
     */
    protected Implication(String n, TermComponents cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    public Implication clone() {
        return new Implication(name, this.components.deepClone(), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMPLICATION_RELATION;
    }
}
