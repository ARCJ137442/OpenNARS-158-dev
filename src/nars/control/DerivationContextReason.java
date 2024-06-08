package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.RuleTables;
import nars.language.Term;
import nars.main.Reasoner;

/**
 * 🆕新的「概念推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务链-信念链」的「概念推理」
 * * 📌类名源自入口函数{@link RuleTables#reason}
 */
public class DerivationContextReason extends DerivationContextTransform {

    /**
     * 用于构建「直接推理上下文」对象
     */
    public static final void verify(DerivationContextReason self) {
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
        if (self.getTermLinksToReason().isEmpty() && !self.getTermLinksToReason().isEmpty()) // * 📝可空：有可能只有一个词项链
            throw new AssertionError("termLinksToReason: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     */
    public DerivationContextReason(
            final Reasoner reasoner,
            final Concept currentConcept,
            final Task currentTask,
            final TaskLink currentTaskLink,
            final TermLink currentBeliefLink,
            final LinkedList<TermLink> toReasonLinks) {
        // * 🚩从基类构造，并预先检验
        super(reasoner, currentConcept, currentTaskLink);
        // * 🚩赋值
        this.setCurrentBeliefLink(currentBeliefLink);
        this.termLinksToReason = toReasonLinks;
        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        updateCurrentBelief();
        // * 🚩检验
        verify(this);
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
        final TermLink currentBeliefLink = this.termLinksToReason.poll();

        // * 🚩若没有更多词项链了⇒返回空表示「已结束」
        if (currentBeliefLink == null)
            return null;

        // * 🚩更新「当前信念链」 | 此举保证「信念链」永不为空
        this.setCurrentBeliefLink(currentBeliefLink);

        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        updateCurrentBelief();

        // * ♻️回收弹出的旧词项链（所有权转移）
        this.getCurrentConcept().__putTermLinkBack(oldTermLink);

        // * 🚩收尾：返回被替换下来的「旧词项链」
        return oldTermLink;
    }

    /**
     * 通过设置好的（非空的）「当前信念链」更新「当前信念」与「新时间戳」
     * * ❓是否要考虑「归还信念链」？此处使用的是值还是引用？所有权如何变更？
     */
    protected void updateCurrentBelief() {
        // * 🚩背景变量
        final TermLink newBeliefLink = this.currentBeliefLink;
        // * 🚩尝试从「当前信念链的目标」获取「当前信念」所对应的概念
        final Term beliefTerm = newBeliefLink.getTarget();
        final Concept beliefConcept = this.termToConcept(beliefTerm);
        final Judgement newBelief = beliefConcept == null
                ? null
                // * 🚩找到新的「信念」充当currentBelief
                // * 🚩将「当前任务」和新的「信念」合并成「新时间戳」
                : beliefConcept.getBelief(this.getCurrentTask()); // ! may be null
        // * 🚩最后设置当前信念（可空性相对独立）
        this.setCurrentBelief(newBelief);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * The selected TermLink
     * * 📝相比「转换推理上下文」仅多了个可查的「当前信念链」
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 构造后不重新赋值，但内部可变（预算推理/反馈预算值）
     * * 📝所有权：具所有权，无需共享 | 存储「拿出的词项链」
     */
    private TermLink currentBeliefLink;

    public TermLink getCurrentBeliefLink() {
        return currentBeliefLink;
    }

    /**
     * 🆕所有要参与「概念推理」的词项链（信念链）
     * * 🎯装载「准备好的词项链（信念链）」，简化「概念推理准备阶段」的传参
     * * 📌Java没有像元组那样方便的「规范化临时结构」类型，对函数返回值的灵活性限制颇多
     * * 🚩目前对于「第一个要准备的词项链」会直接存储在「当前词项链（信念链）」中
     * * 📌类似Rust所有权规则：始终只有一处持有「完全独占引用（所有权）」
     */
    private LinkedList<TermLink> termLinksToReason = new LinkedList<>();

    public LinkedList<TermLink> getTermLinksToReason() {
        return termLinksToReason;
    }

    /**
     * 设置当前任务链
     * * 📝仅在「开始推理」之前设置，并且只在「概念推理」中出现（构建推理上下文）
     * * 📝构造后除「切换信念链」不再重新赋值
     */
    protected void setCurrentBeliefLink(TermLink currentBeliefLink) {
        this.currentBeliefLink = currentBeliefLink;
    }

    @Override
    public void absorbedByReasoner(Reasoner reasoner) {
        // * 🚩将最后一个「当前信念链」归还给「当前信念」（所有权转移）
        this.getCurrentConcept().__putTermLinkBack(currentBeliefLink);
        // * 🚩从基类方法继续
        super.absorbedByReasoner(reasoner);
    }
}
