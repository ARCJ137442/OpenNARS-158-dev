package nars.inference;

import static nars.io.Symbols.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import nars.control.DerivationContext.DerivationContextCore;
import nars.language.CompoundTerm;
import nars.language.ImageExt;
import nars.language.ImageInt;
import nars.language.Term;
import nars.language.Variable;
import static nars.language.MakeTerm.*;

/**
 * 🆕所有跟「NAL-6 变量处理」相关的方法
 * * 🎯避免在专注「数据结构」的「词项」language包中放太多「逻辑推理」相关代码
 */
public abstract class VariableInference {

    // from CompoundTerm //

    /**
     * Blank method to be override in CompoundTerm
     * * 📝对原子词项（词语）而言，没什么可以「重命名」的
     */
    public static void renameVariables(Term term) {
        // * 🚩依据「是否为变量词项」分派
        if (term instanceof CompoundTerm) {
            final CompoundTerm c = (CompoundTerm) term;
            // * 🚩有变量⇒重命名变量
            if (c.containVar())
                renameCompoundVariables(c, new HashMap<Variable, Variable>());
            // * 🚩无论是否重命名，始终更新（内置则会影响推理结果）
            c.updateAfterRenameVariables();
        }
    }

    /**
     * Rename the variables in the compound, called from Sentence constructors
     */
    public static void renameVariables(CompoundTerm term) {
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
        for (int i = 0; i < self.size(); i++) {
            // * 🚩取变量词项
            final Term inner = self.componentAt(i);
            // * 🚩是「变量」词项⇒重命名
            if (inner instanceof Variable) {
                final Variable innerV = (Variable) inner;
                // * 🚩构造新编号与名称 | 采用顺序编号
                // * 📄类型相同，名称改变
                final int newVarNum = map.size() + 1;
                final long newId = newVarNum;
                final boolean isAnonymousVariableFromInput = inner.getName().length() == 1;
                // * 🚩决定将产生的「新变量」
                final Variable newV =
                        // * 🚩用户输入的匿名变量 || 映射表中没有变量 ⇒ 新建变量
                        isAnonymousVariableFromInput || !map.containsKey(innerV)
                                // anonymous variable from input
                                ? makeVarSimilar(innerV, newId)
                                // * 🚩否则（非匿名 && 映射表中有） ⇒ 使用已有变量
                                : map.get(innerV);
                // * 🚩真正逻辑：替换变量词项
                // * 📌【2024-06-09 13:55:13】修改逻辑：只有「不等于」时才设置变量
                if (!inner.equals(newV)) {
                    self.setTermWhenRenamingVariables(i, newV);
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
                innerC.updateNameAfterRenameVariables();
            }
        }
    }

    // from Variable //

    // ! 🚩【2024-06-09 14:19:35】弃用：目前无需用到
    // /**
    // * To unify two terms
    // * * ⚠️会改变词项自身
    // *
    // * @param type The type of variable that can be substituted
    // * @param t1 The first term
    // * @param t2 The second term
    // * @return Whether the unification is possible
    // */
    // private static boolean unify(char type, Term t1, Term t2) {
    // // * 🚩以词项自身开始
    // return unify(type, t1, t2, t1, t2);
    // }

    /**
     * To unify two terms
     * * ⚠️会改变词项自身
     *
     * @param type      The type of variable that can be substituted
     * @param t1        The first term to be unified
     * @param t2        The second term to be unified
     * @param compound1 The compound containing the first term
     * @param compound2 The compound containing the second term
     * @return Whether the unification is possible
     */
    private static boolean unify(final char type, Term t1, Term t2, Term compound1, Term compound2) {
        // * 🚩主逻辑：寻找替代
        final HashMap<Term, Term> map1 = new HashMap<>();
        final HashMap<Term, Term> map2 = new HashMap<>();
        final boolean hasSubs = findUnification(type, t1, t2, map1, map2); // find substitution
        // * 🚩有替代⇒应用替代
        if (hasSubs) {
            // * 🚩此时假定「有替代的一定是复合词项」
            // renameVar(map1, compound1, "-1");
            // renameVar(map2, compound2, "-2");
            applyUnifyOne((CompoundTerm) compound1, map1);
            applyUnifyOne((CompoundTerm) compound2, map2);
        }
        // * 🚩返回「是否替代成功」
        return hasSubs;
    }

    /** 🆕【对外接口】统一独立变量 */
    static boolean unifyI(Term t1, Term t2, Term compound1, Term compound2) {
        return unify(VAR_INDEPENDENT, t1, t2, compound1, compound2);
    }

    /** 🆕【对外接口】统一非独变量 */
    static boolean unifyD(Term t1, Term t2, Term compound1, Term compound2) {
        return unify(VAR_DEPENDENT, t1, t2, compound1, compound2);
    }

    /** 🆕【对外接口】统一查询变量 */
    static boolean unifyQ(Term t1, Term t2, Term compound1, Term compound2) {
        return unify(VAR_QUERY, t1, t2, compound1, compound2);
    }

    /** 🆕得出「替代结果」后，将映射表应用到词项上 */
    private static void applyUnifyOne(CompoundTerm compound, HashMap<Term, Term> map) {
        // * 🚩映射表非空⇒替换
        if (map.isEmpty())
            return;
        // * 🚩应用 & 重命名
        compound.applySubstitute(map);
        renameVariables(compound);
    }

    /**
     * 判断两个复合词项是否「容器相同」
     * * 🚩只判断有关「怎么包含词项」的信息，不判断具体内容
     */
    private static boolean isSameKindCompound(final CompoundTerm t1, final CompoundTerm t2) {
        // * 🚩容量大小不等⇒直接否决
        if (t1.size() != t2.size())
            return false;
        // * 🚩判断「像」的关系位置（占位符位置）
        final boolean differentImage =
                // * 🚩外延像
                (t1 instanceof ImageExt)
                        && (((ImageExt) t1).getRelationIndex() != ((ImageExt) t2).getRelationIndex())
                        || // * 🚩内涵像
                        (t1 instanceof ImageInt)
                                && (((ImageInt) t1).getRelationIndex() != ((ImageInt) t2).getRelationIndex());
        if (differentImage)
            return false;
        // * 🚩验证通过
        return true;
    }

    /**
     * To recursively find a substitution that can unify two Terms without
     * changing them
     * * ⚠️会修改两个映射表
     *
     * @param type  The type of Variable to be substituted
     * @param term1 The first Term to be unified
     * @param term2 The second Term to be unified
     * @param map1  The substitution for term1 formed so far
     * @param map2  The substitution for term2 formed so far
     * @return Whether there is a substitution that unifies the two Terms
     */
    private static boolean findUnification(
            final char type,
            final Term term1, final Term term2,
            HashMap<Term, Term> map1, HashMap<Term, Term> map2) {
        // * 🚩🆕预先计算好判据（及早求值）
        final boolean isCorrectVar1 = term1 instanceof Variable && ((Variable) term1).getType() == type;
        final boolean isCorrectVar2 = term2 instanceof Variable && ((Variable) term2).getType() == type;
        final boolean isSameTypeCompound = term1 instanceof CompoundTerm && term1.isSameType(term2);
        // * 🚩[$1 x ?] 对应位置是变量
        if (isCorrectVar1) {
            final Variable var1 = (Variable) term1;
            // * 🚩已有替换⇒直接使用已有替换（看子项有无替换） | 递归深入
            if (map1.containsKey(var1)) // already mapped
                return findUnification(type, map1.get(var1), term2, map1, map2);
            // * 🚩[$1 x $2] 若同为变量⇒统一二者（制作一个「共同变量」）
            if (isCorrectVar2) { // not mapped yet
                // * 🚩生成一个外界输入中不可能的变量词项作为「匿名变量」
                final Variable commonVar = new CommonVariable(term1, term2);
                // * 🚩建立映射：var1 -> commonVar @ term1
                // * 🚩建立映射：term2 -> commonVar @ term2
                map1.put(var1, commonVar); // unify
                map2.put(term2, commonVar); // unify
            }
            // * 🚩[$1 x _2] 若并非变量⇒尝试消元划归
            // * 📝此处意味「两个变量合并成一个变量」 | 后续「重命名变量」会将其消去
            else {
                // * 🚩建立映射：var1 -> term2 @ term1
                map1.put(var1, term2); // elimination
                // * 🚩尝试消除「共同变量」
                if (CommonVariable.is(var1))
                    // * 🚩建立映射：var1 -> term2 @ term2
                    map2.put(var1, term2);
            }
            return true;
        }
        // * 🚩[? x $2] 对应位置是变量
        else if (isCorrectVar2) {
            final Variable var2 = (Variable) term2;
            // * 🚩已有替换⇒直接使用已有替换（看子项有无替换） | 递归深入
            if (map2.containsKey(var2)) // already mapped
                return findUnification(type, term1, map2.get(var2), map1, map2);
            // not mapped yet
            // * 🚩[_1 x $2] 均非变量⇒尝试消元划归
            /*
             * 📝【2024-04-22 00:13:19】发生在如下场景：
             * <(&&, <A-->C>, <B-->$2>) ==> <C-->$2>>.
             * <(&&, <A-->$1>, <B-->D>) ==> <$1-->D>>.
             * <(&&, <A-->C>, <B-->D>) ==> <C-->D>>?
             * 📌要点：可能两边各有「需要被替换」的地方
             */
            // * 🚩建立映射：var2 -> term1 @ term2
            map2.put(var2, term1); // elimination
            // * 🚩尝试消除「共同变量」
            if (CommonVariable.is(var2))
                // * 🚩建立映射：var2 -> term1 @ term2
                map1.put(var2, term1);
            return true;
        }
        // * 🚩均非变量，但都是复合词项
        else if (isSameTypeCompound) {
            // * 🚩替换前提：容器相似（大小相同、像占位符位置相同）
            final CompoundTerm cTerm1 = (CompoundTerm) term1;
            final CompoundTerm cTerm2 = (CompoundTerm) term2;
            if (!isSameKindCompound(cTerm1, cTerm2))
                return false;
            // * 🚩复制词项列表 | 需要在「随机打乱」的同时不影响遍历
            final ArrayList<Term> list = cTerm1.cloneComponents();
            // * 🚩可交换⇒打乱 | 需要让算法（对两个词项）的时间复杂度为定值（O(n)而非O(n!)）
            if (cTerm1.isCommutative())
                Collections.shuffle(list, DerivationContextCore.randomNumber);
            // * 🚩逐个寻找替换
            for (int i = 0; i < cTerm1.size(); i++) { // assuming matching order
                final Term inner1 = list.get(i);
                final Term inner2 = cTerm2.componentAt(i);
                // * 🚩对每个子项寻找替换 | 复用已有映射表
                if (!findUnification(type, inner1, inner2, map1, map2))
                    return false;
            }
            return true;
        }
        // * 🚩其它原子词项
        return term1.equals(term2); // for atomic constant terms
    }

    /** 特别为「共同变量」创建一个类 */
    private static class CommonVariable extends Variable {

        CommonVariable(Term v1, Term v2) {
            // super('/', (long) ((v1.getName() + v2.getName() + '$').hashCode()));
            super(v1.getName() + v2.getName() + '$');
        }

        static boolean is(Term v) {
            // * 🚩判断这个词项是否是「匿名变量」
            // final String s = v.getName();
            // return s.charAt(s.length() - 1) == '$';
            return v instanceof CommonVariable;
        }
    }

    /**
     * Check if two terms can be unified
     *
     * @param type  The type of variable that can be substituted
     * @param term1 The first term to be unified
     * @param term2 The second term to be unified
     * @return Whether there is a substitution
     */
    public static boolean hasUnification(char type, Term term1, Term term2) {
        return findUnification(
                type,
                term1, term2,
                new HashMap<Term, Term>(), new HashMap<Term, Term>());
    }

    /**
     * Rename the variables to prepare for unification of two terms
     *
     * @param map    The substitution so far
     * @param term   The term to be processed
     * @param suffix The suffix that distinguish the variables in one premise
     *               from those from the other
     */
    // private static void renameVar(HashMap<Term, Term> map, Term term, String
    // suffix) {
    // if (term instanceof Variable) {
    // Term t = map.get(term);
    // if (t == null) { // new mapped yet
    // map.put(term, new Variable(term.getName() + suffix)); // rename
    // }
    // } else if (term instanceof CompoundTerm) {
    // for (Term t : ((CompoundTerm) term).components) { // assuming matching order,
    // to be refined in the future
    // renameVar(map, t, suffix);
    // }
    // }
    // }

}