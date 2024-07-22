package nars.language;

import nars.io.Symbols;

/**
 * A variable term, which does not correspond to a concept
 */
public class Variable extends Term {
    /**
     * Constructor, from a given variable name
     *
     * 🆕通过（作为字符的）类型和id构造「变量词项」
     * * 📌用于「MakeTerm」中构造三种常见变量
     *
     * @param type [] The type of the variable
     * @param id   [] The id of the variable
     */
    protected Variable(char type, long id) {
        super("" + type + id); // * ✅其「名称」由且只由「类型」和「编号」决定
        this.type = type;
        this.id = id;
    }

    /**
     * 🆕数字编号化结果：表示「独立变量/非独变量/查询变量/……」的「类型」标签
     */
    private char type;
    /** 🆕数字编号化结果：除「类型」外唯一标识的编码 */
    private long id;

    /**
     * 获取变量id
     * * 🎯用于「获取词项内最大变量id」
     */
    long getId() {
        return this.id;
    }

    /**
     * Clone a Variable
     *
     * @return The cloned Variable
     */
    @Override
    public Variable clone() {
        return new Variable(this.type, this.id);
    }

    /**
     * Get the type of the variable
     *
     * @return The variable type
     */
    public char getType() {
        return this.type;
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
     * Whether this compound term contains any variable term
     *
     * @return Whether the name contains a variable
     */
    public static boolean containVar(Term t) {
        return containVar(t.getName());
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
