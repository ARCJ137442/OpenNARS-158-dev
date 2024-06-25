package nars.inference;

import nars.control.DerivationContext;
import nars.control.DerivationContextConcept;
import nars.control.DerivationContextReason;
import nars.entity.Concept;
import nars.entity.TermLink;
import nars.inference.BudgetFunctions.BudgetInferenceFunction;
import nars.inference.BudgetFunctions.BudgetInferenceResult;
import nars.inference.BudgetFunctions.ReviseResult;
import nars.language.*;

/**
 * Budget functions for resources allocation
 * * 📌【2024-06-07 13:15:14】暂时还不能封闭：具体推理控制中要用到
 *
 * * 📝参数可变性标注语法：
 * * * [] ⇒ 传递所有权（深传递，整体只读）
 * * * [m] ⇒ 传递所有权，且可变（深传递，读写）
 * * * [&] ⇒ 传递不可变引用（浅传递，只读）
 * * * [&m] ⇒ 传递可变引用（浅传递，独占可写）
 * * * [R] ⇒ 传递不可变共享引用（共享只读）
 * * * [Rm] ⇒ 传递可变共享引用（共享读写）
 */
public final class BudgetInference {

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     * * 🚩【2024-05-21 10:30:50】现在仅用于直接推理，但逻辑可以共用：「反馈到链接」与「具体任务计算」并不矛盾
     *
     * @param newBeliefTruth    [&] The truth value of the judgment in the task
     * @param oldBeliefTruth    [&] The truth value of the belief
     * @param revisedTruth      [&] The truth value of the conclusion of revision
     * @param currentTaskBudget [&m] The budget of the current task
     * @return [] The budget for the new task
     */
    static Budget revise(
            final Truth newBeliefTruth,
            final Truth oldBeliefTruth,
            final Truth revisedTruth,
            // boolean feedbackToLinks = false,
            Budget currentTaskBudget) {
        // * 🚩计算
        final ReviseResult result = BudgetFunctions.revise(
                newBeliefTruth, oldBeliefTruth, revisedTruth,
                currentTaskBudget,
                null, null);
        // * 🚩应用修改
        currentTaskBudget.copyBudgetFrom(result.newTaskBudget);
        // * 🚩返回
        return result.newBudget;
    }

