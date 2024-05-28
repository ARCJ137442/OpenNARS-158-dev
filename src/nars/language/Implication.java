package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A Statement about an Inheritance relation.
 */
public class Implication extends Statement {

    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    public Implication(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n   The name of the term
     * @param cs  Component list
     * @param con Whether it is a constant term
     * @param i   Syntactic complexity of the compound
     */
    protected Implication(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    public Implication clone() {
        return new Implication(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param memory    Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    public static Implication make(Term subject, Term predicate, Memory memory) {
        if ((subject == null) || (predicate == null)) {
            return null;
        }
        if ((subject == null) || (predicate == null)) {
            return null;
        }
        if ((subject instanceof Implication) || (subject instanceof Equivalence)
                || (predicate instanceof Equivalence)) {
            return null;
        }
        if (invalidStatement(subject, predicate)) {
            return null;
        }
        String name = makeStatementName(subject, Symbols.IMPLICATION_RELATION, predicate);
        Term t = memory.nameToListedTerm(name);
        if (t != null) {
            return (Implication) t;
        }
        if (predicate instanceof Implication) {
            Term oldCondition = ((Implication) predicate).getSubject();
            if ((oldCondition instanceof Conjunction) && ((Conjunction) oldCondition).containComponent(subject)) {
                return null;
            }
            Term newCondition = Conjunction.make(subject, oldCondition, memory);
            return make(newCondition, ((Implication) predicate).getPredicate(), memory);
        } else {
            ArrayList<Term> argument = argumentsToList(subject, predicate);
            return new Implication(argument);
        }
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    public String operator() {
        return Symbols.IMPLICATION_RELATION;
    }
}
