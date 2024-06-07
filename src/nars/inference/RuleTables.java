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
        final Sentence belief = context.getCurrentBelief();
        // * 🚩先尝试本地处理，若本地处理成功（修正&答问），就返回
        if (belief != null) {
            LocalRules.match(context);
        }
        // ! 📝此处OpenNARS原意是：若「之前通过『直接推理』或『概念推理/本地推理』获得了结果」，则不再进行下一步推理
        // * 📌依据：`long_term_stability.nal`
        // * 📄ONA中的结果有两个：
        // * 1. `Answer: <{tom} --> murder>. %1.000000; 0.729000%`
        // * 2. `<{tim} --> murder>. %1.000000; 0.810000%`
        // * 📄OpenNARS 3.1.0的结果：`Answer <{tim} --> murder>. %1.00;0.85%`
        // * 📝目前的结果是：`ANSWER: <{tim} --> murder>. %1.00;0.81% {195 : 5;7}`
        // * 🚩
        if (!context.getMemory().noResult() && task.isJudgment()) {
            return;
        }

        // * 📝词项链所指的词项，不一定指向一个确切的「信念」（并非「语句链」）
        final short tIndex = tLink.getIndex(0);
        final short bIndex = bLink.getIndex(0);
        final TLinkType tLinkType = tLink.getType();
        final TLinkType bLinkType = bLink.getType();

        // * 📝【2024-06-04 19:33:10】实质上这里的「链接类型分派」就是基于「词项链/任务链」的「内容相关性信息」分派
        // * 📄A @ (&&, A, B) => 点火「A」将以`COMPOUND`（从元素指向复合词项整体）
        // ! ❌尝试「摊平switch」失败：枚举变种无法被视作「常量」使用
        // * 报错信息：case expressions must be constant expressionsJava(536871065)
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
                        compoundAndCompound((CompoundTerm) taskTerm, (CompoundTerm) beliefTerm, context);
                        return;
                    case COMPOUND_STATEMENT: // * 🚩conceptTerm ∈ taskTerm, conceptTerm ∈ beliefTerm isa Statement
                        // * 📄T="(&&,<{tim} --> #1>,<{tom} --> #1>)"
                        // * + B="<{tom} --> murder>"
                        // * @ C="{tom}"
                        compoundAndStatement((CompoundTerm) taskTerm, tIndex, (Statement) beliefTerm, bIndex,
                                beliefTerm, context);
                        return;
                    case COMPOUND_CONDITION:
                        // *📄T="(||,<{tom}-->[aggressive]>,<{tom}-->(/,livingIn,_,{graz})>)"
                        // *+B="<(&&,<$1-->[aggressive]>,<$1-->(/,livingIn,_,{graz})>)==><$1-->murder>>"
                        // * @ C="(/,livingIn,_,{graz})"
                        if (belief != null) {
                            if (beliefTerm instanceof Implication) {
                                final boolean canDetach = Variable.unify(
                                        VAR_INDEPENDENT,
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
                        compoundAndStatement((CompoundTerm) beliefTerm, bIndex, (Statement) taskTerm, tIndex,
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
    private static void syllogisms(TaskLink tLink, TermLink bLink, Statement taskTerm, Statement beliefTerm,
            DerivationContextReason context) {
        // * ❌【2024-06-04 21:21:08】放弃使用switch「case 常量+常量」方式：无法「部分default」
        final Sentence taskSentence = context.getCurrentTask();
        final Sentence belief = context.getCurrentBelief();
        final int figure;
        switch (taskTerm.operator() + beliefTerm.operator()) {
            // * 🚩继承 +
            case INHERITANCE_RELATION + INHERITANCE_RELATION: // * 🚩继承
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, context);
                return;
            case INHERITANCE_RELATION + SIMILARITY_RELATION: // * 🚩相似
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, context);
                return;
            case INHERITANCE_RELATION + IMPLICATION_RELATION: // * 🚩蕴含
            case INHERITANCE_RELATION + EQUIVALENCE_RELATION: // * 🚩等价
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0), context);
                return;
            // * 🚩相似 +
            case SIMILARITY_RELATION + INHERITANCE_RELATION: // * 🚩继承
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, context);
                return;
            case SIMILARITY_RELATION + SIMILARITY_RELATION: // * 🚩相似
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, context);
                return;
            case SIMILARITY_RELATION + IMPLICATION_RELATION: // * 🚩蕴含
            case SIMILARITY_RELATION + EQUIVALENCE_RELATION: // * 🚩等价
                return;
            // * 🚩蕴含 +
            case IMPLICATION_RELATION + INHERITANCE_RELATION: // * 🚩继承
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), context);
                return;
            case IMPLICATION_RELATION + SIMILARITY_RELATION: // * 🚩相似
                return;
            case IMPLICATION_RELATION + IMPLICATION_RELATION: // * 🚩蕴含
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, context);
                return;
            case IMPLICATION_RELATION + EQUIVALENCE_RELATION: // * 🚩等价
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, context);
                return;
            // * 🚩等价 +
            case EQUIVALENCE_RELATION + INHERITANCE_RELATION: // * 🚩继承
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), context);
                return;
            case EQUIVALENCE_RELATION + SIMILARITY_RELATION: // * 🚩相似
                return;
            case EQUIVALENCE_RELATION + IMPLICATION_RELATION: // * 🚩蕴含
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, context);
                return;
            case EQUIVALENCE_RELATION + EQUIVALENCE_RELATION: // * 🚩等价
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, context);
                return;
            // * ❌域外情况
            default:
                throw new IllegalArgumentException("未知的陈述类型：" + tLink + "; " + bLink);
        }
    }

    /**
     * Decide the figure of syllogism according to the locations of the common
     * term in the premises
     *
     * @param link1 The link to the first premise
     * @param link2 The link to the second premise
     * @return The figure of the syllogism, one of the four: 11, 12, 21, or 22
     */
    private static <U, V> int indexToFigure(TLink<U> link1, TLink<V> link2) {
        return (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1);
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param sentence The taskSentence in the task
     * @param belief   The judgment in the belief
     * @param figure   The location of the shared term
     * @param context  Reference to the derivation context
     */
    private static void asymmetricAsymmetric(Sentence sentence, Sentence belief, int figure,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement s1 = (Statement) sentence.cloneContent();
        final Statement s2 = (Statement) belief.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11: // induction
                if (Variable.unify(VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getPredicate();
                    t2 = s1.getPredicate();
                    CompositionalRules.composeCompound(s1, s2, 0, context);
                    SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, context);
                }
                return;
            case 12: // deduction
                if (Variable.unify(VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getSubject();
                    t2 = s1.getPredicate();
                    if (Variable.unify(VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(context);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, context);
                    }
                }
                return;
            case 21: // exemplification
                if (Variable.unify(VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getPredicate();
                    if (Variable.unify(VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(context);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, context);
                    }
                }
                return;
            case 22: // abduction
                if (Variable.unify(VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getSubject();
                    if (!SyllogisticRules.conditionalAbd(t1, t2, s1, s2, context)) { // if conditional abduction, skip
                        // the following
                        CompositionalRules.composeCompound(s1, s2, 1, context);
                        SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, context);
                    }
                }
                return;
            default:
                throw new IllegalArgumentException("figure must be 11, 12, 21, or 22");
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
    private static void asymmetricSymmetric(Sentence asym, Sentence sym, int figure, DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement asymSt = (Statement) asym.cloneContent();
        final Statement symSt = (Statement) sym.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11:
                if (Variable.unify(VAR_INDEPENDENT, asymSt.getSubject(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, context);
                    }
                }
                return;
            case 12:
                if (Variable.unify(VAR_INDEPENDENT, asymSt.getSubject(), symSt.getPredicate(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getSubject();
                    if (Variable.unify(VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, context);
                    }
                }
                return;
            case 21:
                if (Variable.unify(VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, context);
                    }
                }
                return;
            case 22:
                if (Variable.unify(VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getPredicate(), asymSt,
                        symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getSubject();
                    if (Variable.unify(VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, context);
                    }
                }
                return;
            default:
                throw new IllegalArgumentException("figure must be 11, 12, 21, or 22");
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
    private static void symmetricSymmetric(Sentence belief, Sentence taskSentence, int figure,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement s1 = (Statement) belief.cloneContent();
        final Statement s2 = (Statement) taskSentence.cloneContent();
        switch (figure) {
            case 11:
                if (Variable.unify(VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getPredicate(), belief, taskSentence, figure,
                            context);
                }
                return;
            case 12:
                if (Variable.unify(VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getSubject(), belief, taskSentence, figure,
                            context);
                }
                return;
            case 21:
                if (Variable.unify(VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getPredicate(), belief, taskSentence, figure,
                            context);
                }
                return;
            case 22:
                if (Variable.unify(VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getSubject(), belief, taskSentence, figure,
                            context);
                }
                return;
            default:
                throw new IllegalArgumentException("figure must be 11, 12, 21, or 22");
        }
    }

    /* ----- conditional inferences ----- */
    /**
     * The detachment rule, with variable unification
     *
     * @param originalMainSentence The premise that is an Implication or
     *                             Equivalence
     * @param subSentence          The premise that is the subject or predicate of
     *                             the
     *                             first one
     * @param index                The location of the second premise in the first
     * @param context.getMemory()  Reference to the context.getMemory()
     */
    private static void detachmentWithVar(Sentence originalMainSentence, Sentence subSentence, int index,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Sentence mainSentence = originalMainSentence.cloneSentence(); // for substitution
        final Statement statement = (Statement) mainSentence.getContent();
        final Term component = statement.componentAt(index);
        final CompoundTerm content = (CompoundTerm) subSentence.getContent();
        if (!context.hasCurrentBelief())
            return;
        if (!(component instanceof Inheritance || component instanceof Negation))
            return;
        if (component.isConstant()) {
            SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            return;
        } else if (Variable.unify(VAR_INDEPENDENT, component, content, statement, content)) {
            SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            return;
        } else if ((statement instanceof Implication) && (statement.getPredicate() instanceof Statement)
                && (context.getCurrentTask().isJudgment())) {
            final Statement s2 = (Statement) statement.getPredicate();
            if (s2.getSubject().equals(((Statement) content).getSubject())) {
                CompositionalRules.introVarInner((Statement) content, s2, statement, context);
            }
            CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                    index, context);
            return;
        } else if ((statement instanceof Equivalence) && (statement.getPredicate() instanceof Statement)
                && (context.getCurrentTask().isJudgment())) {
            CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                    index, context);
            return;
        } else {
            return;
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
    private static void conditionalDedIndWithVar(Implication conditional, short index, Statement statement, short side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final CompoundTerm condition = (CompoundTerm) conditional.getSubject();
        final Term component = condition.componentAt(index);
        final Term component2;
        if (statement instanceof Inheritance) {
            component2 = statement;
            side = -1;
        } else if (statement instanceof Implication) {
            component2 = statement.componentAt(side);
        } else {
            component2 = null;
        }
        if (component2 == null)
            return;
        boolean unifiable = Variable.unify(VAR_INDEPENDENT, component, component2, conditional, statement);
        if (!unifiable) {
            // * 🚩惰性求值：第一次替换成功，就无需再次替换
            unifiable = Variable.unify(VAR_DEPENDENT, component, component2, conditional, statement);
        }
        if (unifiable) {
            SyllogisticRules.conditionalDedInd(conditional, index, statement, side, context);
        }
    }

    /* ----- structural inferences ----- */
    /**
     * Inference between a compound term and a component of it
     *
     * @param compound     The compound term
     * @param component    The component term
     * @param compoundTask Whether the compound comes from the task
     * @param context      Reference to the derivation context
     */
    private static void compoundAndSelf(CompoundTerm compound, Term component, boolean compoundTask,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if ((compound instanceof Conjunction) || (compound instanceof Disjunction)) {
            if (context.hasCurrentBelief()) {
                CompositionalRules.decomposeStatement(compound, component, compoundTask, context);
                return;
            } else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(compound, component, compoundTask, context);
                return;
            }
            // } else if ((compound instanceof Negation) &&
            // !context.getCurrentTask().isStructural()) {
            else {
                return;
            }
        } else if (compound instanceof Negation) {
            if (compoundTask) {
                StructuralRules.transformNegation(((Negation) compound).componentAt(0), context);
                return;
            } else {
                StructuralRules.transformNegation(compound, context);
                return;
            }
        } else {
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
    private static void compoundAndCompound(CompoundTerm taskTerm, CompoundTerm beliefTerm,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (taskTerm.getClass() != beliefTerm.getClass())
            return;
        if (taskTerm.size() > beliefTerm.size()) {
            compoundAndSelf(taskTerm, beliefTerm, true, context);
            return;
        } else if (taskTerm.size() < beliefTerm.size()) {
            compoundAndSelf(beliefTerm, taskTerm, false, context);
            return;
        } else {
            return;
        }
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
    private static void compoundAndStatement(CompoundTerm compound, short index, Statement statement, short side,
            Term beliefTerm, DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Term component = compound.componentAt(index);
        final Task task = context.getCurrentTask();
        if (component.getClass() == statement.getClass()) {
            if ((compound instanceof Conjunction) && (context.hasCurrentBelief())) {
                if (Variable.unify(VAR_DEPENDENT, component, statement, compound, statement)) {
                    SyllogisticRules.eliminateVarDep(compound, component, statement.equals(beliefTerm), context);
                } else if (task.isJudgment()) { // && !compound.containComponent(component)) {
                    CompositionalRules.introVarInner(statement, (Statement) component, compound, context);
                } else if (Variable.unify(VAR_QUERY, component, statement, compound, statement)) {
                    CompositionalRules.decomposeStatement(compound, component, true, context);
                }
            }
        } else {
            // if (!task.isStructural() && task.isJudgment()) {
            if (task.isJudgment()) {
                if (statement instanceof Inheritance) {
                    StructuralRules.structuralCompose1(compound, index, statement, context);
                    // if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                    if (!(compound instanceof SetExt || compound instanceof SetInt || compound instanceof Negation)) {
                        StructuralRules.structuralCompose2(compound, index, statement, side, context);
                    } // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                } else if ((statement instanceof Similarity) && !(compound instanceof Conjunction)) {
                    StructuralRules.structuralCompose2(compound, index, statement, side, context);
                } // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
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
    private static void componentAndStatement(CompoundTerm compound, short index, Statement statement, short side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        // if (!context.getCurrentTask().isStructural()) {
        if (statement instanceof Inheritance) {
            StructuralRules.structuralDecompose1(compound, index, statement, context);
            if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
                StructuralRules.structuralDecompose2(statement, index, context);
                return;
            } else {
                StructuralRules.transformSetRelation(compound, statement, side, context);
                return;
            }
        } else if (statement instanceof Similarity) {
            StructuralRules.structuralDecompose2(statement, index, context); // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            if ((compound instanceof SetExt) || (compound instanceof SetInt)) {
                StructuralRules.transformSetRelation(compound, statement, side, context);
            }
            return;
        } else if ((statement instanceof Implication) && (compound instanceof Negation)) {
            if (index == 0) {
                StructuralRules.contraposition(statement, context.getCurrentTask(), context);
                return;
            } else {
                StructuralRules.contraposition(statement, context.getCurrentBelief(), context);
                return;
            }
        } else {
            return;
        }
        // }
    }
}
