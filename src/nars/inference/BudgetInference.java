package nars.inference;

import nars.control.DerivationContext;
import nars.control.DerivationContextConcept;
import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.inference.BudgetFunctions.BudgetInferenceResult;
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
public final class BudgetInference extends BudgetFunctions {
    // TODO: 过程注释 & 参数标注

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */
    /**
     * Forward inference result and adjustment
     *
     * @param truth   [&] The truth value of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    static Budget forward(Truth truth, DerivationContextConcept context) {
        // TODO: 过程笔记注释
        return budgetInference(
                truthToQuality(truth),
                1,
                context);
    }

    /**
     * Backward inference result and adjustment, stronger case
     *
     * @param truth   [&] The truth value of the belief deriving the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    public static Budget backward(Truth truth, DerivationContextConcept context) {
        // TODO: 过程笔记注释
        return budgetInference(
                truthToQuality(truth),
                1,
                context);
    }

    /**
     * Backward inference result and adjustment, weaker case
     *
     * @param truth   [&] The truth value of the belief deriving the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget value of the conclusion
     */
    public static Budget backwardWeak(Truth truth, DerivationContextConcept context) {
        // TODO: 过程笔记注释
        return budgetInference(
                W2C1 * truthToQuality(truth),
                1,
                context);
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
        // TODO: 过程笔记注释
        return budgetInference(
                truthToQuality(truth),
                content.getComplexity(),
                context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, stronger case
     *
     * @param content [&] The content of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget of the conclusion
     */
    public static Budget compoundBackward(Term content, DerivationContextConcept context) {
        // TODO: 过程笔记注释
        return budgetInference(
                1,
                content.getComplexity(),
                context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, weaker case
     *
     * @param content [&] The content of the conclusion
     * @param context [&m] The derivation context
     * @return [] The budget of the conclusion
     */
    public static Budget compoundBackwardWeak(Term content, DerivationContextConcept context) {
        // TODO: 过程笔记注释
        return budgetInference(
                W2C1,
                content.getComplexity(),
                context);
    }

    /**
     * Common processing for all inference step
     *
     * @param inferenceQuality [] Quality of the inference
     * @param complexity       [] Syntactic complexity of the conclusion
     * @param context          [&m] The derivation context
     * @return [] Budget of the conclusion task
     */
    private static Budget budgetInference(
            final float inferenceQuality,
            final int complexity,
            final DerivationContextConcept context) {
        // * 🚩获取有关「词项链」「任务链」的有关参数
        final Item tLink = context.getCurrentTaskLink();
        if (tLink == null)
            // ! 📝【2024-05-17 15:41:10】`t`不可能为`null`：参见`{@link Concept.fire}`
            throw new AssertionError("t shouldn't be `null`!");
        final TermLink beliefLink = context.getBeliefLinkForBudgetInference();
        final float targetActivation = beliefLink == null
                // * 🚩空值⇒空置（转换推理不会用到）
                ? 0.0f
                // * 🚩其它⇒计算
                : getConceptActivation(beliefLink.getTarget(), context);
        // * 🚩不带「推理上下文」参与计算
        return budgetInference(inferenceQuality, complexity, tLink, beliefLink, targetActivation);
    }

    /**
     * Get the current activation level of a concept.
     * * 🚩从「概念」中来
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
     * Common processing for all inference step
     *
     * @param inferenceQuality [] Quality of the inference
     * @param complexity       [] Syntactic complexity of the conclusion
     * @param taskLinkBudget   [&] Budget value from task-link
     * @param beliefLink       [&m] Budget value from belief-link (will be updated)
     * @param targetActivation [] The priority of belief-link's target concept
     * @return [] Budget of the conclusion task
     */
    private static Budget budgetInference(
            final float inferenceQuality,
            final int complexity,
            final Budget taskLinkBudget,
            final Budget beliefLinkBudget, // 📌跟下边这个参数是捆绑的：有「信念链」就要获取「目标词项」的优先级
            final float targetActivation) {
        // * 🚩计算新结果
        final BudgetInferenceResult result = BudgetFunctions.budgetInferenceCalc(
                inferenceQuality, complexity,
                taskLinkBudget,
                beliefLinkBudget, targetActivation);
        // * 🚩应用新结果
        return budgetInferenceApply(result, beliefLinkBudget);
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
