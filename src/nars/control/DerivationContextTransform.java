package nars.control;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.SentenceV1;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TaskV1;
import nars.entity.TruthValue;
import nars.inference.Budget;
import nars.inference.RuleTables;
import nars.inference.Truth;
import nars.language.Term;
import nars.main.Reasoner;

/**
 * 「转换推理上下文」
 * * 📄从「推理上下文」中派生，用于在「直接推理」「概念推理」之间的「转换推理」
 * * 📌唯一的理由仅仅只是「此时没有『当前信念』『当前信念链』与『待推理词项链表』」
 * * 📌类名源自「预备函数」{@link ProcessReason#preprocessConcept}
 * * 📝以{@link RuleTables#transformTask}
 * * 🚩此处的`currentBelief`总是`null`，实际上不使用（以免产生更复杂的类型）
 */
public class DerivationContextTransform extends DerivationContext {

    /**
     * 用于构建「直接推理上下文」对象
     */
    public static final void verify(DerivationContextTransform self) {
        // * 🚩系列断言与赋值（实际使用中可删）
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         * currentTaskLink
         * currentBelief?
         * }
         */
        if (self.getCurrentConcept() == null)
            throw new AssertionError("currentConcept: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new AssertionError("currentTerm: 不符预期的可空情况");
        if (self.getCurrentTask() == null)
            throw new AssertionError("currentTask: 不符预期的可空情况");
        if (self.getCurrentTaskLink() == null)
            throw new AssertionError("currentTaskLink: 不符预期的可空情况");
        if (self.getCurrentBelief() == null && self.getCurrentBelief() != null) // * 📝可空
            throw new AssertionError("currentBelief: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     */
    public DerivationContextTransform(
            final Reasoner reasoner,
            final Concept currentConcept,
            final TaskLink currentTaskLink) {
        // * 🚩从基类构造
        super(reasoner);
        // * 🚩赋值
        this.setCurrentConcept(currentConcept);
        // this.setCurrentTask(currentTask);
        this.setCurrentTaskLink(currentTaskLink);
        // * 🚩检验
        verify(this);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * The selected belief
     *
     * * ️📝可空性：可空
     * * 📝可变性：可变 | 仅切换值，不修改内部 @ 切换信念/修正
     * * 📝所有权：具所有权
     *
     * * 🚩【2024-05-30 09:25:15】内部不被修改，同时「语句」允许被随意复制（内容固定，占用小）
     */
    private Judgement currentBelief;

    public Judgement getCurrentBelief() {
        return currentBelief;
    }

    /** 🆕实用方法：用于简化「推理规则分派」的代码 */
    public boolean hasCurrentBelief() {
        return currentBelief != null;
    }

    /**
     * 设置当前信念
     * * 📝仅在「直接推理」之前、「概念推理」切换概念时用到
     */
    protected void setCurrentBelief(Judgement currentBelief) {
        this.currentBelief = currentBelief;
    }

    // TODO: 「直接推理」没有「当前信念」

    // ! 📌删除「新时间戳」：只需在推理的最后「导出结论」时构造

    /** 🆕产生新时间戳 from 单前提 */
    protected Stamp generateNewStampSingle() {
        if (this.getCurrentTask().isJudgment() || !this.hasCurrentBelief()) {
            return new Stamp(this.getCurrentTask(), this.getTime());
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            return new Stamp(this.getCurrentBelief(), this.getTime());
        }
    }

    /** 🆕产生新时间戳 from 双前提 */
    protected Stamp generateNewStampDouble() {
        // * 🚩使用「当前任务」和「当前信念」产生新时间戳
        return this.hasCurrentBelief()
                // * 🚩具有「当前信念」⇒直接合并
                ? Stamp.uncheckedMerge( // ! 此前已在`getBelief`处检查
                        this.getCurrentTask(),
                        // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                        // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                        this.getCurrentBelief(),
                        this.getTime())
                : null;
    }

    /**
     * * 📄「转换推理上下文」「概念推理上下文」仅作为「当前任务链之目标」
     */
    @Override
    public Task getCurrentTask() {
        return this.getCurrentTaskLink().getTarget();
    }

    /**
     * The selected TaskLink
     * * 📌【2024-05-21 20:26:30】不可空！
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 构造后不重新赋值，但内部可变（预算推理/反馈预算值）
     * * 📝所有权：具所有权，无需共享 | 存储「拿出的词项链」
     */
    private TaskLink currentTaskLink;

    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }

    /**
     * 设置当前任务链
     * * 📝仅在「开始推理」之前设置，并且只在「概念推理」中出现
     * * 📝构造后不再重新赋值
     */
    protected void setCurrentTaskLink(TaskLink currentTaskLink) {
        this.currentTaskLink = currentTaskLink;
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
    public void doublePremiseTask(Term newContent, Truth newTruth, Budget newBudget) {
        // * 🚩引入「当前任务」与「新时间戳」
        doublePremiseTask(this.getCurrentTask(), newContent, newTruth, newBudget, this.generateNewStampDouble());
    }

    /**
     * 🆕其直接调用来自组合规则、本地规则（修正）
     * * 🎯避免对`currentTask`的赋值，解耦调用（并让`currentTask`不可变）
     * * 🎯避免对`newStamp`的复制，解耦调用（让「新时间戳」的赋值止步在「推理开始」之前）
     *
     * @param currentTask
     * @param newContent
     * @param newTruth
     * @param newBudget
     * @param newStamp
     */
    public void doublePremiseTask(
            final Task currentTask,
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp) {
        if (newContent == null)
            return;
        // * 🚩仅在「任务内容」可用时构造
        final char newPunctuation = currentTask.getPunctuation();
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(newContent, newPunctuation, newTruth,
                newStamp, true);
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), this.currentBelief);
        derivedTask(newTask);
    }

    /** 🆕重定向 */
    public void doublePremiseTask(Term newContent, Truth newTruth, Budget newBudget, boolean revisable) {
        doublePremiseTask(newContent, generateNewStampDouble(), newTruth, newBudget, revisable);
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param newStamp   The stamp in sentence
     * @param revisable  Whether the sentence is revisable
     */

    private void doublePremiseTask(
            final Term newContent,
            final Stamp newStamp,
            final Truth newTruth,
            final Budget newBudget,
            final boolean revisable) {
        if (newContent == null)
            return;

        // * 🚩仅在「任务内容」可用时构造
        final Sentence taskSentence = this.getCurrentTask();
        final char newPunctuation = taskSentence.getPunctuation();
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(newContent, newPunctuation, newTruth,
                newStamp,
                revisable);
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), currentBelief);
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
    public void singlePremiseTask(Term newContent, Truth newTruth, Budget newBudget) {
        singlePremiseTask(newContent, this.getCurrentTask().getPunctuation(), newTruth, newBudget);
    }

    public void singlePremiseTask(Term newContent, Task currentTask, Budget newBudget) {
        singlePremiseTask(newContent, this.getCurrentTask().getPunctuation(), currentTask, newBudget);
    }

    public void singlePremiseTask(Term newContent, char punctuation, Task currentTask, Budget newBudget) {
        // * 🚩根据「是否为『判断』」复制真值
        final Truth newTruth = currentTask.isJudgment() ? TruthValue.from(currentTask.asJudgement()) : null;
        singlePremiseTask(newContent, punctuation, newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public void singlePremiseTask(Term newContent, char punctuation, Truth newTruth, Budget newBudget) {
        final Task parentTask = this.getCurrentTask().getParentTask();
        // * 🚩对于「结构转换」的单前提推理，若已有父任务且该任务与父任务相同⇒中止，避免重复推理
        if (parentTask != null && newContent.equals(parentTask.getContent()))
            return; // to avoid circular structural inference
        final Sentence taskSentence = this.getCurrentTask();
        // * 🚩构造新时间戳
        final Stamp newStamp = this.generateNewStampSingle();
        // * 🚩使用新内容构造新语句
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(
                newContent, punctuation,
                newTruth, newStamp,
                taskSentence.getRevisable());
        // * 🚩构造新任务
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), null);
        // * 🚩导出
        derivedTask(newTask);
    }

    @Override
    public void absorbedByReasoner(Reasoner reasoner) {
        // * 🚩销毁「当前信念」 | 变量值仅临时推理用
        this.currentBelief = null;
        // * 🚩将「当前任务链」归还给「当前概念」（所有权转移）
        this.getCurrentConcept().__putTaskLinkBack(this.currentTaskLink);
        // * 🚩从基类方法继续
        super.absorbedByReasoner(reasoner);
    }
}
