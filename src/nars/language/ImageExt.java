package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * An extension image.
 * <p>
 * B --> (/,P,A,_)) iff (*,A,B) --> P
 * <p>
 * Internally, it is actually (/,A,P)_1, with an index.
 */
public class ImageExt extends Image {

    /**
     * Constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    ImageExt(String n, ArrayList<Term> arg, short index) {
        super(n, arg, index);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     * @param index      The index of relation in the component list
     */
    private ImageExt(String name, TermComponents components, short index) {
        super(name, components, index);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageExt
     */
    public ImageExt clone() {
        return new ImageExt(name, this.components.deepClone(), this.relationIndex);
    }

    /**
     * override the default in making the name of the current term from existing
     * fields
     *
     * @return the name of the term
     */
    @Override
    public String makeName() {
        return makeImageName(Symbols.IMAGE_EXT_OPERATOR, components, relationIndex);
    }

    /**
     * get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMAGE_EXT_OPERATOR;
    }
}
