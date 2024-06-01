package nars.inference;

import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirect processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
public class RuleTables {

    /**
     * Entry point of the inference engine
     * 
     * TODO: 追溯调用是否均以「导出结论」终止（若有）
     *
     * @param tLink   The selected TaskLink, which will provide a task
     * @param bLink   The selected TermLink, which may provide a belief
     * @param context Reference to the derivation context
     */
    public static void reason(DerivationContextReason context) {
        final TaskLink tLink = context.getCurrentTaskLink();
        final TermLink bLink = context.getCurrentBeliefLink();
        final Task task = context.getCurrentTask();
        final Sentence taskSentence = task.getSentence();
        final Term taskTerm = taskSentence.getContent().clone(); // cloning for substitution
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
        if (!context.getMemory().noResult() && task.getSentence().isJudgment()) {
            return;
        }
        // * 📝词项链所指的词项，不一定指向一个确切的「信念」（并非「语句链」）
        final short tIndex = tLink.getIndex(0);
        final short bIndex = bLink.getIndex(0);
        switch (tLink.getType()) { // dispatch first by TaskLink type
            case TermLink.SELF:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        compoundAndSelf((CompoundTerm) taskTerm, beliefTerm, true, context);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndSelf((CompoundTerm) beliefTerm, taskTerm, false, context);
                        break;
                    case TermLink.COMPONENT_STATEMENT:
                        if (belief != null) {
                            SyllogisticRules.detachment(task.getSentence(), belief, bIndex, context);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            SyllogisticRules.detachment(belief, task.getSentence(), bIndex, context);
                        }
                        break;
                    case TermLink.COMPONENT_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) taskTerm, bIndex2, beliefTerm, tIndex,
                                    context);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex2, taskTerm, tIndex,
                                    context);
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        compoundAndCompound((CompoundTerm) taskTerm, (CompoundTerm) beliefTerm, context);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        compoundAndStatement((CompoundTerm) taskTerm, tIndex, (Statement) beliefTerm, bIndex,
                                beliefTerm, context);
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            if (beliefTerm instanceof Implication) {
                                if (Variable.unify(Symbols.VAR_INDEPENDENT, ((Implication) beliefTerm).getSubject(),
                                        taskTerm, beliefTerm, taskTerm)) {
                                    detachmentWithVar(belief, taskSentence, bIndex, context);
                                } else {
                                    SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex, taskTerm, -1,
                                            context);
                                }
                            } else if (beliefTerm instanceof Equivalence) {
                                SyllogisticRules.conditionalAna((Equivalence) beliefTerm, bIndex, taskTerm, -1,
                                        context);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_STATEMENT:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        componentAndStatement((CompoundTerm) context.getCurrentTerm(), bIndex, (Statement) taskTerm,
                                tIndex,
                                context);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndStatement((CompoundTerm) beliefTerm, bIndex, (Statement) taskTerm, tIndex,
                                beliefTerm, context);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            syllogisms(tLink, bLink, taskTerm, beliefTerm, context);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            if (beliefTerm instanceof Implication) {
                                conditionalDedIndWithVar((Implication) beliefTerm, bIndex2, (Statement) taskTerm,
                                        tIndex, context);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_CONDITION:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        if (belief != null) {
                            detachmentWithVar(taskSentence, belief, tIndex, context);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            // TODO maybe put instanceof test within conditionalDedIndWithVar()
                            if (taskTerm instanceof Implication) {
                                Term subj = ((Implication) taskTerm).getSubject();
                                if (subj instanceof Negation) {
                                    if (task.getSentence().isJudgment()) {
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
                            break;
                        }
                        break;
                }
        }
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
    private static void syllogisms(TaskLink tLink, TermLink bLink, Term taskTerm, Term beliefTerm,
            DerivationContextReason context) {
        final Sentence taskSentence = context.getCurrentTask().getSentence();
        final Sentence belief = context.getCurrentBelief();
        final int figure;
        if (taskTerm instanceof Inheritance) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, context);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, context);
            } else {
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0), context);
            }
        } else if (taskTerm instanceof Similarity) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, context);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, context);
            }
        } else if (taskTerm instanceof Implication) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, context);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, context);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), context);
            }
        } else if (taskTerm instanceof Equivalence) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, context);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, context);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), context);
            }
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
        final Statement s1 = (Statement) sentence.cloneContent();
        final Statement s2 = (Statement) belief.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11: // induction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getPredicate();
                    t2 = s1.getPredicate();
                    CompositionalRules.composeCompound(s1, s2, 0, context);
                    SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, context);
                }

                break;
            case 12: // deduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getSubject();
                    t2 = s1.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(context);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, context);
                    }
                }
                break;
            case 21: // exemplification
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(context);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, context);
                    }
                }
                break;
            case 22: // abduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
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
                break;
            default:
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
        final Statement asymSt = (Statement) asym.cloneContent();
        final Statement symSt = (Statement) sym.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, context);
                    }
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getPredicate(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getSubject();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, context);
                    }
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, context);
                    }
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getPredicate(), asymSt,
                        symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getSubject();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, context);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, context);
                    }
                }
                break;
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
        final Statement s1 = (Statement) belief.cloneContent();
        final Statement s2 = (Statement) taskSentence.cloneContent();
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getPredicate(), belief, taskSentence, figure,
                            context);
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getSubject(), belief, taskSentence, figure,
                            context);
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getPredicate(), belief, taskSentence, figure,
                            context);
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getSubject(), belief, taskSentence, figure,
                            context);
                }
                break;
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
        final Sentence mainSentence = originalMainSentence.cloneSentence(); // for substitution
        final Statement statement = (Statement) mainSentence.getContent();
        final Term component = statement.componentAt(index);
        final CompoundTerm content = (CompoundTerm) subSentence.getContent();
        if (((component instanceof Inheritance) || (component instanceof Negation))
                && (context.getCurrentBelief() != null)) {
            if (component.isConstant()) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            } else if (Variable.unify(Symbols.VAR_INDEPENDENT, component, content, statement, content)) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, context);
            } else if ((statement instanceof Implication) && (statement.getPredicate() instanceof Statement)
                    && (context.getCurrentTask().getSentence().isJudgment())) {
                final Statement s2 = (Statement) statement.getPredicate();
                if (s2.getSubject().equals(((Statement) content).getSubject())) {
                    CompositionalRules.introVarInner((Statement) content, s2, statement, context);
                }
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                        index, context);
            } else if ((statement instanceof Equivalence) && (statement.getPredicate() instanceof Statement)
                    && (context.getCurrentTask().getSentence().isJudgment())) {
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                        index, context);
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
    private static void conditionalDedIndWithVar(Implication conditional, short index, Statement statement, short side,
            DerivationContextReason context) {
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
        if (component2 != null) {
            boolean unifiable = Variable.unify(Symbols.VAR_INDEPENDENT, component, component2, conditional, statement);
            if (!unifiable) {
                unifiable = Variable.unify(Symbols.VAR_DEPENDENT, component, component2, conditional, statement);
            }
            if (unifiable) {
                SyllogisticRules.conditionalDedInd(conditional, index, statement, side, context);
            }
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
        if ((compound instanceof Conjunction) || (compound instanceof Disjunction)) {
            if (context.getCurrentBelief() != null) {
                CompositionalRules.decomposeStatement(compound, component, compoundTask, context);
            } else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(compound, component, compoundTask, context);
            }
            // } else if ((compound instanceof Negation) &&
            // !context.getCurrentTask().isStructural()) {
        } else if (compound instanceof Negation) {
            if (compoundTask) {
                StructuralRules.transformNegation(((Negation) compound).componentAt(0), context);
            } else {
                StructuralRules.transformNegation(compound, context);
            }
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
        if (taskTerm.getClass() == beliefTerm.getClass()) {
            if (taskTerm.size() > beliefTerm.size()) {
                compoundAndSelf(taskTerm, beliefTerm, true, context);
            } else if (taskTerm.size() < beliefTerm.size()) {
                compoundAndSelf(beliefTerm, taskTerm, false, context);
            }
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
        final Term component = compound.componentAt(index);
        final Task task = context.getCurrentTask();
        if (component.getClass() == statement.getClass()) {
            if ((compound instanceof Conjunction) && (context.getCurrentBelief() != null)) {
                if (Variable.unify(Symbols.VAR_DEPENDENT, component, statement, compound, statement)) {
                    SyllogisticRules.eliminateVarDep(compound, component, statement.equals(beliefTerm), context);
                } else if (task.getSentence().isJudgment()) { // && !compound.containComponent(component)) {
                    CompositionalRules.introVarInner(statement, (Statement) component, compound, context);
                } else if (Variable.unify(Symbols.VAR_QUERY, component, statement, compound, statement)) {
                    CompositionalRules.decomposeStatement(compound, component, true, context);
                }
            }
        } else {
            // if (!task.isStructural() && task.getSentence().isJudgment()) {
            if (task.getSentence().isJudgment()) {
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
        // if (!context.getCurrentTask().isStructural()) {
        if (statement instanceof Inheritance) {
            StructuralRules.structuralDecompose1(compound, index, statement, context);
            if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                StructuralRules.structuralDecompose2(statement, index, context); // {(C-B) --> (C-A), A @ (C-A)} |- A
                                                                                 // -->
                                                                                 // B
            } else {
                StructuralRules.transformSetRelation(compound, statement, side, context);
            }
        } else if (statement instanceof Similarity) {
            StructuralRules.structuralDecompose2(statement, index, context); // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            if ((compound instanceof SetExt) || (compound instanceof SetInt)) {
                StructuralRules.transformSetRelation(compound, statement, side, context);
            }
        } else if ((statement instanceof Implication) && (compound instanceof Negation)) {
            if (index == 0) {
                StructuralRules.contraposition(statement, context.getCurrentTask().getSentence(), context);
            } else {
                StructuralRules.contraposition(statement, context.getCurrentBelief(), context);
            }
        }
        // }
    }

}
