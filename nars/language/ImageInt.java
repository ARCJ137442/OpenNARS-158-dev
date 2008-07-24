/*
 * ImageInt.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.main.Memory;

/**
 * An intension image.
 * <p>
 * (\,P,A,_)) --> B iff P --> (*,A,B)
 * <p>
 * Internally, it is actually (\,A,P)_1, with an index.
 */
public class ImageInt extends CompoundTerm {

    /** The index of relation in the component list */
    private short relationIndex;

    /**
     * constructor with partial values, called by make
     * @param n The name of the term
     * @param arg The component list of the term
     * @param index The index of relation in the component list
     */
    private ImageInt(String n, ArrayList<Term> arg, short index) {
        super(n, arg);
        relationIndex = index;
    }

    /**
     * Constructor with full values, called by clone
     * @param n The name of the term
     * @param cs Component list
     * @param open Open variable list
     * @param complexity Syntactic complexity of the compound
     * @param index The index of relation in the component list
     */
    private ImageInt(String n, ArrayList<Term> cs, ArrayList<Variable> open, short complexity, short index) {
        super(n, cs, open, complexity);
        relationIndex = index;
    }

    /**
     * Clone an object
     * @return A new object, to be casted into an ImageInt
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        return new ImageInt(name, (ArrayList<Term>) cloneList(components), (ArrayList<Variable>) cloneList(openVariables), complexity, relationIndex);
    }

    /**
     * Try to make a new ImageExt. Called by StringParser.
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term make(ArrayList<Term> argList) {
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
        return make(argument, (short) index);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference rules.
     * @param product The product
     * @param relation The relation
     * @param index The index of the place-holder
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Product product, Term relation, short index) {
        if (relation instanceof Product) {
            Product p2 = (Product) relation;
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {// (\,_,(*,a,b),b) is reduced to a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {// (\,(*,a,b),a,_) is reduced to b
                    return p2.componentAt(1);
                }
            }
        }
        ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return make(argument, index);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the inference rules.
     * @param oldImage The existing Image
     * @param component The component to be added into the component list
     * @param index The index of the place-holder in the new Image
     * @return A compound generated or a term it reduced to
     */
    public static Term make(ImageInt oldImage, Term component, short index) {
        ArrayList<Term> argList = oldImage.cloneComponents();
        int oldIndex = oldImage.getRelationIndex();
        Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return make(argList, index);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public make methods.
     * @param argument The argument list
     * @param index The index of the place-holder in the new Image
     * @return the Term generated from the arguments
     */
    public static Term make(ArrayList<Term> argument, short index) {
        String name = makeImageName(Symbols.IMAGE_INT_OPERATOR, argument, index);
        Term t = Memory.nameToListedTerm(name);
        return (t != null) ? t : new ImageInt(name, argument, index);
    }

    /**
     * get the index of the relation in the component list
     * @return the index of relation
     */
    public short getRelationIndex() {
        return relationIndex;
    }

    /**
     * Override the default in making the name of the current term from existing fields
     * @return the name of the term
     */
    @Override
    public String makeName() {
        return makeImageName(Symbols.IMAGE_INT_OPERATOR, components, relationIndex);
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMAGE_INT_OPERATOR;
    }
}
