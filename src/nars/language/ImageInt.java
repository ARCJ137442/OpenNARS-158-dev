package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

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
    public ImageInt(String n, ArrayList<Term> arg, short index) {
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
        return new ImageInt(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity, relationIndex);
    }

    /**
     * Try to make a new ImageExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term make(ArrayList<Term> argList, Memory memory) {
        if (argList.size() < 2) {
            return null;
        }
        Term relation = argList.get(0);
        ArrayList<Term> argument = new ArrayList<Term>();
        int index = 0;
        for (int j = 1; j < argList.size(); j++) {
            if (argList.get(j).getName().charAt(0) == Symbols.IMAGE_PLACE_HOLDER) {
                index = j - 1;
                argument.add(relation);
            } else {
                argument.add(argList.get(j));
            }
        }
        return make(argument, (short) index, memory);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference
     * rules.
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @param memory   Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Product product, Term relation, short index, Memory memory) {
        if (relation instanceof Product) {
            Product p2 = (Product) relation;
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                    // (\,_,(*,a,b),b) is reduced to a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                    // (\,(*,a,b),a,_) is reduced to b
                    return p2.componentAt(1);
                }
            }
        }
        ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return make(argument, index, memory);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the
     * inference rules.
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(ImageInt oldImage, Term component, short index, Memory memory) {
        ArrayList<Term> argList = oldImage.cloneComponents();
        int oldIndex = oldImage.getRelationIndex();
        Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return make(argList, index, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @param memory   Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(ArrayList<Term> argument, short index, Memory memory) {
        String name = makeImageName(Symbols.IMAGE_INT_OPERATOR, argument, index);
        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new ImageInt(name, argument, index);
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
