package nars.language;

import static nars.io.Symbols.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import nars.control.DerivationContext.DerivationContextCore;

import static nars.language.MakeTerm.*;

/**
 * 🆕所有跟「NAL-6 变量处理」相关的方法
 * * 🎯避免在专注「数据结构」的「词项」language包中放太多「逻辑推理」相关代码
 */
public abstract class VariableProcess {

    // from CompoundTerm //

    /**
     * Recursively apply a substitute to the current CompoundTerm
     *
     * @param subs
     */
    public static void applySubstitute(Term self, final HashMap<Term, Term> subs) {
        // final Term original = self.clone();
        // final Term n = applySubstitute2New(self, subs, true);
        // * 🚩【2024-06-15 12:10:14】除了下边这一行，其它都是验证「跟函数式替换是否一致」的代码
        // * ✅【2024-06-15 12:10:54】目前验证结果：替换后不等⇔当且仅当替换后是空的——替换结果的无效性被提前揭露
        if (self instanceof CompoundTerm) // 只有复合词项能替换
            _applySubstitute((CompoundTerm) self, subs);
        // if (!((n == null) == !self.equals(n)))
        // throw new AssertionError("【2024-06-14 23:09:32】替换后不等 当且仅当替换后是空的！");
        // if (n == null)
        // System.err.println("新的替换后是空的！" + self + ", sub = " + subs);
        // if (!self.equals(n))
        // System.err.println("新旧替换不等！" + self + ", n = " + n + ", subs = " + subs);
    }

