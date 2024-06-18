package nars.language;

import java.util.ArrayList;

import nars.io.Symbols;

/**
 * A statement is a compound term, consisting of a subject, a predicate, and a
 * relation symbol in between. It can be of either first-order or higher-order.
 */
public abstract class Statement extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     */
    protected Statement(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param name       The nameStr of the term
     * @param components Component list
     */
    protected Statement(String name, TermComponents components) {
        super(name, components);
    }

    /**
     * Override the default in making the nameStr of the current term from
     * existing fields
     *
     * @return the nameStr of the term
     */
    @Override
    protected String makeName() {
        return makeStatementName(getSubject(), operator(), getPredicate());
    }

    /**
     * Default method to make the nameStr of an image term from given fields
     *
     * @param subject   The first component
     * @param predicate The second component
     * @param relation  The relation operator
     * @return The nameStr of the term
     */
    public static String makeStatementName(Term subject, String relation, Term predicate) {
        final StringBuilder nameStr = new StringBuilder();
        nameStr.append(Symbols.STATEMENT_OPENER);
        nameStr.append(subject.getName());
        nameStr.append(' ').append(relation).append(' ');
        nameStr.append(predicate.getName());
        nameStr.append(Symbols.STATEMENT_CLOSER);
        return nameStr.toString();
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return Whether The Statement is invalid
     */
    public static final boolean invalidStatement(Term subject, Term predicate) {
        if (subject.equals(predicate)) {
            return true;
        }
        if (invalidReflexive(subject, predicate)) {
            return true;
        }
        if (invalidReflexive(predicate, subject)) {
            return true;
        }
        if ((subject instanceof Statement) && (predicate instanceof Statement)) {
            final Statement s1 = (Statement) subject;
            final Statement s2 = (Statement) predicate;
            final Term t11 = s1.getSubject();
            final Term t12 = s1.getPredicate();
            final Term t21 = s2.getSubject();
            final Term t22 = s2.getPredicate();
            if (t11.equals(t22) && t12.equals(t21)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if one term is identical to or included in another one, except in a
     * reflexive relation
     * <p>
     *
     * @param t1 The first term
     * @param t2 The second term
     * @return Whether they cannot be related in a statement
     */
    private static boolean invalidReflexive(Term t1, Term t2) {
        if (!(t1 instanceof CompoundTerm)) {
            return false;
        }
        final CompoundTerm com = (CompoundTerm) t1;
        if ((com instanceof ImageExt) || (com instanceof ImageInt)) {
            return false;
        }
        return com.containComponent(t2);
    }

    public static boolean invalidPair(String s1, String s2) {
        if (Variable.containVarI(s1) && !Variable.containVarI(s2)) {
            return true;
        } else if (!Variable.containVarI(s1) && Variable.containVarI(s2)) {
            return true;
        }
        return false;
    }

    /**
     * Check the validity of a potential Statement. [To be refined]
     * <p>
     * Minimum requirement: the two terms cannot be the same, or containing each
     * other as component
     *
     * @return Whether The Statement is invalid
     */
    public boolean invalid() {
        return invalidStatement(getSubject(), getPredicate());
    }

    /**
     * Return the first component of the statement
     *
     * @return The first component
     */
    public Term getSubject() {
        return components.get(0);
    }

    /**
     * Return the second component of the statement
     *
     * @return The second component
     */
    public Term getPredicate() {
        return components.get(1);
    }
}
