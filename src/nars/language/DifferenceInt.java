package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A compound term whose extension is the difference of the intensions of its
 * components
 */
public class DifferenceInt extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    DifferenceInt(ArrayList<Term> arg) {
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
    private DifferenceInt(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a DifferenceInt
     */
    public DifferenceInt clone() {
        final ArrayList<Term> cs = ArrayUtils.cloneList(components);
        return new DifferenceInt(name, cs, isConstant(), complexity);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.DIFFERENCE_INT_OPERATOR;
    }
}
