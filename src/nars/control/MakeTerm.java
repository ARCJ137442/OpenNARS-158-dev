package nars.control;

import java.util.ArrayList;
import java.util.TreeSet;

import nars.io.Symbols;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Statement;
import nars.language.DifferenceExt;
import nars.language.DifferenceInt;
import nars.language.Disjunction;
import nars.language.Equivalence;
import nars.language.ImageExt;
import nars.language.ImageInt;
import nars.language.Implication;
import nars.language.Inheritance;
import nars.language.IntersectionExt;
import nars.language.IntersectionInt;
import nars.language.Negation;
import nars.language.Product;
import nars.language.SetExt;
import nars.language.SetInt;
import nars.language.Similarity;
import nars.language.Term;

/**
 * 所有原`nars.language`包的{@link Term}子类中有关`make`的静态方法
 * * 🎯解耦`nars.language`与`nars.storage`
 * * 📝实际上主要有两种功能：
 * * 1. 在创建前简化词项内容：如`(&&, A, (&&, B, C))` => `(&&, A, B, C)`
 * * 2. 根据「名称」在记忆区中寻找已有缓存，记忆区已有缓存⇒直接使用
 * * 📝【2024-05-30 08:51:04】此中对「记忆区」的访问均为只读访问：只需判断「是否已有概念」
 * * 🚩【2024-06-01 12:14:43】现在不再涉及与「记忆区」有关的功能：缓存并不会对推理功能造成显著影响
 */
public abstract class MakeTerm {

    /* CompoundTerm */

    /* static methods making new compounds, which may return null */
    /**
     * Try to make a compound term from a template and a list of components
     * * 📝基于一个「模板词项」与「元素」
     *
     * @param compound   The template
     * @param components The components
     * @return A compound term or null
     */
    public static Term makeCompoundTerm(CompoundTerm compound, ArrayList<Term> components) {
        if (compound instanceof ImageExt)
            // * 🚩外延像
            return makeImageExt(components, ((ImageExt) compound).getRelationIndex());
        else if (compound instanceof ImageInt)
            // * 🚩内涵像
            return makeImageInt(components, ((ImageInt) compound).getRelationIndex());
        else
            // * 🚩其它
            return makeCompoundTerm(compound.operator(), components);
    }

