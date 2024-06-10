package nars.inference;

import java.util.*;

import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.inference.TruthFunctions.TruthFDouble;
import nars.language.*;

import static nars.io.Symbols.JUDGMENT_MARK;
import static nars.io.Symbols.QUESTION_MARK;
import static nars.language.MakeTerm.*;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
public final class CompositionalRules {

    static void IntroVarSameSubjectOrPredicate(
            Judgement originalMainSentence,
            Judgement subSentence, Term component,
            CompoundTerm content, int index,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Sentence cloned = originalMainSentence.sentenceClone();
        final Term T1 = cloned.getContent();
        if (!(T1 instanceof CompoundTerm) || !(content instanceof CompoundTerm)) {
            return;
        }
        CompoundTerm T = (CompoundTerm) T1;
        CompoundTerm T2 = content.clone();
        if ((component instanceof Inheritance && content instanceof Inheritance) ||
                (component instanceof Similarity && content instanceof Similarity)) {
            // CompoundTerm result = T;
            if (component.equals(content)) {
                // wouldn't make sense to create a conjunction here,
                // would contain a statement twice
                return;
            }
            if (((Statement) component).getPredicate().equals(((Statement) content).getPredicate())
                    && !(((Statement) component).getPredicate() instanceof Variable)) {
                final Variable V = Variable.newVarD("depIndVar1"); // * ✅不怕重名：其它变量一定会被命名为数字
                final CompoundTerm zw = (CompoundTerm) T.getComponents().get(index).clone();
                final CompoundTerm zw2 = (CompoundTerm) setComponent(zw, 1, V);
                T2 = (CompoundTerm) setComponent(T2, 1, V);
                if (zw2 == null || T2 == null || zw2.equals(T2)) {
                    return;
                }
                final Conjunction res = (Conjunction) makeConjunction(zw, T2);
                T = (CompoundTerm) setComponent(T, index, res);
            } else if (((Statement) component).getSubject().equals(((Statement) content).getSubject())
                    && !(((Statement) component).getSubject() instanceof Variable)) {
                final Variable V = Variable.newVarD("depIndVar2"); // * ✅不怕重名：其它变量一定会被命名为数字
                final CompoundTerm zw = (CompoundTerm) T.getComponents().get(index).clone();
                final CompoundTerm zw2 = (CompoundTerm) setComponent(zw, 0, V);
                T2 = (CompoundTerm) setComponent(T2, 0, V);
                if (zw2 == null || T2 == null || zw2.equals(T2)) {
                    return;
                }
                final Conjunction res = (Conjunction) makeConjunction(zw2, T2);
                T = (CompoundTerm) setComponent(T, index, res);
            }
            final Truth truth = TruthFunctions.induction(originalMainSentence, subSentence);
            final Budget budget = BudgetFunctions.compoundForward(truth, T, context);
            context.doublePremiseTask(T, truth, budget);
        }
    }

