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
     * @param name       The name of the term
     * @param components Component list
     */
    private Product(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone a Product
     *
     * @return A new object, to be casted into an ImageExt
     */
    public Product clone() {
        return new Product(name, this.components.deepClone());
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
