package nars.inference;

import nars.control.DerivationContext;
import nars.control.DerivationContextReason;
import nars.control.DerivationContextTransform;
import nars.entity.*;
import nars.language.*;

/**
 * Budget functions for resources allocation
 * * 📌【2024-06-07 13:15:14】暂时还不能封闭：具体推理控制中要用到
 * TODO: 过程笔记注释
 */
public final class BudgetFunctions extends UtilityFunctions {

    /* ----------------------- Belief evaluation ----------------------- */
    /**
     * Determine the quality of a judgment by its truth value alone
     * <p>
     * Mainly decided by confidence, though binary judgment is also preferred
     *
     * @param t The truth value of a judgment
     * @return The quality of the judgment, according to truth value only
     */
    public static float truthToQuality(TruthValue t) {
        // * 🚩真值⇒质量：期望与「0.75(1-期望)」的最大值
        // * 📝函数：max(c * (f - 0.5) + 0.5, 0.375 - 0.75 * c * (f - 0.5))
        // * 📍最小值：当exp=3/7时，全局最小值为3/7（max的两端相等）
        // * 🔑max(x,y) = (x+y+|x-y|)/2
        final float exp = t.getExpectation();
        return (float) Math.max(exp, not(exp) * 0.75);
    }

    /**
     * Determine the rank of a judgment by its quality and originality (stamp
     * length), called from Concept
     *
     * @param judgment The judgment to be ranked
     * @return The rank of the judgment, according to truth value only
     */
    public static float rankBelief(Sentence judgment) {
        // * 🚩两个指标：信度 + 原创性（时间戳长度）
        final float confidence = judgment.getTruth().getConfidence();
        final float originality = 1.0f / (judgment.getStamp().length() + 1);
        return or(confidence, originality);
    }

    /* ----- Functions used both in direct and indirect processing of tasks ----- */
    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * TODO: 后续或许需要依「直接推理」「概念推理」拆分
     *
     * @param problem      The problem (question or goal) to be solved
     * @param solution     The belief as solution
     * @param questionTask The task to be immediately processed, or null for
     *                     continued
     *                     process
     * @return The budget for the new task which is the belief activated, if
     *         necessary
     */
    static BudgetValue solutionEval(
            final Sentence problem,
            final Sentence solution,
            final Task questionTask/*
                                    * ,
                                    * final DerivationContext context
                                    */) {
        // final BudgetValue budget;
        // final boolean feedbackToLinks;
        if (problem == null || !problem.isQuestion())
            throw new NullPointerException("待解决的问题必须是疑问句");
        if (solution == null || !solution.isJudgment())
            throw new NullPointerException("解决方案必须是「判断」");
        if (questionTask == null || !questionTask.isQuestion())
            // * 🚩实际上不会有「feedbackToLinks=true」的情况（当前任务非空）
            throw new IllegalArgumentException("问题任务必须为「问题」 | solutionEval is Never called in continued processing");
        // feedbackToLinks = true;
        // else
        // feedbackToLinks = false;
        // * 🚩【2024-06-06 10:32:15】断言judgmentTask为false
        // final boolean judgmentTask = questionTask.isJudgment();
        final float solutionQuality = LocalRules.solutionQuality(problem, solution);
        /*
         * if (judgmentTask) {
         * budget = null;
         * questionTask.incPriority(quality);
         * } else
         */ {
            final float taskPriority = questionTask.getPriority();
            final float newP = or(taskPriority, solutionQuality);
            final float newD = questionTask.getDurability();
            final float newQ = truthToQuality(solution.getTruth());
            final BudgetValue budget = new BudgetValue(newP, newD, newQ);
            // 更新「源任务」的预算值（优先级）
            final float updatedQuestionPriority = Math.min(not(solutionQuality), taskPriority);
            questionTask.setPriority(updatedQuestionPriority);
            return budget;
        }
        // if (feedbackToLinks && context instanceof DerivationContextReason) {
        // final DerivationContextReason contextReason = (DerivationContextReason)
        // context;
        // final TaskLink tLink = contextReason.getCurrentTaskLink();
        // tLink.setPriority(Math.min(not(quality), tLink.getPriority()));
        // final TermLink bLink = contextReason.getCurrentBeliefLink();
        // bLink.incPriority(quality);
        // }
        // return budget;
    }

