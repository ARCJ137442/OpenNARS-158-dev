package nars.language;

import java.util.*;

/**
 * An image, probably extensional or intentional.
 * <p>
 * B --> (/,P,A,_)) iff (*,A,B) --> P
 * <p>
 * Internally, it is actually (/,A,P)_1, with an index.
 *
 * <p>
 * (\,P,A,_)) --> B iff P --> (*,A,B)
 * <p>
 * Internally, it is actually (\,A,P)_1, with an index.
 */
public abstract class Image extends CompoundTerm {

    /** The index of relation in the component list */
    protected final short relationIndex;

    /**
     * Constructor with partial values, called by make
     *
     * @param n     The name of the term
     * @param arg   The component list of the term
     * @param index The index of relation in the component list
     */
    Image(String n, ArrayList<Term> arg, short index) {
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
    private Image(String name, TermComponents components, short index) {
        super(name, components);
        relationIndex = index;
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into an Image
     */
    public abstract Image clone();

    /**
     * get the index of the relation in the component list
     *
     * @return the index of relation
     */
    public final short getRelationIndex() {
        return relationIndex;
    }

    /**
     * Get the relation term in the Image
     *
     * @return The term representing a relation
     */
    public final Term getRelation() {
        return components.get(relationIndex);
    }

    /**
     * Get the other term in the Image
     *
     * @return The term related
     */
    public final Term getTheOtherComponent() {
        if (components.size() != 2) {
            return null;
        }
        return (relationIndex == 0) ? components.get(1) : components.get(0);
    }

    /**
     * override the default in making the name of the current term from existing
     * fields
     *
     * @return the name of the term
     */
    @Override
    public abstract String makeName();

    /**
     * get the operator of the term.
     *
     * @return the operator of the term
     */
    public abstract String operator();
}
