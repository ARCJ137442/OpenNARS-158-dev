package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A compound term whose extension is the difference of the extensions of its
 * components
 */
public class DifferenceExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private DifferenceExt(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private DifferenceExt(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a DifferenceExt
     */
    public DifferenceExt clone() {
        return new DifferenceExt(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term make(ArrayList<Term> argList, Memory memory) {
        if (argList.size() == 1) { // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        }
        if (argList.size() != 2) {
            return null;
        }
        if ((argList.get(0) instanceof SetExt) && (argList.get(1) instanceof SetExt)) {
            TreeSet<Term> set = new TreeSet<Term>(((CompoundTerm) argList.get(0)).cloneComponents());
            set.removeAll(((CompoundTerm) argList.get(1)).cloneComponents()); // set difference
            return SetExt.make(set, memory);
        }
        String name = makeCompoundName(Symbols.DIFFERENCE_EXT_OPERATOR, argList);
        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new DifferenceExt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param t1     The first component
     * @param t2     The second component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t1, Term t2, Memory memory) {
        if (t1.equals(t2)) {
            return null;
        }
        ArrayList<Term> list = argumentsToList(t1, t2);
        return make(list, memory);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.DIFFERENCE_EXT_OPERATOR;
    }
}
