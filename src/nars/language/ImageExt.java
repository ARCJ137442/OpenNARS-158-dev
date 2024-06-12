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
public class ImageExt extends CompoundTerm {

    /** The index of relation in the component list */
    private final short relationIndex;

    /**
     * Constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    ImageExt(String n, ArrayList<Term> arg, short index) {
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
    private ImageExt(String n, ArrayList<Term> cs, boolean con, short complexity, short index) {
        super(n, cs, con, complexity);
        relationIndex = index;
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an ImageExt
     */
    public ImageExt clone() {
        final ArrayList<Term> cs = ArrayUtils.cloneList(components);
        return new ImageExt(name, cs, isConstant(), complexity, relationIndex);
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
