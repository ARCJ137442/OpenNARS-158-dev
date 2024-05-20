package nars.inference;

import nars.storage.Memory;
import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
public class LocalRules {

    /* -------------------- same contents -------------------- */
    /**
     * The task and belief have the same content
     * <p>
     * called in RuleTables.reason
     *
     * @param task    The task
     * @param belief  The belief
     * @param context Reference to the derivation context
     */
    public static void match(DerivationContext context) {
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`task`一定是`context.currentTask`
        final Task task = context.getCurrentTask();
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`belief`一定是`context.currentBelief`
        final Sentence belief = context.getCurrentBelief();

        final Sentence sentence = task.getSentence().clone();
        if (sentence.isJudgment()) {
            if (revisable(sentence, belief)) {
                revision(sentence, belief, true, context);
            }
        } else if (Variable.unify(Symbols.VAR_QUERY, sentence.getContent(), belief.getContent().clone())) {
            trySolution(belief, task, context);
        }
    }

    /**
     * Check whether two sentences can be used in revision
     * * 📝【2024-05-19 13:09:40】这里的`s1`、`s2`必定是「判断」类型
     *
     * @param s1 The first sentence
     * @param s2 The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisable(Sentence s1, Sentence s2) {
        if (s1.isJudgment() && s2.isJudgment()) {
            return (s1.getContent().equals(s2.getContent()) && s1.getRevisable());
        } else {
            throw new Error("Function revisable is only applicable for judgments");
        }
    }

    /**
     * Belief revision
     * <p>
     * called from Concept.reviseTable and match
     *
     * @param newBelief       The new belief in task
     * @param oldBelief       The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param context         Reference to the derivation context
     */
    public static void revision(Sentence newBelief, Sentence oldBelief, boolean feedbackToLinks,
            DerivationContext context) {
        final TruthValue newTruth = newBelief.getTruth();
        final TruthValue oldTruth = oldBelief.getTruth();
        final TruthValue truth = TruthFunctions.revision(newTruth, oldTruth);
        final BudgetValue budget = BudgetFunctions.revise(newTruth, oldTruth, truth, feedbackToLinks,
                context.getMemory());
        final Term content = newBelief.getContent();
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief  The proposed answer
     * @param task    The task to be processed
     * @param context Reference to the derivation context
     */
    public static void trySolution(Sentence belief, Task task, DerivationContext context) {
        final Sentence problem = task.getSentence();
        final Sentence oldBest = task.getBestSolution();
        // * 🚩验证这个信念是否为「解决问题的最优解」
        final float newQ = solutionQuality(problem, belief);
        if (oldBest != null) {
            float oldQ = solutionQuality(problem, oldBest);
            if (oldQ >= newQ) {
                return;
            }
        }
        // * 🚩若比先前「最优解」还优，那就确立新的「最优解」
        task.setBestSolution(belief);
        if (task.isInput()) { // moved from Sentence
            // * 🚩同时在此确立「回答」：只在回应「输入的任务」时反映
            context.report(belief, Memory.ReportType.ANSWER);
        }
        // * 🚩后续收尾：预算值更新 | ⚠️在此处改变当前任务的预算值
        final BudgetValue budget = BudgetFunctions.solutionEval(problem, belief, task, context.getMemory());
        if (budget != null && budget.aboveThreshold()) {
            // * 🚩激活任务 | 在此过程中将「当前任务」添加回「新任务」
            context.activatedTask(budget, belief, task.getParentBelief());
        }
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     *
     * @param problem  A goal or question
     * @param solution The solution to be evaluated
     * @return The quality of the judgment as the solution
     */
    public static float solutionQuality(Sentence problem, Sentence solution) {
        if (problem == null) {
            return solution.getTruth().getExpectation();
        }
        final TruthValue truth = solution.getTruth();
        if (problem.containQueryVar()) { // "yes/no" question
            return truth.getExpectation() / solution.getContent().getComplexity();
        } else { // "what" question or goal
            return truth.getConfidence();
        }
    }

    /* -------------------- same terms, difference relations -------------------- */
    /**
     * The task and belief match reversely
     *
     * @param context Reference to the derivation context
     */
    public static void matchReverse(DerivationContext context) {
        final Task task = context.getCurrentTask();
        final Sentence belief = context.getCurrentBelief();
        final Sentence sentence = task.getSentence();
        if (sentence.isJudgment()) {
            inferToSym((Sentence) sentence, belief, context);
        } else {
            conversion(context);
        }
    }

    /**
     * Inheritance/Implication matches Similarity/Equivalence
     *
     * @param asym    A Inheritance/Implication sentence
     * @param sym     A Similarity/Equivalence sentence
     * @param figure  location of the shared term
     * @param context Reference to the derivation context
     */
    public static void matchAsymSym(Sentence asym, Sentence sym, int figure, DerivationContext context) {
        if (context.getCurrentTask().getSentence().isJudgment()) {
            inferToAsym((Sentence) asym, (Sentence) sym, context);
        } else {
            convertRelation(context);
        }
    }

    /* -------------------- two-premise inference rules -------------------- */
    /**
     * {<S --> P>, <P --> S} |- <S <-> p>
     * Produce Similarity/Equivalence from a pair of reversed
     * Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param context   Reference to the derivation context
     */
    private static void inferToSym(Sentence judgment1, Sentence judgment2, DerivationContext context) {
        final Statement s1 = (Statement) judgment1.getContent();
        final Term t1 = s1.getSubject();
        final Term t2 = s1.getPredicate();
        final Term content;
        if (s1 instanceof Inheritance) {
            content = Similarity.make(t1, t2, context.getMemory());
        } else {
            content = Equivalence.make(t1, t2, context.getMemory());
        }
        final TruthValue value1 = judgment1.getTruth();
        final TruthValue value2 = judgment2.getTruth();
        final TruthValue truth = TruthFunctions.intersection(value1, value2);
        final BudgetValue budget = BudgetFunctions.forward(truth, context.getMemory());
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym    The asymmetric premise
     * @param sym     The symmetric premise
     * @param context Reference to the derivation context
     */
    private static void inferToAsym(Sentence asym, Sentence sym, DerivationContext context) {
        final Statement statement = (Statement) asym.getContent();
        final Term sub = statement.getPredicate();
        final Term pre = statement.getSubject();
        final Statement content = Statement.make(statement, sub, pre, context.getMemory());
        final TruthValue truth = TruthFunctions.reduceConjunction(sym.getTruth(), asym.getTruth());
        final BudgetValue budget = BudgetFunctions.forward(truth, context.getMemory());
        context.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param context Reference to the derivation context
     */
    private static void conversion(DerivationContext context) {
        final TruthValue truth = TruthFunctions.conversion(context.getCurrentBelief().getTruth());
        final BudgetValue budget = BudgetFunctions.forward(truth, context.getMemory());
        convertedJudgment(truth, budget, context);
    }

    /**
     * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param context Reference to the derivation context
     */
    private static void convertRelation(DerivationContext context) {
        final TruthValue truth = context.getCurrentBelief().getTruth();
        final TruthValue newTruth;
        if (((Statement) context.getCurrentTask().getContent()).isCommutative()) {
            newTruth = TruthFunctions.abduction(truth, 1.0f);
        } else {
            newTruth = TruthFunctions.deduction(truth, 1.0f);
        }
        final BudgetValue budget = BudgetFunctions.forward(newTruth, context.getMemory());
        convertedJudgment(newTruth, budget, context);
    }

    /**
     * Convert judgment into different relation
     * <p>
     * called in MatchingRules
     *
     * @param budget  The budget value of the new task
     * @param truth   The truth value of the new task
     * @param context Reference to the derivation context
     */
    private static void convertedJudgment(TruthValue newTruth, BudgetValue newBudget, DerivationContext context) {
        Statement content = (Statement) context.getCurrentTask().getContent();
        final Statement beliefContent = (Statement) context.getCurrentBelief().getContent();
        final Term subjT = content.getSubject();
        final Term predT = content.getPredicate();
        final Term subjB = beliefContent.getSubject();
        final Term predB = beliefContent.getPredicate();
        Term otherTerm;
        if (Variable.containVarQ(subjT.getName())) {
            otherTerm = (predT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, otherTerm, predT, context.getMemory());
        }
        if (Variable.containVarQ(predT.getName())) {
            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, subjT, otherTerm, context.getMemory());
        }
        context.singlePremiseTask(content, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