    /**
     * 🆕同{@link BudgetInference#revise}，但是「概念推理」专用
     * * 🚩在「共用逻辑」后，将预算值反馈回「词项链」「任务链」
     *
     * @param newBeliefTruth [&]
     * @param oldBeliefTruth [&]
     * @param revisedTruth   [&]
     * @param context        [&m]
     * @return []
     */
    static Budget reviseMatching(
            final Truth newBeliefTruth,
            final Truth oldBeliefTruth,
            final Truth revisedTruth,
            final DerivationContextReason context) {
        final Budget currentTaskBudget = context.getCurrentTask();
        final Budget currentTaskLinkBudget = context.getCurrentTaskLink();
        final Budget currentBeliefLinkBudget = context.getCurrentBeliefLink();
        // * 🚩计算
        final ReviseResult result = BudgetFunctions.revise(
                newBeliefTruth, oldBeliefTruth, revisedTruth,
                context.getCurrentTask(),
                context.getCurrentTaskLink(),
                context.getCurrentBeliefLink());
        // * 🚩应用修改
        currentTaskBudget.copyBudgetFrom(result.newTaskBudget);
        currentTaskLinkBudget.copyBudgetFrom(result.newTaskLinkBudget);
        currentBeliefLinkBudget.copyBudgetFrom(result.newBeliefLinkBudget);
        // * 🚩返回
        return result.newBudget;
    }

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */
    /**
     * Forward inference result and adjustment
     *
     * @param truth   [&] The truth value of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    static Budget forward(Truth truth, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.Forward, truth, null, context);
    }

    /**
     * Backward inference result and adjustment, stronger case
     *
     * @param truth   [&] The truth value of the belief deriving the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    public static Budget backward(Truth truth, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.Backward, truth, null, context);
    }

    /**
     * Backward inference result and adjustment, weaker case
     *
     * @param truth   [&] The truth value of the belief deriving the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    public static Budget backwardWeak(Truth truth, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.BackwardWeak, truth, null, context);
    }

    /* ----- Task derivation in CompositionalRules and StructuralRules ----- */
    /**
     * Forward inference with CompoundTerm conclusion
     *
     * @param truth   [&] The truth value of the conclusion
     * @param content [&] The content of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget of the conclusion
     */
    public static Budget compoundForward(Truth truth, Term content, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.CompoundForward, truth, content, context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, stronger case
     *
     * @param content [&] The content of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget of the conclusion
     */
    public static Budget compoundBackward(Term content, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.CompoundBackward, null, content, context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, weaker case
     *
     * @param content [&] The content of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget of the conclusion
     */
    public static Budget compoundBackwardWeak(Term content, DerivationContextConcept context) {
        return budgetInference(BudgetInferenceFunction.CompoundBackwardWeak, null, content, context);
    }

    /**
     * Common processing for all inference step
     *
     * @param inferenceQuality [] Quality of the inference
     * @param complexity       [] Syntactic complexity of the conclusion
     * @param taskLinkBudget   [&] Budget value from task-link
     * @param beliefLink       [&m] Budget value from belief-link (will be updated)
     * @param targetActivation [] The priority of belief-link's target concept
     * @return [] Budget of the conclusion task
     */
    public static Budget budgetInference(
            final BudgetInferenceFunction function,
            final Truth truth,
            final Term content,
            final DerivationContextConcept context) {
        // * 🚩获取有关「词项链」「任务链」的有关参数
        final Budget tLink = context.getCurrentTaskLink();
        if (tLink == null)
            // ! 📝【2024-05-17 15:41:10】`t`不可能为`null`：参见`{@link Concept.fire}`
            throw new AssertionError("t shouldn't be `null`!");
        final TermLink beliefLink = context.getBeliefLinkForBudgetInference();
        final float targetActivation = beliefLink == null
                // * 🚩空值⇒空置（转换推理不会用到）
                ? 0.0f
                // * 🚩其它⇒计算
                : getConceptActivation(beliefLink.getTarget(), context);
        // * 🚩计算新结果
        final BudgetInferenceResult result = BudgetFunctions.budgetForInference(
                function,
                truth, content,
                tLink, beliefLink, targetActivation);
        // * 🚩应用新结果
        return budgetInferenceApply(result, context.getBeliefLinkForBudgetInference());
    }

    /**
     * Get the current activation level of a concept.
     * * 🚩从「概念」中来
     * * 🚩【2024-06-22 16:59:34】因涉及控制机制（推理上下文），故放入此中
     *
     * @param t       [&] The Term naming a concept
     * @param context [&] The derivation context
     * @return [] the priority value of the concept
     */
    private static float getConceptActivation(Term t, DerivationContext context) {
        // * 🚩尝试获取概念，并获取其优先级；若无概念，返回0
        final Concept c = context.termToConcept(t);
        return c == null ? 0f : c.getPriority();
    }

    /**
     * 🆕根据计算出的「预算函数」应用其中的结果
     * * 🚩覆盖各处预算值，并以此更新
     * * 🚩返回得出的「新预算值」
     */
    public static Budget budgetInferenceApply(final BudgetInferenceResult result, Budget beliefLinkBudget) {
        // * 🚩拿出「新信念链预算」并更新
        if (beliefLinkBudget != null) {
            final Budget newBeliefLinkBudget = result.extractNewBeliefLinkBudget();
            beliefLinkBudget.copyBudgetFrom(newBeliefLinkBudget);
        }
        // * 🚩拿出「新预算」并返回
        final Budget newBudget = result.extractNewBudget();
        return newBudget;
    }
}
