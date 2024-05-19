package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

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
    private Product(ArrayList<Term> arg) {
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
     * Try to make a new compound. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argument The list of components
     * @param memory   Reference to the memory
     */
    public static Term make(ArrayList<Term> argument, Memory memory) {
        String name = makeCompoundName(Symbols.PRODUCT_OPERATOR, argument);
        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new Product(argument);
    }

    /**
     * Try to make a Product from an ImageExt/ImageInt and a component. Called by
     * the inference rules.
     *
     * @param image     The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image -- optional
     *                  parameter
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(CompoundTerm image, Term component, int index, Memory memory) {
        ArrayList<Term> argument = image.cloneComponents();
        argument.set(index, component);
        return make(argument, memory);
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
