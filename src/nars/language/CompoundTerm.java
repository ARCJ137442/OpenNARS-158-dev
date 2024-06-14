package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A CompoundTerm is a Term with internal (syntactic) structure
 * <p>
 * A CompoundTerm consists of a term operator with one or more component Terms.
 * <p>
 * This abstract class contains default methods for all CompoundTerms.
 */
public abstract class CompoundTerm extends Term {

    /** 🆕Narsese的「词项」是创建后定长且部分可写的数组 */
    public static class TermComponents extends FixedSizeArray<Term> {
        public TermComponents(ArrayList<Term> list) {
            super(list);
        }

        public Term setTerm(int index, Term term) {
            return this.__set(index, term);
        }

        // 深拷贝 //

        /**
         * Deep clone an array list of terms
         *
         * @param &this
         * @return an identical and separate copy of the list
         */
        public TermComponents deepClone() {
            ArrayList<Term> arr = new ArrayList<>(this.size());
            for (int i = 0; i < this.size(); i++) {
                arr.add((Term) ((Term) this.get(i)).clone());
            }
            return new TermComponents(arr);
        }
    }

    /**
     * list of (direct) components
     */
    protected TermComponents components;
    /**
     * syntactic complexity of the compound, the sum of those of its components
     * plus 1
     */
    protected short complexity;
    /**
     * Whether the term names a concept
     */
    protected boolean isConstant = true;

    /* ----- abstract methods to be implemented in subclasses ----- */
    /**
     * Abstract method to get the operator of the compound
     * * ❌【2024-06-01 11:34:39】不能改为静态方法：不允许静态抽象方法，并且此类中调用只会指向该方法（即便用「未实现错误」）
     *
     * @return The operator in a String
     */
    public abstract String operator();

    /**
     * Abstract clone method
     *
     * @return A clone of the compound term
     */
    @Override
    public abstract CompoundTerm clone();

    /* ----- object builders, called from subclasses ----- */
    /**
     * Constructor called from subclasses constructors to clone the fields
     *
     * @param name       Name
     * @param components Component list
     * @param isConstant Whether the term refers to a concept
     * @param complexity Complexity of the compound term
     */
    protected CompoundTerm(String name, ArrayList<Term> components, boolean isConstant, short complexity) {
        this(name, new TermComponents(components), isConstant, complexity);
    }