    /* -------------------- intersections and differences -------------------- */
    /**
     * {<S ==> M>, <P ==> M>} |- {
     * <(S|P) ==> M>, <(S&P) ==> M>,
     * <(S-P) ==> M>, <(P-S) ==> M>
     * }
     *
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param sharedTermI  The location of the shared term
     * @param context      Reference to the derivation context
     */
    static void composeCompound(
            Statement taskContent,
            Statement beliefContent,
            int sharedTermI,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if ((!context.getCurrentTask().isJudgment())
                || !(taskContent.isSameType(beliefContent))) {
            return;
        }
        final Term componentT = taskContent.componentAt(1 - sharedTermI);
        final Term componentB = beliefContent.componentAt(1 - sharedTermI);
        final Term componentCommon = taskContent.componentAt(sharedTermI);
        if ((componentT instanceof CompoundTerm) && ((CompoundTerm) componentT).containAllComponents(componentB)) {
            decomposeCompound((CompoundTerm) componentT, componentB, componentCommon, sharedTermI, true, context);
            return;
        } else if ((componentB instanceof CompoundTerm)
                && ((CompoundTerm) componentB).containAllComponents(componentT)) {
            decomposeCompound((CompoundTerm) componentB, componentT, componentCommon, sharedTermI, false, context);
            return;
        }
        final Truth truthT = context.getCurrentTask().asJudgement();
        final Truth truthB = context.getCurrentBelief();
        final Truth truthOr = TruthFunctions.union(truthT, truthB);
        final Truth truthAnd = TruthFunctions.intersection(truthT, truthB);
        Truth truthDif = null;
        Term termOr = null;
        Term termAnd = null;
        Term termDif = null;
        if (sharedTermI == 0) {
            if (taskContent instanceof Inheritance) {
                termOr = makeIntersectionInt(componentT, componentB);
                termAnd = makeIntersectionExt(componentT, componentB);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = makeDifferenceExt(componentT, componentB);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = makeDifferenceExt(componentB, componentT);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = makeDisjunction(componentT, componentB);
                termAnd = makeConjunction(componentT, componentB);
            }
            processComposed(taskContent, componentCommon.clone(), termOr, truthOr, context);
            processComposed(taskContent, componentCommon.clone(), termAnd, truthAnd, context);
            processComposed(taskContent, componentCommon.clone(), termDif, truthDif, context);
        } else { // index == 1
            if (taskContent instanceof Inheritance) {
                termOr = makeIntersectionExt(componentT, componentB);
                termAnd = makeIntersectionInt(componentT, componentB);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = makeDifferenceInt(componentT, componentB);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = makeDifferenceInt(componentB, componentT);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = makeConjunction(componentT, componentB);
                termAnd = makeDisjunction(componentT, componentB);
            }
            processComposed(taskContent, termOr, componentCommon.clone(), truthOr, context);
            processComposed(taskContent, termAnd, componentCommon.clone(), truthAnd, context);
            processComposed(taskContent, termDif, componentCommon.clone(), truthDif, context);
        }
        if (taskContent instanceof Inheritance) {
            introVarOuter(taskContent, beliefContent, sharedTermI, context);
            // introVarImage(taskContent, beliefContent, index);
        }
    }

