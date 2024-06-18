package nars.language;

import static nars.io.Symbols.VAR_QUERY;

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

    // * ✅【2024-06-18 17:09:20】删掉已经是final的`complexity`字段：现在交给算法实时计算，牺牲性能以简化逻辑

    // ! 💥【2024-06-18 15:22:34】破坏性省去`isConstant`字段：仅影响「长期稳定性」的结果，不影响交叉测试

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
    protected CompoundTerm(String name, TermComponents components) {
        super(name);
        this.components = components;
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param components Component list
     */
    protected CompoundTerm(ArrayList<Term> components) {
        this.components = new TermComponents(components);
        this.name = makeName();
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param name       Name of the compound
     * @param components Component list
     */
    protected CompoundTerm(String name, ArrayList<Term> components) {
        super(name);
        this.components = new TermComponents(components);
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
    private short calcComplexity() {
        short complexity = 1;
        for (Term t : components) {
            complexity += t.getComplexity();
        }
        return complexity;
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
        return this.calcComplexity();
    }

    /**
     * check if the term contains free variable
     *
     * @return if the term is a constant
     */
    @Override
    public boolean isConstant() {
        return this.calcIsConstant();
    }

    private boolean calcIsConstant() {
        // * 🚩快速检验「是否有变量」
        if (!Variable.containVar(this)) {
            return true;
        }
        // * 🚩判断「是否有孤立的变量」
        // ! 💥【2024-06-18 03:48:44】会改变「长期稳定性」的结果
        return this.containsSoleVar();
    }

    /**
     * 🆕检验词项中是否包含「孤立非查询变量」
     * * 🎯判断是否可作为「常量词项」看待
     */
    private boolean containsSoleVar() {
        HashMap<Variable, Integer> nVar = new HashMap<>();
        calcVarCount(nVar, this);
        return verifyVarCount(nVar);
    }

    /** 🆕递归计算词项里各个变量词项（原子词项）的数目 */
    private static void calcVarCount(HashMap<Variable, Integer> nVar, Term current) {
        if (current instanceof Variable) {
            final Variable v = (Variable) current;
            if (nVar.containsKey(v)) {
                nVar.put(v, nVar.get(v) + 1);
            } else {
                nVar.put(v, 1);
            }
        } else if (current instanceof CompoundTerm) {
            for (final Term nextCurrent : ((CompoundTerm) current).components) {
                calcVarCount(nVar, nextCurrent);
            }
        }
    }

    /** 🆕检验其中是否有「孤立的非查询变量」 */
    private static boolean verifyVarCount(HashMap<Variable, Integer> nVar) {
        for (final Variable v : nVar.keySet()) {
            // * 🚩查询变量允许其单独作为一个「概念」：
            // * * 💭其所代表的「所问」往往是单独出现的
            // * * 📄"<{?who} --> murder>?"
            if (v.getType() == VAR_QUERY)
                continue;
            // * 🚩若全局只有一个变量⇒开放变量⇒非「常量词项」
            if (nVar.get(v) < 2)
                return false;
        }
        return true;
    }

    // ! ✅【2024-06-18 15:21:51】不再需要`setConstantTrue`
    // * * ℹ️详情参见`Sentence.java`

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
        return this.isSameType(t)
                // * 🚩同类⇒深入比较
                ? components.containsAll(((CompoundTerm) t).getComponents())
                // * 🚩异类⇒判断包含
                : components.contains(t);
    }

    /* ----- variable-related utilities ----- */

    /**
     * 🆕在变量处理中设置词项
     * * 🎯变量推理需要使用其方法
     *
     * @param &m-this
     * @param index   []
     * @param term    []
     */
    public void setTermWhenDealingVariables(int index, Term term) {
        this.components.setTerm(index, term);
    }

    /**
     * 重命名变量后，更新「是常量」与名称
     *
     * @param &m-this
     */
    public void updateAfterRenameVariables() {
        // * 🚩更新名称
        this.updateNameAfterRenameVariables();
    }

    public void updateNameAfterRenameVariables() {
        // * 🚩重新生成名称
        this.setName(this.makeName());
    }

    /**
     * 🆕对于「可交换词项」排序去重其中的元素
     * * 🚩【2024-06-13 18:05:40】只在「应用替换」时用到
     * * 🚩包含「排序」「去重」两个作用
     */
    public void reorderComponents() {
        // * 🚩将自身组分暂时移交所有权
        final ArrayList<Term> termsToReorder = this.components;
        // * 🚩对移交出来的词项数组重排去重
        final ArrayList<Term> newTerms = reorderTerms(termsToReorder);
        // * 🚩【2024-06-18 21:13:05】对于定长2的「陈述」不作处理：相同的时候不能简化
        if (this instanceof Statement && newTerms.size() < 2)
            return;
        // * 🚩基于整理好的词项数组，装填回自家类型
        this.components = new TermComponents(newTerms);
    }

    /**
     * 🆕重排去重给定的词项数组
     * * 🎯用于「变量替换到新词项」
     *
     * @param old [] 传入所有权
     */
    public static ArrayList<Term> reorderTerms(final ArrayList<Term> old) {
        final TreeSet<Term> s = new TreeSet<>(old);
        return new ArrayList<>(s);
    }
}
