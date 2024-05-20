package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
public final class SyllogisticRules {

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
    static void dedExe(Term term1, Term term2, Sentence sentence, Sentence belief, DerivationContext context) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        final TruthValue value1 = sentence.getTruth();
        final TruthValue value2 = belief.getTruth();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        final BudgetValue budget1, budget2;
        if (sentence.isQuestion()) {
            budget1 = BudgetFunctions.backwardWeak(value2, context);
            budget2 = BudgetFunctions.backwardWeak(value2, context);
        } else {
            truth1 = TruthFunctions.deduction(value1, value2);
            truth2 = TruthFunctions.exemplification(value1, value2);
            budget1 = BudgetFunctions.forward(truth1, context);
            budget2 = BudgetFunctions.forward(truth2, context);
        }
        final Statement content = (Statement) sentence.getContent();
        final Statement content1 = Statement.make(content, term1, term2, context.getMemory());
        final Statement content2 = Statement.make(content, term2, term1, context.getMemory());
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
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence, Sentence belief, int figure,
            DerivationContext context) {
        if (Statement.invalidStatement(term1, term2) || Statement.invalidPair(term1.getName(), term2.getName())) {
            return;
        }
        final Statement taskContent = (Statement) taskSentence.getContent();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        TruthValue truth3 = null;
        final BudgetValue budget1, budget2, budget3;
        final TruthValue value1 = taskSentence.getTruth();
        final TruthValue value2 = belief.getTruth();
        if (taskSentence.isQuestion()) {
            budget1 = BudgetFunctions.backward(value2, context);
            budget2 = BudgetFunctions.backwardWeak(value2, context);
            budget3 = BudgetFunctions.backward(value2, context);
        } else {
            truth1 = TruthFunctions.abduction(value1, value2);
            truth2 = TruthFunctions.abduction(value2, value1);
            truth3 = TruthFunctions.comparison(value1, value2);
            budget1 = BudgetFunctions.forward(truth1, context);
            budget2 = BudgetFunctions.forward(truth2, context);
            budget3 = BudgetFunctions.forward(truth3, context);
        }
        final Statement statement1 = Statement.make(taskContent, term1, term2, context.getMemory());
        final Statement statement2 = Statement.make(taskContent, term2, term1, context.getMemory());
        final Statement statement3 = Statement.makeSym(taskContent, term1, term2, context.getMemory());
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
            DerivationContext context) {
        if (Statement.invalidStatement(subj, pred)) {
            return;
        }
        final Statement st = (Statement) asymmetric.getContent();
        final TruthValue truth;
        final BudgetValue budget;
        final Sentence sentence = context.getCurrentTask().getSentence();
        final CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
        if (sentence.isQuestion()) {
            truth = null;
            if (taskTerm.isCommutative()) {
                budget = BudgetFunctions.backwardWeak(asymmetric.getTruth(), context);
            } else {
                budget = BudgetFunctions.backward(symmetric.getTruth(), context);
            }
        } else {
            truth = TruthFunctions.analogy(asymmetric.getTruth(), symmetric.getTruth());
            budget = BudgetFunctions.forward(truth, context);
        }
        Term content = Statement.make(st, subj, pred, context.getMemory());
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
    static void resemblance(Term term1, Term term2, Sentence belief, Sentence sentence, int figure,
            DerivationContext context) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        final Statement st = (Statement) belief.getContent();
        final TruthValue truth;
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backward(belief.getTruth(), context);
        } else {
            truth = TruthFunctions.resemblance(belief.getTruth(), sentence.getTruth());
            budget = BudgetFunctions.forward(truth, context);
        }
        final Term statement = Statement.make(st, term1, term2, context.getMemory());
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
    static void detachment(Sentence mainSentence, Sentence subSentence, int side, DerivationContext context) {
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
        final Sentence taskSentence = context.getCurrentTask().getSentence();
        final Sentence beliefSentence = context.getCurrentBelief();
        final TruthValue beliefTruth = beliefSentence.getTruth();
        final TruthValue truth1 = mainSentence.getTruth();
        final TruthValue truth2 = subSentence.getTruth();
        final TruthValue truth;
        final BudgetValue budget;
        if (taskSentence.isQuestion()) {
            truth = null;
            if (statement instanceof Equivalence) {
                budget = BudgetFunctions.backward(beliefTruth, context);
            } else if (side == 0) {
                budget = BudgetFunctions.backwardWeak(beliefTruth, context);
            } else {
                budget = BudgetFunctions.backward(beliefTruth, context);
            }
        } else {
            if (statement instanceof Equivalence) {
                truth = TruthFunctions.analogy(truth2, truth1);
            } else if (side == 0) {
                truth = TruthFunctions.deduction(truth1, truth2);
            } else {
                truth = TruthFunctions.abduction(truth2, truth1);
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
            DerivationContext context) {
        final Task task = context.getCurrentTask();
        final Sentence taskSentence = task.getSentence();
        final Sentence belief = context.getCurrentBelief();
        final boolean deduction = (side != 0);
        final boolean conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
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
            boolean match = Variable.unify(Symbols.VAR_INDEPENDENT, oldCondition.componentAt(index), commonComponent,
                    premise1, premise2);
            if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
                match = Variable.unify(Symbols.VAR_INDEPENDENT, oldCondition.componentAt(index),
                        ((CompoundTerm) commonComponent).componentAt(index), premise1, premise2);
            }
            if (!match) {
                return;
            }
        }
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index, newComponent, context.getMemory());
        }
        final Term content;
        if (newCondition != null) {
            content = Statement.make(premise1, newCondition, premise1.getPredicate(), context.getMemory());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        final TruthValue truth1 = taskSentence.getTruth();
        final TruthValue truth2 = belief.getTruth();
        final TruthValue truth;
        final BudgetValue budget;
        if (taskSentence.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backwardWeak(truth2, context);
        } else {
            if (deduction) {
                truth = TruthFunctions.deduction(truth1, truth2);
            } else if (conditionalTask) {
                truth = TruthFunctions.induction(truth2, truth1);
            } else {
                truth = TruthFunctions.induction(truth1, truth2);
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
    static void conditionalAna(Equivalence premise1, short index, Term premise2, int side, DerivationContext context) {
        final Task task = context.getCurrentTask();
        final Sentence taskSentence = task.getSentence();
        final Sentence belief = context.getCurrentBelief();
        final boolean conditionalTask = Variable.hasSubstitute(Symbols.VAR_INDEPENDENT, premise2, belief.getContent());
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
        boolean match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index), commonComponent,
                premise1, premise2);
        if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
            match = Variable.unify(Symbols.VAR_DEPENDENT, oldCondition.componentAt(index),
                    ((CompoundTerm) commonComponent).componentAt(index), premise1, premise2);
        }
        if (!match) {
            return;
        }
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.setComponent(oldCondition, index, newComponent, context.getMemory());
        }
        final Term content;
        if (newCondition != null) {
            content = Statement.make(premise1, newCondition, premise1.getPredicate(), context.getMemory());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        final TruthValue truth1 = taskSentence.getTruth();
        final TruthValue truth2 = belief.getTruth();
        final TruthValue truth;
        final BudgetValue budget;
        if (taskSentence.isQuestion()) {
            truth = null;
            budget = BudgetFunctions.backwardWeak(truth2, context);
        } else {
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, truth2);
            } else {
                truth = TruthFunctions.analogy(truth1, truth2);
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
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1, Statement st2, DerivationContext context) {
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
            term1 = CompoundTerm.reduceComponents((Conjunction) cond1, cond2, context.getMemory());
        } else {
            term1 = null;
        }
        // if ((cond2 instanceof Conjunction) &&
        // !Variable.containVarDep(cond2.getName())) {
        if (cond2 instanceof Conjunction) {
            term2 = CompoundTerm.reduceComponents((Conjunction) cond2, cond1, context.getMemory());
        } else {
            term2 = null;
        }
        if ((term1 == null) && (term2 == null)) {
            return false;
        }
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final Sentence belief = context.getCurrentBelief();
        final TruthValue value1 = sentence.getTruth();
        final TruthValue value2 = belief.getTruth();
        if (term1 != null) {
            final Term content;
            final TruthValue truth;
            final BudgetValue budget;
            if (term2 != null) {
                content = Statement.make(st2, term2, term1, context.getMemory());
            } else {
                content = term1;
            }
            if (sentence.isQuestion()) {
                truth = null;
                budget = BudgetFunctions.backwardWeak(value2, context);
            } else {
                truth = TruthFunctions.abduction(value2, value1);
                budget = BudgetFunctions.forward(truth, context);
            }
            context.doublePremiseTask(content, truth, budget);
        }
        if (term2 != null) {
            final Term content2;
            final TruthValue truth2;
            final BudgetValue budget2;
            if (term1 != null) {
                content2 = Statement.make(st1, term1, term2, context.getMemory());
            } else {
                content2 = term2;
            }
            if (sentence.isQuestion()) {
                truth2 = null;
                budget2 = BudgetFunctions.backwardWeak(value2, context);
            } else {
                truth2 = TruthFunctions.abduction(value1, value2);
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
            DerivationContext context) {
        final Term content = CompoundTerm.reduceComponents(compound, component, context.getMemory());
        if ((content == null) || ((content instanceof Statement) && ((Statement) content).invalid()))
            return;
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final Sentence belief = context.getCurrentBelief();
        final TruthValue v1 = sentence.getTruth();
        final TruthValue v2 = belief.getTruth();
        final TruthValue truth;
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            truth = null;
            budget = (compoundTask ? BudgetFunctions.backward(v2, context)
                    : BudgetFunctions.backwardWeak(v2, context));
        } else {
            truth = (compoundTask ? TruthFunctions.anonymousAnalogy(v1, v2) : TruthFunctions.anonymousAnalogy(v2, v1));
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }
}
