package nars.language;

import java.util.*;

import nars.io.Symbols;

/**
 * A CompoundTerm is a Term with internal (syntactic) structure
 * <p>
 * A CompoundTerm consists of a term operator with one or more component Terms.
 * <p>
 * This abstract class contains default methods for all CompoundTerms.
 *
 * * 📝【2024-06-12 14:41:42】本质上「词项」是一个【可共享】的【写时复制(COW)】类型
 * * * 写时复制：所有属性只读，要写属性时拷贝一个写后的元素
 */
public abstract class CompoundTerm extends Term {

    /**
     * 🆕Java数组缺少很多ArrayList所用到的方法，此处一一补上实现
     */
    public static abstract class ArrayUtils {

        /**
         * Deep clone an array list of terms
         *
         * @param original [&] The original component list
         * @return [] an identical and separate copy of the list
         */
        public static final ArrayList<Term> cloneList(Term[] original) {
            if (original == null)
                return null;
            // * 🚩深拷贝数组
            final ArrayList<Term> arr = new ArrayList<>(original.length);
            for (int i = 0; i < original.length; i++) {
                arr.add(i, (Term) original[i].clone());
            }
            return arr;
        }

        /**
         * Deep clone an array list of terms
         *
         * @param original [&]
         * @return []
         */
        public static final Term[] cloneArray(Term[] original) {
            if (original == null)
                return null;
            // * 🚩深拷贝数组
            final Term[] arr = new Term[original.length];
            for (int i = 0; i < original.length; i++) {
                arr[i] = (Term) original[i].clone();
            }
            return arr;
        }

        /**
         * 🆕动态数组→静态数组
         *
         * @param terms []
         * @return []
         */
        public static Term[] arrayFromList(ArrayList<Term> terms) {
            final Term[] lockedTerms = new Term[terms.size()];
            for (int i = 0; i < terms.size(); i++) {
                lockedTerms[i] = terms.get(i);
            }
            return lockedTerms;
        }

        /**
         * 🆕静态数组→动态数组
         *
         * @param terms []
         * @return []
         */
        public static ArrayList<Term> listFromArray(Term[] terms) {
            final ArrayList<Term> list = new ArrayList<>(terms.length);
            for (int i = 0; i < terms.length; i++) {
                list.add(i, terms[i]);
            }
            return list;
            // ! ❌不能用：ArrayList<Object>不能直接转换成ArrayList<Term>
            // return (ArrayList<Term>) Arrays.asList(terms);
        }

        /**
         * 🆕静态数组→动态数组（引用）
         * * 🎯用于下方「移除所有词项」
         *
         * @param terms []
         * @return [&]
         */
        public static ArrayList<Term> refListFromArray(Term[] terms) {
            return listFromArray(terms);
        }

        /**
         * 🆕从（一个词项的）元素列表中删除一个词项
         *
         * @param terms [&m]
         * @param term  [&]
         * @return [] 是否移除成功
         */
        public static boolean removeAll(
                final boolean isSameType,
                ArrayList<Term> terms,
                final Term term) {
            return isSameType
                    // * 🚩同类⇒删除term内所有元素
                    ? terms.removeAll(refListFromArray(((CompoundTerm) term).getComponents()))
                    // * 🚩默认⇒删除term（若含）
                    : terms.remove(term);
        }

        /**
         * 🆕复刻ArrayList.indexOf
         *
         * @param terms [&]
         * @param term  [&]
         * @return [] index or -1
         */
        public static int indexOf(Term[] terms, Term term) {
            // * 📃迁移自ArrayList<T>实现
            for (int i = 0; i < terms.length; i++)
                if (terms[i].equals(term))
                    return i;
            return -1;
            // return listFromArray(terms).indexOf(term);
        }

        /**
         * 🆕复刻ArrayList.contains
         *
         * @param terms [&]
         * @param term  [&]
         * @return []
         */
        public static boolean contains(Term[] terms, Term term) {
            return indexOf(terms, term) >= 0;
        }

        /**
         * 🆕检查是否包含另一集合的所有元素
         */
        public static boolean containsAll(Term[] terms, Term[] otherTerms) {
            // * 📃迁移自ArrayList<T>实现
            for (Term e : otherTerms)
                if (!contains(terms, e))
                    return false;
            return true;
            // return listFromArray(terms).containsAll(otherTerms);
        }

