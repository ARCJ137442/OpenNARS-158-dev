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
     * @param n          The name of the term
     * @param cs         Component list
     * @param open       Open variable list
     * @param complexity Syntactic complexity of the compound
     * @param index      The index of relation in the component list
     */
    private ImageInt(String n, ArrayList<Term> cs, boolean con, short complexity, short index) {
        super(n, cs, con, complexity);
        relationIndex = index;
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageInt
     */
    public ImageInt clone() {
        final ArrayList<Term> cs = ArrayUtils.cloneList(components);
        return new ImageInt(name, cs, isConstant(), complexity, relationIndex);
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
        return components[relationIndex];
    }

    /**
     * Get the other term in the Image
     *
     * @return The term related
     */
    public Term getTheOtherComponent() {
        if (components.length != 2) {
            return null;
        }
        return (relationIndex == 0) ? components[1] : components[0];
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