    /**
     * Try to make a compound term from an operator and a list of components
     * <p>
     * Called from StringParser
     * * 📝只会被解析器调用
     * * ⚠️结果可空
     *
     * @param op  Term operator
     * @param arg Component list
     * @return A compound term or null
     */
    public static Term makeCompoundTerm(String op, ArrayList<Term> arg) {
        // * 🚩从「连接词长度→连接词」分派，对「词项集」有特别安排
        switch (op.length()) {
            case 1:
                // * 🚩词项集对应"{"与"["
                if (op.charAt(0) == Symbols.SET_EXT_OPENER)
                    return makeSetExt(arg);
                if (op.charAt(0) == Symbols.SET_INT_OPENER)
                    return makeSetInt(arg);
                switch (op) {
                    case Symbols.INTERSECTION_EXT_OPERATOR:
                        return makeIntersectionExt(arg);
                    case Symbols.INTERSECTION_INT_OPERATOR:
                        return makeIntersectionInt(arg);
                    case Symbols.DIFFERENCE_EXT_OPERATOR:
                        return makeDifferenceExt(arg);
                    case Symbols.DIFFERENCE_INT_OPERATOR:
                        return makeDifferenceInt(arg);
                    case Symbols.PRODUCT_OPERATOR:
                        return makeProduct(arg);
                    case Symbols.IMAGE_EXT_OPERATOR:
                        return makeImageExt(arg);
                    case Symbols.IMAGE_INT_OPERATOR:
                        return makeImageInt(arg);
                    default:
                        return null;
                }
            case 2:
                switch (op) {
                    case Symbols.NEGATION_OPERATOR:
                        return makeNegation(arg);
                    case Symbols.DISJUNCTION_OPERATOR:
                        return makeDisjunction(arg);
                    case Symbols.CONJUNCTION_OPERATOR:
                        return makeConjunction(arg);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    // /**
    // * Try to add a component into a compound
    // * * 📝尝试增加复合词项的一个元素，
    // * * ⚠️返回**新增一个元素后的**【新】词项
    // * * 📌【2024-06-01 10:29:52】目前未发现有用到的地方
    // *
    // * @param t1 The compound
    // * @param t2 The component
    // * @return The new compound
    // */
    // public static Term addComponents(CompoundTerm t1, Term t2) {
    // if (t2 == null)
    // return t1;
    // final ArrayList<Term> list = t1.cloneComponents();
    // final boolean success;
    // if (t1.getClass() == t2.getClass())
    // success = list.addAll(((CompoundTerm) t2).getComponents());
    // else
    // success = list.add(t2);
    // return (success ? makeCompoundTerm(t1, list) : null);
    // }

    /**
     * Try to remove a component from a compound
     * * 🚩从复合词项中删去一个元素，或从同类复合词项中删除所有其内元素，然后尝试约简
     * * ⚠️结果可空
     *
     * @param t1 The compound
     * @param t2 The component
     * @return The new compound
     */
    public static Term reduceComponents(CompoundTerm t1, Term t2) {
        final boolean success;
        final ArrayList<Term> components = t1.cloneComponents();
        if (t1.getClass() == t2.getClass())
            // * 🚩同类⇒删除t2内所有元素
            success = components.removeAll(((CompoundTerm) t2).getComponents());
        else
            // * 🚩默认⇒删除t2（若含）
            success = components.remove(t2);
        if (!success)
            return null;
        // * 🚩删除成功⇒继续
        if (components.size() > 1) {
            // * 🚩元素数量>1⇒以t1为模板构造新词项
            return makeCompoundTerm(t1, components);
        } else if (components.size() == 1) {
            // * 🚩元素数量=1⇒尝试「集合约简」
            // * 📝「集合约简」：若为【只有一个元素】的「集合性操作」复合词项类型⇒语义上与其元素等价
            final boolean canExtract = t1 instanceof Conjunction || t1 instanceof Disjunction
                    || t1 instanceof IntersectionExt || t1 instanceof IntersectionInt
                    || t1 instanceof DifferenceExt || t1 instanceof DifferenceInt;
            if (canExtract)
                return components.get(0);
            // ? 为何对「不可约简」的其它复合词项无效，如 (*, A) 就会返回null
        }
        // * 🚩空集⇒始终失败
        return null;
    }

    /**
     * Try to replace a component in a compound at a given index by another one
     * * 🚩替换指定索引处的词项，始终返回替换后的新词项
     * * 🚩若要替换上的词项为空（⚠️t可空），则与「删除元素」等同
     * * ⚠️结果可空
     *
     * @param compound The compound
     * @param index    The location of replacement
     * @param t        The new component
     * @return The new compound
     */
    public static Term setComponent(CompoundTerm compound, int index, Term t) {
        // * 🚩在元素列表中删去词项
        final ArrayList<Term> list = compound.cloneComponents();
        list.remove(index);
        // * 🚩非空⇒替换
        if (t != null) {
            if (compound.getClass() == t.getClass()) {
                // * 🚩同类⇒所有元素并入 | (*, 1, a)[1] = (*, 2, 3) => (*, 1, 2, 3)
                final ArrayList<Term> list2 = ((CompoundTerm) t).cloneComponents();
                for (int i = 0; i < list2.size(); i++) {
                    list.add(index + i, list2.get(i));
                }
            } else {
                // * 🚩非同类⇒直接插入 | (&&, a, b)[1] = (||, b, c) => (&&, a, (||, b, c))
                list.add(index, t);
            }
        }
        // * 🚩以当前词项为模板构造新词项
        return makeCompoundTerm(compound, list);
    }

    /**
     * build a component list from two terms
     *
     * @param t1 the first component
     * @param t2 the second component
     * @return the component list
     */
    private static ArrayList<Term> argumentsToList(Term t1, Term t2) {
        final ArrayList<Term> list = new ArrayList<>(2);
        list.add(t1);
        list.add(t2);
        return list;
    }

    /* Statement */

    /**
     * Make a Statement from String, called by StringParser
     * * 🚩从字符串解析器中分派（系词+主谓项）
     * * ⚠️结果可空
     *
     * @param relation  The relation String
     * @param subject   The first component
     * @param predicate The second component
     * @return The Statement built
     */
    public static Statement makeStatement(String relation, Term subject, Term predicate) {
        // * 📌【2024-06-01 10:46:42】原则：不让`nars.language`依赖MakeTerm
        if (Statement.invalidStatement(subject, predicate))
            return null;
        // * 🚩根据陈述系词分派
        switch (relation) {
            case Symbols.INHERITANCE_RELATION:
                return makeInheritance(subject, predicate);
            case Symbols.SIMILARITY_RELATION:
                return makeSimilarity(subject, predicate);
            case Symbols.INSTANCE_RELATION:
                return makeInstance(subject, predicate);
            case Symbols.PROPERTY_RELATION:
                return makeProperty(subject, predicate);
            case Symbols.INSTANCE_PROPERTY_RELATION:
                return makeInstanceProperty(subject, predicate);
            case Symbols.IMPLICATION_RELATION:
                return makeImplication(subject, predicate);
            case Symbols.EQUIVALENCE_RELATION:
                return makeEquivalence(subject, predicate);
            default:
                return null;
        }
    }

    /**
     * Make a Statement from given components, called by the rules
     * * 🚩从现有的陈述模板中构造
     * * ⚠️结果可空
     *
     * @return The Statement built
     * @param subj      The first component
     * @param pred      The second component
     * @param statement A sample statement providing the class type
     */
    public static Statement makeStatement(Statement statement, Term subj, Term pred) {
        // * 🚩按四种基本系词构造
        if (statement instanceof Inheritance)
            return makeInheritance(subj, pred);
        if (statement instanceof Similarity)
            return makeSimilarity(subj, pred);
        if (statement instanceof Implication)
            return makeImplication(subj, pred);
        if (statement instanceof Equivalence)
            return makeEquivalence(subj, pred);
        return null;
    }

    /**
     * Make a symmetric Statement from given components and temporal
     * information, called by the rules
     * * ⚠️结果可空
     *
     * @param statement A sample asymmetric statement providing the class type
     * @param subj      The first component
     * @param pred      The second component
     * @return The Statement built
     */
    public static Statement makeStatementSym(Statement statement, Term subj, Term pred) {
        // * 🚩非对称陈述⇒对称陈述
        if (statement instanceof Inheritance)
            // * 🚩继承⇒相似
            return makeSimilarity(subj, pred);
        if (statement instanceof Implication)
            // * 🚩蕴含⇒等价
            return makeEquivalence(subj, pred);
        return null;
    }

    /* Conjunction */

    /**
     * Try to make a new compound from a list of components. Called by
     * StringParser.
     * * 🚩从字符串解析器中构造「合取」
     * * ⚠️结果可空
     *
     * @return the Term generated from the arguments
     * @param argList the list of arguments
     */
    public static Term makeConjunction(ArrayList<Term> argList) {
        final TreeSet<Term> set = new TreeSet<>(argList); // sort/merge arguments
        return makeConjunction(set);
    }

    /**
     * Try to make a new Disjunction from a set of components. Called by the
     * public make methods.
     * * 🚩从一个词项集合中构造「合取」
     * * ️📝是一个相对原始的方法：只考虑元素个数
     * * ⚠️结果可空
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    private static Term makeConjunction(TreeSet<Term> set) {
        // * 🚩不允许空集
        if (set.isEmpty())
            return null;
        // * 🚩单元素⇒直接用元素
        // special case: single component
        if (set.size() == 1)
            return set.first();
        // * 🚩将集合转换为数组，直接构造之
        final ArrayList<Term> argument = new ArrayList<>(set);
        return new Conjunction(argument);
    }

    // overload this method by term type?
    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 🚩从两个词项中构造一个「合取」，等同于(A, B) => (&&, A, B)的操作
     * * 📝在这些操作的过程中，元素会根据一些规则被约简
     * * ⚠️结果可空
     *
     * @param term1 The first component
     * @param term2 The second component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeConjunction(Term term1, Term term2) {
        // * 📝通过这个集合消除重复项 | 比对函数在Collection.class基于`Object.equals`方法，所以不会存在「按引用不按值」的情况
        final TreeSet<Term> set;
        // * 🚩同类合并 | 📝实际上可以用模式匹配
        final boolean containable1 = term1 instanceof Conjunction;
        final boolean containable2 = term2 instanceof Conjunction;
        if (containable1) {
            set = new TreeSet<>(((CompoundTerm) term1).cloneComponents());
            // (&&,P,Q) && (&&,R,S) = (&&,P,Q,R,S)
            if (containable2)
                set.addAll(((CompoundTerm) term2).cloneComponents());
            // (&&,P,Q) && R = (&&,P,Q,R)
            else
                set.add(term2.clone());
        } else if (containable2) {
            // (&&,R,(&&,P,Q)) = (&&,P,Q,R)
            set = new TreeSet<>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone());
        }
        // * 🚩否则：纯粹构造二元集
        else {
            // P && Q = (&&,P,Q)
            set = new TreeSet<>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        // * 🚩继续通过集合构建词项
        return makeConjunction(set);
    }

    /* DifferenceExt */

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     * * 🚩从解析器构造「外延差」
     * * ⚠️结果可空
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeDifferenceExt(ArrayList<Term> argList) {
        // * 🚩单个元素：约简为内部元素 | (-,A) = A
        if (argList.size() == 1) // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        // * 🚩太多元素/空集：构造失败 | (-,A,B,C) = null
        if (argList.size() != 2)
            return null;
        // * 🚩外延集的差：求差，构造外延集 | {A, B} - {A} = {B}
        if ((argList.get(0) instanceof SetExt) && (argList.get(1) instanceof SetExt)) {
            final ArrayList<Term> left = ((CompoundTerm) argList.get(0)).cloneComponents();
            final ArrayList<Term> right = ((CompoundTerm) argList.get(1)).cloneComponents();
            final TreeSet<Term> set = new TreeSet<Term>(left);
            set.removeAll(right); // set difference
            return makeSetExt(set);
        }
        // * 🚩否则：直接构造外延差 | A - B = (-,A,B)
        return new DifferenceExt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 🚩从推理规则构造外延差
     *
     * @param t1 The first component
     * @param t2 The second component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeDifferenceExt(Term t1, Term t2) {
        // * 🚩自己减自己⇒空集⇒null
        if (t1.equals(t2))
            return null;
        // * 🚩否则⇒直接从二元列表构造
        final ArrayList<Term> list = argumentsToList(t1, t2);
        return makeConjunction(list);
    }

    /* DifferenceInt */

    /**
     * Try to make a new DifferenceExt. Called by StringParser.
     * * 📝与「外延差」对应方法相似
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeDifferenceInt(ArrayList<Term> argList) {
        if (argList.size() == 1) // special case from CompoundTerm.reduceComponent
            return argList.get(0);
        if (argList.size() != 2)
            return null;
        if ((argList.get(0) instanceof SetInt) && (argList.get(1) instanceof SetInt)) {
            final TreeSet<Term> set = new TreeSet<Term>(((CompoundTerm) argList.get(0)).cloneComponents());
            set.removeAll(((CompoundTerm) argList.get(1)).cloneComponents()); // set difference
            return makeSetInt(set);
        }
        return new DifferenceInt(argList);
    }

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 📝与「外延差」对应方法相似
     *
     * @param t1 The first component
     * @param t2 The second component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeDifferenceInt(Term t1, Term t2) {
        if (t1.equals(t2))
            return null;
        final ArrayList<Term> list = argumentsToList(t1, t2);
        return makeDifferenceInt(list);
    }

    /* Disjunction */

    /**
     * Try to make a new Disjunction from two components. Called by the inference
     * rules.
     * * 📝与「合取」对应方法相似
     *
     * @param term1 The first component
     * @param term2 The first component
     * @return A Disjunction generated or a Term it reduced to
     */
    public static Term makeDisjunction(Term term1, Term term2) {
        final TreeSet<Term> set;
        if (term1 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof Disjunction) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (||,P,Q) || (||,R,S)) = (||,P,Q,R,S)
            else {
                set.add(term2.clone());
            } // (||,P,Q) || R = (||,P,Q,R)
        } else if (term2 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term2).cloneComponents());
            set.add(term1.clone()); // R || (||,P,Q) = (||,P,Q,R)
        } else {
            set = new TreeSet<>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeDisjunction(set);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     *
     * @param argList a list of Term as components
     * @return the Term generated from the arguments
     */
    public static Term makeDisjunction(ArrayList<Term> argList) {
        final TreeSet<Term> set = new TreeSet<>(argList); // sort/merge arguments
        return makeDisjunction(set);
    }

    /**
     * Try to make a new Disjunction from a set of components. Called by the public
     * make methods.
     * * 📝与「合取」对应方法相似
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    public static Term makeDisjunction(TreeSet<Term> set) {
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        final ArrayList<Term> argument = new ArrayList<>(set);
        return new Disjunction(argument);
    }

    /* Equivalence */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 🚩在推理时构造「等价」陈述
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Equivalence makeEquivalence(Term subject, Term predicate) {
        // to be extended to check if subject is Conjunction
        // * 🚩检查非法主谓组合
        if (subject instanceof Implication || subject instanceof Equivalence)
            return null; // ! <<A ==> B> <=> C>
        if (predicate instanceof Implication || predicate instanceof Equivalence)
            return null; // ! <C <=> <C ==> D>>
        if (Statement.invalidStatement(subject, predicate))
            return null; // ! <A <=> A>, <<A --> B> <=> <B --> A>>
        // * 🚩自动排序
        if (subject.compareTo(predicate) > 0) {
            final Term inner = subject;
            subject = predicate;
            predicate = inner;
        }
        // * 🚩构造
        final ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Equivalence(argument);
    }

    /* ImageExt */

    /**
     * Try to make a new ImageExt. Called by StringParser.
     * * 🚩从解析器构造外延像
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeImageExt(ArrayList<Term> argList) {
        // * 🚩拒绝元素过少的词项 | 第一个词项需要是「关系」，除此之外必须含有至少一个元素 & 占位符
        if (argList.size() < 2)
            return null;
        // * 🚩第一个词项是「关系」词项 | (/, R, a, _) 中的 R
        final Term relation = argList.get(0);
        final ArrayList<Term> argument = new ArrayList<Term>();
        // * 🚩开始填充「关系词项」
        int index = 0;
        for (int j = 1; j < argList.size(); j++) {
            // * 🚩在「占位符」的位置放置「关系」，以便节省存储空间
            // * 📄 (/, R, a, _) => Image { op: "/", arr: [a, R], r_index: 1 }
            if (argList.get(j).isPlaceholder()) {
                index = j - 1;
                argument.add(relation);
            } else {
                argument.add(argList.get(j));
            }
        }
        // * 🚩构造
        return makeImageExt(argument, (short) index);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference
     * rules.
     * * 🚩从「乘积」构造外延像
     * * 📄(*, A, B) --> R @ 0 = A --> (/, R, _, B)
     * * 📄{<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageExt(Product product, Term relation, short index) {
        // * 🚩关系词项是「乘积」⇒可能可以简化
        if (relation instanceof Product) {
            final Product p2 = (Product) relation;
            // * 🚩对「二元外延像」作特别的「取索引」简化
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                    // (/,(*,a,b),_,b) with [(*,a,b),b]#0
                    // is reduced to self[0][0] = (*,a,b)[0] = a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                    // (/,(*,a,b),a,_) with [a,(*,a,b)]#1
                    // is reduced to self[1][1] = (*,a,b)[1] = b
                    return p2.componentAt(1);
                }
            }
        }
        // * 🚩从「乘积」中设置「关系词项」（直接表示占位符位置），然后直接构造
        final ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return makeImageExt(argument, index);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the
     * inference rules.
     * * 🚩从一个已知的外延像中构造新外延像，并切换占位符的位置
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageExt(ImageExt oldImage, Term component, short index) {
        final ArrayList<Term> argList = oldImage.cloneComponents();
        final int oldIndex = oldImage.getRelationIndex();
        final Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return makeImageExt(argList, index);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 🚩预先构造好名称，然后传入类构造函数中（这样无需再创建名称）
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @return the Term generated from the arguments
     */
    private static Term makeImageExt(ArrayList<Term> argument, short index) {
        final String name = CompoundTerm.makeImageName(Symbols.IMAGE_EXT_OPERATOR, argument, index);
        return new ImageExt(name, argument, index);
    }

    /* ImageInt */

    /**
     * Try to make a new ImageExt. Called by StringParser.
     * * 📝与「外延像」对应方法相似
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeImageInt(ArrayList<Term> argList) {
        if (argList.size() < 2)
            return null;
        final Term relation = argList.get(0);
        final ArrayList<Term> argument = new ArrayList<Term>();
        int index = 0;
        for (int j = 1; j < argList.size(); j++) {
            if (argList.get(j).isPlaceholder()) {
                index = j - 1;
                argument.add(relation);
            } else {
                argument.add(argList.get(j));
            }
        }
        return makeImageInt(argument, (short) index);
    }

    /**
     * Try to make an Image from a Product and a relation. Called by the inference
     * rules.
     * * 📝与「外延像」对应方法相似
     *
     * @param product  The product
     * @param relation The relation
     * @param index    The index of the place-holder
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageInt(Product product, Term relation, short index) {
        if (relation instanceof Product) {
            final Product p2 = (Product) relation;
            if ((product.size() == 2) && (p2.size() == 2)) {
                if ((index == 0) && product.componentAt(1).equals(p2.componentAt(1))) {
                    // (\,_,(*,a,b),b) is reduced to a
                    return p2.componentAt(0);
                }
                if ((index == 1) && product.componentAt(0).equals(p2.componentAt(0))) {
                    // (\,(*,a,b),a,_) is reduced to b
                    return p2.componentAt(1);
                }
            }
        }
        final ArrayList<Term> argument = product.cloneComponents();
        argument.set(index, relation);
        return makeImageInt(argument, index);
    }

    /**
     * Try to make an Image from an existing Image and a component. Called by the
     * inference rules.
     * * 📝与「外延像」对应方法相似
     *
     * @param oldImage  The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image
     * @return A compound generated or a term it reduced to
     */
    public static Term makeImageInt(ImageInt oldImage, Term component, short index) {
        final ArrayList<Term> argList = oldImage.cloneComponents();
        final int oldIndex = oldImage.getRelationIndex();
        final Term relation = argList.get(oldIndex);
        argList.set(oldIndex, component);
        argList.set(index, relation);
        return makeImageInt(argList, index);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 📝与「外延像」对应方法相似
     *
     * @param argument The argument list
     * @param index    The index of the place-holder in the new Image
     * @return the Term generated from the arguments
     */
    private static Term makeImageInt(ArrayList<Term> argument, short index) {
        final String name = CompoundTerm.makeImageName(Symbols.IMAGE_INT_OPERATOR, argument, index);
        return new ImageInt(name, argument, index);
    }

    /* Implication */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or a term it reduced to
     */
    public static Implication makeImplication(Term subject, Term predicate) {
        // * 🚩检查有效性：任意元素为空⇒空 | 保证后续非空，并接受「自反性」等检验
        if (subject == null || predicate == null)
            return null;
        if (Statement.invalidStatement(subject, predicate))
            return null;
        // * 🚩检查主词类型
        if (subject instanceof Implication || subject instanceof Equivalence)
            // ! ❌ <<A ==> B> ==> C> | <<A <=> B> ==> C>
            return null;
        if (predicate instanceof Equivalence)
            // ! ❌ <A ==> <B <=> C>>
            return null;
        if (predicate instanceof Implication) {
            /** B in <A ==> <B ==> C>> */
            final Term oldCondition = ((Implication) predicate).getSubject();
            if (oldCondition instanceof Conjunction && ((Conjunction) oldCondition).containComponent(subject)) {
                // ! ❌ <A ==> <(&&, A, B) ==> C>>
                // ? ❓为何不能合并：实际上A && (&&, A, B) = (&&, A, B)
                return null;
            }
            // * ♻️ <A ==> <B ==> C>> ⇒ <(&&, A, B) ==> C>
            final Term newCondition = makeConjunction(subject, oldCondition);
            return makeImplication(newCondition, ((Implication) predicate).getPredicate());
        } else {
            final ArrayList<Term> argument = argumentsToList(subject, predicate);
            return new Implication(argument);
        }
    }

    /* Inheritance */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 📝此处只检查有效性（重言式、反推式，等等），无需做其它约简/检验
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Inheritance makeInheritance(Term subject, Term predicate) {
        // * 🚩检查有效性
        if (Statement.invalidStatement(subject, predicate))
            return null;
        // * 🚩直接构造
        final ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Inheritance(argument);
    }

    /*
     * Instance
     * A Statement about an Instance relation, which is used only in Narsese for
     * I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A {-- B becomes {A} --> B
     * * 📝实例 = {主项} --> 谓项
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Statement makeInstance(Term subject, Term predicate) {
        return makeInheritance(makeSetExt(subject), predicate);
    }

    /*
     * Property
     * A Statement about a Property relation, which is used only in Narsese for I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A --] B becomes A --> [B]
     * * 📝属性 = 主项 --> [谓项]
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Inheritance makeProperty(Term subject, Term predicate) {
        return makeInheritance(subject, makeSetInt(predicate));
    }

    /*
     * InstanceProperty
     *
     * A Statement about an InstanceProperty relation, which is used only in Narsese
     * for I/O,
     * and translated into Inheritance for internal use.
     */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * <p>
     * A {-] B becomes {A} --> [B]
     * * 📝实例属性 = {主项} --> [谓项]
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Statement makeInstanceProperty(Term subject, Term predicate) {
        return makeInheritance(makeSetExt(subject), makeSetInt(predicate));
    }

    /* IntersectionExt */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param term1 The first component
     * @param term2 The first component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeIntersectionExt(Term term1, Term term2) {
        final TreeSet<Term> set;
        final CompoundTerm s1, s2;
        // * 🚩两个内涵集取外延交 ⇒ 外延交=内涵并 ⇒ 取并集
        // * 📄[A,B] & [C,D] = [A,B,C,D]
        if (term1 instanceof SetInt && term2 instanceof SetInt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            set.addAll(s2.cloneComponents()); // set union
            return makeSetInt(set);
        }
        // * 🚩两个外延集取外延交 ⇒ 取交集
        // * 📄{A,B} & {B,C} = {B}
        else if (term1 instanceof SetExt && term2 instanceof SetExt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            set.retainAll(s2.cloneComponents()); // set intersection
            return makeSetExt(set);
        }
        // * 🚩左边是外延交 ⇒ 选择性取交集
        else if (term1 instanceof IntersectionExt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            // * 📄(&,P,Q) & (&,R,S) = (&,P,Q,R,S)
            if (term2 instanceof IntersectionExt)
                set.addAll(s2.cloneComponents());
            // * 📄(&,P,Q) & R = (&,P,Q,R)
            else
                set.add(term2.clone());
        }
        // * 🚩左边不是外延交，右边是外延交 ⇒ 直接并入到右边
        // * 📄R & (&,P,Q) = (&,P,Q,R)
        else if (term2 instanceof IntersectionExt) {
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s2.cloneComponents());
            set.add(term1.clone());
        }
        // * 🚩纯默认 ⇒ 直接添加
        // * 📄P & Q = (&,P,Q)
        else {
            set = new TreeSet<Term>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        // * 🚩构造
        return makeIntersectionExt(set);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     * * 🚩用户输入的集合不作操作
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeIntersectionExt(ArrayList<Term> argList) {
        // TODO: 后续有待验证可行性
        // if (argList.isEmpty())
        // return null;
        // // * 🚩做一个reduce的操作
        // Term term = argList.get(0).clone();
        // for (Term t : argList.subList(1, argList.size())) {
        // final Term new_term = makeIntersectionExt(term, t.clone());
        // term = new_term;
        // }
        // return term;
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeIntersectionExt(set);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 🚩只依照集合数量进行化简
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    private static Term makeIntersectionExt(TreeSet<Term> set) {
        // special case: single component
        // * 🚩单个元素⇒直接取元素
        // * 📄(&, A) = A
        if (set.size() == 1)
            return set.first();
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        return new IntersectionExt(argument);
    }

    /* IntersectionInt */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     * * 📝类似「外延交」对应方法，但一些地方是对偶的
     *
     * @param term1 The first component
     * @param term2 The first component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeIntersectionInt(Term term1, Term term2) {
        final TreeSet<Term> set;
        final CompoundTerm s1, s2;
        // * 🚩两个外延集取内涵交 ⇒ 内涵交=外延并 ⇒ 取并集
        // * 📄{A,B} | {C,D} = {A,B,C,D}
        if (term1 instanceof SetExt && term2 instanceof SetExt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            set.addAll(s2.cloneComponents()); // set union
            return makeSetExt(set);
        }
        // * 🚩两个内涵集取内涵交 ⇒ 取交集
        // * 📄[A,B] | [B,C] = [B]
        else if (term1 instanceof SetInt && term2 instanceof SetInt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            set.retainAll(s2.cloneComponents()); // set intersection
            return makeSetInt(set);
        }
        // * 🚩左边是内涵交 ⇒ 选择性取交集
        else if (term1 instanceof IntersectionInt) {
            s1 = (CompoundTerm) term1;
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s1.cloneComponents());
            // * 📄(|,P,Q) | (|,R,S) = (|,P,Q,R,S)
            if (term2 instanceof IntersectionInt)
                set.addAll(s2.cloneComponents());
            // * 📄(|,P,Q) | R = (|,P,Q,R)
            else
                set.add(term2.clone());
        }
        // * 🚩左边不是内涵交，右边是内涵交 ⇒ 直接并入到右边
        // * 📄R | (|,P,Q) = (|,P,Q,R)
        else if (term2 instanceof IntersectionInt) {
            s2 = (CompoundTerm) term2;
            set = new TreeSet<Term>(s2.cloneComponents());
            set.add(term1.clone());
        }
        // * 🚩纯默认 ⇒ 直接添加
        // * 📄P | Q = (|,P,Q)
        else {
            set = new TreeSet<Term>();
            set.add(term1.clone());
            set.add(term2.clone());
        }
        return makeIntersectionInt(set);
    }

    /**
     * Try to make a new IntersectionInt. Called by StringParser.
     * * 🚩用户输入的集合不作操作
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeIntersectionInt(ArrayList<Term> argList) {
        // TODO: 后续有待验证可行性
        // if (argList.isEmpty())
        // return null;
        // // * 🚩做一个reduce的操作
        // Term term = argList.get(0).clone();
        // for (Term t : argList.subList(1, argList.size())) {
        // final Term new_term = makeIntersectionInt(term, t.clone());
        // term = new_term;
        // }
        // return term;
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeIntersectionInt(set);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 🚩只依照集合数量进行化简
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    public static Term makeIntersectionInt(TreeSet<Term> set) {
        // special case: single component
        // * 🚩单个元素⇒直接取元素
        // * 📄(&, A) = A
        if (set.size() == 1)
            return set.first();
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        return new IntersectionInt(argument);
    }

    /* Negation */

    /**
     * Try to make a Negation of one component. Called by the inference rules.
     *
     * @param t The component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeNegation(Term t) {
        // * 🚩双重否定⇒肯定
        // * 📄-- (--,P) = P
        if (t instanceof Negation)
            return ((CompoundTerm) t).cloneComponents().get(0);
        final ArrayList<Term> argument = new ArrayList<>();
        argument.add(t);
        return makeNegation(argument);
    }

    /**
     * Try to make a new Negation. Called by StringParser.
     * * 🚩仅检查长度
     *
     * @return the Term generated from the arguments
     * @param argument The list of components
     */
    public static Term makeNegation(ArrayList<Term> argument) {
        if (argument.size() != 1)
            return null;
        return new Negation(argument);
    }

    /* Product */

    /**
     * Try to make a new compound. Called by StringParser.
     * * 🚩直接构造，无需检查内部参数
     *
     * @return the Term generated from the arguments
     * @param argument The list of components
     */
    public static Term makeProduct(ArrayList<Term> argument) {
        return new Product(argument);
    }

    /**
     * Try to make a Product from an ImageExt/ImageInt and a component. Called by
     * the inference rules.
     * * 🚩从「外延像/内涵像」构造，用某个词项替换掉指定索引处的元素
     * * 📝<a --> (/, R, _, b)> => <(*, a, b) --> R>，其中就要用 a 替换 [R,b] 中的R
     *
     * @param image     The existing Image
     * @param component The component to be added into the component list
     * @param index     The index of the place-holder in the new Image -- optional
     *                  parameter
     * @return A compound generated or a term it reduced to
     */
    public static Term makeProduct(CompoundTerm image, Term component, int index) {
        final ArrayList<Term> argument = image.cloneComponents();
        argument.set(index, component);
        return makeProduct(argument);
    }

    /* SetExt */

    /**
     * Try to make a new set from one component. Called by the inference rules.
     * * 🚩单个词项⇒直接从一元集构造
     *
     * @param t The component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeSetExt(Term t) {
        final TreeSet<Term> set = new TreeSet<Term>();
        set.add(t);
        return makeSetExt(set);
    }

    /**
     * Try to make a new SetExt. Called by StringParser.
     * * 🚩单个列表⇒转换为集合（此时去重&排序）
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeSetExt(ArrayList<Term> argList) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeSetExt(set);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 🚩单个集合⇒排序后数组⇒构造
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    public static Term makeSetExt(TreeSet<Term> set) {
        if (set.isEmpty())
            return null;
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        return new SetExt(argument);
    }

    /* SetInt */

    /**
     * Try to make a new set from one component. Called by the inference rules.
     * * 📝类似{@link MakeTerm#makeSetExt}的做法
     *
     * @param t The component
     * @return A compound generated or a term it reduced to
     */
    public static Term makeSetInt(Term t) {
        final TreeSet<Term> set = new TreeSet<Term>();
        set.add(t);
        return makeSetInt(set);
    }

    /**
     * Try to make a new SetInt. Called by StringParser.
     * * 📝类似{@link MakeTerm#makeSetExt}的做法
     *
     * @return the Term generated from the arguments
     * @param argList The list of components
     */
    public static Term makeSetInt(ArrayList<Term> argList) {
        final TreeSet<Term> set = new TreeSet<Term>(argList); // sort/merge arguments
        return makeSetInt(set);
    }

    /**
     * Try to make a new compound from a set of components. Called by the public
     * make methods.
     * * 📝类似{@link MakeTerm#makeSetExt}的做法
     *
     * @param set a set of Term as components
     * @return the Term generated from the arguments
     */
    public static Term makeSetInt(TreeSet<Term> set) {
        if (set.isEmpty())
            return null;
        final ArrayList<Term> argument = new ArrayList<Term>(set);
        return new SetInt(argument);
    }

    /* Similarity */

    /**
     * Try to make a new compound from two components. Called by the inference
     * rules.
     *
     * @param subject   The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    public static Similarity makeSimilarity(Term subject, Term predicate) {
        // * 🚩仅检查有效性
        if (Statement.invalidStatement(subject, predicate))
            return null;
        // * 🚩调整顺序（递归）
        if (subject.compareTo(predicate) > 0)
            return makeSimilarity(predicate, subject);
        // * 🚩从二元数组构造
        final ArrayList<Term> argument = argumentsToList(subject, predicate);
        return new Similarity(argument);
    }
}
