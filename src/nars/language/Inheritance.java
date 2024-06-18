package nars.language;

import java.util.ArrayList;

import nars.io.Symbols;

/**
 * A Statement about an Inheritance relation.
 */
public class Inheritance extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    Inheritance(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     */
    private Inheritance(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a SetExt
     */
    public Inheritance clone() {
        return new Inheritance(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.INHERITANCE_RELATION;
    }

}
