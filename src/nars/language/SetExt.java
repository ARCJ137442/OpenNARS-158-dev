package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * An extensionally defined set, which contains one or more instances.
 */
public class SetExt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private SetExt(ArrayList<Term> arg) {
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
    private SetExt(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone a SetExt
     *
     * @return A new object, to be casted into a SetExt
     */
    public SetExt clone() {
        return new SetExt(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Try to make a new set from one component. Called by the inference rules.
     *
     * @param t      The component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Term make(Term t, Memory memory) {
        TreeSet<Term> set = new TreeSet<Term>();
        set.add(t);
        return make(set, memory);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     * @param memory  Reference to the memory
     */
    public static Term make(ArrayList<Term> argList, Memory memory) {
        TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return make(set, memory);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(TreeSet<Term> set, Memory memory) {
        if (set.isEmpty()) {
            return null;
        }
        ArrayList<Term> argument = new ArrayList<Term>(set);
        String name = makeSetName(Symbols.SET_EXT_OPENER, argument, Symbols.SET_EXT_CLOSER);
        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new SetExt(argument);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return "" + Symbols.SET_EXT_OPENER;
    }

    /**
     * Check if the compound is communicative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }

    /**
     * Make a String representation of the set, override the default.
     *
     * @return true for commutative
     */
    @Override
    public String makeName() {
        return makeSetName(Symbols.SET_EXT_OPENER, components, Symbols.SET_EXT_CLOSER);
    }
}