        public static ArrayList<Term> sortedList(Term[] terms) {
            final TreeSet<Term> s = new TreeSet<>(ArrayUtils.listFromArray(terms));
            return new ArrayList<>(s);
        }
    }

    /**
     * list of (direct) components
     */ // ! ⚠️【2024-06-12 11:28:56】目前重排需要设置值 TODO: 解除限制
    protected Term[] components; // TODO: 断言验证「构造后长度不可变」
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
        this.components = ArrayUtils.arrayFromList(components);
        this.isConstant = isConstant;
        this.complexity = complexity;
    }

    /**
     * Constructor called from subclasses constructors to initialize the fields
     *
     * @param components Component list
     */
    protected CompoundTerm(ArrayList<Term> components) {
        this.components = ArrayUtils.arrayFromList(components);
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
        this.components = ArrayUtils.arrayFromList(components);
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
    public static String makeCompoundName(String op, Term[] arg) {
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
    public static String makeSetName(char opener, Term[] arg, char closer) {
        StringBuilder name = new StringBuilder();
        name.append(opener);
        name.append(arg[0].getName());
        for (int i = 1; i < arg.length; i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            name.append(arg[i].getName());
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

    /** 🆕Java的ArrayList<T>和T[]不互通，非要复制两个方法出来 */
    public static String makeImageName(String op, Term[] arg, int relationIndex) {
        StringBuilder name = new StringBuilder();
        name.append(Symbols.COMPOUND_TERM_OPENER);
        name.append(op);
        name.append(Symbols.ARGUMENT_SEPARATOR);
        name.append(arg[relationIndex].getName());
        for (int i = 0; i < arg.length; i++) {
            name.append(Symbols.ARGUMENT_SEPARATOR);
            if (i == relationIndex) {
                name.append(Symbols.IMAGE_PLACE_HOLDER);
            } else {
                name.append(arg[i].getName());
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
     * @param &this
     * @return [] the size of the component list
     */
    public int size() {
        return components.length;
    }

    /**
     * get a component by index
     *
     * @param &this
     * @param i     [] index of the component
     * @return [&] the component
     */
    public Term componentAt(int i) {
        return components[i];
    }

    /**
     * 🆕根据指定元素找到对应索引位置
     *
     * @param &this
     * @param component [&]
     * @return [] index or -1
     */
    public int indexOfComponent(Term component) {
        return ArrayUtils.indexOf(this.components, component);
    }

    /**
     * Get the component list
     *
     * @return The component list
     */
    public Term[] getComponents() {
        return components;
    }

    /**
     * Clone the component list
     *
     * @return The cloned component list
     */
    public ArrayList<Term> cloneComponents() {
        return ArrayUtils.cloneList(this.components);
    }

    /**
     * Check whether the compound contains a certain component
     *
     * @param t The component to be checked
     * @return Whether the component is in the compound
     */
    public boolean containComponent(Term t) {
        return this.indexOfComponent(t) >= 0;
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
                // * 🚩类型相同⇒判断元素是否包含于
                ? ArrayUtils.containsAll(this.components, ((CompoundTerm) t).getComponents())
                // * 🚩类型不同⇒判断是否属于
                : ArrayUtils.contains(components, t);
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
        // * 🚩有变量⇒重命名变量
        if (this.containVar())
            renameCompoundVariables(this, new HashMap<Variable, Variable>());
        // * 🚩设置「为常量」
        // ? ❓【2024-06-09 13:26:43】为何要如此？
        setConstant(true);
        // * 🚩重新生成名称
        setName(makeName());
    }

    /**
     * Recursively rename the variables in the compound
     *
     * @param map The substitution established so far
     */
    private static void renameCompoundVariables(
            CompoundTerm self,
            HashMap<Variable, Variable> map) {
        // * 🚩没有变量⇒返回
        // ? 💭【2024-06-09 13:33:08】似乎对实际逻辑无用
        if (!self.containVar())
            return;
        // * 🚩只有「包含变量」才要继续重命名
        for (int i = 0; i < self.components.length; i++) {
            // * 🚩取变量词项
            final Term inner = self.componentAt(i);
            // * 🚩是「变量」词项⇒重命名
            if (inner instanceof Variable) {
                final Variable innerV = (Variable) inner;
                // * 🚩构造新编号与名称 | 采用顺序编号
                // * 📄类型相同，名称改变
                final int newVarNum = map.size() + 1;
                final String newName = innerV.getType() + "" + newVarNum;
                final boolean isAnonymousVariableFromInput = inner.getName().length() == 1;
                // * 🚩决定将产生的「新变量」
                final Variable newV =
                        // * 🚩用户输入的匿名变量 || 映射表中没有变量 ⇒ 新建变量
                        isAnonymousVariableFromInput || !map.containsKey(innerV)
                                // anonymous variable from input
                                ? new Variable(newName)
                                // * 🚩否则（非匿名 && 映射表中有） ⇒ 使用已有变量
                                : map.get(innerV);
                // * 🚩真正逻辑：替换变量词项
                // * 📌【2024-06-09 13:55:13】修改逻辑：只有「不等于」时才设置变量
                if (!inner.equals(newV)) {
                    self.components[i] = newV;
                }
                // * 🚩将该变量记录在映射表中
                // * ⚠️即便相等也要记录 | 影响的测试：NAL 6.20,6.21
                map.put(innerV, newV);
            }
            // * 🚩复合词项⇒继续递归深入
            // * 📌逻辑统一：无论是「序列」「集合」还是「陈述」都是这一套逻辑
            else if (inner instanceof CompoundTerm) {
                final CompoundTerm innerC = (CompoundTerm) inner;
                // * 🚩重命名内层复合词项
                renameCompoundVariables(innerC, map);
                // * 🚩重命名变量后生成名称
                innerC.setName(innerC.makeName());
            }
        }
    }

    /**
     * Recursively apply a substitute to the current CompoundTerm
     *
     * @param subs
     */
    public void applySubstitute(final HashMap<Term, Term> subs) {
        applySubstitute(this, subs);
    }

    /** 📌静态方法形式 */
    public static void applySubstitute(CompoundTerm self, final HashMap<Term, Term> subs) {
        // * 🚩遍历替换内部所有元素
        for (int i = 0; i < self.size(); i++) {
            final Term inner = self.componentAt(i);
            // * 🚩若有「替换方案」⇒替换
            if (subs.containsKey(inner)) {
                // * ⚠️此处的「被替换词项」可能不是「变量词项」
                // * 📄NAL-6变量引入时会建立「临时共同变量」匿名词项，以替换非变量词项
                // * 🚩一路追溯到「没有再被传递性替换」的词项（最终点）
                final Term substituteT = chainGet(subs, inner);
                // * 🚩复制并替换元素
                final Term substitute = substituteT.clone();
                self.components[i] = substitute;
            }
            // * 🚩复合词项⇒递归深入
            else if (inner instanceof CompoundTerm) {
                applySubstitute((CompoundTerm) inner, subs);
            }
        }
        // * 🚩可交换⇒替换之后重排顺序
        if (self.isCommutative()) // re-order
            self.reorderComponents();
        // * 🚩重新生成名称
        self.name = self.makeName();
    }

    /**
     * 层级获取「变量替换」最终点
     * * 🚩一路查找到头
     * * 📄{A -> B, B -> C} + A => C
     */
    private static <T> T chainGet(final HashMap<T, T> map, final T startPoint) {
        // * ⚠️此时应该传入非空值
        // * 🚩从「起始点」开始查找
        T endPoint = map.get(startPoint);
        // * 🚩非空⇒一直溯源
        while (map.containsKey(endPoint)) {
            endPoint = map.get(endPoint);
            if (endPoint == startPoint)
                throw new Error("不应有「循环替换」的情况");
        }
        return endPoint;
    }

    /** 🆕对于「可交换词项」重排其中的元素 */
    private void reorderComponents() {
        if (this.size() < 2)
            return;
        final Term[] newC = ArrayUtils.arrayFromList(ArrayUtils.sortedList(this.components));
        this.components = newC;
    }
}
