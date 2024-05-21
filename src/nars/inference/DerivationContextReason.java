package nars.inference;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.storage.Memory;

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
         * newStamp?
         * }
         */
        if (self.getCurrentTask() == null)
            throw new Error("currentTask: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new Error("currentTerm: 不符预期的可空情况");
        if (self.getCurrentConcept() == null)
            throw new Error("currentConcept: 不符预期的可空情况");
        if (self.getCurrentBelief() == null && self.getCurrentBelief() != null) // * 📝可空
            throw new Error("currentBelief: 不符预期的可空情况");
        if (self.getCurrentBeliefLink() == null)
            throw new Error("currentBeliefLink: 不符预期的可空情况");
        if (self.getCurrentTaskLink() == null)
            throw new Error("currentTaskLink: 不符预期的可空情况");
        if (self.getNewStamp() != null && self.getNewStamp() == null)
            // * 📝溯源其在这之前被赋值的场所：getBelief⇒processConcept
            throw new Error("newStamp: 不符预期的可空情况");
        if (self.getSubstitute() != null)
            throw new Error("substitute: 不符预期的可空情况");
        if (self.getTermLinksToReason().isEmpty() && !self.getTermLinksToReason().isEmpty()) // * 📝可空：有可能只有一个词项链
            throw new Error("termLinksToReason: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    public DerivationContextReason(
            final Memory memory,
            final Concept currentConcept,
            final Task currentTask,
            final TaskLink currentTaskLink,
            final TermLink currentBeliefLink,
            final LinkedList<TermLink> toReasonLinks) {
        // * 🚩从基类构造，并预先检验
        super(memory, currentConcept, currentTask, currentTaskLink);
        // * 🚩赋值
        this.setCurrentBeliefLink(currentBeliefLink);
        this.termLinksToReason = toReasonLinks;
        // * 🚩检验
        verify(this);
    }

    /**
     * 切换到新的信念（与信念链）
     * * 🚩只搬迁引用，并不更改所有权
     * * 📌【2024-05-21 10:26:59】现在是「概念推理上下文」独有
     */
    public void switchToNewBelief(
            TermLink currentBeliefLink,
            Sentence currentBelief,
            Stamp newStamp) {
        // * 🚩搬迁引用
        this.currentBeliefLink = currentBeliefLink;
        this.setCurrentBelief(currentBelief);
        this.setNewStamp(newStamp);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * The selected TermLink
     */
    private TermLink currentBeliefLink = null;

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
     */
    public void setCurrentBeliefLink(TermLink currentBeliefLink) {
        this.currentBeliefLink = currentBeliefLink;
    }
}
