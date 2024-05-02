package nars.language;

import java.util.ArrayList;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A Statement about an Inheritance relation.
 */
public class Inheritance extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Inheritance(ArrayList<Term> arg) {
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
    private Inheritance(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a SetExt
     */
    public Object clone() {
        return new Inheritance(name, (ArrayList<Term>) cloneList(components), isConstant, complexity);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or null
     */
    public static Inheritance make(Term subject, Term predicate, Memory memory) {
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        String name = makeStatementName(subject, Symbols.INHERITANCE_RELATION, predicate);
        Term t = memory.nameToListedTerm(name);
        if (t != null) {
            return (Inheritance) t;
        }
        ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Inheritance(argument);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.INHERITANCE_RELATION;
    }

}
