package nars.inference;

import java.util.*;
import nars.entity.*;
import nars.language.*;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
public final class CompositionalRules {

    static void IntroVarSameSubjectOrPredicate(
            Sentence originalMainSentence,
            Sentence subSentence, Term component,
            Term content, int index, DerivationContext context) {
        final Sentence cloned = (Sentence) originalMainSentence.clone();
        final Term T1 = cloned.getContent();
        if (!(T1 instanceof CompoundTerm) || !(content instanceof CompoundTerm)) {
            return;
        }
        CompoundTerm T = (CompoundTerm) T1;
        CompoundTerm T2 = (CompoundTerm) content.clone();
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
                final Variable V = new Variable("#depIndVar1");
                final CompoundTerm zw = (CompoundTerm) T.getComponents().get(index).clone();
                final CompoundTerm zw2 = (CompoundTerm) CompoundTerm.setComponent(zw, 1, V, context.memory);
                T2 = (CompoundTerm) CompoundTerm.setComponent(T2, 1, V, context.memory);
                if (zw2 == null || T2 == null || zw2.equals(T2)) {
                    return;
                }
                final Conjunction res = (Conjunction) Conjunction.make(zw, T2, context.memory);
                T = (CompoundTerm) CompoundTerm.setComponent(T, index, res, context.memory);
            } else if (((Statement) component).getSubject().equals(((Statement) content).getSubject())
                    && !(((Statement) component).getSubject() instanceof Variable)) {
                final Variable V = new Variable("#depIndVar2");
                final CompoundTerm zw = (CompoundTerm) T.getComponents().get(index).clone();
                final CompoundTerm zw2 = (CompoundTerm) CompoundTerm.setComponent(zw, 0, V, context.memory);
                T2 = (CompoundTerm) CompoundTerm.setComponent(T2, 0, V, context.memory);
                if (zw2 == null || T2 == null || zw2.equals(T2)) {
                    return;
                }
                final Conjunction res = (Conjunction) Conjunction.make(zw2, T2, context.memory);
                T = (CompoundTerm) CompoundTerm.setComponent(T, index, res, context.memory);
            }
            final TruthValue truth = TruthFunctions.induction(originalMainSentence.getTruth(), subSentence.getTruth());
            final BudgetValue budget = BudgetFunctions.compoundForward(truth, T, context.memory);
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
     * @param taskSentence   The first premise
     * @param belief         The second premise
     * @param index          The location of the shared term
     * @param context.memory Reference to the context.memory
     */
    static void composeCompound(
            Statement taskContent,
            Statement beliefContent,
            int index,
            DerivationContext context) {
        if ((!context.currentTask.getSentence().isJudgment())
                || (taskContent.getClass() != beliefContent.getClass())) {
            return;
        }
        final Term componentT = taskContent.componentAt(1 - index);
        final Term componentB = beliefContent.componentAt(1 - index);
        final Term componentCommon = taskContent.componentAt(index);
        if ((componentT instanceof CompoundTerm) && ((CompoundTerm) componentT).containAllComponents(componentB)) {
            decomposeCompound((CompoundTerm) componentT, componentB, componentCommon, index, true, context);
            return;
        } else if ((componentB instanceof CompoundTerm)
                && ((CompoundTerm) componentB).containAllComponents(componentT)) {
            decomposeCompound((CompoundTerm) componentB, componentT, componentCommon, index, false, context);
            return;
        }
        final TruthValue truthT = context.currentTask.getSentence().getTruth();
        final TruthValue truthB = context.currentBelief.getTruth();
        final TruthValue truthOr = TruthFunctions.union(truthT, truthB);
        final TruthValue truthAnd = TruthFunctions.intersection(truthT, truthB);
        TruthValue truthDif = null;
        Term termOr = null;
        Term termAnd = null;
        Term termDif = null;
        if (index == 0) {
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionInt.make(componentT, componentB, context.memory);
                termAnd = IntersectionExt.make(componentT, componentB, context.memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceExt.make(componentT, componentB, context.memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceExt.make(componentB, componentT, context.memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = Disjunction.make(componentT, componentB, context.memory);
                termAnd = Conjunction.make(componentT, componentB, context.memory);
            }
            processComposed(taskContent, (Term) componentCommon.clone(), termOr, truthOr, context);
            processComposed(taskContent, (Term) componentCommon.clone(), termAnd, truthAnd, context);
            processComposed(taskContent, (Term) componentCommon.clone(), termDif, truthDif, context);
        } else { // index == 1
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionExt.make(componentT, componentB, context.memory);
                termAnd = IntersectionInt.make(componentT, componentB, context.memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceInt.make(componentT, componentB, context.memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceInt.make(componentB, componentT, context.memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = Conjunction.make(componentT, componentB, context.memory);
                termAnd = Disjunction.make(componentT, componentB, context.memory);
            }
            processComposed(taskContent, termOr, (Term) componentCommon.clone(), truthOr, context);
            processComposed(taskContent, termAnd, (Term) componentCommon.clone(), truthAnd, context);
            processComposed(taskContent, termDif, (Term) componentCommon.clone(), truthDif, context);
        }
        if (taskContent instanceof Inheritance) {
            introVarOuter(taskContent, beliefContent, index, context);
            // introVarImage(taskContent, beliefContent, index, context.memory);
        }
    }

    /**
     * Finish composing implication term
     *
     * @param premise1       Type of the contentInd
     * @param subject        Subject of contentInd
     * @param predicate      Predicate of contentInd
     * @param truth          TruthValue of the contentInd
     * @param context.memory Reference to the context.memory
     */
    private static void processComposed(
            Statement statement,
            Term subject, Term predicate, TruthValue truth,
            DerivationContext context) {
        if ((subject == null) || (predicate == null)) {
            return;
        }
        final Term content = Statement.make(statement, subject, predicate, context.memory);
        if ((content == null) || content.equals(statement)
                || content.equals(context.currentBelief.getContent())) {
            return;
        }
        final BudgetValue budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(S|P) ==> M>, <P ==> M>} |- <S ==> M>
     *
     * @param implication     The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param term1           The other term in the contentInd
     * @param index           The location of the shared term: 0 for subject, 1 for
     *                        predicate
     * @param compoundTask    Whether the implication comes from the task
     * @param context.memory  Reference to the context.memory
     */
    private static void decomposeCompound(
            CompoundTerm compound, Term component,
            Term term1, int index,
            boolean compoundTask, DerivationContext context) {
        if ((compound instanceof Statement) || (compound instanceof ImageExt) || (compound instanceof ImageInt)) {
            return;
        }
        final Term term2 = CompoundTerm.reduceComponents(compound, component, context.memory);
        if (term2 == null) {
            return;
        }
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        final Sentence belief = context.currentBelief;
        final Statement oldContent = (Statement) task.getContent();
        final TruthValue v1, v2;
        if (compoundTask) {
            v1 = sentence.getTruth();
            v2 = belief.getTruth();
        } else {
            v1 = belief.getTruth();
            v2 = sentence.getTruth();
        }
        TruthValue truth = null;
        final Term content;
        if (index == 0) {
            content = Statement.make(oldContent, term1, term2, context.memory);
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
            content = Statement.make(oldContent, term2, term1, context.memory);
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
            final BudgetValue budget = BudgetFunctions.compoundForward(truth, content, context.memory);
            context.doublePremiseTask(content, truth, budget);
        }
    }

    /**
     * {(||, S, P), P} |- S {(&&, S, P), P} |- S
     *
     * @param implication     The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param compoundTask    Whether the implication comes from the task
     * @param context.memory  Reference to the context.memory
     */
    static void decomposeStatement(
            CompoundTerm compound, Term component,
            boolean compoundTask, DerivationContext context) {
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        final Sentence belief = context.currentBelief;
        final Term content = CompoundTerm.reduceComponents(compound, component, context.memory);
        if (content == null) {
            return;
        }
        TruthValue truth = null;
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
            context.doublePremiseTask(content, truth, budget);
            // special inference to answer conjunctive questions with query variables
            if (Variable.containVarQ(sentence.getContent().getName())) {
                final Concept contentConcept = context.memory.termToConcept(content);
                if (contentConcept == null) {
                    return;
                }
                final Sentence contentBelief = contentConcept.getBelief(task);
                if (contentBelief == null) {
                    return;
                }
                final Task contentTask = new Task(contentBelief, task.getBudget());
                context.currentTask = contentTask;
                final Term conj = Conjunction.make(component, content, context.memory);
                // * ↓不会用到`context.currentTask`
                truth = TruthFunctions.intersection(contentBelief.getTruth(), belief.getTruth());
                // * ↓不会用到`context.currentTask`
                budget = BudgetFunctions.compoundForward(truth, conj, context.memory);
                // ! ⚠️↓会用到`context.currentTask`：构建新结论时要用到
                context.doublePremiseTask(conj, truth, budget);
            }
        } else {
            final TruthValue v1, v2;
            if (compoundTask) {
                v1 = sentence.getTruth();
                v2 = belief.getTruth();
            } else {
                v1 = belief.getTruth();
                v2 = sentence.getTruth();
            }
            if (compound instanceof Conjunction) {
                if (sentence instanceof Sentence) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                }
            } else if (compound instanceof Disjunction) {
                if (sentence instanceof Sentence) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            } else {
                return;
            }
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
            context.doublePremiseTask(content, truth, budget);
        }
    }

    /* --------------- rules used for variable introduction --------------- */
    /**
     * Introduce a dependent variable in an outer-layer conjunction
     *
     * @param taskContent    The first premise <M --> S>
     * @param beliefContent  The second premise <M --> P>
     * @param index          The location of the shared term: 0 for subject, 1 for
     *                       predicate
     * @param context.memory Reference to the context.memory
     */
    private static void introVarOuter(
            Statement taskContent,
            Statement beliefContent,
            int index, DerivationContext context) {
        final TruthValue truthT = context.currentTask.getSentence().getTruth();
        final TruthValue truthB = context.currentBelief.getTruth();
        final Variable varInd = new Variable("$varInd1");
        final Variable varInd2 = new Variable("$varInd2");
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

        final Statement state1 = Inheritance.make(term11, term12, context.memory);
        final Statement state2 = Inheritance.make(term21, term22, context.memory);
        Term content = Implication.make(state1, state2, context.memory);
        if (content == null) {
            return;
        }
        TruthValue truth;
        BudgetValue budget;
        truth = TruthFunctions.induction(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        context.doublePremiseTask(content, truth, budget);

        content = Implication.make(state2, state1, context.memory);
        truth = TruthFunctions.induction(truthB, truthT);
        budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        context.doublePremiseTask(content, truth, budget);

        content = Equivalence.make(state1, state2, context.memory);
        truth = TruthFunctions.comparison(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        context.doublePremiseTask(content, truth, budget);

        final Variable varDep = new Variable("#varDep");
        final Statement newState1, newState2;
        if (index == 0) {
            newState1 = Inheritance.make(varDep, taskContent.getPredicate(), context.memory);
            newState2 = Inheritance.make(varDep, beliefContent.getPredicate(), context.memory);
        } else {
            newState1 = Inheritance.make(taskContent.getSubject(), varDep, context.memory);
            newState2 = Inheritance.make(beliefContent.getSubject(), varDep, context.memory);
        }
        content = Conjunction.make(newState1, newState2, context.memory);
        truth = TruthFunctions.intersection(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        context.doublePremiseTask(content, truth, budget, false);
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent    The first premise directly used in internal induction,
     *                       <M --> S>
     * @param beliefContent  The componentCommon to be used as a premise in
     *                       internal induction, <M --> P>
     * @param oldCompound    The whole contentInd of the first premise, Implication
     *                       or Conjunction
     * @param context.memory Reference to the context.memory
     */
    static void introVarInner(Statement premise1, Statement premise2, CompoundTerm oldCompound,
            DerivationContext context) {
        final Task task = context.currentTask;
        final Sentence taskSentence = task.getSentence();
        if (!taskSentence.isJudgment() || (premise1.getClass() != premise2.getClass())
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
        final Sentence belief = context.currentBelief;
        final HashMap<Term, Term> substitute = new HashMap<>();
        substitute.put(commonTerm1, new Variable("#varDep2"));
        CompoundTerm content = (CompoundTerm) Conjunction.make(premise1, oldCompound, context.memory);
        content.applySubstitute(substitute);
        TruthValue truth = TruthFunctions.intersection(taskSentence.getTruth(), belief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, context.memory);
        context.doublePremiseTask(content, truth, budget, false);
        substitute.clear();
        substitute.put(commonTerm1, new Variable("$varInd1"));
        if (commonTerm2 != null) {
            substitute.put(commonTerm2, new Variable("$varInd2"));
        }
        content = Implication.make(premise1, oldCompound, context.memory);
        if (content == null) {
            return;
        }
        content.applySubstitute(substitute);
        if (premise1.equals(taskSentence.getContent())) {
            truth = TruthFunctions.induction(belief.getTruth(), taskSentence.getTruth());
        } else {
            truth = TruthFunctions.induction(taskSentence.getTruth(), belief.getTruth());
        }
        budget = BudgetFunctions.forward(truth, context.memory);
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