    protected CompoundTerm(String name, TermComponents components, boolean isConstant, short complexity) {
        super(name);
        this.components = components;
        this.isConstant = isConstant;
        this.complexity = complexity;
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param components Component list
     */
    protected CompoundTerm(ArrayList<Term> components) {
        this.components = new TermComponents(components);
        calcComplexity();
        name = makeName();
        isConstant = !Variable.containVar(this);
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param name       Name of the compound
     * @param components Component list
     */
    protected CompoundTerm(String name, ArrayList<Term> components) {
        super(name);
        isConstant = !Variable.containVar(this);
        this.components = new TermComponents(components);
        calcComplexity();
    }

    /**
     * Change the oldName of a CompoundTerm, called after variable substitution
     *
     * @param s The new oldName
     */
    protected void setName(String s) {
        name = s;
    }

    /**
     * The complexity of the term is the sum of those of the components plus 1
     */
    private void calcComplexity() {
        complexity = 1;
        for (Term t : components) {
            complexity += t.getComplexity();
        }
    }

    @Override
    public boolean equals(Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.components);
        return hash;
    }

    /**
     * Orders among terms: variable < atomic < compound
     *
     * @param that The Term to be compared with the current Term
     * @return The same as compareTo as defined on Strings
     * @return The order of the two terms
     */
    @Override
    public int compareTo(final Term that) {
        if (!(that instanceof CompoundTerm)) {
            return 1;
        }
        final CompoundTerm t = (CompoundTerm) that;
        int diff = size() - t.size();
        if (diff != 0) {
            return diff;
        }
        diff = this.operator().compareTo(t.operator());
        if (diff != 0) {
            return diff;
        }
        for (int i = 0; i < size(); i++) {
            diff = componentAt(i).compareTo(t.componentAt(i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    /* ----- utilities for oldName ----- */
    /**
     * default method to make the oldName of the current term from existing
     * fields
     *
     * @return the oldName of the term
     */
    protected String makeName() {
        return makeCompoundName(operator(), getComponents());
    }

    /**
     * default method to make the oldName of a compound term from given fields
     *
     * @param op  the term operator
     * @param arg the list of components
     * @return the oldName of the term
     */
    public static String makeCompoundName(String op, ArrayList<Term> arg) {
        StringBuilder name = new StringBuilder();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        for (Term t : arg) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (t instanceof CompoundTerm) {
                ((CompoundTerm) t).setName(((CompoundTerm) t).makeName());
            }
            name.append(t.getName());
        }
        name.append(Symbols.COMPOUND_TERM_CLOSER);
        return name.toString();
    }

    /**
     * make the oldName of an ExtensionSet or IntensionSet
     *
     * @param opener the set opener
     * @param closer the set closer
     * @param arg    the list of components
     * @return the oldName of the term
     */
    public static String makeSetName(char opener, TermComponents arg, char closer) {
        StringBuilder name = new StringBuilder();
        name.append(opener);
        name.append(arg.get(0).getName());
        for (int i = 1; i < arg.size(); i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            name.append(arg.get(i).getName());
        }
        name.append(closer);
        return name.toString();
    }

    /**
     * default method to make the oldName of an image term from given fields
     *
     * @param op            the term operator
     * @param arg           the list of components
     * @param relationIndex the location of the place holder
     * @return the oldName of the term
     */
    public static String makeImageName(String op, ArrayList<Term> arg, int relationIndex) {
        return makeImageName(op, new TermComponents(arg), relationIndex);
    }

    public static String makeImageName(String op, TermComponents arg, int relationIndex) {
        StringBuilder name = new StringBuilder();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        name.append(Symbols.ARGUMENT_SEPARATOR);
        name.append(arg.get(relationIndex).getName());
        for (int i = 0; i < arg.size(); i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (i == relationIndex) {
                name.append(Symbols.IMAGE_PLACE_HOLDER);
            } else {
                name.append(arg.get(i).getName());
            }
        }
        name.append(Symbols.COMPOUND_TERM_CLOSER);
        return name.toString();
    }

    /* ----- utilities for other fields ----- */
    /**
     * report the term's syntactic complexity
     *
     * @return the complexity value
     */
    @Override
    public int getComplexity() {
        return complexity;
    }

    /**
     * check if the term contains free variable
     *
     * @return if the term is a constant
     */
    @Override
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * Set the constant status
     *
     * @param isConstant
     */
    private void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * Check if the order of the components matters
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    public boolean isCommutative() {
        return false;
    }

    /* ----- extend Collection methods to component list ----- */
    /**
     * get the number of components
     *
     * @return the size of the component list
     */
    public int size() {
        return components.size();
    }

    /**
     * get a component by index
     *
     * @param i index of the component
     * @return the component
     */
    public Term componentAt(int i) {
        return components.get(i);
    }

    /**
     * Get the component list
     * * 🚩【2024-06-14 10:48:44】现在减少其可见性，不在包外使用
     *
     * @return The component list
     */
    ArrayList<Term> getComponents() {
        return this.components.asList();
    }

    /**
     * 🆕Get the index of a component
     *
     * @param t [&]
     * @return [] index or -1
     */
    public int indexOfComponent(Term t) {
        return this.components.indexOf(t);
    }

    /**
     * Clone the component list
     *
     * @return The cloned component list
     */
    public ArrayList<Term> cloneComponents() {
        return this.components.deepClone().toArrayList();
    }

    /**
     * Check whether the compound contains a certain component
     *
     * @param t The component to be checked
     * @return Whether the component is in the compound
     */
    public boolean containComponent(Term t) {
        return components.contains(t);
    }

    /**
     * Recursively check if a compound contains a term
     *
     * @param target The term to be searched
     * @return Whether the target is in the current term
     */
    @Override
    public boolean containTerm(Term target) {
        for (Term term : components) {
            if (term.containTerm(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the compound contains all components of another term, or
     * that term as a whole
     *
     * @param t The other term
     * @return Whether the components are all in the compound
     */
    public boolean containAllComponents(Term t) {
        if (this.isSameType(t)) {
            return components.containsAll(((CompoundTerm) t).getComponents());
        } else {
            return components.contains(t);
        }
    }

    /* ----- variable-related utilities ----- */

    public void setTermWhenDealingVariables(int index, Term term) {
        this.components.setTerm(index, term);
    }

    public void updateAfterRenameVariables() {
        // * 🚩设置「为常量」
        // ? ❓【2024-06-09 13:26:43】为何要如此？
        this.setConstant(true);
        // * 🚩更新名称
        this.updateNameAfterRenameVariables();
    }

    public void updateNameAfterRenameVariables() {
        // * 🚩重新生成名称
        this.setName(this.makeName());
    }

    /**
     * 🆕对于「可交换词项」重排其中的元素
     * * 🚩【2024-06-13 18:05:40】只在「应用替换」时用到
     */
    public void reorderComponents() {
        final TreeSet<Term> s = new TreeSet<>(this.components);
        this.components = new TermComponents(new ArrayList<>(s));
    }
}
