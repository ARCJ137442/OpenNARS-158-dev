package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * An intension image.
 * <p>
 * (\,P,A,_)) --> B iff P --> (*,A,B)
 * <p>
 * Internally, it is actually (\,A,P)_1, with an index.
 */
public class ImageInt extends Image {

    /**
     * constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    ImageInt(String n, ArrayList<Term> arg, short index) {
        super(n, arg, index);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     * @param index      The index of relation in the component list
     */
    private ImageInt(String name, TermComponents components, short complexity, short index) {
        super(name, components, index);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageInt
     */
    public ImageInt clone() {
        return new ImageInt(name, this.components.deepClone(), relationIndex);
    }

    /**
     * Override the default in making the name of the current term from existing
     * fields
     *
     * @return the name of the term
     */
    @Override
    public String makeName() {
        return makeImageName(Symbols.IMAGE_INT_OPERATOR, components, relationIndex);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMAGE_INT_OPERATOR;
    }
}
