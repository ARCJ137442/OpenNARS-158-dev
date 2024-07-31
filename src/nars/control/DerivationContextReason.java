package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.language.Term;
import nars.storage.Memory;

import static nars.control.DerivationContext.drop;

/**
 * 🆕新的「概念推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务链-信念链」的「概念推理」
 * * 📌类名源自入口函数{@link RuleTables#reason}
 * * 🚩【2024-06-09 10:55:22】「转换推理」和「概念推理」总归是不同的两种推理，需要彻底拆分而不留任何继承关系
 */
public final class DerivationContextReason implements DerivationContextConcept {

    // struct DerivationContextReason

    /**
     * 🆕内部存储的「上下文核心」
     *
     * * 📝可空性：非空
     * * 📝可变性：可变
     * * 📝所有权：具所有权
     */
    private final DerivationContextCore core;

    /**
     * 对「记忆区」的反向引用
     * * 🚩【2024-05-18 17:00:12】目前需要访问其「输出」「概念」等功能
     *
     * * 📝可空性：非空
     * * 📝可变性：不变
     * * 📝所有权：不可变引用
     */
    private final Memory memory;

    /**
     * The selected TaskLink
     * * 📌【2024-05-21 20:26:30】不可空！
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 构造后不重新赋值，但内部可变（预算推理/反馈预算值）
     * * 📝所有权：具所有权，无需共享 | 存储「拿出的词项链」
     */
    private final TaskLink currentTaskLink;

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

    /**
     * The selected TermLink
     * * 📝相比「转换推理上下文」仅多了个可查的「当前信念链」
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 构造后不重新赋值，但内部可变（预算推理/反馈预算值）
     * * 📝所有权：具所有权，无需共享 | 存储「拿出的词项链」
     */
    private TermLink currentBeliefLink;

    /**
     * 🆕所有要参与「概念推理」的词项链（信念链）
     * * 🎯装载「准备好的词项链（信念链）」，简化「概念推理准备阶段」的传参
     * * 📌Java没有像元组那样方便的「规范化临时结构」类型，对函数返回值的灵活性限制颇多
     * * 🚩目前对于「第一个要准备的词项链」会直接存储在「当前词项链（信念链）」中
     * * 📌类似Rust所有权规则：始终只有一处持有「完全独占引用（所有权）」
     */
    private final LinkedList<TermLink> beliefLinksToReason;

    // impl DerivationContextReason

