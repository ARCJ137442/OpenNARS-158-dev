package nars.inference;

import nars.entity.*;
import nars.control.DerivationContext;
import nars.control.ReportType;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence;
 * revision: between judgments;
 * satisfy: between a Sentence and a Question/Goal;
 * merge: between items of the same type and stamp;
 * conversion: between different inheritance relations.
 * * 🚩【2024-06-10 10:04:13】此注释已过时；现在仅用于「直接推理」
 * * 🚩【2024-06-29 03:31:42】再更新：此内函数现在只用于「共用逻辑」
 * * * 📄「直接推理」与「匹配推理」均要「解决问题」
 */
final class LocalRules {

    /* -------------------- same contents -------------------- */

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief       The proposed answer
     * @param questionTask The task to be processed
     * @param context      Reference to the derivation context
     */
    static void trySolution(Judgement belief, Task questionTask, DerivationContext context) {
        // * 🚩预设&断言
        final Judgement oldBest = questionTask.getBestSolution();
        if (belief == null)
            throw new AssertionError("将解答的必须是「判断」");
        if (questionTask == null || !questionTask.isQuestion())
            throw new AssertionError("要解决的必须是「问题」");

        // * 🚩验证这个信念是否为「解决问题的最优解」
        final float newQ = BudgetFunctions.solutionQuality(questionTask, belief);
        if (oldBest != null) {
            final float oldQ = BudgetFunctions.solutionQuality(questionTask, oldBest);
            // * 🚩新解比旧解还差⇒驳回
            if (oldQ >= newQ)
                return;
        }

        // * 🚩若比先前「最优解」还优，那就确立新的「最优解」
        questionTask.setBestSolution(belief);
        if (questionTask.isInput()) { // moved from Sentence
            // * 🚩同时在此确立「回答」：只在回应「输入的任务」时反映
            context.report(belief, ReportType.ANSWER);
        }
        // * 🚩计算新预算值
        final Question problem = questionTask.asQuestion();
        final Budget budget = BudgetFunctions.solutionEval(problem, belief, questionTask);
        // * 🚩更新「问题任务」的预算值
        // * 📝解决问题后，在「已解决的问题」之预算中 降低（已经解决了，就将算力多留到「未解决问题」上）
        final float solutionQuality = BudgetFunctions.solutionQuality(problem, belief);
        final float updatedQuestionPriority = Math.min(
                UtilityFunctions.not(solutionQuality),
                questionTask.getPriority());
        questionTask.setPriority(updatedQuestionPriority);

        // * 🚩尝试「激活任务」
        if (budget == null)
            throw new AssertionError("【2024-06-09 00:45:04】计算出的新预算值不可能为空");
        if (budget.budgetAboveThreshold()) {
            // * 🚩激活任务 | 在此过程中将「当前任务」添加回「新任务」
            context.activatedTask(budget, belief, questionTask.getParentBelief());
        }
    }
}
