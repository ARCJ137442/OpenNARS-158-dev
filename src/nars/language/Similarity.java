package nars.language;

import java.util.ArrayList;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A Statement about a Similarity relation.
 */
public class Similarity extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Similarity(ArrayList<Term> arg) {
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
    private Similarity(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object, to be casted into a Similarity
     */
    @Override
    public Similarity clone() {
        return new Similarity(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
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
    public static Similarity make(Term subject, Term predicate, Memory memory) {
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        if (subject.compareTo(predicate) > 0) {
            return make(predicate, subject, memory);
        }
        String name = makeStatementName(subject, Symbols.SIMILARITY_RELATION, predicate);
        Term t = memory.nameToListedTerm(name);
        if (t != null) {
            return (Similarity) t;
        }
        ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Similarity(argument);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.SIMILARITY_RELATION;
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