    /**
     * Finish composing implication term
     *
     * @param premise1  Type of the contentInd
     * @param subject   Subject of contentInd
     * @param predicate Predicate of contentInd
     * @param truth     Truth of the contentInd
     * @param context   Reference to the derivation context
     */
    private static void processComposed(
            Statement statement,
            Term subject, Term predicate, Truth truth,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if ((subject == null) || (predicate == null)) {
            return;
        }
        final Term content = makeStatement(statement, subject, predicate);
        if ((content == null) || content.equals(statement)
                || content.equals(context.getCurrentBelief().getContent())) {
            return;
        }
        final Budget budget = BudgetFunctions.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(S|P) ==> M>, <P ==> M>} |- <S ==> M>
     *
     * @param implication        The implication term to be decomposed
     * @param componentCommon    The part of the implication to be removed
     * @param term1              The other term in the contentInd
     * @param index              The location of the shared term: 0 for subject, 1
     *                           for predicate
     * @param isCompoundFromTask Whether the implication comes from the task
     * @param context            Reference to the derivation context
     */
    private static void decomposeCompound(
            CompoundTerm compound, Term component,
            Term term1, int index,
            boolean isCompoundFromTask, DerivationContextReason context) {
        // TODO: 过程笔记注释
        if ((compound instanceof Statement) || (compound instanceof ImageExt) || (compound instanceof ImageInt)) {
            return;
        }
        final Term term2 = reduceComponents(compound, component);
        if (term2 == null) {
            return;
        }
        final Judgement taskJudgement = context.getCurrentTask().asJudgement();
        final Judgement belief = context.getCurrentBelief();
        final Statement oldContent = (Statement) taskJudgement.getContent();
        final Truth v1, v2;
        if (isCompoundFromTask) {
            v1 = taskJudgement;
            v2 = belief;
        } else {
            v1 = belief;
            v2 = taskJudgement;
        }
        Truth truth = null;
        final Term content;
        if (index == 0) {
            content = makeStatement(oldContent, term1, term2);
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceExt) {
                    if (compound.componentAt(0).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        } else {
            content = makeStatement(oldContent, term2, term1);
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceInt) {
                    if (compound.componentAt(1).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        }
        if (truth != null) {
            final Budget budget = BudgetFunctions.compoundForward(truth, content, context);
            context.doublePremiseTask(content, truth, budget);
        }
    }

    /**
     * {(||, S, P), P} |- S
     * {(&&, S, P), P} |- S
     *
     * @param implication        The implication term to be decomposed
     * @param componentCommon    The part of the implication to be removed
     * @param isCompoundFromTask Whether the implication comes from the task
     * @param context            Reference to the derivation context
     */
    static void decomposeStatement(
            CompoundTerm compound, Term component,
            boolean isCompoundFromTask,
            DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        // * 🚩删去指定的那个元素，用删去之后的剩余元素做结论
        final Term content = reduceComponents(compound, component);
        if (content == null)
            return;
        final Truth truth;
        final Budget budget;
        switch (task.getPunctuation()) {
            case QUESTION_MARK:
                // * 📄(||,A,B)? + A. => B?
                // * 🚩先将剩余部分作为「问题」提出
                // ! 📄原版bug：当输入 (||,A,?1)? 时，因「弹出的变量复杂度为零」预算推理「除以零」爆炸
                if (!content.zeroComplexity()) {
                    budget = BudgetFunctions.compoundBackward(content, context);
                    context.doublePremiseTask(content, null, budget);
                }
                // * 🚩再将对应有「概念」与「信念」的内容作为新的「信念」放出
                // special inference to answer conjunctive questions with query variables
                if (!Variable.containVarQ(task.getContent()))
                    return;
                // * 🚩只有在「回答合取问题」时，取出其中的项构建新任务
                final Concept contentConcept = context.termToConcept(content);
                if (contentConcept == null)
                    return;
                // * 🚩只在「内容对应了概念」时，取出「概念」中的信念
                final Judgement contentBelief = contentConcept.getBelief(task);
                if (contentBelief == null)
                    return;
                // * 🚩只在「概念中有信念」时，以这个信念作为「当前信念」构建新任务
                final Stamp newStamp = Stamp.uncheckedMerge(
                        task,
                        contentBelief, // * 🚩实际上就是需要与「已有信念」的证据基合并
                        context.getTime());
                // * 🚩【2024-06-07 13:41:16】现在直接从「任务」构造新的「预算值」
                final Task contentTask = new TaskV1(contentBelief, task);
                // ! 🚩【2024-05-19 20:29:17】现在移除：直接在「导出结论」处指定
                final Term conj = makeConjunction(component, content);
                // * ↓不会用到`context.getCurrentTask()`、`newStamp`
                final Truth truth1 = TruthFunctions.intersection(contentBelief, belief);
                // * ↓不会用到`context.getCurrentTask()`、`newStamp`
                final Budget budget1 = BudgetFunctions.compoundForward(truth1, conj, context);
                // ! ⚠️↓会用到`context.getCurrentTask()`、`newStamp`：构建新结论时要用到
                // * ✅【2024-05-21 22:38:52】现在通过「参数传递」抵消了对`context.getCurrentTask`的访问
                context.doublePremiseTask(contentTask, conj, truth1, budget1, newStamp);
                return;
            case JUDGMENT_MARK:
                // * 🚩选取前提真值 | ⚠️前后件语义不同
                final Truth v1, v2;
                if (isCompoundFromTask) {
                    v1 = task.asJudgement();
                    v2 = belief;
                } else {
                    v1 = belief;
                    v2 = task.asJudgement();
                }
                // * 🚩选取真值函数
                final TruthFDouble truthF;
                if (compound instanceof Conjunction)
                    truthF = TruthFunctions::reduceConjunction;
                else if (compound instanceof Disjunction)
                    truthF = TruthFunctions::reduceDisjunction;
                else
                    return;
                // * 🚩构造真值、预算值，双前提结论
                truth = truthF.call(v1, v2);
                budget = BudgetFunctions.compoundForward(truth, content, context);
                context.doublePremiseTask(content, truth, budget);
                return;
            default:
                System.err.println("未知的语句类型: " + task);
        }
    }

    /* --------------- rules used for variable introduction --------------- */
    /**
     * Introduce a dependent variable in an outer-layer conjunction
     *
     * @param taskContent   The first premise <M --> S>
     * @param beliefContent The second premise <M --> P>
     * @param index         The location of the shared term: 0 for subject, 1 for
     *                      predicate
     * @param context       Reference to the derivation context
     */
    private static void introVarOuter(
            Statement taskContent,
            Statement beliefContent,
            int index, DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Truth truthT = context.getCurrentTask().asJudgement();
        final Truth truthB = context.getCurrentBelief();
        final Variable varInd = Variable.newVarI("varInd1");
        final Variable varInd2 = Variable.newVarI("varInd2");
        final Term term11, term12, term21, term22;
        Term commonTerm;
        final HashMap<Term, Term> subs = new HashMap<>();
        if (index == 0) {
            term11 = varInd;
            term21 = varInd;
            term12 = taskContent.getPredicate();
            term22 = beliefContent.getPredicate();
            if ((term12 instanceof ImageExt) && (term22 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term12).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term22).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term22).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term12).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageExt) term12).applySubstitute(subs);
                    ((ImageExt) term22).applySubstitute(subs);
                }
            }
        } else {
            term11 = taskContent.getSubject();
            term21 = beliefContent.getSubject();
            term12 = varInd;
            term22 = varInd;
            if ((term11 instanceof ImageInt) && (term21 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term11).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term21).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term21).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageInt) term11).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageInt) term11).applySubstitute(subs);
                    ((ImageInt) term21).applySubstitute(subs);
                }
            }
        }

        final Statement state1 = makeInheritance(term11, term12);
        final Statement state2 = makeInheritance(term21, term22);
        Term content = makeImplication(state1, state2);
        if (content == null) {
            return;
        }
        Truth truth;
        Budget budget;
        truth = TruthFunctions.induction(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);

        content = makeImplication(state2, state1);
        truth = TruthFunctions.induction(truthB, truthT);
        budget = BudgetFunctions.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);

        content = makeEquivalence(state1, state2);
        truth = TruthFunctions.comparison(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);

        final Variable varDep = Variable.newVarD("varDep");
        final Statement newState1, newState2;
        if (index == 0) {
            newState1 = makeInheritance(varDep, taskContent.getPredicate());
            newState2 = makeInheritance(varDep, beliefContent.getPredicate());
        } else {
            newState1 = makeInheritance(taskContent.getSubject(), varDep);
            newState2 = makeInheritance(beliefContent.getSubject(), varDep);
        }
        content = makeConjunction(newState1, newState2);
        truth = TruthFunctions.intersection(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget, false);
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent   The first premise directly used in internal induction,
     *                      <M --> S>
     * @param beliefContent The componentCommon to be used as a premise in
     *                      internal induction, <M --> P>
     * @param oldCompound   The whole contentInd of the first premise, Implication
     *                      or Conjunction
     * @param context       Reference to the derivation context
     */
    static void introVarInner(
            Statement premise1, Statement premise2,
            CompoundTerm oldCompound,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        if (!task.isJudgment() || (!premise1.isSameType(premise2))
                || oldCompound.containComponent(premise1)) {
            return;
        }
        final Term subject1 = premise1.getSubject();
        final Term subject2 = premise2.getSubject();
        final Term predicate1 = premise1.getPredicate();
        final Term predicate2 = premise2.getPredicate();
        final Term commonTerm1, commonTerm2;
        if (subject1.equals(subject2)) {
            commonTerm1 = subject1;
            commonTerm2 = secondCommonTerm(predicate1, predicate2, 0);
        } else if (predicate1.equals(predicate2)) {
            commonTerm1 = predicate1;
            commonTerm2 = secondCommonTerm(subject1, subject2, 0);
        } else {
            return;
        }
        final Judgement belief = context.getCurrentBelief();
        final HashMap<Term, Term> substitute = new HashMap<>();
        substitute.put(commonTerm1, Variable.newVarD("varDep2"));
        CompoundTerm content = (CompoundTerm) makeConjunction(premise1, oldCompound);
        content.applySubstitute(substitute);
        Truth truth = TruthFunctions.intersection(task.asJudgement(), belief);
        Budget budget = BudgetFunctions.forward(truth, context);
        context.doublePremiseTask(content, truth, budget, false);
        substitute.clear();
        substitute.put(commonTerm1, Variable.newVarI("varInd1"));
        if (commonTerm2 != null) {
            substitute.put(commonTerm2, Variable.newVarI("varInd2"));
        }
        content = makeImplication(premise1, oldCompound);
        if (content == null) {
            return;
        }
        content.applySubstitute(substitute);
        if (premise1.equals(task.getContent())) {
            truth = TruthFunctions.induction(belief, task.asJudgement());
        } else {
            truth = TruthFunctions.induction(task.asJudgement(), belief);
        }
        budget = BudgetFunctions.forward(truth, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * Introduce a second independent variable into two terms with a common
     * component
     *
     * @param term1 The first term
     * @param term2 The second term
     * @param index The index of the terms in their statement
     */
    private static Term secondCommonTerm(Term term1, Term term2, int index) {
        // TODO: 过程笔记注释
        Term commonTerm = null;
        if (index == 0) {
            if ((term1 instanceof ImageExt) && (term2 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        } else {
            if ((term1 instanceof ImageInt) && (term2 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        }
        return commonTerm;
    }
}
