package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A negation of a statement.
 */
public class Negation extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    Negation(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The name of the term
     * @param components Component list
     * @param i          Syntactic complexity of the compound
     */
    private Negation(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Negation clone() {
        return new Negation(name, this.components.deepClone());
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.NEGATION_OPERATOR;
    }

    /**
     * 🆕获取「否定」唯一的词项
     * * 🎯明确语义
     */
    public Term getTheComponent() {
        return this.components.get(0);
    }
}
