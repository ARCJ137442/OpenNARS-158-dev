package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A Product is a sequence of terms.
 */
public class Product extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    Product(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n          The name of the term
     * @param cs         Component list
     * @param open       Open variable list
     * @param complexity Syntactic complexity of the compound
     */
    private Product(String n, ArrayList<Term> cs, boolean con, short complexity) {
        super(n, cs, con, complexity);
    }

    /**
     * Clone a Product
     *
     * @return A new object, to be casted into an ImageExt
     */
    public Product clone() {
        return new Product(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.PRODUCT_OPERATOR;
    }
}