    /**
     * 用于构建「直接推理上下文」对象
     */
    private static final void verify(DerivationContextReason self) {
        // * 🚩系列断言与赋值（实际使用中可删）
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         * currentTaskLink
         * currentBelief?
         * currentBeliefLink
         * }
         */
        if (self.getCurrentTask() == null)
            throw new AssertionError("currentTask: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new AssertionError("currentTerm: 不符预期的可空情况");
        if (self.getCurrentConcept() == null)
            throw new AssertionError("currentConcept: 不符预期的可空情况");
        if (self.getCurrentBelief() == null && self.getCurrentBelief() != null) // * 📝可空
            throw new AssertionError("currentBelief: 不符预期的可空情况");
        if (self.getCurrentBeliefLink() == null)
            throw new AssertionError("currentBeliefLink: 不符预期的可空情况");
        if (self.getCurrentTaskLink() == null)
            throw new AssertionError("currentTaskLink: 不符预期的可空情况");
        if (self.beliefLinksToReason.isEmpty() && !self.beliefLinksToReason.isEmpty()) // * 📝可空：有可能只有一个词项链
            throw new AssertionError("termLinksToReason: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     */
    public DerivationContextReason(
            final Reasoner reasoner,
            final Concept currentConcept,
            final TaskLink currentTaskLink,
            final LinkedList<TermLink> beliefLinksToReason) {
        // * 🚩构造核心
        this.core = new DerivationContextCore(reasoner, currentConcept);

        // * 🚩特有字段
        this.currentTaskLink = currentTaskLink;
        this.memory = reasoner.getMemory();

        // * 🚩 先将首个元素作为「当前信念链」
        this.currentBeliefLink = beliefLinksToReason.poll();
        this.beliefLinksToReason = beliefLinksToReason;

        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        this.currentBelief = this.updatedCurrentBelief();

        // * 🚩检验
        verify(this);
    }

    /**
     * 获取「当前信念链」
     *
     * * 📝可空性：非空
     * * 📝可变性：可变 | 需要内部修改
     * * 📝所有权：可变引用
     */
    public TermLink getCurrentBeliefLink() {
        // ? 【2024-06-26 00:45:39】后续可做：内化「预算更新」，使之变为不可变引用
        return this.currentBeliefLink;
    }

    /**
     * 切换到新的信念（与信念链）
     * * 📌【2024-05-21 10:26:59】现在是「概念推理上下文」独有
     * * 🚩【2024-05-21 22:51:09】只在自身内部搬迁所有权：从「待推理词项链表」中取出一个「词项链」替代原有词项链
     * * 🚩能取出⇒返回旧词项链，已空⇒返回`null`
     * * ✅【2024-05-21 23:13:10】内存安全：整个过程中`currentBeliefLink`不可能为空
     * * ✅每行代码后加`verify`都不会有事
     */
    public TermLink nextBelief() {
        // * 🚩先尝试拿出下一个词项链，若拿不出则返回空值
        final TermLink oldTermLink = this.getCurrentBeliefLink();
        final TermLink currentBeliefLink = this.beliefLinksToReason.poll();

        // * 🚩若没有更多词项链了⇒返回空表示「已结束」
        if (currentBeliefLink == null)
            return null;

        // * 🚩更新「当前信念链」 | 此举保证「信念链」永不为空
        this.currentBeliefLink = currentBeliefLink;

        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        updateCurrentBelief();

        // * ♻️回收弹出的旧词项链（所有权转移）
        this.getCurrentConcept().putTermLinkBack(oldTermLink);

        // * 🚩收尾：返回被替换下来的「旧词项链」
        return oldTermLink;
    }

    /**
     * 通过设置好的（非空的）「当前信念链」更新「当前信念」
     * * ❓是否要考虑「归还信念链」？此处使用的是值还是引用？所有权如何变更？
     */
    private void updateCurrentBelief() {
        // * 🚩设置当前信念（可空性相对独立）
        this.currentBelief = this.updatedCurrentBelief();
    }

    /** 🆕通过设置好的（非空的）「当前信念链」返回更新的「当前信念」（所有权） */
    private Judgement updatedCurrentBelief() {
        // * 🚩处理所有旧任务的「导出」
        for (Derivation derivation : this.core.derivations)
            this.handleDerivation(derivation);
        this.core.derivations.clear();
        // * 🚩背景变量
        final TermLink newBeliefLink = this.currentBeliefLink;
        // * 🚩尝试从「当前信念链的目标」获取「当前信念」所对应的概念
        final Term beliefTerm = newBeliefLink.getTarget();
        final Concept beliefConcept = this.termToConcept(beliefTerm);
        final Judgement newBelief = beliefConcept == null
                ? null
                // * 🚩找到新的「信念」充当currentBelief
                : beliefConcept.getBelief(this.getCurrentTask()); // ! may be null
        // * 🚩最后返回当前信念（可空性相对独立）
        return newBelief;
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    // impl DerivationContextConcept for DerivationContextReason

    @Override
    public Judgement getCurrentBelief() {
        return this.currentBelief;
    }

    @Override
    public TermLink getBeliefLinkForBudgetInference() {
        return this.currentBeliefLink;
    }

    @Override
    public TaskLink getCurrentTaskLink() {
        return this.currentTaskLink;
    }

    // impl DerivationContext for DerivationContextReason

    @Override
    public Memory getMemory() {
        return this.memory;
    }

    @Override
    public long getTime() {
        return this.core.time;
    }

    @Override
    public float getSilencePercent() {
        return this.core.getSilencePercent();
    }

    @Override
    public boolean noNewTask() {
        return this.core.newTasks.isEmpty();
    }

    @Override
    public int numNewTasks() {
        return this.core.newTasks.size();
    }

    @Override
    public void addNewTask(Task newTask) {
        this.core.newTasks.add(newTask);
    }

    @Override
    public void addExportString(String exportedString) {
        this.core.exportStrings.add(exportedString);
    }

    @Override
    public void addStringToRecord(String stringToRecord) {
        this.core.stringsToRecord.add(stringToRecord);
    }

    @Override
    public Concept getCurrentConcept() {
        return this.core.currentConcept;
    }

    @Override
    public void absorbedByReasoner(Reasoner reasoner) {
        // * 🚩处理最后一个「当前信念」的所有「导出」
        for (Derivation derivation : this.core.derivations)
            this.handleDerivation(derivation);
        this.core.derivations.clear();
        // * 🚩将最后一个「当前信念链」归还给「当前信念」（所有权转移）
        this.getCurrentConcept().putTermLinkBack(this.currentBeliefLink);
        // * 🚩将「当前任务链」归还给「当前概念」（所有权转移）
        this.getCurrentConcept().putTaskLinkBack(this.currentTaskLink);
        // * 🚩销毁「当前信念」 | 变量值仅临时推理用
        this.currentBelief = null;
        drop(currentBelief);
        // * 🚩吸收核心
        this.core.absorbedByReasoner(reasoner);
    }

    @Override
    public void sendDerivation(Derivation derivation) {
        this.core.sendDerivation(derivation);
    }

    @Override
    public void handleDerivation(Derivation derivation) {
        System.err.println("TODO: handleDerivation");
    }
}
