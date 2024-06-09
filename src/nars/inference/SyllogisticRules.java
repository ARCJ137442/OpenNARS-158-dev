package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import static nars.control.MakeTerm.*;

import nars.control.DerivationContextReason;
import nars.control.VariableInference;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
final class SyllogisticRules {

    /*
     * --------------- rules used in both
     * first-tense inference and higher-tense
     * inference ---------------
     */
    /**
     * <pre>
     * {<S ==> M>, <M ==> P>} |- {<S ==> P>, <P ==> S>}
     * </pre>
     *
     * @param term1    Subject of the first new task
     * @param term2    Predicate of the first new task
     * @param sentence The first premise
     * @param belief   The second premise
     * @param context  Reference to the derivation context
     */
    static void dedExe(Term term1, Term term2, Sentence sentence, Judgement belief,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        Truth truth1 = null;
        Truth truth2 = null;
        final Budget budget1, budget2;
        if (sentence.isQuestion()) {
            budget1 = BudgetFunctions.backwardWeak(belief, context);
            budget2 = BudgetFunctions.backwardWeak(belief, context);
        } else {
            final Truth value1 = sentence.asJudgement();
            truth1 = TruthFunctions.deduction(value1, belief);
            truth2 = TruthFunctions.exemplification(value1, belief);
            budget1 = BudgetFunctions.forward(truth1, context);
            budget2 = BudgetFunctions.forward(truth2, context);
        }
        final Statement content = (Statement) sentence.getContent();
        final Statement content1 = makeStatement(content, term1, term2);
        final Statement content2 = makeStatement(content, term2, term1);
        context.doublePremiseTask(content1, truth1, budget1);
        context.doublePremiseTask(content2, truth2, budget2);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     *
     * @param term1        Subject of the first new task
     * @param term2        Predicate of the first new task
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param figure       Locations of the shared term in premises
     * @param context      Reference to the derivation context
     */
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence, Judgement belief, int figure,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2) || Statement.invalidPair(term1.getName(), term2.getName())) {
            return;
        }
        final Statement taskContent = (Statement) taskSentence.getContent();
        Truth truth1 = null;
        Truth truth2 = null;
        Truth truth3 = null;
        final Budget budget1, budget2, budget3;
        if (taskSentence.isQuestion()) {
            budget1 = BudgetFunctions.backward(belief, context);
            budget2 = BudgetFunctions.backwardWeak(belief, context);
            budget3 = BudgetFunctions.backward(belief, context);
        } else {
            final Truth value1 = taskSentence.asJudgement();
            truth1 = TruthFunctions.abduction(value1, belief);
            truth2 = TruthFunctions.abduction(belief, value1);
            truth3 = TruthFunctions.comparison(value1, belief);
            budget1 = BudgetFunctions.forward(truth1, context);
            budget2 = BudgetFunctions.forward(truth2, context);
            budget3 = BudgetFunctions.forward(truth3, context);
        }
        final Statement statement1 = makeStatement(taskContent, term1, term2);
        final Statement statement2 = makeStatement(taskContent, term2, term1);
        final Statement statement3 = makeStatementSym(taskContent, term1, term2);
        context.doublePremiseTask(statement1, truth1, budget1);
        context.doublePremiseTask(statement2, truth2, budget2);
        context.doublePremiseTask(statement3, truth3, budget3);
    }

    /**
     * {<S ==> P>, <M <=> P>} |- <S ==> P>
     *
     * @param subj       Subject of the new task
     * @param pred       Predicate of the new task
     * @param asymmetric The asymmetric premise
     * @param symmetric  The symmetric premise
     * @param figure     Locations of the shared term in premises
     * @param context    Reference to the derivation context
     */
    static void analogy(Term subj, Term pred, Sentence asymmetric, Sentence symmetric, int figure,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(subj, pred)) {
            return;
        }
        final Statement st = (Statement) asymmetric.getContent();
        final Truth truth;
        final Budget budget;
        final Sentence sentence = context.getCurrentTask();
        final CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
        if (sentence.isQuestion()) {
            truth = null;
            if (taskTerm.isCommutative()) {
                budget = BudgetFunctions.backwardWeak(asymmetric.asJudgement(), context);
            } else {
                budget = BudgetFunctions.backward(symmetric.asJudgement(), context);
            }
        } else {
            truth = TruthFunctions.analogy(asymmetric.asJudgement(), symmetric.asJudgement());
            budget = BudgetFunctions.forward(truth, context);
        }
        Term content = makeStatement(st, subj, pred);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param term1    Subject of the new task
     * @param term2    Predicate of the new task
     * @param belief   The first premise
     * @param sentence The second premise
     * @param figure   Locations of the shared term in premises
     * @param context  Reference to the derivation context
     */
    static void resemblance(Term term1, Term term2, Judgement belief, Sentence sentence, int figure,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        final Statement st = (Statement) belief.getContent();
        final Truth truth;
        final Budget budget;
        if (sentence.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backward(belief, context);
        } else {
            truth = TruthFunctions.resemblance(belief, sentence.asJudgement());
            budget = BudgetFunctions.forward(truth, context);
        }
        final Term statement = makeStatement(st, term1, term2);
        context.doublePremiseTask(statement, truth, budget);
    }

    /* --------------- rules used only in conditional inference --------------- */
    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S>
     * {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence  The premise on part of s1
     * @param side         The location of s2 in s1
     * @param context      Reference to the derivation context
     */
    static void detachment(Sentence mainSentence, Sentence subSentence, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement statement = (Statement) mainSentence.getContent();
        if (!(statement instanceof Implication) && !(statement instanceof Equivalence)) {
            return;
        }
        final Term subject = statement.getSubject();
        final Term predicate = statement.getPredicate();
        final Term content;
        final Term term = subSentence.getContent();
        if ((side == 0) && term.equals(subject)) {
            content = predicate;
        } else if ((side == 1) && term.equals(predicate)) {
            content = subject;
        } else {
            return;
        }
        if ((content instanceof Statement) && ((Statement) content).invalid()) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            if (statement instanceof Equivalence) {
                budget = BudgetFunctions.backward(belief, context);
            } else if (side == 0) {
                budget = BudgetFunctions.backwardWeak(belief, context);
            } else {
                budget = BudgetFunctions.backward(belief, context);
            }
        } else {
            if (statement instanceof Equivalence) {
                truth = TruthFunctions.analogy(subSentence.asJudgement(), mainSentence.asJudgement());
            } else if (side == 0) {
                truth = TruthFunctions.deduction(mainSentence.asJudgement(), subSentence.asJudgement());
            } else {
                truth = TruthFunctions.abduction(subSentence.asJudgement(), mainSentence.asJudgement());
            }
            budget = BudgetFunctions.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P>
     * {<(&&, S2, S3) ==> P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P>
     * {<(&&, S1, S3) ==> P>, <S1 ==> S2>} |- <(&&, S2, S3) ==> P>
     *
     * @param premise1 The conditional premise
     * @param index    The location of the shared term in the condition of
     *                 premise1
     * @param premise2 The premise which, or part of which, appears in the
     *                 condition of premise1
     * @param side     The location of the shared term in premise2: 0 for
     *                 subject, 1
     *                 for predicate, -1 for the whole term
     * @param context  Reference to the derivation context
     */
    static void conditionalDedInd(Implication premise1, short index, Term premise2, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean deduction = (side != 0);
        final boolean conditionalTask = VariableInference.hasUnification(
                Symbols.VAR_INDEPENDENT,
                premise2, belief.getContent());
        final Term commonComponent;
        final Term newComponent;
        if (side == 0) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
            newComponent = null;
        }
        final Term subj = premise1.getSubject();
        if (!(subj instanceof Conjunction)) {
            return;
        }
        final Conjunction oldCondition = (Conjunction) subj;
        final int index2 = oldCondition.getComponents().indexOf(commonComponent);
        if (index2 >= 0) {
            index = (short) index2;
        } else {
            // * 🚩尝试数次匹配
            boolean hasMatch = VariableInference.unify(
                    Symbols.VAR_INDEPENDENT,
                    oldCondition.componentAt(index), commonComponent,
                    premise1, premise2);
            if (!hasMatch && (commonComponent.getClass() == oldCondition.getClass())) {
                hasMatch = VariableInference.unify(
                        Symbols.VAR_INDEPENDENT,
                        oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index),
                        premise1, premise2);
            }
            if (!hasMatch) {
                return;
            }
        }
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = setComponent(oldCondition, index, newComponent);
        }
        final Term content;
        if (newCondition != null) {
            content = makeStatement(premise1, newCondition, premise1.getPredicate());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backwardWeak(belief, context);
        } else {
            final Truth truth1 = task.asJudgement();
            if (deduction) {
                truth = TruthFunctions.deduction(truth1, belief);
            } else if (conditionalTask) {
                truth = TruthFunctions.induction(belief, truth1);
            } else {
                truth = TruthFunctions.induction(truth1, belief);
            }
            budget = BudgetFunctions.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     *
     * @param premise1 The equivalence premise
     * @param index    The location of the shared term in the condition of
     *                 premise1
     * @param premise2 The premise which, or part of which, appears in the
     *                 condition of premise1
     * @param side     The location of the shared term in premise2: 0 for
     *                 subject, 1
     *                 for predicate, -1 for the whole term
     * @param context  Reference to the derivation context
     */
    static void conditionalAna(Equivalence premise1, short index, Term premise2, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean conditionalTask = VariableInference.hasUnification(
                Symbols.VAR_INDEPENDENT,
                premise2, belief.getContent());
        final Term commonComponent;
        final Term newComponent;
        if (side == 0) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
            newComponent = null;
        }
        final Term tm = premise1.getSubject();
        if (!(tm instanceof Conjunction))
            return;
        final Conjunction oldCondition = (Conjunction) tm;
        boolean match = VariableInference.unify(
                Symbols.VAR_DEPENDENT,
                oldCondition.componentAt(index), commonComponent,
                premise1, premise2);
        if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
            match = VariableInference.unify(
                    Symbols.VAR_DEPENDENT,
                    oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index),
                    premise1, premise2);
        }
        if (!match) {
            return;
        }
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = setComponent(oldCondition, index, newComponent);
        }
        final Term content;
        if (newCondition != null) {
            content = makeStatement(premise1, newCondition, premise1.getPredicate());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backwardWeak(belief, context);
        } else {
            final Truth truth1 = task.asJudgement();
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, belief);
            } else {
                truth = TruthFunctions.analogy(truth1, belief);
            }
            budget = BudgetFunctions.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     *
     * @param cond1   The condition of the first premise
     * @param cond2   The condition of the second premise
     * @param st1     The first premise
     * @param st2     The second premise
     * @param context Reference to the derivation context
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1, Statement st2,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) {
            return false;
        }
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) {
            return false;
        }
        final Term term1;
        final Term term2;
        // if ((cond1 instanceof Conjunction) &&
        // !Variable.containVarDep(cond1.getName())) {
        if (cond1 instanceof Conjunction) {
            term1 = reduceComponents((Conjunction) cond1, cond2);
        } else {
            term1 = null;
        }
        // if ((cond2 instanceof Conjunction) &&
        // !Variable.containVarDep(cond2.getName())) {
        if (cond2 instanceof Conjunction) {
            term2 = reduceComponents((Conjunction) cond2, cond1);
        } else {
            term2 = null;
        }
        if ((term1 == null) && (term2 == null)) {
            return false;
        }
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        if (term1 != null) {
            final Term content;
            final Truth truth;
            final Budget budget;
            if (term2 != null) {
                content = makeStatement(st2, term2, term1);
            } else {
                content = term1;
            }
            if (task.isQuestion()) {
                truth = null;
                budget = BudgetFunctions.backwardWeak(belief, context);
            } else {
                truth = TruthFunctions.abduction(belief, task.asJudgement());
                budget = BudgetFunctions.forward(truth, context);
            }
            context.doublePremiseTask(content, truth, budget);
        }
        if (term2 != null) {
            final Term content2;
            final Truth truth2;
            final Budget budget2;
            if (term1 != null) {
                content2 = makeStatement(st1, term1, term2);
            } else {
                content2 = term2;
            }
            if (task.isQuestion()) {
                truth2 = null;
                budget2 = BudgetFunctions.backwardWeak(belief, context);
            } else {
                truth2 = TruthFunctions.abduction(task.asJudgement(), belief);
                budget2 = BudgetFunctions.forward(truth2, context);
            }
            context.doublePremiseTask(content2, truth2, budget2);
        }
        return true;
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>>, <M --> P>} |- <M --> S>
     *
     * @param compound     The compound term to be decomposed
     * @param component    The part of the compound to be removed
     * @param compoundTask Whether the compound comes from the task
     * @param context      Reference to the derivation context
     */
    static void eliminateVarDep(CompoundTerm compound, Term component, boolean compoundTask,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Term content = reduceComponents(compound, component);
        if ((content == null) || ((content instanceof Statement) && ((Statement) content).invalid()))
            return;
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = (compoundTask ? BudgetFunctions.backward(belief, context)
                    : BudgetFunctions.backwardWeak(belief, context));
        } else {
            final Truth v1 = task.asJudgement();
            truth = (compoundTask ? TruthFunctions.anonymousAnalogy(v1, belief)
                    : TruthFunctions.anonymousAnalogy(belief, v1));
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }
}