    /**
     * Evaluate the quality of a revision, then de-prioritize the premises
     *
     * @param tTruth The truth value of the judgment in the task
     * @param bTruth The truth value of the belief
     * @param truth  The truth value of the conclusion of revision
     * @return The budget for the new task
     */
    static BudgetValue revise(
            final TruthValue tTruth,
            final TruthValue bTruth,
            final TruthValue truth,
            // boolean feedbackToLinks = false,
            final DerivationContext context) {
        // * 🚩【2024-05-21 10:30:50】现在仅用于直接推理，但逻辑可以共用：「反馈到链接」与「具体任务计算」并不矛盾
        final float difT = truth.getExpDifAbs(tTruth);
        // TODO: 🎯将「预算反馈」延迟处理（❓可以返回「推理结果」等，然后用专门的「预算更新」再处理预算）
        final Task task = context.getCurrentTask();
        task.decPriority(not(difT));
        task.decDurability(not(difT));
        final float dif = truth.getConfidence() - Math.max(tTruth.getConfidence(), bTruth.getConfidence());
        final float priority = or(dif, task.getPriority());
        final float durability = aveAri(dif, task.getDurability());
        final float quality = truthToQuality(truth);
        return new BudgetValue(priority, durability, quality);
    }

    /**
     * 🆕同{@link BudgetFunctions#revise}，但是「概念推理」专用
     * * 🚩在「共用逻辑」后，将预算值反馈回「词项链」「任务链」
     * 
     * @param tTruth
     * @param bTruth
     * @param truth
     * @param context
     * @return
     */
    static BudgetValue revise(
            final TruthValue tTruth,
            final TruthValue bTruth,
            final TruthValue truth,
            // final boolean feedbackToLinks = true,
            final DerivationContextReason context) {
        final float difT = truth.getExpDifAbs(tTruth); // * 🚩【2024-05-21 10:43:44】此处暂且需要重算一次
        final BudgetValue revised = revise(tTruth, bTruth, truth, (DerivationContext) context);
        { // * 🚩独有逻辑：反馈到任务链、信念链
            final TaskLink tLink = context.getCurrentTaskLink();
            tLink.decPriority(not(difT));
            tLink.decDurability(not(difT));
            final TermLink bLink = context.getCurrentBeliefLink();
            final float difB = truth.getExpDifAbs(bTruth);
            bLink.decPriority(not(difB));
            bLink.decDurability(not(difB));
        }
        return revised;
    }

    /**
     * Update a belief
     *
     * @param task   The task containing new belief
     * @param bTruth Truth value of the previous belief
     * @return Budget value of the updating task
     */
    static BudgetValue update(Task task, TruthValue bTruth) {
        final TruthValue tTruth = task.getTruth();
        final float dif = tTruth.getExpDifAbs(bTruth);
        final float priority = or(dif, task.getPriority());
        final float durability = aveAri(dif, task.getDurability());
        final float quality = truthToQuality(bTruth);
        return new BudgetValue(priority, durability, quality);
    }

    /* ----------------------- Links ----------------------- */
    /**
     * Distribute the budget of a task among the links to it
     * * 🚩【2024-05-30 00:53:02】产生新预算值，不会修改旧预算值
     * * 📝【2024-05-30 00:53:41】逻辑：仅优先级随链接数指数级降低
     *
     * @param original The original budget
     * @param nLinks   Number of links
     * @return Budget value for each link
     */
    public static BudgetValue distributeAmongLinks(final BudgetValue original, final int nLinks) {
        final float priority = (float) (original.getPriority() / Math.sqrt(nLinks));
        return new BudgetValue(priority, original.getDurability(), original.getQuality());
    }

    /* ----------------------- Concept ----------------------- */
    /**
     * Activate a concept by an incoming TaskLink
     * * 📝【2024-05-30 01:08:26】调用溯源：仅在「直接推理」中使用
     * * 📝【2024-05-30 01:03:01】逻辑：优先级「析取」提升，耐久度「算术」平均
     * * 📌新の优先级 = 概念 | 参考
     * * 📌新の耐久度 = (概念 + 参考) / 2
     * * 📌新の质量 = 综合所有词项链后的新「质量」
     *
     * @param concept The concept
     * @param budget  The budget for the new item
     */
    public static void activate(final Concept concept, final BudgetValue budget) {
        final float cP = concept.getPriority();
        final float cD = concept.getDurability();
        final float bP = budget.getPriority();
        final float bD = budget.getDurability();
        final float p = or(cP, bP);
        final float d = aveAri(cD, bD);
        final float q = concept.getTotalQuality(); // ! 📌【2024-05-30 01:25:51】若注释此行，将破坏「同义重构」
        concept.setPriority(p);
        concept.setDurability(d);
        concept.setQuality(q);
        // * 📝此「质量」非上头「质量」：上头的「质量」实为「总体质量」，与「词项链」「词项复杂度」均有关
    }

