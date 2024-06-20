package nars.inference;

import nars.control.DerivationContext;
import nars.control.DerivationContextConcept;
import nars.control.DerivationContextReason;
import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Item;
import nars.entity.Judgement;
import nars.entity.Question;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.language.Term;
import nars.language.Variable;

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
public abstract class BudgetFunctions extends UtilityFunctions {

    // TODO: 后续或许能使用「预算函数枚举」实现「传递『要用哪个函数』的信息，控制端独立计算预算值」的「推理器与控制区分离」

    /* ----------------------- Belief evaluation ----------------------- */
    /**
     * Determine the quality of a judgment by its truth value alone
     * <p>
     * Mainly decided by confidence, though binary judgment is also preferred
     *
     * @param t [&] The truth value of a judgment
     * @return [] The quality of the judgment, according to truth value only
     */
    public static float truthToQuality(Truth t) {
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
     * * 📝因为其自身涉及「资源竞争」故放在「预算函数」而非「真值函数」中
     *
     * @param judgment [&] The judgment to be ranked
     * @return [] The rank of the judgment, according to truth value only
     */
    public static float rankBelief(Judgement judgment) {
        // * 🚩两个指标：信度 + 原创性（时间戳长度）
        // * 📝与信度正相关，与「时间戳长度」负相关；二者有一个好，那就整体好
        final float confidence = judgment.getConfidence();
        final float originality = 1.0f / (judgment.evidenceLength() + 1);
        return or(confidence, originality);
    }

    /**
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     * * 📝用于概念的「激活」函数上
     *
     * @param concept [&] The concept to be evaluated
     * @return [] The quality value
     */
    public static float conceptTotalQuality(Concept concept) {
        // * 🚩计算所有词项链的「平均优先级」
        final float linkPriority = concept.termLinksAveragePriority();
        // * 🚩词项复杂性指标：自身复杂性倒数
        final float termComplexityFactor = 1.0f / concept.getTerm().getComplexity();
        // * 🚩总体：任意更大就行；结构简单的基本总是最好的；词项越复杂，质量下限越低
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /* ----- Functions used both in direct and indirect processing of tasks ----- */

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     * * ⚠️这个返回值必须在0~1之间
     *
     * @param query    [&] A goal or question
     * @param solution [&] The solution to be evaluated
     * @return [] The quality of the judgment as the solution
     */
    public static float solutionQuality(Sentence query, Judgement solution) {
        // * 🚩断言
        if (query == null)
            // return solution.getExpectation();
            throw new AssertionError("要查询的语句不应为空");
        if (solution == null)
            throw new AssertionError("要对应的解不应为空");
        // * 🚩根据「一般疑问 | 特殊疑问/目标」拆解
        if (Variable.containVarQ(query.getContent())) {
            // * 🚩【一般疑问】 "yes/no" question
            return solution.getExpectation() / solution.getContent().getComplexity();
        } else {
            // * 🚩【特殊疑问/目标】 "what" question or goal
            return solution.getConfidence();
        }
    }

    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param problem      [&] The problem (question or goal) to be solved
     * @param solution     [&] The belief as solution
     * @param questionTask [&] The task to be immediately processed, or null for
     *                     continued process
     * @return [] The budget for the new task which is the belief activated, if
     *         necessary
     */
    static Budget solutionEval(
            final Question problem,
            final Judgement solution,
            final Task questionTask) {
        if (problem == null)
            throw new AssertionError("待解决的问题必须是疑问句");
        if (solution == null)
            throw new AssertionError("解决方案必须是「判断」");
        if (questionTask == null || !questionTask.isQuestion())
            // * 🚩实际上不会有「feedbackToLinks=true」的情况（当前任务非空）
            throw new AssertionError("问题任务必须为「问题」 | solutionEval is Never called in continued processing");
        // * ️📝新优先级 = 任务优先级 | 解决方案质量
        final float newP = or(questionTask.getPriority(), solutionQuality(problem, solution));
        // * 📝新耐久度 = 任务耐久度
        final float newD = questionTask.getDurability();
        // * ️📝新质量 = 解决方案の真值→质量
        final float newQ = truthToQuality(solution);
        // 返回
        return new BudgetValue(newP, newD, newQ);
    }

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
        final float priority = or(dif, currentTaskBudget.getPriority());
        // * 📝耐久度 = (差 + 当前任务) / 2
        final float durability = aveAri(dif, currentTaskBudget.getDurability());
        // * 📝质量 = 新真值→质量
        final float quality = truthToQuality(truth);
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

    // /**
    // * Update a belief
    // * * ⚠️要求此中之「任务」必须是「判断句」
    // * * ❓【2024-06-11 00:02:46】此函数似乎并不使用：304、312均不用
    // *
    // * @param task [&] The task containing new belief
    // * @param bTruth [&] Truth value of the previous belief
    // * @return [] Budget value of the updating task
    // */
    // private static Budget update(Task task, Truth bTruth) {
    // // * 🚩计算落差
    // final float dif = task.asJudgement().getExpDifAbs(bTruth);
    // // * 🚩根据落差计算预算值
    // // * 📝优先级 = 落差 | 任务
    // // * 📝耐久度 = (落差 + 任务) / 2
    // // * 📝质量 = 信念真值→质量
    // final float priority = or(dif, task.getPriority());
    // final float durability = aveAri(dif, task.getDurability());
    // final float quality = truthToQuality(bTruth);
    // return new BudgetValue(priority, durability, quality);
    // }

    /* ----------------------- Links ----------------------- */
    /**
     * Distribute the budget of a task among the links to it
     * * 🚩【2024-05-30 00:53:02】产生新预算值，不会修改旧预算值
     * * 📝【2024-05-30 00:53:41】逻辑：仅优先级随链接数指数级降低
     *
     * @param original [&] The original budget
     * @param nLinks   [] Number of links
     * @return [] Budget value for each link
     */
    public static Budget distributeAmongLinks(final Budget original, final int nLinks) {
        // * 🚩直接计算
        // * 📝优先级 = 原 / √链接数
        // * 📝耐久度 = 原
        // * 📝质量 = 原
        final float priority = (float) (original.getPriority() / Math.sqrt(nLinks));
        return new BudgetValue(priority, original.getDurability(), original.getQuality());
    }

    /* ----------------------- Concept ----------------------- */
    /**
     * Activate a concept by an incoming TaskLink
     * * 📝【2024-05-30 01:08:26】调用溯源：仅在「直接推理」中使用
     * * 📝【2024-05-30 01:03:01】逻辑：优先级「析取」提升，耐久度「算术」平均
     *
     * @param concept [&] The concept
     * @param budget  [&] The budget for the new item
     * @return [] Budget value for the new item
     */
    public static Budget activate(final Concept concept, final Budget budget) {
        // * 🚩直接计算
        final float cP = concept.getPriority();
        final float cD = concept.getDurability();
        final float bP = budget.getPriority();
        final float bD = budget.getDurability();
        // * 📝优先级 = 概念 | 参考
        // * 📝耐久度 = (概念 + 参考) / 2
        // * 📝质量 = 综合所有词项链后的新「质量」
        final float p = or(cP, bP);
        final float d = aveAri(cD, bD);
        final float q = conceptTotalQuality(concept); // * 📝此「质量」非上头「质量」：上头的「质量」实为「总体质量」，与「词项链」「词项复杂度」均有关
        return new BudgetValue(p, d, q);
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
     * @param budgetToBeForget  [&] The previous budget value
     * @param forgetRate        [] The budget for the new item
     * @param relativeThreshold [] The relative threshold of the bag
     * @return [] The new priority value
     */
    public static float forget(Budget budgetToBeForget, int forgetRate, float relativeThreshold) {
        final float bP = budgetToBeForget.getPriority();
        final float bD = budgetToBeForget.getDurability();
        final float bQ = budgetToBeForget.getQuality();
        // * 🚩先放缩「质量」
        final double scaledQ = bQ * relativeThreshold; // re-scaled quality
        // * 🚩计算优先级和「放缩后质量」的差
        final double difPQ = bP - scaledQ; // priority above quality
        // * 🚩决定新的优先级
        final double newPriority;
        if (difPQ > 0)
            // * 🚩差值 > 0 | 衰减 | 📝Math.pow接收两个float，返回一个double
            // priority Durability
            newPriority = scaledQ + difPQ * Math.pow(bD, 1.0 / (forgetRate * difPQ));
        else
            // * 🚩差值 < 0 | 恒定
            newPriority = scaledQ;
        // * 🚩返回计算出的新优先级
        return (float) newPriority;
    }

    /**
     * Merge an item into another one in a bag, when the two are identical
     * except in budget values
     *
     * @param baseValue   [&m] The budget value to be modified
     * @param adjustValue [&] The budget doing the adjusting
     */
    public static void merge(Budget baseValue, Budget adjustValue) {
        final Budget newBudget = mergeToNew(baseValue, adjustValue);
        baseValue.copyBudgetFrom(newBudget);
    }

    /**
     * 🆕「合并」两个预算值，但输出到新值
     *
     * @param baseValue   [&] The budget value to merge
     * @param adjustValue [&] The budget doing the adjusting
     * @return The merged budget
     */
    public static BudgetValue mergeToNew(Budget baseValue, Budget adjustValue) {
        final float bP = baseValue.getPriority();
        final float bD = baseValue.getDurability();
        final float bQ = baseValue.getQuality();
        final float aP = adjustValue.getPriority();
        final float aD = adjustValue.getDurability();
        final float aQ = adjustValue.getQuality();
        // * 📝三×最大值
        final float p = Math.max(bP, aP);
        final float d = Math.max(bD, aD);
        final float q = Math.max(bQ, aQ);
        return new BudgetValue(p, d, q);
    }

    // TODO: 过程注释 & 参数标注

    /* ----- Task derivation in LocalRules and SyllogisticRules ----- */
    // /**
    // * Forward inference result and adjustment
    // *
    // * @param truth [&] The truth value of the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget value of the conclusion
    // */
    // static Budget forward(Truth truth, DerivationContextConcept context) {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // truthToQuality(truth),
    // 1,
    // context);
    // }

    // /**
    // * Backward inference result and adjustment, stronger case
    // *
    // * @param truth [&] The truth value of the belief deriving the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget value of the conclusion
    // */
    // public static Budget backward(Truth truth, DerivationContextConcept context)
    // {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // truthToQuality(truth),
    // 1,
    // context);
    // }

    // /**
    // * Backward inference result and adjustment, weaker case
    // *
    // * @param truth [&] The truth value of the belief deriving the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget value of the conclusion
    // */
    // public static Budget backwardWeak(Truth truth, DerivationContextConcept
    // context) {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // W2C1 * truthToQuality(truth),
    // 1,
    // context);
    // }

    // /* ----- Task derivation in CompositionalRules and StructuralRules ----- */
    // /**
    // * Forward inference with CompoundTerm conclusion
    // *
    // * @param truth [&] The truth value of the conclusion
    // * @param content [&] The content of the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget of the conclusion
    // */
    // public static Budget compoundForward(Truth truth, Term content,
    // DerivationContextConcept context) {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // truthToQuality(truth),
    // content.getComplexity(),
    // context);
    // }

    // /**
    // * Backward inference with CompoundTerm conclusion, stronger case
    // *
    // * @param content [&] The content of the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget of the conclusion
    // */
    // public static Budget compoundBackward(Term content, DerivationContextConcept
    // context) {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // 1,
    // content.getComplexity(),
    // context);
    // }

    // /**
    // * Backward inference with CompoundTerm conclusion, weaker case
    // *
    // * @param content [&] The content of the conclusion
    // * @param context [&m] The derivation context
    // * @return [] The budget of the conclusion
    // */
    // public static Budget compoundBackwardWeak(Term content,
    // DerivationContextConcept context) {
    // // TODO: 过程笔记注释
    // return budgetInference(
    // W2C1,
    // content.getComplexity(),
    // context);
    // }

    // /**
    // * Common processing for all inference step
    // *
    // * @param inferenceQuality [] Quality of the inference
    // * @param complexity [] Syntactic complexity of the conclusion
    // * @param context [&m] The derivation context
    // * @return [] Budget of the conclusion task
    // */
    // private static Budget budgetInference(
    // final float inferenceQuality,
    // final int complexity,
    // final DerivationContextConcept context) {
    // // * 🚩获取有关「词项链」「任务链」的有关参数
    // final Item tLink = context.getCurrentTaskLink();
    // if (tLink == null)
    // // ! 📝【2024-05-17 15:41:10】`t`不可能为`null`：参见`{@link Concept.fire}`
    // throw new AssertionError("t shouldn't be `null`!");
    // final TermLink beliefLink = context.getBeliefLinkForBudgetInference();
    // final float targetActivation = beliefLink == null
    // // * 🚩空值⇒空置（转换推理不会用到）
    // ? 0.0f
    // // * 🚩其它⇒计算
    // : getConceptActivation(beliefLink.getTarget(), context);
    // // * 🚩不带「推理上下文」参与计算
    // return budgetInference(inferenceQuality, complexity, tLink, beliefLink,
    // targetActivation);
    // }

    // /**
    // * Get the current activation level of a concept.
    // * * 🚩从「概念」中来
    // *
    // * @param t [&] The Term naming a concept
    // * @param context [&] The derivation context
    // * @return [] the priority value of the concept
    // */
    // private static float getConceptActivation(Term t, DerivationContext context)
    // {
    // // * 🚩尝试获取概念，并获取其优先级；若无概念，返回0
    // final Concept c = context.termToConcept(t);
    // return c == null ? 0f : c.getPriority();
    // }

    // /**
    // * Common processing for all inference step
    // *
    // * @param inferenceQuality [] Quality of the inference
    // * @param complexity [] Syntactic complexity of the conclusion
    // * @param taskLinkBudget [&] Budget value from task-link
    // * @param beliefLink [&m] Budget value from belief-link (will be updated)
    // * @param targetActivation [] The priority of belief-link's target concept
    // * @return [] Budget of the conclusion task
    // */
    // private static Budget budgetInference(
    // final float inferenceQuality,
    // final int complexity,
    // final Budget taskLinkBudget,
    // final Budget beliefLinkBudget, // 📌跟下边这个参数是捆绑的：有「信念链」就要获取「目标词项」的优先级
    // final float targetActivation) {
    // // * 🚩计算新结果
    // final BudgetInferenceResult result = budgetInferenceCalc(
    // inferenceQuality, complexity,
    // taskLinkBudget,
    // beliefLinkBudget, targetActivation);
    // // * 🚩应用新结果
    // return budgetInferenceApply(result, beliefLinkBudget);
    // }

    public static BudgetInferenceResult budgetInferenceCalc(
            final float inferenceQuality,
            final int complexity,
            final Budget taskLinkBudget,
            final Budget beliefLinkBudget, // 📌跟下边这个参数是捆绑的：有「信念链」就要获取「目标词项」的优先级
            final float targetActivation) {
        // * 🚩基于「任务链」计算默认的预算值
        // * 🚩有「信念链」⇒根据「信念链」计算更新的预算值，并在其中更新「信念链」的预算值
        // * 🚩根据「是否有信念链」用「任务链」「信念链」更新已有预算
        final float bLinkPriority, bLinkDurability;
        final float tLinkPriority, tLinkDurability;
        tLinkPriority = taskLinkBudget.getPriority();
        tLinkDurability = taskLinkBudget.getDurability();
        if (beliefLinkBudget == null) {
            // * 🚩无信念链⇒默认值
            bLinkPriority = 0.0f; // 默认为0（or照常）
            bLinkDurability = 1.0f; // 默认为1（and照常）
        } else {
            // * 🚩有信念链⇒取其值
            bLinkPriority = beliefLinkBudget.getPriority();
            bLinkDurability = beliefLinkBudget.getDurability();
        }
        // * 🚩更新预算
        // * 📝p = task | belief
        // * 📝d = (task / complexity) & belief
        // * 📝q = inferenceQuality / complexity
        final float priority = or(tLinkPriority, bLinkPriority);
        final float durability = and(tLinkDurability / complexity, bLinkDurability);
        final float quality = inferenceQuality / complexity;
        // * 🚩有信念链⇒更新信念链预算值
        // * 🚩【2024-06-20 17:11:30】现在返回一个新的预算值
        final Budget newBeliefLinkBudget;
        if (beliefLinkBudget != null) {
            // TODO: 此处仅在「概念推理」中出现，后续或可分离拆分
            // * 📝p = belief | quality | targetActivation
            // * 📝d = belief | quality
            // * 📝q = belief
            // * 🚩提升优先级
            final float newBeliefLinkPriority = UtilityFunctions.or(
                    beliefLinkBudget.getPriority(),
                    // * ✅【2024-06-20 18:44:13】↓以下两个值的or嵌套可以消除：差异精度控制在5.9604645E-8内
                    quality,
                    targetActivation);
            // * 🚩提升耐久度
            final float newBeliefLinkDurability = UtilityFunctions.or(
                    beliefLinkBudget.getDurability(),
                    quality);
            final float newBeliefLinkQuality = beliefLinkBudget.getQuality();
            newBeliefLinkBudget = new BudgetValue(newBeliefLinkPriority, newBeliefLinkDurability, newBeliefLinkQuality);
        } else {
            newBeliefLinkBudget = null;
        }
        // * 🚩返回最终的预算值
        final Budget newBudget = new BudgetValue(priority, durability, quality);
        return new BudgetInferenceResult(newBudget, newBeliefLinkBudget);
    }

    public static final class BudgetInferenceResult {
        /**
         * 推理出来的新预算
         *
         * * 📝可空性：可空
         * * 📝可变性：不变
         * * 📝所有权：具所有权
         */
        private Budget newBudget;
        /**
         * 新的「任务链预算值」（若有）
         *
         * * 📝可空性：非空
         * * 📝可变性：不变
         * * 📝所有权：具所有权
         */
        private Budget newBeliefLinkBudget;

        BudgetInferenceResult(final Budget newBudget, final Budget newBeliefLinkBudget) {
            this.newBudget = newBudget;
            this.newBeliefLinkBudget = newBeliefLinkBudget;
        }

        /** 提取「新预算」 */
        public Budget extractNewBudget() {
            final Budget budget = this.newBudget;
            this.newBudget = null;
            return budget;
        }

        /** 提取「新信念链预算」 */
        public Budget extractNewBeliefLinkBudget() {
            final Budget budget = this.newBeliefLinkBudget;
            this.newBeliefLinkBudget = null;
            return budget;
        }
    }
}