    private static void _applySubstitute(CompoundTerm self, final HashMap<Term, Term> subs) {
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
                self.setTermWhenDealingVariables(i, substitute);
            }
            // * 🚩复合词项⇒递归深入
            else if (inner instanceof CompoundTerm) {
                _applySubstitute((CompoundTerm) inner, subs);
            }
        }
        // * 🚩可交换⇒替换之后重排顺序
        if (self.isCommutative()) // re-order
            self.reorderComponents();
        // * 🚩重新生成名称
        self.updateNameAfterRenameVariables();
    }

    /**
     * 链式获取「变量替换」最终点
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
            if (endPoint.equals(startPoint))
                throw new Error("不应有「循环替换」的情况");
        }
        return endPoint;
    }

    /**
     * Blank method to be override in CompoundTerm
     * Rename the variables in the compound, called from Sentence constructors
     * * 📝对原子词项（词语）而言，没什么可以「重命名」的
     * * ❓其是否要作为「变量推理」的一部分，仍待存疑——需要内化成「语言」库自身提供的特性吗？
     * * * 诸多时候并非在「语言」中使用：解析器、语句构造 等
     */
    public static void renameVariables(Term term) {
        // * 🚩依据「是否为变量词项」分派
        if (term instanceof CompoundTerm) {
            final CompoundTerm c = (CompoundTerm) term;
            // * 🚩有变量⇒重命名变量
            if (Variable.containVar(c))
                // * ✅目前从「长期稳定性」中证明这俩等价（纯可变式🆚半函数式）
                // renameCompoundVariables(c, new HashMap<Variable, Variable>());
                renameCompoundVariables(c);
            // * 🚩无论是否重命名，始终更新（内置则会影响推理结果）
            c.updateAfterRenameVariables();
        }
    }

    /**
     * Recursively rename the variables in the compound
     *
     * @param map The substitution established so far
     */
    @SuppressWarnings("unused")
    private static void renameCompoundVariables(
            CompoundTerm self,
            HashMap<Variable, Variable> map) {
        // * 🚩没有变量⇒返回
        // ? 💭【2024-06-09 13:33:08】似乎对实际逻辑无用
        if (!Variable.containVar(self))
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
                // * 🚩此处特别区分「用户输入产生的匿名变量词项」亦即【只有类型是Variable，整体名称并未改变】的新变量词项
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
                    self.setTermWhenDealingVariables(i, newV);
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

    /**
     * @param type [] 要寻找的变量类型
     * @param t1   [&] 寻找所发生在的词项1
     * @param t2   [&] 寻找所发生在的词项2
     * @return [] 「归一替换」的词项映射表
     */
    private static Unification unifyFind(final char type, final Term t1, final Term t2) {
        // * 🚩主逻辑/寻找替代
        // * 📝仅在「当前词项」t1、t2中寻找替代
        final HashMap<Term, Term> map1 = new HashMap<>();
        final HashMap<Term, Term> map2 = new HashMap<>();
        final boolean hasSubs = findUnification(type, t1, t2, map1, map2); // find substitution
        return new Unification(hasSubs, map1, map2);
    }

    /** 🆕【对外接口】统一独立变量 */
    public static Unification unifyFindI(Term t1, Term t2) {
        return unifyFind(VAR_INDEPENDENT, t1, t2);
    }

    /** 🆕【对外接口】统一非独变量 */
    public static Unification unifyFindD(Term t1, Term t2) {
        return unifyFind(VAR_DEPENDENT, t1, t2);
    }

    /** 🆕【对外接口】统一查询变量 */
    public static Unification unifyFindQ(Term t1, Term t2) {
        return unifyFind(VAR_QUERY, t1, t2);
    }

    /** 多值输出：寻找「归一替换」的中间结果 */
    public static final class Unification {
        private final boolean hasUnification;
        /** 可变，因为要交出所有权 */
        private HashMap<Term, Term> unification1;
        /** 可变，因为要交出所有权 */
        private HashMap<Term, Term> unification2;

        Unification(boolean hasUnification, HashMap<Term, Term> unification1, HashMap<Term, Term> unification2) {
            this.hasUnification = hasUnification;
            this.unification1 = unification1;
            this.unification2 = unification2;
        }

        public boolean hasUnification() {
            return this.hasUnification;
            // return this.unification1.isEmpty() && this.unification2.isEmpty();
        }

        public HashMap<Term, Term> extractUnification1() {
            final HashMap<Term, Term> map = this.unification1;
            this.unification1 = null;
            return map;
        }

        public HashMap<Term, Term> extractUnification2() {
            final HashMap<Term, Term> map = this.unification2;
            this.unification2 = null;
            return map;
        }

        /**
         * 重定向到{@link VariableProcess#unifyApply}
         * * 🚩【2024-07-09 21:48:43】目前作为一个实用的「链式应用方法」用以替代公开的`unifyApply`
         *
         * @param this
         * @param parent1 [&m]
         * @param parent2 [&m]
         * @return
         */
        public boolean applyTo(CompoundTerm parent1, CompoundTerm parent2) {
            return VariableProcess.unifyApply(parent1, parent2, this);
        }
    }

    /**
     * 使用「统一结果」统一两个复合词项
     * * ⚠️会修改原有的复合词项
     *
     * @param parent1 [&m] 要被修改的复合词项1
     * @param parent2 [&m] 要被修改的复合词项2
     * @param result  [] 上一个「寻找归一映射」的结果
     */
    private static boolean unifyApply(CompoundTerm parent1, CompoundTerm parent2, Unification result) {
        // * 🚩主逻辑/应用替代
        // * 📝就是在这里修改了两个复合词项
        if (!result.hasUnification())
            return false;
        // * 🚩有替代⇒应用替代
        // * 🚩拿出里头生成的两个映射表
        final HashMap<Term, Term> map1 = result.extractUnification1();
        final HashMap<Term, Term> map2 = result.extractUnification2();
        // * 🚩此时假定「有替代的一定是复合词项」
        // renameVar(map1, compound1, "-1");
        // renameVar(map2, compound2, "-2");
        applyUnifyOne(parent1, map1);
        applyUnifyOne(parent2, map2);
        return result.hasUnification();
    }

    /**
     * 使用「统一结果」统一两个复合词项，并将结果保存到「统一结果」中去
     * * ⚠️不会修改原有的复合词项
     * ! ❌【2024-06-19 23:31:01】尽可能少用泛型：仅仅为了减少几个强制转换就大幅增加代码复杂度，不值得
     *
     * @param parent1 [&m] 要被修改的复合词项1
     * @param parent2 [&m] 要被修改的复合词项2
     * @param result  [] 上一个「寻找归一映射」的结果
     * @return [?] 替换后的结果（可能没有）
     */
    public static AppliedCompounds unifyApplied(CompoundTerm parent1, CompoundTerm parent2, Unification result) {
        // * 🚩主逻辑/应用替代
        // * 📝就是在这里修改了两个复合词项
        if (!result.hasUnification())
            return null;
        // * 🚩有替代⇒应用替代
        // * 🚩拿出里头生成的两个映射表
        final HashMap<Term, Term> map1 = result.extractUnification1();
        final HashMap<Term, Term> map2 = result.extractUnification2();
        // * 🚩此时假定「有替代的一定是复合词项」
        // renameVar(map1, compound1, "-1");
        // renameVar(map2, compound2, "-2");
        final CompoundTerm applied1 = applyUnifyToNew(parent1, map1);
        final CompoundTerm applied2 = applyUnifyToNew(parent2, map2);
        return new AppliedCompounds(applied1, applied2);
    }

    /** 多值输出：寻找「归一替换」的中间结果 */
    public static final class AppliedCompounds {
        /** 可变，因为要交出所有权 */
        private CompoundTerm applied1;
        /** 可变，因为要交出所有权 */
        private CompoundTerm applied2;

        AppliedCompounds(CompoundTerm applied1, CompoundTerm applied2) {
            this.applied1 = applied1;
            this.applied2 = applied2;
        }

        public CompoundTerm extractApplied1() {
            final CompoundTerm term = this.applied1;
            this.applied1 = null;
            return term;
        }

        public CompoundTerm extractApplied2() {
            final CompoundTerm term = this.applied2;
            this.applied2 = null;
            return term;
        }
    }

    /**
     * 🆕得出「替代结果」后，将映射表应用到词项上
     *
     * @param compound [&m] 要被应用映射表的复合词项
     * @param map      映射表
     */
    private static void applyUnifyOne(CompoundTerm compound, HashMap<Term, Term> map) {
        // * 🚩映射表非空⇒替换
        if (map.isEmpty())
            return;
        // * 🚩应用 & 重命名
        applySubstitute(compound, map);
        renameVariables(compound);
    }

    /**
     * 🆕得出「替代结果」后，将映射表应用到词项上
     * * 🚩【2024-06-19 23:21:14】返回一个新的词项，而原有词项不变
     *
     * @param compound [&] 所参考的复合词项
     * @param map      映射表
     * @return 新的（应用之后的）复合词项
     */
    private static CompoundTerm applyUnifyToNew(CompoundTerm compound, HashMap<Term, Term> map) {
        CompoundTerm toBeApply = compound.clone();
        // * 🚩映射表非空⇒替换
        if (map.isEmpty())
            return toBeApply;
        // * 🚩应用 & 重命名
        applySubstitute(toBeApply, map);
        renameVariables(toBeApply);
        return toBeApply;
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
     * * 📌名称：变量统一/变量归一化
     * * ⚠️会修改两个映射表
     * * ⚠️【2024-06-14 23:11:42】对「含变量的可交换词项」带有随机成分
     *
     * @param type  [] The type of Variable to be substituted
     * @param term1 [] The first Term to be unified
     * @param term2 [] The second Term to be unified
     * @param map1  [&m] The substitution for term1 formed so far
     * @param map2  [&m] The substitution for term2 formed so far
     * @return Whether there is a substitution that unifies the two Terms
     */
    private static boolean findUnification(
            final char type,
            final Term term1, final Term term2,
            HashMap<Term, Term> map1, HashMap<Term, Term> map2) {
        // * 🚩🆕预先计算好判据（及早求值）
        // * 📝此中的「共同变量」类型一定是「当前类型」：
        // * * 存在条件`isCorrectVar1 && term1 instanceof CommonVariable`成立
        // * 📌亦即如下条件恒成立：
        // * * `!(term1 instanceof CommonVariable) || isCorrectVar1`
        // * * `!(term2 instanceof CommonVariable) || isCorrectVar2`
        // * 📝【2024-07-09 22:47:34】似乎只在 `to_be_unified_1` 中出现「共用变量」
        final boolean isCorrectVar1 = CommonVariable.is(term1)
                || (term1 instanceof Variable && ((Variable) term1).getType() == type);
        final boolean isCorrectVar2 = CommonVariable.is(term2)
                || (term2 instanceof Variable && ((Variable) term2).getType() == type);
        // if (term1 instanceof CommonVariable && !isCorrectVar1)
        // throw new AssertionError();
        // if (term2 instanceof CommonVariable && !isCorrectVar2)
        // throw new AssertionError();
        final boolean isSameTypeCompound = term1 instanceof CompoundTerm && term1.isSameType(term2);
        final Variable var1, var2;
        // * 🚩[$1 x ?] 对应位置是变量
        if (isCorrectVar1) {
            var1 = (Variable) term1;
            // * 🚩已有替换⇒直接使用已有替换（看子项有无替换） | 递归深入
            if (map1.containsKey(var1)) // already mapped
                return findUnification(type, map1.get(var1), term2, map1, map2);
            // * 🚩[$1 x $2] 若同为变量⇒统一二者（制作一个「共同变量」）
            if (isCorrectVar2) { // not mapped yet
                var2 = (Variable) term2;
                // * 🚩生成一个外界输入中不可能的变量词项作为「匿名变量」
                final Variable commonVar = new CommonVariable(var1, var2);
                // * 🚩建立映射：var1 -> commonVar @ term1
                // * 🚩建立映射：var2 -> commonVar @ term2
                map1.put(var1, commonVar); // unify
                map2.put(var2, commonVar); // unify
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
            var2 = (Variable) term2;
            // * 🚩已有替换⇒直接使用已有替换（看子项有无替换） | 递归深入
            if (map2.containsKey(var2)) // already mapped
                return findUnification(type, term1, map2.get(var2), map1, map2);
            // not mapped yet
            // * 🚩[_1 x $2] 若非变量⇒尝试消元划归
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

    /**
     * 🆕特别为「共同变量」创建一个类
     * * 📌仅在「变量统一」中出现
     * * 🚩【2024-06-13 08:37:01】技术上使用「多字符类型」替代「根据名字生成的编号」
     * * * ⚠️后者会影响「长期稳定性」的测试结果
     * * * * 📄 ANSWER: <{tom} --> murder>. %1.00;0.77% {2817 : 2;11;3;9}
     * * * * 📄 ANSWER: <{tim} --> murder>. %1.00;0.81% {195 : 5;7}
     * * 📌原则：「共同变量」的「变量类型」要与「合并前的两个变量」一致
     * * * ⚠️否则会导致「长期稳定性」不一致
     */
    private static class CommonVariable extends Variable {
        CommonVariable(Variable v1, Variable v2) {
            // super('/', (long) ((v1.getName() + v2.getName() + '$').hashCode()));
            // super(v1.getName() + v2.getName() + '$');
            // super(v1.getType() + v1.getName() + v2.getName() + '&', 0);
            super(v1.getType(), (long) ((v1.getName() + v2.getName() + '$').hashCode()));
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
    private static boolean hasUnification(char type, Term term1, Term term2) {
        return findUnification(
                type,
                term1, term2,
                new HashMap<Term, Term>(), new HashMap<Term, Term>());
    }

    /** 🆕【对外接口】查找独立变量归一方式 */
    public static boolean hasUnificationI(Term term1, Term term2) {
        return hasUnification(VAR_INDEPENDENT, term1, term2);
    }

    /** 🆕【对外接口】查找非独变量归一方式 */
    public static boolean hasUnificationD(Term term1, Term term2) {
        return hasUnification(VAR_DEPENDENT, term1, term2);
    }

    /** 🆕【对外接口】查找查询变量归一方式 */
    public static boolean hasUnificationQ(Term term1, Term term2) {
        return hasUnification(VAR_QUERY, term1, term2);
    }

    // /**
    // * Rename the variables to prepare for unification of two terms
    // *
    // * @param map The substitution so far
    // * @param term The term to be processed
    // * @param suffix The suffix that distinguish the variables in one premise
    // * from those from the other
    // */
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

    // 尝试「不可变化」「函数式化」废稿 //
    // * 🕒更新时间：【2024-06-15 12:44:48】
    // * 🎯此处「函数式」的目标：让「词项」成为一个绝对的不可变（写时复制）类型
    // * 📝NAL-6的「变量统一」是为数不多「修改词项本身比创建新词项更经济」的词项处理机制
    // * 📝三大核心逻辑分别是「寻找归一字典」「应用替代」和「重命名变量」
    // * * 寻找归一字典：扫描要统一的两个词项，并在「变量位置相对应」的地方构建映射
    // * * 应用替代：扫描复合词项的所有元素，在【有映射】的地方替换元素
    // * * 重命名变量：将各复合词项的变量重命名到特定编号之中，以便在各处「词项判等」逻辑中将其认定为相同词项
    // * ⚠️缺陷
    // * * 📌若不借助可变性，在此过程中需要创建大量中间对象
    // * * * 性能开销相对较大，特别是对于大词项而言
    // * * 📌若不修改MakeTerm，则会让「词项无效性」提前显现
    // * * * 这会让许多「先前利用可变性的逻辑」需要大幅修改

    /**
     * Blank method to be override in CompoundTerm
     * Rename the variables in the compound, called from Sentence constructors
     * * 📝对原子词项（词语）而言，没什么可以「重命名」的
     * * ❓其是否要作为「变量推理」的一部分，仍待存疑——需要内化成「语言」库自身提供的特性吗？
     * * * 诸多时候并非在「语言」中使用：解析器、语句构造 等
     */
    public static Term renameVariables2New(Term term) {
        // * 🚩依据「是否为变量词项」分派
        if (term instanceof CompoundTerm) {
            final CompoundTerm c = (CompoundTerm) term;
            // * 🚩有变量⇒重命名变量
            if (Variable.containVar(c))
                // * ✅目前从「长期稳定性」中证明这俩等价（纯可变式🆚半函数式）
                // renameCompoundVariables(c, new HashMap<Variable, Variable>());
                return renameCompoundVariables2New(c);
            // * 🚩产生了新词项，就不用重命名
        }
        return term;
    }

    /**
     * Recursively rename the variables in the compound
     * * 📝这个函数本质上是个「半函数式」逻辑
     * * * 🚩首先用函数式逻辑（词项不可变）得到「替换映射」
     * * * 🚩随后用这个「替换映射」【修改】词项自身
     *
     * @param map The substitution established so far
     */
    private static void renameCompoundVariables(CompoundTerm self) {
        final HashMap<Term, Term> map = new HashMap<>();
        renameCompoundVariablesMap(self, map);
        // * 🚩重命名变量均非「链式替换」
        applySubstituteSingle(self, map);
    }

    /**
     * Recursively rename the variables in the compound
     * * 📌全函数式逻辑
     * * * 🎯动因：「重命名变量」的逻辑（被外部调用者）只存在于「语句创建」的部分
     *
     * @param map The substitution established so far
     */
    private static Term renameCompoundVariables2New(CompoundTerm self) {
        final HashMap<Term, Term> map = new HashMap<>();
        renameCompoundVariablesMap(self, map);
        // * 🚩重命名变量均非「链式替换」
        return applySubstitute2New(self, map, false);
    }

    private static void renameCompoundVariablesMap(
            CompoundTerm self,
            HashMap<Term, Term> map) {
        // * 🚩没有变量⇒返回
        // ? 💭【2024-06-09 13:33:08】似乎对实际逻辑无用
        if (!Variable.containVar(self))
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
                // * 🚩此处特别区分「用户输入产生的匿名变量词项」亦即【只有类型是Variable，整体名称并未改变】的新变量词项
                final boolean isAnonymousVariableFromInput = inner.getName().length() == 1;
                // * 🚩决定将产生的「新变量」
                final Term newV =
                        // * 🚩用户输入的匿名变量 || 映射表中没有变量 ⇒ 新建变量
                        isAnonymousVariableFromInput || !map.containsKey(innerV)
                                // anonymous variable from input
                                ? makeVarSimilar(innerV, newId)
                                // * 🚩否则（非匿名 && 映射表中有） ⇒ 使用已有变量
                                : map.get(innerV);
                // * 🚩将该变量记录在映射表中
                // * ⚠️即便相等也要记录：会因上头`map.containsKey(innerV)`影响后续判断
                // * * 📄影响的测试：NAL 6.20,6.21
                // * 🎯后续只要一层：所有变量⇒编号好了的匿名变量
                map.put(innerV, newV);
            }
            // * 🚩复合词项⇒继续递归深入
            // * 📌逻辑统一：无论是「序列」「集合」还是「陈述」都是这一套逻辑
            else if (inner instanceof CompoundTerm) {
                final CompoundTerm innerC = (CompoundTerm) inner;
                // * 🚩重命名内层复合词项
                renameCompoundVariablesMap(innerC, map);
                // * 🚩重命名变量后生成名称
                innerC.updateNameAfterRenameVariables();
            }
        }
    }

    /** 🆕没有chainGet的applySubstitute */
    public static void applySubstituteSingle(CompoundTerm self, final HashMap<Term, Term> subs) {
        // * 🚩遍历替换内部所有元素
        for (int i = 0; i < self.size(); i++) {
            final Term inner = self.componentAt(i);
            // * 🚩若有「替换方案」⇒替换
            if (subs.containsKey(inner)) {
                // * 🚩追溯一次，替换变量词项
                final Term substituteT = subs.get(inner);
                // * 🚩复制并替换元素
                final Term substitute = substituteT.clone();
                self.setTermWhenDealingVariables(i, substitute);
            }
            // * 🚩复合词项⇒递归深入
            else if (inner instanceof CompoundTerm) {
                applySubstituteSingle((CompoundTerm) inner, subs);
            }
        }
        // * 🚩可交换⇒替换之后重排顺序
        if (self.isCommutative()) // re-order
            self.reorderComponents();
        // * 🚩重新生成名称
        self.updateNameAfterRenameVariables();
    }

    /** 一次性返回多个值，所以需要这个临时性类 */
    public static final class UnificationResult {
        public final boolean hasSubs;
        // 📝替换后make，可能不再是正常词项
        public final Term substituted1;
        public final Term substituted2;

        public UnificationResult(
                final boolean hasSubs,
                final Term substituted1,
                final Term substituted2) {
            this.hasSubs = hasSubs;
            this.substituted1 = substituted1;
            this.substituted2 = substituted2;
        }
    }

    private static UnificationResult unify2New(
            final char type,
            Term t1, Term t2,
            CompoundTerm compound1,
            CompoundTerm compound2) {
        // * 🚩主逻辑：寻找替代
        final HashMap<Term, Term> map1 = new HashMap<>();
        final HashMap<Term, Term> map2 = new HashMap<>();
        final boolean hasSubs = findUnification(type, t1, t2, map1, map2); // find substitution
        // * 🚩有替代⇒应用替代
        final Term newCompound1, newCompound2;
        if (hasSubs) {
            // * 🚩此时假定「有替代的一定是复合词项」
            // renameVar(map1, compound1, "-1");
            // renameVar(map2, compound2, "-2");
            newCompound1 = applyUnifyOne2New(compound1, map1);
            newCompound2 = applyUnifyOne2New(compound2, map1);
        } else {
            // * 🚩找不到替代⇒双方皆为null
            newCompound1 = null;
            newCompound2 = null;
        }
        // * 🚩返回「是否替代成功」
        return new UnificationResult(hasSubs, newCompound1, newCompound2);
    }

    /** 🆕得出「替代结果」后，将映射表应用到词项上 */
    private static Term applyUnifyOne2New(CompoundTerm compound, HashMap<Term, Term> map) {
        // * 🚩映射表非空⇒替换
        if (map.isEmpty())
            return compound;
        // * 🚩应用到新词项，此时无需重命名 | 变量统一均为「链式替换」
        return applySubstitute2New(compound, map, true);
    }

    public static UnificationResult unifyI2New(Term t1, Term t2, CompoundTerm compound1, CompoundTerm compound2) {
        return unify2New(VAR_INDEPENDENT, t1, t2, compound1, compound2);
    }

    public static UnificationResult unifyD2New(Term t1, Term t2, CompoundTerm compound1, CompoundTerm compound2) {
        return unify2New(VAR_DEPENDENT, t1, t2, compound1, compound2);
    }

    public static UnificationResult unifyQ2New(Term t1, Term t2, CompoundTerm compound1, CompoundTerm compound2) {
        return unify2New(VAR_QUERY, t1, t2, compound1, compound2);
    }

    /**
     * 🆕应用替换到新词项
     * * 🎯纯函数，不涉及内部状态的改变
     *
     * @param old
     * @param subs
     * @return
     */
    private static Term applySubstitute2New(
            final CompoundTerm old,
            final HashMap<Term, Term> subs,
            final boolean chainSubstitute // * 📌区分「单层替换」与「链式替换」，🎯节省代码
    ) {
        // * 🚩生成新词项的内部元素
        final ArrayList<Term> components = new ArrayList<>();
        // * 🚩遍历替换内部所有元素
        for (int i = 0; i < old.size(); i++) {
            // * 🚩获取内部词项的引用
            final Term inner = old.componentAt(i);
            // * 🚩若有「替换方案」⇒添加被替换的项
            if (subs.containsKey(inner)) {
                // * ⚠️此处的「被替换词项」可能不是「变量词项」
                // * 📄NAL-6变量引入时会建立「临时共同变量」匿名词项，以替换非变量词项
                // * 🚩一路追溯到「没有再被传递性替换」的词项（最终点）
                final Term substituteT = chainSubstitute ? chainGet(subs, inner) : subs.get(inner);
                // * 🚩预先判空并返回
                if (substituteT == null)
                    throw new AssertionError("【2024-06-14 23:05:26】此处有替代就一定非空");
                // * 🚩复制并新增元素
                final Term substitute = substituteT.clone();
                components.add(substitute);
            }
            // * 🚩否则⇒复制or深入
            else {
                final Term newInner = inner instanceof CompoundTerm
                        // * 🚩复合词项⇒递归深入
                        ? applySubstitute2New((CompoundTerm) inner, subs, chainSubstitute)
                        // * 🚩原子词项⇒直接复制
                        : inner.clone();
                // * 🚩预先判空并返回 | 内部词项有可能在替换之后并不合法，会返回空
                if (newInner == null)
                    return null;
                // * 🚩增加
                components.add(newInner);
            }
        }
        // * 🚩选择性处理「可交换性」
        final ArrayList<Term> newComponents = old.isCommutative()
                // * 🚩可交换⇒替换之后重排顺序
                ? CompoundTerm.reorderTerms(components) // re-order
                // * 🚩否则按原样
                : components;
        // * 🚩以旧词项为模板生成新词项，顺带在其中生成名称
        // ! ⚠️【2024-06-14 23:01:56】可以使用`make`系列方法，但这其中可能会产生空值（不是一个「有效词项」）
        final Term newTerm = makeCompoundTermOrStatement(old, newComponents);
        // * 🚩返回
        return newTerm;
    }
}