package nars.language;

import nars.io.Symbols;

/**
 * Term is the basic component of Narsese, and the object of processing in NARS.
 * <p>
 * A Term may have an associated Concept containing relations with other Terms.
 * It is not linked in the Term, because a Concept may be forgot while the Term
 * exists. Multiple objects may represent the same Term.
 */
public class Term implements Cloneable, Comparable<Term> {

    /**
     * A Term is identified uniquely by its name, a sequence of characters in a
     * given alphabet (ASCII or Unicode)
     */
    protected String name;

    /**
     * Default constructor that build an internal Term
     * * 📌【2024-06-01 11:52:19】目前还不能移除：在「复合词项」中需要「构造后获取连接词」因此（暂时）需要空的名称
     */
    protected Term() {
    }

    /**
     * Constructor with a given name
     *
     * @param name A String as the name of the Term
     */
    Term(String name) {
        this.name = name;
    }

    /**
     * Reporting the name of the current Term.
     *
     * @return The name of the term as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Make a new Term with the same name.
     *
     * @return The new Term
     */
    @Override
    public Term clone() {
        return new Term(name);
    }

    /**
     * Equal terms have identical name, though not necessarily the same
     * reference.
     *
     * @return Whether the two Terms are equal
     * @param that The Term to be compared with the current Term
     */
    @Override
    public boolean equals(Object that) {
        return (that instanceof Term) && name.equals(((Term) that).getName());
    }

    /**
     * Produce a hash code for the term
     *
     * @return An integer hash code
     */
    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 7);
    }

    /**
     * Check whether the current Term can name a Concept.
     *
     * @return A Term is constant by default
     */
    public boolean isConstant() {
        return true;
    }

    /**
     * 🆕是否为占位符
     * * 🚩判断自身名称是否以"_"开头
     * * 📝从「构造词项MakeTerm」抽象而来
     *
     * @return
     */
    public boolean isPlaceholder() {
        return this.getName().charAt(0) == Symbols.IMAGE_PLACE_HOLDER;
    }

    /**
     * The syntactic complexity, for constant atomic Term, is 1.
     *
     * @return The complexity of the term, an integer
     */
    public int getComplexity() {
        return 1;
    }

    /**
     * 🆕判断是否为「零复杂度」
     * * 🎯用于部分「除以复杂度」的函数
     */
    public boolean zeroComplexity() {
        return this.getComplexity() == 0;
    }

    /**
     * 🆕判断俩词项是否类型相同
     * * 🎯明确语义，封装内部「class判等」逻辑
     */
    public final boolean isSameType(final Term that) {
        return that.getClass() == this.getClass();
    }

    /**
     * Orders among terms: variable < atomic < compound
     *
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     */
    @Override
    public int compareTo(Term that) {
        if (that instanceof CompoundTerm) {
            return -1;
        } else if (that instanceof Variable) {
            return 1;
        } else {
            return name.compareTo(that.getName());
        }
    }

    /**
     * Recursively check if a compound contains a term
     *
     * @param target The term to be searched
     * @return Whether the two have the same content
     */
    public boolean containTerm(Term target) {
        return equals(target);
    }

    /**
     * The same as getName by default, used in display only.
     *
     * @return The name of the term as a String
     */
    @Override
    public final String toString() {
        return name;
    }
}
