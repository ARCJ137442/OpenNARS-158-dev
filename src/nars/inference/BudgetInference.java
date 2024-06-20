package nars.inference;

import nars.control.DerivationContextConcept;
import nars.control.DerivationContextReason;
import nars.entity.BudgetValue;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.BudgetFunctions.BudgetInferenceFunction;
import nars.inference.BudgetFunctions.BudgetInferenceResult;
import nars.language.*;
import static nars.inference.UtilityFunctions.*;

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
    // TODO: 过程注释 & 参数标注

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     * * 🚩【2024-05-21 10:30:50】现在仅用于直接推理，但逻辑可以共用：「反馈到链接」与「具体任务计算」并不矛盾
     *
     * @param tTruth            [&] The truth value of the judgment in the task
     * @param bTruth            [&] The truth value of the belief
     * @param truth             [&] The truth value of the conclusion of revision
     * @param currentTaskBudget [&m] The budget of the current task
     * @return [] The budget for the new task
     */
    static Budget revise(
            final Truth tTruth,
            final Truth bTruth,
            final Truth truth,
            // boolean feedbackToLinks = false,
            Budget currentTaskBudget) {
        // * 🚩计算期望之差
        final float difT = truth.getExpDifAbs(tTruth);
        // ! ⚠️【2024-06-10 23:45:42】现场降低预算值，降低之后要立马使用
        // * 💭或许亦可用「写时复制」的方法（最后再合并回「当前词项链」和「当前任务链」）
        // * 🚩用落差降低优先级、耐久度
        // * 📝当前任务 &= !落差
        currentTaskBudget.decPriority(not(difT));
        currentTaskBudget.decDurability(not(difT));
        // * 🚩用更新后的值计算新差 | ❓此时是否可能向下溢出？
        final float dif = truth.getConfidence() - Math.max(tTruth.getConfidence(), bTruth.getConfidence());
        if (dif < 0)
            throw new AssertionError("【2024-06-10 23:48:25】此处差异不应小于零");
        // * 🚩计算新预算值
        // * 📝优先级 = 差 | 当前任务
        // * 📝耐久度 = (差 + 当前任务) / 2
        // * 📝质量 = 新真值→质量
        final float priority = or(dif, currentTaskBudget.getPriority());
        final float durability = aveAri(dif, currentTaskBudget.getDurability());
        final float quality = BudgetFunctions.truthToQuality(truth);
        // 返回
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * 🆕同{@link BudgetInference#revise}，但是「概念推理」专用
     * * 🚩在「共用逻辑」后，将预算值反馈回「词项链」「任务链」
     *
     * @param tTruth  [&]
     * @param bTruth  [&]
     * @param truth   [&]
     * @param context [&m]
     * @return []
     */
    static Budget reviseMatching(
            final Truth tTruth,
            final Truth bTruth,
            final Truth truth,
            final DerivationContextReason context) {
        // * 🚩计算落差 | 【2024-05-21 10:43:44】此处暂且需要重算一次
        final float difT = truth.getExpDifAbs(tTruth);
        final float difB = truth.getExpDifAbs(bTruth);
        // * 🚩独有逻辑：反馈到任务链、信念链
        {
            // * 🚩反馈到任务链
            // * 📝当前任务链 &= !落差T
            final TaskLink tLink = context.getCurrentTaskLink();
            tLink.decPriority(not(difT));
            tLink.decDurability(not(difT));
            // * 🚩反馈到信念链
            // * 📝当前信念链 &= !落差B
            final TermLink bLink = context.getCurrentBeliefLink();
            bLink.decPriority(not(difB));
            bLink.decDurability(not(difB));
        }
        // * 🚩按「非概念推理」计算并返回
        return revise(tTruth, bTruth, truth, context.getCurrentTask());
    }

    /**
     * Merge an item into another one in a bag, when the two are identical
     * except in budget values
     *
     * @param baseValue   [&m] The budget value to be modified
     * @param adjustValue [&] The budget doing the adjusting
     */
    public static void merge(Budget baseValue, Budget adjustValue) {
        final Budget newBudget = BudgetFunctions.mergeToNew(baseValue, adjustValue);
        baseValue.copyBudgetFrom(newBudget);
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
        return budgetInference(BudgetInferenceFunction.BackwardWeak, null, content, context);
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
        // * 🚩计算新结果
        final BudgetInferenceResult result = BudgetFunctions.budgetForInference(
                function,
                truth, content, context);
        // * 🚩应用新结果
        return budgetInferenceApply(result, context.getBeliefLinkForBudgetInference());
    }

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
