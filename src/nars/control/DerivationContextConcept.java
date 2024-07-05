package nars.control;

import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.SentenceV1;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.entity.TruthValue;
import nars.inference.Budget;
import nars.inference.Truth;
import nars.language.Term;

/**
 * 「概念推理（中层）上下文」
 * * 🎯用于统一「转换推理」与「概念推理」的逻辑
 * * * 🚩统一的「当前信念」（一致可空）、「用于预算推理的当前信念链」等附加要求
 * * * ✨更多的「单前提结论」「多前提结论」导出方法
 */
public interface DerivationContextConcept extends DerivationContext {

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * 获取「当前信念」
     * * 📌仅在「概念推理」中用到
     * * 🚩对于用不到的实现者，只需实现为空
     *
     * * 📝可空性：可空
     * * 📝可变性：不变
     * * 📝所有权：不可变引用
     */
    public Judgement getCurrentBelief();

    /**
     * 获取用于「预算推理」的「当前信念链」
     * * 📌仅在「概念推理」中非空
     * * 🚩对于用不到的实现者，只需实现为空
     * * 🎯【2024-06-09 11:25:14】规避对`instanceof DerivationContextReason`的滥用
     *
     * * 📝可空性：可空
     * * 📝可变性：可变 | 内部可变（更新预算值）
     * * 📝所有权：可变引用
     */
    public TermLink getBeliefLinkForBudgetInference();

    /** 🆕实用方法：用于简化「推理规则分派」的代码 */
    public default boolean hasCurrentBelief() {
        return this.getCurrentBelief() != null;
    }

    /**
     * * 📄「转换推理上下文」「概念推理上下文」仅作为「当前任务链之目标」
     */
    @Override
    public default Task getCurrentTask() {
        return this.getCurrentTaskLink().getTarget();
    }

    public TaskLink getCurrentTaskLink();

    /* ---- 与「导出结论」有关的方法 ---- */

    // ! 📌删除「新时间戳」：只需在推理的最后「导出结论」时构造

    /** 🆕产生新时间戳 from 单前提 */
    default Stamp generateNewStampSingle() {
        return (this.getCurrentTask().isJudgement() || !this.hasCurrentBelief()
                ? new Stamp(this.getCurrentTask(), this.getTime())
                // to answer a question with negation in NAL-5 --- move to activated task?
                : new Stamp(this.getCurrentBelief(), this.getTime()));
    }

    /**
     * 🆕产生新时间戳 from 双前提
     * * ⚠️产生的时间戳可能为空：必须要有「当前信念」
     */
    default Stamp generateNewStampDouble() {
        // * 🚩使用「当前任务」和「当前信念」产生新时间戳
        if (!this.hasCurrentBelief())
            throw new AssertionError("【2024-06-27 00:13:29】调用此函数时一定具有「当前信念」");
        return this.hasCurrentBelief()
                // * 🚩具有「当前信念」⇒直接合并
                ? Stamp.uncheckedMerge( // ! 此前已在`getBelief`处检查
                        this.getCurrentTask(),
                        // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                        // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                        this.getCurrentBelief(),
                        this.getTime(),
                        // * 🚩【2024-06-21 17:08:14】暂且将超参数放置于此
                        this.getMaxEvidenceBaseLength())
                : null;
    }

    /* --------------- new task building --------------- */

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     * * 🚩【2024-05-19 12:44:55】构造函数简化：导出的结论<b>始终可修正</b>
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public default void doublePremiseTask(Term newContent, Truth newTruth, Budget newBudget) {
        // * 🚩引入「当前任务」与「新时间戳」
        doublePremiseTask(this.getCurrentTask(), newContent, newTruth, newBudget, this.generateNewStampDouble());
    }

    /**
     * 🆕其直接调用来自组合规则、匹配规则（修正）
     * * 📌完全参数方法
     * * 🎯避免对`currentTask`的赋值，解耦调用（并让`currentTask`不可变）
     * * 🎯避免对`newStamp`的复制，解耦调用（让「新时间戳」的赋值止步在「推理开始」之前）
     *
     * @param currentTask
     * @param newContent
     * @param newTruth
     * @param newBudget
     * @param newStamp
     * @param revisable
     */
    public default void doublePremiseTask(
            final Task currentTask,
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp) {
        // * 🚩默认「可修正」，其它相同
        doublePremiseTask(currentTask, newContent, newTruth, newBudget, newStamp, true);
    }

    /** 🆕重定向 */
    public default void doublePremiseTaskNotRevisable(
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget) {
        // * 🚩默认「不可修正」，其它相同
        doublePremiseTask(this.getCurrentTask(), newContent, newTruth, newBudget, generateNewStampDouble(), false);
    }

    /**
     * Shared final operations by all double-premise rules,
     * called from the rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param newStamp   The stamp in sentence
     * @param revisable  Whether the sentence is revisable
     */
    default void doublePremiseTask(
            final Task currentTask,
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp,
            final boolean revisable) {
        if (newContent == null)
            throw new AssertionError("【2024-06-27 00:54:04】任务内容不可能为空");
        // * 🚩仅在「任务内容」可用时构造
        final char newPunctuation = currentTask.getPunctuation();
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(
                newContent,
                newPunctuation,
                newTruth,
                newStamp, revisable);
        final Task newTask = new Task(
                newSentence,
                newBudget,
                this.getCurrentTask(),
                this.getCurrentBelief());
        derivedTask(newTask);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public default void singlePremiseTaskStructural(Term newContent, Truth newTruth, Budget newBudget) {
        singlePremiseTask(newContent, this.getCurrentTask().getPunctuation(), newTruth, newBudget);
    }

    /**
     * 🆕原「单前提结论」中「从某任务提取真值」的函数
     * * 🎯统一将「获取任务信息」外推到「推理过程」中
     * * * 📌尽量不要在「导出结论」时读取额外信息
     */
    public static Truth truthFromTask(Task task) {
        // * 🚩根据「传入的任务」推理
        return task.isJudgement()
                // * 🚩判断句⇒拷贝真值
                ? TruthValue.from(task.asJudgement())
                // * 🚩其它⇒空
                : null;
    }

    /**
     * Shared final operations by all single-premise rules,
     * called in StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public default void singlePremiseTask(
            Term newContent,
            char punctuation,
            Truth newTruth,
            Budget newBudget) {
        final Task parentTask = this.getCurrentTask().getParentTask();
        // * 🚩对于「结构转换」的单前提推理，若已有父任务且该任务与父任务相同⇒中止，避免重复推理
        if (parentTask != null && newContent.equals(parentTask.getContent()))
            return; // to avoid circular structural inference
        final Sentence taskSentence = this.getCurrentTask();
        // * 🚩构造新时间戳
        final Stamp newStamp = this.generateNewStampSingle();
        // * 🚩使用新内容构造新语句
        final boolean revisable = taskSentence.isJudgement()
                // * 🚩判断句⇒返回实际的「可修订」
                ? taskSentence.asJudgement().getRevisable()
                // * 🚩疑问句⇒返回一个用不到的空值
                : false;
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(
                newContent, punctuation,
                newTruth, newStamp,
                revisable);
        // * 🚩构造新任务
        final Task newTask = new Task(newSentence, newBudget, this.getCurrentTask(), null);
        // * 🚩导出
        derivedTask(newTask);
    }
}
