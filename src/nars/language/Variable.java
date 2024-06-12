package nars.language;

import nars.io.Symbols;

/**
 * A variable term, which does not correspond to a concept
 */
public class Variable extends Term {
    // TODO: 完全「数字编号化」尝试
    /**
     * Constructor, from a given variable name
     *
     * @param s A String of name without prefix
     */
    protected Variable(String s) {
        // * 🚩【2024-06-09 15:18:20】现在封闭构造入口
        super(s);
    }

    protected Variable(char type, long id) {
        super("" + type + id);
    }

    /**
     * Clone a Variable
     *
     * @return The cloned Variable
     */
    @Override
    public Variable clone() {
        return new Variable(name);
    }

    /**
     * Get the type of the variable
     *
     * @return The variable type
     */
    public char getType() {
        return name.charAt(0);
    }

    /**
     * A variable is not constant
     *
     * @return false
     */
    @Override
    public boolean isConstant() {
        return false;
    }

    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */
    @Override
    public int getComplexity() {
        return 0;
    }

    /**
     * Check whether a string represent a name of a term that contains an
     * independent variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains an independent variable
     */
    public static boolean containVarI(String n) {
        return n.indexOf(Symbols.VAR_INDEPENDENT) >= 0;
    }

    public static boolean containVarI(Term t) {
        return containVarI(t.getName());
    }

    /**
     * Check whether a string represent a name of a term that contains a
     * dependent variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a dependent variable
     */
    public static boolean containVarD(String n) {
        return n.indexOf(Symbols.VAR_DEPENDENT) >= 0;
    }

    public static boolean containVarD(Term t) {
        return containVarD(t.getName());
    }

    /**
     * Check whether a string represent a name of a term that contains a query
     * variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a query variable
     */
    public static boolean containVarQ(String n) {
        return n.indexOf(Symbols.VAR_QUERY) >= 0;
    }

    public static boolean containVarQ(Term t) {
        return containVarQ(t.getName());
    }

    /**
     * Check whether a string represent a name of a term that contains a
     * variable
     *
     * @param n The string name to be checked
     * @return Whether the name contains a variable
     */
    public static boolean containVar(String n) {
        return containVarI(n) || containVarD(n) || containVarQ(n);
    }

    public static boolean containVar(Term t) {
        return containVar(t.getName());
    }

    /**
     * variable terms are listed first alphabetically
     *
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     */
    @Override
    public final int compareTo(Term that) {
        return (that instanceof Variable) ? name.compareTo(that.getName()) : -1;
    }
}
