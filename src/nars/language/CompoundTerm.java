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

    /**
     * list of (direct) components
     */
    protected ArrayList<Term> components;
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
        this.components = components;
        calcComplexity();
        name = makeName();
        isConstant = !Variable.containVar(name);
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param name       Name of the compound
     * @param components Component list
     */
    protected CompoundTerm(String name, ArrayList<Term> components) {
        super(name);
        isConstant = !Variable.containVar(name);
        this.components = components;
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

    /**
     * Check CompoundTerm operator symbol
     *
     * @return if the given String is an operator symbol
     * @param s The String to be checked
     */
    public static boolean isOperator(String s) {
        if (s.length() == 1) {
            return (s.equals(Symbols.INTERSECTION_EXT_OPERATOR)
                    || s.equals(Symbols.INTERSECTION_INT_OPERATOR)
                    || s.equals(Symbols.DIFFERENCE_EXT_OPERATOR)
                    || s.equals(Symbols.DIFFERENCE_INT_OPERATOR)
                    || s.equals(Symbols.PRODUCT_OPERATOR)
                    || s.equals(Symbols.IMAGE_EXT_OPERATOR)
                    || s.equals(Symbols.IMAGE_INT_OPERATOR));
        }
        if (s.length() == 2) {
            return (s.equals(Symbols.NEGATION_OPERATOR)
                    || s.equals(Symbols.DISJUNCTION_OPERATOR)
                    || s.equals(Symbols.CONJUNCTION_OPERATOR));
        }
        return false;
    }

    /* ----- utilities for oldName ----- */
    /**
     * default method to make the oldName of the current term from existing
     * fields
     *
     * @return the oldName of the term
     */
    protected String makeName() {
        return makeCompoundName(operator(), components);
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
    public static String makeSetName(char opener, ArrayList<Term> arg, char closer) {
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
    public void setConstant(boolean isConstant) {
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
     *
     * @return The component list
     */
    public ArrayList<Term> getComponents() {
        return components;
    }

    /**
     * Clone the component list
     *
     * @return The cloned component list
     */
    public ArrayList<Term> cloneComponents() {
        return cloneList(components);
    }

    /**
     * Deep clone an array list of terms
     *
     * @param original The original component list
     * @return an identical and separate copy of the list
     */
    public static ArrayList<Term> cloneList(ArrayList<Term> original) {
        if (original == null) {
            return null;
        }
        ArrayList<Term> arr = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) {
            arr.add((Term) ((Term) original.get(i)).clone());
        }
        return arr;
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
        if (getClass() == t.getClass()) {
            return components.containsAll(((CompoundTerm) t).getComponents());
        } else {
            return components.contains(t);
        }
    }

    /* ----- variable-related utilities ----- */
    /**
     * Whether this compound term contains any variable term
     *
     * @return Whether the name contains a variable
     */
    public boolean containVar() {
        return Variable.containVar(name);
    }

    /**
     * Rename the variables in the compound, called from Sentence constructors
     */
    @Override
    public void renameVariables() {
        // TODO: 过程笔记注释
        if (containVar()) {
            renameVariables(new HashMap<Variable, Variable>());
        }
        setConstant(true);
        setName(makeName());
    }

    /**
     * Recursively rename the variables in the compound
     *
     * @param map The substitution established so far
     */
    private void renameVariables(HashMap<Variable, Variable> map) {
        // TODO: 过程笔记注释
        if (containVar()) {
            for (int i = 0; i < components.size(); i++) {
                Term term = componentAt(i);
                if (term instanceof Variable) {
                    Variable var;
                    if (term.getName().length() == 1) { // anonymous variable from input
                        var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                    } else {
                        var = (Variable) map.get((Variable) term);
                        if (var == null) {
                            var = new Variable(term.getName().charAt(0) + "" + (map.size() + 1));
                        }
                    }
                    if (!term.equals(var)) {
                        components.set(i, var);
                    }
                    map.put((Variable) term, var);
                } else if (term instanceof CompoundTerm) {
                    ((CompoundTerm) term).renameVariables(map);
                    ((CompoundTerm) term).setName(((CompoundTerm) term).makeName());
                }
            }
        }
    }

    /**
     * Recursively apply a substitute to the current CompoundTerm
     *
     * @param subs
     */
    public void applySubstitute(HashMap<Term, Term> subs) {
        // TODO: 过程笔记注释
        Term t1, t2;
        for (int i = 0; i < size(); i++) {
            t1 = componentAt(i);
            if (subs.containsKey(t1)) {
                t2 = subs.get(t1);
                while (subs.containsKey(t2)) {
                    t2 = subs.get(t2);
                }
                components.set(i, t2.clone());
            } else if (t1 instanceof CompoundTerm) {
                ((CompoundTerm) t1).applySubstitute(subs);
            }
        }
        if (this.isCommutative()) { // re-order
            TreeSet<Term> s = new TreeSet<>(components);
            components = new ArrayList<>(s);
        }
        name = makeName();
    }
}
