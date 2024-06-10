package nars.inference;

import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.entity.TLink.TLinkType;
import nars.language.*;
import static nars.io.Symbols.*;

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirect processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
public class RuleTables {

    /**
     * Entry point of the inference engine
     * * 📌推理引擎「概念推理」的入口
     *
     * TODO: 追溯调用是否均以「导出结论」终止（若有）
     *
     * @param tLink   The selected TaskLink, which will provide a task
     * @param bLink   The selected TermLink, which may provide a belief
     * @param context Reference to the derivation context
     */
    static void reason(DerivationContextReason context) {
        final Term conceptTerm = context.getCurrentTerm();
        final TaskLink tLink = context.getCurrentTaskLink();
        final TermLink bLink = context.getCurrentBeliefLink();
        final Task task = context.getCurrentTask();
        final Term taskTerm = task.getContent().clone(); // cloning for substitution
        final Term beliefTerm = bLink.getTarget().clone(); // cloning for substitution
        final Judgement belief = context.getCurrentBelief();

        // * 📝词项链所指的词项，不一定指向一个确切的「信念」（并非「语句链」）
        final short tIndex = tLink.getIndex(0);
        final short bIndex = bLink.getIndex(0);
        final TLinkType tLinkType = tLink.getType();
        final TLinkType bLinkType = bLink.getType();

        // * 📝【2024-06-04 19:33:10】实质上这里的「链接类型分派」就是基于「词项链/任务链」的「内容相关性信息」分派
        // * 📄A @ (&&, A, B) => 点火「A」将以`COMPOUND`（从元素指向复合词项整体）
        // ! ❌尝试「摊平switch」失败：枚举变种无法被视作「常量」使用
        // * 报错信息：case expressions must be constant expressions Java(536871065)
        switch (tLinkType) { // dispatch first by TaskLink type
            // * 🚩只有「从复合词项」
            default:
                // System.out.println("RuleTables.reason: unexpected TLinkType: " + tLinkType +
                // "," + bLinkType + " @ ("
                // + tLink + ";" + bLink + ")");
                return;
            case SELF: // * 🚩conceptTerm = taskTerm
                switch (bLinkType) {
                    default:
                        // System.out.println("RuleTables.reason: unexpected TLinkType: " + tLinkType +
                        // "," + bLinkType
                        // + " @ (" + tLink + ";" + bLink + ")");
                        return;
                    case COMPONENT:
                        // * 📄T="(&&,<#1 --> object>,<#1 --> (/,made_of,_,plastic)>)"
                        // * + B="object"
                        // * @ C="(&&,<#1 --> object>,<#1 --> (/,made_of,_,plastic)>)"
                        compoundAndSelf((CompoundTerm) taskTerm, beliefTerm, true, context);
                        return;
                    case COMPOUND:
                        // * 📄T="<<$1 --> [aggressive]> ==> <$1 --> murder>>"
                        // * + B="[aggressive]"
                        // * @ C="<<$1 --> [aggressive]> ==> <$1 --> murder>>"
                        compoundAndSelf((CompoundTerm) beliefTerm, taskTerm, false, context);
                        return;
                    case COMPONENT_STATEMENT:
                        // * 📄T="<{tim} --> (/,livingIn,_,{graz})>"
                        // * + B="{tim}"
                        // * @ C="<{tim} --> (/,livingIn,_,{graz})>"
                        if (belief != null)
                            SyllogisticRules.detachment(task, belief, bIndex, context);
                        return;
                    case COMPOUND_STATEMENT:
                        // *📄T="<{tim} --> (/,own,_,sunglasses)>"
                        // * + B="<<{tim} --> (/,own,_,sunglasses)> ==> <{tim} --> murder>>"
                        // * @ C=T
                        if (belief != null)
                            SyllogisticRules.detachment(belief, task, bIndex, context);
                        return;
                    case COMPONENT_CONDITION:
                        // *📄T="<(&&,<$1-->[aggressive]>,<$1-->(/,livingIn,_,{graz})>)==><$1-->murder>>"
                        // * + B="[aggressive]"
                        // * @ C=T
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) taskTerm, bIndex2, beliefTerm, tIndex,
                                    context);
                        }
                        return;
                    case COMPOUND_CONDITION:
                        // * 📄T="<(*,{tim},{graz}) --> livingIn>"
                        // * + B="<(&&,<{tim} --> [aggressive]>,<(*,{tim},{graz}) --> livingIn>) ==>
                        // <{tim} --> murder>>"
                        // * @ C=T
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd(
                                    (Implication) beliefTerm, bIndex2,
                                    taskTerm, tIndex,
                                    context);
                        }
                        return;
                }
            case COMPOUND: // * 🚩conceptTerm ∈ taskTerm (normal)
                switch (bLinkType) {
                    default:
                        // System.out.println("RuleTables.reason: unexpected TLinkType: " + tLinkType +
                        // "," + bLinkType
                        // + " @ (" + tLink + ";" + bLink + ")");
                        return;
                    case COMPOUND: // * 🚩conceptTerm ∈ taskTerm, conceptTerm ∈ beliefTerm
                        // * 📄T="(&&,<cup --> #1>,<toothbrush --> #1>)"
                        // * + B="<cup --> [bendable]>"
                        // * @ C="cup"
                        compoundAndCompound(
                                (CompoundTerm) taskTerm,
                                (CompoundTerm) beliefTerm,
                                context);
                        return;
                    case COMPOUND_STATEMENT: // * 🚩conceptTerm ∈ taskTerm, conceptTerm ∈ beliefTerm isa Statement
                        // * 📄T="(&&,<{tim} --> #1>,<{tom} --> #1>)"
                        // * + B="<{tom} --> murder>"
                        // * @ C="{tom}"
                        compoundAndStatement(
                                (CompoundTerm) taskTerm, tIndex,
                                (Statement) beliefTerm, bIndex,
                                beliefTerm, context);
                        return;
                    case COMPOUND_CONDITION:
                        // *📄T="(||,<{tom}-->[aggressive]>,<{tom}-->(/,livingIn,_,{graz})>)"
                        // *+B="<(&&,<$1-->[aggressive]>,<$1-->(/,livingIn,_,{graz})>)==><$1-->murder>>"
                        // * @ C="(/,livingIn,_,{graz})"
                        if (belief != null) {
                            if (beliefTerm instanceof Implication) {
                                final boolean canDetach = VariableInference.unifyI(
                                        ((Implication) beliefTerm).getSubject(), taskTerm,
                                        beliefTerm, taskTerm);
                                if (canDetach) {
                                    detachmentWithVar(belief, task, bIndex, context);
                                } else {
                                    SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex, taskTerm, -1,
                                            context);
                                }
                            } else if (beliefTerm instanceof Equivalence) {
                                SyllogisticRules.conditionalAna((Equivalence) beliefTerm, bIndex, taskTerm, -1,
                                        context);
                            }
                        }
                        return;
                }
            case COMPOUND_STATEMENT: // * 🚩conceptTerm ∈ taskTerm (statement)
                switch (bLinkType) {
                    default:
                        // System.out.println("RuleTables.reason: unexpected TLinkType: " + tLinkType +
                        // "," + bLinkType
                        // + " @ (" + tLink + ";" + bLink + ")");
                        return;
                    case COMPONENT:
                        // * 📄T="<{tim} --> (/,livingIn,_,{graz})>"
                        // * + B="tim"
                        // * @ C="{tim}"
                        componentAndStatement((CompoundTerm) conceptTerm, bIndex, (Statement) taskTerm,
                                tIndex,
                                context);
                        return;
                    case COMPOUND:
                        // * 📄T="<{tim} --> (/,livingIn,_,{graz})>"
                        // * + B="{tim}"
                        // * @ C="tim"
                        compoundAndStatement(
                                (CompoundTerm) beliefTerm, bIndex,
                                (Statement) taskTerm, tIndex,
                                beliefTerm, context);
                        return;
                    case COMPOUND_STATEMENT:
                        // * 📄T="<{tim} --> (/,livingIn,_,{graz})>"
                        // * + B="<<$1 --> (/,livingIn,_,{graz})> ==> <$1 --> murder>>"
                        // * @ C="(/,livingIn,_,{graz})"
                        if (belief != null)
                            syllogisms(tLink, bLink, (Statement) taskTerm, (Statement) beliefTerm, context);
                        return;
                    case COMPOUND_CONDITION:
                        // * 📄T="<<$1 --> [aggressive]> ==> <$1 --> (/,livingIn,_,{graz})>>"
                        // *+B="<(&&,<$1-->[aggressive]>,<$1-->(/,livingIn,_,{graz})>)==><$1-->murder>>"
                        // * @ C="(/,livingIn,_,{graz})"
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            if (beliefTerm instanceof Implication) {
                                conditionalDedIndWithVar((Implication) beliefTerm, bIndex2, (Statement) taskTerm,
                                        tIndex, context);
                            }
                        }
                        return;
                }
            case COMPOUND_CONDITION: // * 🚩conceptTerm ∈ taskTerm (condition in statement)
                switch (bLinkType) {
                    default:
                        // System.out.println("RuleTables.reason: unexpected TLinkType: " + tLinkType +
                        // "," + bLinkType
                        // + " @ (" + tLink + ";" + bLink + ")");
                        return;
                    case COMPOUND:
                        // * 📄T="<(&&,<{graz} --> (/,livingIn,$1,_)>,(||,<$1 -->
                        // [aggressive]>,<sunglasses --> (/,own,$1,_)>)) ==> <$1 --> murder>>"
                        // * + B="(/,livingIn,_,{graz})"
                        // * @ C="{graz}"
                        if (belief != null)
                            detachmentWithVar(task, belief, tIndex, context);
                        return;
                    case COMPOUND_STATEMENT:
                        // *📄T="<(&&,<$1-->[aggressive]>,<sunglasses-->(/,own,$1,_)>)==><$1-->murder>>"
                        // * + B="<sunglasses --> glasses>"
                        // * @ C="sunglasses"
                        if (belief != null) {
                            // TODO maybe put instanceof test within conditionalDedIndWithVar()
                            if (taskTerm instanceof Implication) {
                                Term subj = ((Implication) taskTerm).getSubject();
                                if (subj instanceof Negation) {
                                    if (task.isJudgment()) {
                                        componentAndStatement((CompoundTerm) subj, bIndex, (Statement) taskTerm, tIndex,
                                                context);
                                    } else {
                                        componentAndStatement((CompoundTerm) subj, tIndex, (Statement) beliefTerm,
                                                bIndex, context);
                                    }
                                } else {
                                    conditionalDedIndWithVar((Implication) taskTerm, tIndex, (Statement) beliefTerm,
                                            bIndex, context);
                                }
                            }
                        }
                        return;
                }
        }
        // ! unreachable
    }

    /* ----- syllogistic inferences ----- */
    /**
     * Meta-table of syllogistic rules, indexed by the content classes of the
     * taskSentence and the belief
     *
     * @param tLink      The link to task
     * @param bLink      The link to belief
     * @param taskTerm   The content of task
     * @param beliefTerm The content of belief
     * @param context    Reference to the derivation context
     */
    private static void syllogisms(
            TaskLink tLink, TermLink bLink,
            Statement taskTerm, Statement beliefTerm,
            DerivationContextReason context) {
        // * 🚩获取变量
        final Sentence task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final SyllogismFigure figure;
        switch (taskTerm.operator() + beliefTerm.operator()) {
            // * 🚩非对称×非对称
            case INHERITANCE_RELATION + INHERITANCE_RELATION: // * 🚩继承+继承
            case IMPLICATION_RELATION + IMPLICATION_RELATION: // * 🚩蕴含+蕴含
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(task, belief, figure, context);
                return;
            // * 🚩非对称×对称
            case INHERITANCE_RELATION + SIMILARITY_RELATION: // * 🚩继承×相似
            case IMPLICATION_RELATION + EQUIVALENCE_RELATION: // * 🚩蕴含×等价
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(task, belief, figure, context);
                return;
            // * 🚩对称×非对称
            case SIMILARITY_RELATION + INHERITANCE_RELATION: // * 🚩相似×继承
            case EQUIVALENCE_RELATION + IMPLICATION_RELATION: // * 🚩等价×蕴含
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, task, figure, context);
                return;
            // * 🚩对称×对称
            case SIMILARITY_RELATION + SIMILARITY_RELATION: // * 🚩相似×相似
            case EQUIVALENCE_RELATION + EQUIVALENCE_RELATION: // * 🚩等价×等价
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, task, figure, context);
                return;
            // * 🚩分离：继承 +
            case INHERITANCE_RELATION + IMPLICATION_RELATION: // * 🚩继承×蕴含
            case INHERITANCE_RELATION + EQUIVALENCE_RELATION: // * 🚩继承×等价
                detachmentWithVar(belief, task, bLink.getIndex(0), context);
                return;
            // * 🚩分离：蕴含 +
            case IMPLICATION_RELATION + INHERITANCE_RELATION: // * 🚩蕴含×继承
            case EQUIVALENCE_RELATION + INHERITANCE_RELATION: // * 🚩等价×继承
                detachmentWithVar(task, belief, tLink.getIndex(0), context);
                return;
            // * 🚩无果匹配：相似×高阶
            case SIMILARITY_RELATION + IMPLICATION_RELATION: // * 🚩相似×蕴含
            case SIMILARITY_RELATION + EQUIVALENCE_RELATION: // * 🚩相似×等价
            case IMPLICATION_RELATION + SIMILARITY_RELATION: // * 🚩蕴含×相似
            case EQUIVALENCE_RELATION + SIMILARITY_RELATION: // * 🚩等价×相似
                return;
            // * ❌域外情况
            default:
                throw new IllegalArgumentException("未知的陈述类型：" + tLink + "; " + bLink);
        }
    }

    /**
     * 📌三段论模式
     * * 🚩公共词项在两个陈述之中的顺序
     * * 📝左边任务（待处理），右边信念（已接纳）
     */
    static enum SyllogismFigure {
        /** 主项×主项 <A --> B> × <A --> C> */
        SS, // induction
        /** 主项×谓项 <A --> B> × <C --> A> */
        SP, // deduction
        /** 谓项×主项 <A --> B> × <B --> C> */
        PS, // exemplification
        /** 谓项×谓项 <A --> B> × <C --> B> */
        PP, // abduction
    }

    /**
     * Decide the figure of syllogism according to the locations of the common
     * term in the premises
     *
     * @param link1 The link to the first premise
     * @param link2 The link to the second premise
     * @return The figure of the syllogism, one of the four: 11, 12, 21, or 22
     */
    private static SyllogismFigure indexToFigure(TLink<?> link1, TLink<?> link2) {
        // // * 🚩本质上就是「数位叠加」
        // return (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1);
        final int figureNum = (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1);
        // * 📄以 <A --> B> × <C --> D> 为例
        switch (figureNum) {
            // * 📌主项×主项 A=C
            case 11: // induction
                return SyllogismFigure.SS;
            // * 📌主项×谓项 A=D
            case 12: // deduction
                return SyllogismFigure.SP;
            // * 📌主项×谓项 B=C
            case 21: // exemplification
                return SyllogismFigure.PS;
            // * 📌谓项×谓项 C=D
            case 22: // abduction
                return SyllogismFigure.PP;
            default:
                throw new AssertionError("【2024-06-10 14:59:04】只可能有四种索引模式");
        }
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param task    The taskSentence in the task
     * @param belief  The judgment in the belief
     * @param figure  The location of the shared term
     * @param context Reference to the derivation context
     */
    private static void asymmetricAsymmetric(
            final Sentence task,
            final Judgement belief,
            final SyllogismFigure figure,
            final DerivationContextReason context) {
        // * 🚩非对称🆚非对称
        final Statement tTerm = (Statement) task.cloneContent();
        final Statement bTerm = (Statement) belief.cloneContent();
        final Term term1, term2;
        final boolean unified;
        switch (figure) {
            // * 🚩主项×主项 <A --> B> × <A --> C>
            case SS: // induction
                // * 🚩先尝试统一独立变量
                unified = VariableInference.unifyI(tTerm.getSubject(), bTerm.getSubject(), tTerm,
                        bTerm);
                // * 🚩不能统一变量⇒终止
                if (!unified)
                    return;
                // * 🚩统一后内容相等⇒终止
                if (tTerm.equals(bTerm))
                    return;
                // * 🚩取其中两个不同的谓项 B + C
                term1 = bTerm.getPredicate();
                term2 = tTerm.getPredicate();
                // * 🚩构造复合词项
                CompositionalRules.composeCompound(tTerm, bTerm, 0, context);
                // * 🚩归因+归纳+比较
                SyllogisticRules.abdIndCom(term1, term2, task, belief, context);
                return;
            // * 🚩主项×谓项 <A --> B> × <C --> A>
            case SP: // deduction
                // * 🚩先尝试统一独立变量
                unified = VariableInference.unifyI(tTerm.getSubject(), bTerm.getPredicate(), tTerm,
                        bTerm);
                // * 🚩不能统一变量⇒终止
                if (!unified)
                    return;
                // * 🚩统一后内容相等⇒终止
                if (tTerm.equals(bTerm))
                    return;
                // * 🚩取其中两个不同的主项和谓项 C + B
                term1 = bTerm.getSubject();
                term2 = tTerm.getPredicate();
                // * 🚩尝试统一查询变量
                if (VariableInference.unifyQ(term1, term2, tTerm, bTerm))
                    // * 🚩成功统一 ⇒ 匹配反向
                    SyllogisticRules.matchReverse(context);
                else
                    // * 🚩未有统一 ⇒ 演绎+举例
                    SyllogisticRules.dedExe(term1, term2, task, belief, context);
                return;
            // * 🚩谓项×主项 <A --> B> × <B --> C>
            case PS: // exemplification
                // * 🚩先尝试统一独立变量
                unified = VariableInference.unifyI(tTerm.getPredicate(), bTerm.getSubject(), tTerm,
                        bTerm);
                // * 🚩不能统一变量⇒终止
                if (!unified)
                    return;
                // * 🚩统一后内容相等⇒终止
                if (tTerm.equals(bTerm))
                    return;
                // * 🚩取其中两个不同的主项和谓项 A + C
                term1 = tTerm.getSubject();
                term2 = bTerm.getPredicate();
                // * 🚩尝试统一查询变量
                if (VariableInference.unifyQ(term1, term2, tTerm, bTerm))
                    // * 🚩成功统一 ⇒ 匹配反向
                    SyllogisticRules.matchReverse(context);
                else
                    // * 🚩未有统一 ⇒ 演绎+举例
                    SyllogisticRules.dedExe(term1, term2, task, belief, context);
                return;
            // * 🚩谓项×谓项 <A --> B> × <C --> B>
            case PP: // abduction
                // * 🚩先尝试统一独立变量
                unified = VariableInference.unifyI(
                        tTerm.getPredicate(), bTerm.getPredicate(),
                        tTerm, bTerm);
                // * 🚩不能统一变量⇒终止
                if (!unified)
                    return;
                // * 🚩统一后内容相等⇒终止
                if (tTerm.equals(bTerm))
                    return;
                // * 🚩取其中两个不同的主项和谓项 A + C
                term1 = tTerm.getSubject();
                term2 = bTerm.getSubject();
                // * 🚩先尝试进行「条件归纳」，有结果⇒返回
                // TODO: 或许可以在这个过程中返回「推理结果」？在其中加入「导出的结论」「要更新的预算」等
                final boolean applied = SyllogisticRules.conditionalAbd(term1, term2, tTerm, bTerm, context);
                if (applied)
                    return;
                // if conditional abduction, skip the following
                // * 🚩尝试构建复合词项
                CompositionalRules.composeCompound(tTerm, bTerm, 1, context);
                // * 🚩归因+归纳+比较
                SyllogisticRules.abdIndCom(term1, term2, task, belief, context);
                return;
        }
    }

    /**
     * Syllogistic rules whose first premise is on an asymmetric relation, and
     * the second on a symmetric relation
     *
     * @param asym    The asymmetric premise
     * @param sym     The symmetric premise
     * @param figure  The location of the shared term
     * @param context Reference to the derivation context
     */
    private static void asymmetricSymmetric(
            Sentence asym, Sentence sym,
            SyllogismFigure figure,
            DerivationContextReason context) {
        // * 🚩非对称🆚对称
        final Statement asymS = (Statement) asym.cloneContent();
        final Statement symS = (Statement) sym.cloneContent();
        final Term term1, term2;
        final boolean unifiedI, unifiedQ;
        switch (figure) {
            // * 🚩主项×主项 <A --> B> × <A <-> C>
            case SS:
                // * 🚩先尝试统一独立变量
                unifiedI = VariableInference.unifyI(
                        asymS.getSubject(), symS.getSubject(),
                        asymS, symS);
                // * 🚩不能统一变量⇒终止
                if (!unifiedI)
                    return;
                // * 🚩取其中两个不同的谓项 B + C
                term1 = asymS.getPredicate();
                term2 = symS.getPredicate();
                // * 🚩再根据「是否可统一查询变量」做分派（可统一⇒已经统一了）
                unifiedQ = VariableInference.unifyQ(term1, term2, asymS, symS);
                if (unifiedQ)
                    // * 🚩能统一 ⇒ 继续分派
                    SyllogisticRules.matchAsymSym(asym, sym, context);
                else
                    // * 🚩未有统一 ⇒ 类比
                    SyllogisticRules.analogy(term2, term1, asym, sym, context);
                return;
            // * 🚩主项×谓项 <A --> B> × <C <-> A>
            case SP:
                // * 🚩先尝试统一独立变量
                unifiedI = VariableInference.unifyI(
                        asymS.getSubject(), symS.getPredicate(),
                        asymS, symS);
                // * 🚩不能统一变量⇒终止
                if (!unifiedI)
                    return;
                // * 🚩取其中两个不同的主项 B + C
                term1 = asymS.getPredicate();
                term2 = symS.getSubject();
                // * 🚩再根据「是否可统一查询变量」做分派（可统一⇒已经统一了）
                unifiedQ = VariableInference.unifyQ(term1, term2, asymS, symS);
                if (unifiedQ)
                    // * 🚩能统一 ⇒ 继续分派
                    SyllogisticRules.matchAsymSym(asym, sym, context);
                else
                    // * 🚩未有统一 ⇒ 类比
                    SyllogisticRules.analogy(term2, term1, asym, sym, context);
                return;
            // * 🚩谓项×主项 <A --> B> × <B <-> C>
            case PS:
                // * 🚩先尝试统一独立变量
                unifiedI = VariableInference.unifyI(
                        asymS.getPredicate(), symS.getSubject(),
                        asymS, symS);
                // * 🚩不能统一变量⇒终止
                if (!unifiedI)
                    return;
                // * 🚩取其中两个不同的主项 A + C
                term1 = asymS.getSubject();
                term2 = symS.getPredicate();
                // * 🚩再根据「是否可统一查询变量」做分派（可统一⇒已经统一了）
                unifiedQ = VariableInference.unifyQ(term1, term2, asymS, symS);
                if (unifiedQ)
                    // * 🚩能统一 ⇒ 继续分派
                    SyllogisticRules.matchAsymSym(asym, sym, context);
                else
                    // * 🚩未有统一 ⇒ 类比
                    SyllogisticRules.analogy(term1, term2, asym, sym, context);
                return;
            // * 🚩谓项×谓项 <A --> B> × <C <-> B>
            case PP:
                // * 🚩先尝试统一独立变量
                unifiedI = VariableInference.unifyI(
                        asymS.getPredicate(), symS.getPredicate(),
                        asymS, symS);
                // * 🚩不能统一变量⇒终止
                if (!unifiedI)
                    return;
                // * 🚩取其中两个不同的主项 A + C
                term1 = asymS.getSubject();
                term2 = symS.getSubject();
                // * 🚩再根据「是否可统一查询变量」做分派（可统一⇒已经统一了）
                unifiedQ = VariableInference.unifyQ(term1, term2, asymS, symS);
                if (unifiedQ)
                    // * 🚩能统一 ⇒ 继续分派
                    SyllogisticRules.matchAsymSym(asym, sym, context);
                else
                    // * 🚩未有统一 ⇒ 类比
                    SyllogisticRules.analogy(term1, term2, asym, sym, context);
                return;
        }
    }

    /**
     * Syllogistic rules whose both premises are on the same symmetric relation
     *
     * @param belief       The premise that comes from a belief
     * @param taskSentence The premise that comes from a task
     * @param figure       The location of the shared term
     * @param context      Reference to the derivation context
     */
    private static void symmetricSymmetric(
            Judgement belief, Sentence taskSentence,
            SyllogismFigure figure,
            DerivationContextReason context) {
        // * 🚩对称🆚对称
        final Statement bTerm = (Statement) belief.cloneContent();
        final Statement tTerm = (Statement) taskSentence.cloneContent();
        final Term bS = bTerm.getSubject();
        final Term tS = tTerm.getSubject();
        final Term bP = bTerm.getPredicate();
        final Term tP = tTerm.getPredicate();
        final boolean unified;
        switch (figure) {
            case SS:
                // * 🚩尝试以不同方式统一查询变量 @ 公共词项
                unified = VariableInference.unifyI(bS, tS, bTerm, tTerm);
                // * 🚩成功统一 ⇒ 相似传递
                if (unified)
                    SyllogisticRules.resemblance(bP, tP, belief, taskSentence, context);
                return;
            case SP:
                // * 🚩尝试以不同方式统一查询变量 @ 公共词项
                unified = VariableInference.unifyI(bS, tP, bTerm, tTerm);
                // * 🚩成功统一 ⇒ 相似传递
                if (unified)
                    SyllogisticRules.resemblance(bP, tS, belief, taskSentence, context);
                return;
            case PS:
                // * 🚩尝试以不同方式统一查询变量 @ 公共词项
                unified = VariableInference.unifyI(bP, tS, bTerm, tTerm);
                // * 🚩成功统一 ⇒ 相似传递
                if (unified)
                    SyllogisticRules.resemblance(bS, tP, belief, taskSentence, context);
                return;
            case PP:
                // * 🚩尝试以不同方式统一查询变量 @ 公共词项
                unified = VariableInference.unifyI(bP, tP, bTerm, tTerm);
                // * 🚩成功统一 ⇒ 相似传递
                if (unified)
                    SyllogisticRules.resemblance(bS, tS, belief, taskSentence, context);
                return;
        }
    }

    /* ----- conditional inferences ----- */
    /**
     * The detachment rule, with variable unification
     *
     * @param highOrderSentence The premise that is an Implication or Equivalence
     * @param subSentence       The premise that is the subject or predicate of the
     *                          first one
     * @param index             The location of the second premise in the first one
     * @param context           Reference to the context
     */
    private static void detachmentWithVar(
            Sentence highOrderSentence,
            Sentence subSentence, int index,
            DerivationContextReason context) {
        if (!context.hasCurrentBelief())
            return; // ? 【2024-06-10 17:37:10】目前不确定是否有「当前信念」
        // * 🚩提取元素
        final Sentence mainSentence = highOrderSentence.sentenceClone(); // for substitution
        final Statement mainStatement = (Statement) mainSentence.getContent();
        final Term component = mainStatement.componentAt(index); // * 🚩前件
        final CompoundTerm content = (CompoundTerm) subSentence.getContent(); // * 🚩子句本身
        // * 🚩非继承或否定⇒提前结束
        if (!(component instanceof Inheritance || component instanceof Negation))
            return;
        // * 🚩常量词项（没有变量）⇒直接分离
        if (component.isConstant()) {
            SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            return;
        }
        // * 🚩若非常量（有变量） ⇒ 尝试统一独立变量
        final boolean unifiedI = VariableInference.unifyI(component, content, mainStatement, content);
        if (unifiedI) {
            // * 🚩统一成功⇒分离
            SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            return;
        }
        // ! ⚠️【2024-06-10 17:52:44】「当前任务」与「主陈述」可能不一致：主陈述可能源自「当前信念」
        // * * 当前任务="<(*,{tom},(&,glasses,[black])) --> own>."
        // * * 主陈述="<<$1 --> (/,livingIn,_,{graz})> ==> <(*,$1,sunglasses) --> own>>"
        // * * 当前信念="<<$1 --> (/,livingIn,_,{graz})> ==> <(*,$1,sunglasses) --> own>>."
        // * 🚩当前任务是「判断句」且是「陈述」（任务、信念皆判断）⇒尝试引入变量
        final boolean isCurrentTaskJudgement = context.getCurrentTask().isJudgment();
        final boolean isStatementMainPredicate = mainStatement.getPredicate() instanceof Statement;
        if (isCurrentTaskJudgement && isStatementMainPredicate) {
            // ? 💫【2024-06-10 17:50:36】此处逻辑尚未能完全理解
            if (mainStatement instanceof Implication) {
                final Statement s2 = (Statement) mainStatement.getPredicate();
                final Term contentSubject = ((Statement) content).getSubject();
                if (s2.getSubject().equals(contentSubject)) {
                    // * 📄【2024-06-10 17:46:02】一例：
                    // * TaskV1@838 "<<toothbrush --> $1> ==> <cup --> $1>>.
                    // * // from task: $0.80;0.80;0.95$ <toothbrush --> [bendable]>. %1.00;0.90%
                    // * // from belief: <cup --> [bendable]>. %1.00;0.90% {460 : 37} "
                    // * content="<cup --> toothbrush>"
                    // * s2="<cup --> $1>"
                    // * mainStatement="<<toothbrush --> $1> ==> <cup --> $1>>"
                    CompositionalRules.introVarInner((Statement) content, s2, mainStatement, context);
                }
                CompositionalRules.IntroVarSameSubjectOrPredicate(
                        highOrderSentence.asJudgement(), subSentence.asJudgement(),
                        component, content,
                        index, context);
                return;
            }
            if (mainStatement instanceof Equivalence) {
                CompositionalRules.IntroVarSameSubjectOrPredicate(
                        highOrderSentence.asJudgement(), subSentence.asJudgement(),
                        component, content,
                        index, context);
                return;
            }
        }
    }

    /**
     * Conditional deduction or induction, with variable unification
     *
     * @param conditional The premise that is an Implication with a Conjunction
     *                    as condition
     * @param index       The location of the shared term in the condition
     * @param statement   The second premise that is a statement
     * @param side        The location of the shared term in the statement
     * @param context     Reference to the derivation context
     */
    private static void conditionalDedIndWithVar(
            final Implication conditional,
            final short index,
            final Statement statement,
            final short side,
            final DerivationContextReason context) {
        // * 🚩提取条件
        final CompoundTerm condition = (CompoundTerm) conditional.getSubject();
        final Term component = condition.componentAt(index);
        // * 🚩决定要尝试消去的第二个元素，以及发生条件演绎、归纳的位置
        final Term component2;
        final short newSide;
        // * 📄一例：
        // * conditional="<(&&,<$1 --> [aggressive]>,<sunglasses --> (/,own,$1,_)>) ==>
        // <$1 --> murder>>"
        // * condition="(&&,<$1 --> [aggressive]>,<sunglasses --> (/,own,$1,_)>)"
        // * component="<$1 --> [aggressive]>"
        // * index = 0
        // * statement="<sunglasses --> glasses>"
        // * side = 0
        if (statement instanceof Inheritance) {
            // * 🚩继承⇒直接作为条件之一
            component2 = statement;
            newSide = -1;
        } else if (statement instanceof Implication) {
            // * 🚩蕴含⇒取其中一处元素（主项/谓项）
            // * 📄【2024-06-10 18:10:39】一例：
            // * statement="<<sunglasses --> (/,own,$1,_)> ==> <$1 --> [aggressive]>>"
            // * component2="<sunglasses --> (/,own,$1,_)>"
            // * component="<sunglasses --> (/,own,$1,_)>"
            // * side=0
            // * newSide=0
            component2 = statement.componentAt(side);
            newSide = side;
        } else {
            // * 📄【2024-06-10 18:13:13】一例：
            // * currentConcept="sunglasses"
            // * condition="(&&,<sunglasses --> (/,own,$1,_)>,(||,<$1 --> [aggressive]>,
            // <$1 --> (/,livingIn,_,{graz})>))"
            // * statement="<sunglasses <-> (&,glasses,[black])>"
            return;
        }
        // * 🚩先尝试替换独立变量
        boolean unified = VariableInference.unifyI(
                component, component2,
                conditional, statement);
        // * 🚩若替换失败，则尝试替换非独变量
        if (!unified)
            // * 🚩惰性求值：第一次替换成功，就无需再次替换
            unified = VariableInference.unifyD(
                    component, component2,
                    conditional, statement);
        // * 🚩成功替换⇒条件 演绎/归纳
        if (unified)
            SyllogisticRules.conditionalDedInd(conditional, index, statement, newSide, context);
    }

    /* ----- structural inferences ----- */
    /**
     * Inference between a compound term and a component of it
     *
     * @param compound           The compound term
     * @param component          The component term
     * @param isCompoundFromTask Whether the compound comes from the task
     * @param context            Reference to the derivation context
     */
    private static void compoundAndSelf(
            CompoundTerm compound,
            Term component,
            boolean isCompoundFromTask,
            DerivationContextReason context) {
        // * 🚩合取/析取
        if (compound instanceof Conjunction || compound instanceof Disjunction) {
            // * 🚩有「当前信念」⇒解构出陈述
            if (context.hasCurrentBelief()) {
                CompositionalRules.decomposeStatement(
                        compound, component,
                        isCompoundFromTask, context);
                return;
            }
            // * 🚩否，但包含元素⇒取出词项
            else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(
                        compound, component,
                        isCompoundFromTask, context);
                return;
            }
            // } else if ((compound instanceof Negation) &&
            // !context.getCurrentTask().isStructural()) {
            else
                return;
        }
        // * 🚩否定
        else if (compound instanceof Negation) {
            // * 🚩从「当前任务」来⇒转换其中的否定
            if (isCompoundFromTask) {
                // * 🚩双重否定⇒肯定
                // * 📄【2024-06-10 19:57:15】一例：
                // * compound="(--,(--,A))"
                // * component="(--,A)"
                // * currentConcept=Concept@63 "(--,(--,A))"
                // * currentTask=TaskV1@807 "$0.8000;0.8000;0.9500$ (--,(--,A)). %1.00;0.90%"
                StructuralRules.transformNegation(
                        ((Negation) compound).getTheComponent(),
                        context);
                return;
            } else {
                // * 🚩否则⇒转换整个否定
                StructuralRules.transformNegation(
                        compound,
                        context);
                return;
            }
        }
        // * 🚩其它⇒无结果
        else {
            return;
        }
    }

    /**
     * Inference between two compound terms
     *
     * @param taskTerm   The compound from the task
     * @param beliefTerm The compound from the belief
     * @param context    Reference to the derivation context
     */
    private static void compoundAndCompound(
            CompoundTerm taskTerm, CompoundTerm beliefTerm,
            DerivationContextReason context) {
        // * 🚩非同类⇒返回
        if (!taskTerm.isSameType(beliefTerm))
            return;
        // * 🚩任务词项 > 信念词项 ⇒ 以「任务词项」为整体
        if (taskTerm.size() > beliefTerm.size()) {
            compoundAndSelf(taskTerm, beliefTerm, true, context);
            return;
        }
        // * 🚩任务词项 < 信念词项 ⇒ 以「信念词项」为整体
        else if (taskTerm.size() < beliefTerm.size()) {
            compoundAndSelf(beliefTerm, taskTerm, false, context);
            return;
        }
        // * 🚩其它情况 ⇒ 返回
        else
            return;
    }

    /**
     * Inference between a compound term and a statement
     *
     * @param compound   The compound term
     * @param index      The location of the current term in the compound
     * @param statement  The statement
     * @param side       The location of the current term in the statement
     * @param beliefTerm The content of the belief
     * @param context    Reference to the derivation context
     */
    private static void compoundAndStatement(
            CompoundTerm compound, short index,
            Statement statement, short side,
            Term beliefTerm, DerivationContextReason context) {
        final Term component = compound.componentAt(index);
        // ! ⚠️可能与「当前概念」的词项不一致：元素"{tom}"🆚概念"tom"
        final Task task = context.getCurrentTask();
        // * 🚩均为陈述，且为同一类型
        if (component.isSameType(statement)) {
            // * 其内元素是「合取」且有「当前信念」
            if (compound instanceof Conjunction && context.hasCurrentBelief()) {
                // * 🚩先尝试消去非独变量 #
                final boolean unifiedD = VariableInference.unifyD(component, statement, compound, statement);
                if (unifiedD)
                    // * 🚩能消去⇒三段论消元
                    SyllogisticRules.eliminateVarDep(
                            compound, component,
                            statement.equals(beliefTerm), // ? 【2024-06-10 19:38:32】为何要如此
                            context);
                /// * 🚩不能消去，但任务是判断句⇒内部引入变量
                else if (task.isJudgment()) // && !compound.containComponent(component)) {
                    CompositionalRules.introVarInner(
                            statement, (Statement) component,
                            compound,
                            context);
                /// * 🚩是疑问句，且能消去查询变量⇒解构出元素作为结论
                else if (VariableInference.unifyQ(component, statement, compound, statement))
                    CompositionalRules.decomposeStatement(
                            compound, component,
                            true,
                            context);
            }
        }
        // if (!task.isStructural() && task.isJudgment()) {
        // * 🚩类型不同 且为双判断
        else if (task.isJudgment()) {
            final boolean canComposeBoth;
            // * 🚩涉及的陈述是「继承」
            if (statement instanceof Inheritance) {
                // * 🚩单侧组合
                StructuralRules.structuralComposeOne(compound, index, statement, context);
                // if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                // * 🚩若能双侧组合⇒双侧组合
                canComposeBoth = !(compound instanceof SetExt || compound instanceof SetInt
                        || compound instanceof Negation);
                if (canComposeBoth)
                    // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                    StructuralRules.structuralComposeBoth(compound, index, statement, side, context);
            }
            // * 🚩涉及的陈述是「相似」，但涉及的另一复合词项不是「合取」
            // * 📝「相似」只能双侧组合，可以组合出除「合取」之外的结论
            else if (statement instanceof Similarity) {
                // * 🚩尝试双侧组合
                canComposeBoth = !(compound instanceof Conjunction);
                if (canComposeBoth)
                    // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
                    StructuralRules.structuralComposeBoth(compound, index, statement, side, context);
            }
        }
    }

    /**
     * Inference between a component term (of the current term) and a statement
     *
     * @param compound  The compound term
     * @param index     The location of the current term in the compound
     * @param statement The statement
     * @param side      The location of the current term in the statement
     * @param context   Reference to the derivation context
     */
    private static void componentAndStatement(
            CompoundTerm compound, short index,
            Statement statement, short side,
            DerivationContextReason context) {
        // * 🚩陈述是「继承」
        // if (context.getCurrentTask().isStructural()) return;
        final boolean canDecomposeBoth;
        if (statement instanceof Inheritance) {
            // * 🚩集合消去
            StructuralRules.structuralDecomposeOne(compound, index, statement, context);
            // * 🚩尝试两侧都消去
            canDecomposeBoth = !(compound instanceof SetExt) && !(compound instanceof SetInt);
            if (canDecomposeBoth) {
                // * 🚩两侧消去
                // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
                StructuralRules.structuralDecomposeBoth(statement, index, context);
                return;
            } else {
                // * 🚩外延集性质：一元集合⇒最小外延 | 内涵集性质：一元集合⇒最小内涵
                // * <A --> {B}> |- <A <-> {B}>
                StructuralRules.transformSetRelation(compound, statement, side, context);
                return;
            }
        }
        // * 🚩陈述是「相似」⇒总是要两侧消去
        else if (statement instanceof Similarity) {
            // {(C-B) <-> (C-A), A @ (C-A)} |- A <-> B
            StructuralRules.structuralDecomposeBoth(statement, index, context);
            // * 🚩外延集/内涵集⇒尝试转换集合关系
            if (compound instanceof SetExt || compound instanceof SetInt) {
                // * 🚩外延集性质：一元集合⇒最小外延 | 内涵集性质：一元集合⇒最小内涵
                // * <A <-> {B}> |- <A --> {B}>
                StructuralRules.transformSetRelation(compound, statement, side, context);
            }
            return;
        }
        // * 🚩蕴含×否定⇒逆否
        else if (statement instanceof Implication && compound instanceof Negation) {
            if (index == 0) {
                StructuralRules.contraposition(
                        statement,
                        context.getCurrentTask(),
                        context);
                return;
            } else {
                StructuralRules.contraposition(
                        statement,
                        context.getCurrentBelief(),
                        context);
                return;
            }
        } else {
            return;
        }
    }
}
