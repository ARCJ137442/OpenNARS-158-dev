package nars.control;

import nars.entity.Concept;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.storage.Memory;

/**
 * 「转换推理上下文」
 * * 📄从「推理上下文」中派生，用于在「直接推理」「概念推理」之间的「转换推理」
 * * 📌唯一的理由仅仅只是「此时没有『当前信念』『当前信念链』与『待推理词项链表』」
 * * 📌类名源自「预备函数」{@link ProcessReason#preprocessConcept}
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
         * currentBeliefLink
         * newStamp?
         * }
         */
        if (self.getCurrentConcept() == null)
            throw new Error("currentConcept: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new Error("currentTerm: 不符预期的可空情况");
        if (self.getCurrentTask() == null)
            throw new Error("currentTask: 不符预期的可空情况");
        if (self.getCurrentTaskLink() == null)
            throw new Error("currentTaskLink: 不符预期的可空情况");
        if (self.getCurrentBelief() == null && self.getCurrentBelief() != null) // * 📝可空
            throw new Error("currentBelief: 不符预期的可空情况");
        if (self.getNewStamp() != null && self.getNewStamp() == null)
            // * 📝溯源其在这之前被赋值的场所：getBelief⇒processConcept
            throw new Error("newStamp: 不符预期的可空情况");
        if (self.getSubstitute() != null)
            throw new Error("substitute: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    public DerivationContextTransform(
            final Memory memory,
            final Concept currentConcept,
            final Task currentTask,
            final TaskLink currentTaskLink) {
        // * 🚩从基类构造
        super(memory);
        // * 🚩赋值
        this.setCurrentConcept(currentConcept);
        this.setCurrentTask(currentTask);
        this.setCurrentTaskLink(currentTaskLink);
        // * 🚩检验
        verify(this);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * The selected TaskLink
     * * 📌【2024-05-21 20:26:30】不可空！
     */
    private TaskLink currentTaskLink;

    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }

    /**
     * 设置当前任务链
     * * 📝仅在「开始推理」之前设置，并且只在「概念推理」中出现
     */
    public void setCurrentTaskLink(TaskLink currentTaskLink) {
        this.currentTaskLink = currentTaskLink;
    }
}