    /* ---------------- Bag functions, on all Items ------------------- */
    /**
     * Decrease Priority after an item is used, called in Bag
     * <p>
     * After a constant time, p should become d*p. Since in this period, the
     * item is accessed c*p times, each time p-q should multiple d^(1/(c*p)).
     * The intuitive meaning of the parameter "forgetRate" is: after this number
     * of times of access, priority 1 will become d, it is a system parameter
     * adjustable in run time.
     *
     * @param budget            The previous budget value
     * @param forgetRate        The budget for the new item
     * @param relativeThreshold The relative threshold of the bag
     */
    public static void forget(BudgetValue budget, float forgetRate, float relativeThreshold) {
        double quality = budget.getQuality() * relativeThreshold; // re-scaled quality
        final double p = budget.getPriority() - quality; // priority above quality
        if (p > 0) {
            quality += p * Math.pow(budget.getDurability(), 1.0 / (forgetRate * p));
        } // priority Durability
        budget.setPriority((float) quality);
    }

    /**
     * Merge an item into another one in a bag, when the two are identical
     * except in budget values
     *
     * @param baseValue   The budget value to be modified
     * @param adjustValue The budget doing the adjusting
     */
    public static void merge(BudgetValue baseValue, BudgetValue adjustValue) {
        baseValue.setPriority(Math.max(baseValue.getPriority(), adjustValue.getPriority()));
        baseValue.setDurability(Math.max(baseValue.getDurability(), adjustValue.getDurability()));
        baseValue.setQuality(Math.max(baseValue.getQuality(), adjustValue.getQuality()));
    }

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */
    /**
     * Forward inference result and adjustment
     *
     * @param truth The truth value of the conclusion
     * @return The budget value of the conclusion
     */
    static BudgetValue forward(TruthValue truth, DerivationContextTransform context) {
        return budgetInference(truthToQuality(truth), 1, context);
    }

    /**
     * Backward inference result and adjustment, stronger case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    public static BudgetValue backward(TruthValue truth, DerivationContextTransform context) {
        return budgetInference(truthToQuality(truth), 1, context);
    }

    /**
     * Backward inference result and adjustment, weaker case
     *
     * @param truth  The truth value of the belief deriving the conclusion
     * @param memory Reference to the memory
     * @return The budget value of the conclusion
     */
    public static BudgetValue backwardWeak(TruthValue truth, DerivationContextTransform context) {
        return budgetInference(w2c(1) * truthToQuality(truth), 1, context);
    }

    /* ----- Task derivation in CompositionalRules and StructuralRules ----- */
    /**
     * Forward inference with CompoundTerm conclusion
     *
     * @param truth   The truth value of the conclusion
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundForward(TruthValue truth, Term content, DerivationContextTransform context) {
        return budgetInference(truthToQuality(truth), content.getComplexity(), context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, stronger case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundBackward(Term content, DerivationContextTransform context) {
        return budgetInference(1, content.getComplexity(), context);
    }

    /**
     * Backward inference with CompoundTerm conclusion, weaker case
     *
     * @param content The content of the conclusion
     * @param memory  Reference to the memory
     * @return The budget of the conclusion
     */
    public static BudgetValue compoundBackwardWeak(
            final Term content,
            final DerivationContextTransform context) {
        return budgetInference(w2c(1), content.getComplexity(), context);
    }

    /**
     * Common processing for all inference step
     *
     * @param qual       Quality of the inference
     * @param complexity Syntactic complexity of the conclusion
     * @param memory     Reference to the memory
     * @return Budget of the conclusion task
     */
    private static BudgetValue budgetInference(
            final float qual,
            final int complexity,
            final DerivationContextTransform context) {
        final Item tLink = context.getCurrentTaskLink();
        // ! 📝【2024-05-17 15:41:10】`t`不可能为`null`：参见`{@link Concept.fire}`
        // if (t == null) {
        // t = context.getCurrentTask();
        // }
        if (tLink == null) {
            throw new NullPointerException("t shouldn't be `null`!");
        }
        float priority = tLink.getPriority();
        float durability = tLink.getDurability() / complexity;
        final float quality = qual / complexity;
        if (context instanceof DerivationContextReason) {
            final TermLink bLink = ((DerivationContextReason) context).getCurrentBeliefLink();
            if (bLink != null) {
                priority = or(priority, bLink.getPriority());
                durability = and(durability, bLink.getDurability());
                final float targetActivation = context.getMemory().getConceptActivation(bLink.getTarget());
                bLink.incPriority(or(quality, targetActivation));
                bLink.incDurability(quality);
            }
        }
        return new BudgetValue(priority, durability, quality);
    }
}
