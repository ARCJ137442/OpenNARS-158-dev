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
public class ImageInt extends CompoundTerm {

    /** The index of relation in the component list */
    private final short relationIndex;

    /**
     * constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    ImageInt(String n, ArrayList<Term> arg, short index) {
        super(n, arg);
        relationIndex = index;
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     * @param index      The index of relation in the component list
     */
    private ImageInt(String name, TermComponents components, short complexity, short index) {
        super(name, components);
        relationIndex = index;
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
     * get the index of the relation in the component list
     *
     * @return the index of relation
     */
    public short getRelationIndex() {
        return relationIndex;
    }

    /**
     * Get the relation term in the Image
     *
     * @return The term representing a relation
     */
    public Term getRelation() {
        return components.get(relationIndex);
    }

    /**
     * Get the other term in the Image
     *
     * @return The term related
     */
    public Term getTheOtherComponent() {
        if (components.size() != 2) {
            return null;
        }
        return (relationIndex == 0) ? components.get(1) : components.get(0);
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
