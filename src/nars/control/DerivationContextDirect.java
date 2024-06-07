package nars.control;

import nars.entity.Concept;
import nars.entity.Task;
import nars.storage.Memory;

/**
 * 🆕新的「直接推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务」的「直接推理」
 */
public class DerivationContextDirect extends DerivationContext {

    /**
     * 用于构建「直接推理上下文」对象
     */
    public static final void verify(DerivationContextDirect self) {
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         *
         * currentBelief? | 用于中途推理
         * }
         */

        // * 🚩系列断言与赋值（实际使用中可删）
        if (self.getCurrentTask() == null)
            throw new Error("currentTask: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new Error("currentTerm: 不符预期的可空情况");
        if (self.getCurrentConcept() == null)
            throw new Error("currentConcept: 不符预期的可空情况");
        if (self.getCurrentBelief() != null)
            throw new Error("currentBelief: 不符预期的可空情况");
        // if (self.getCurrentBeliefLink() != null)
        // throw new Error("currentBeliefLink: 不符预期的可空情况");
        // if (self.getCurrentTaskLink() != null)
        // throw new Error("currentTaskLink: 不符预期的可空情况");
    }

    /**
     * 🆕通过完全的「可空性假设」构建
     * * 🚩每次构造后立即检查参数是否为空
     * * 🎯确保内部字段的可空性：当前任务、当前概念 不可能为空
     */
    public DerivationContextDirect(final Memory memory, final Task currentTask, final Concept currentConcept) {
        super(memory);
        setCurrentTask(currentTask);
        setCurrentConcept(currentConcept);
        verify(this);
    }

    /**
     * The selected Task
     *
     * * ️📝可空性：可空
     * * 📝可变性：可变 | 构造后不重新赋值，但内部可变
     * * 📝所有权：具所有权 | 存储「传入的新任务」
     * * ⚠️共享：需要传入并构造「任务链」或作为「父任务」，使用共享引用
     */
    private Task currentTask;

    /**
     * * 📄「直接推理上下文」将其作为字段
     * * 📝【2024-05-30 10:31:01】在「处理判断」中修改「优先级」
     * * 📝【2024-05-30 10:31:01】在「本地规则/trySolution 答问」中修改「最优解」
     * * 📝【2024-05-30 10:31:01】在「预算函数/solutionEval」中修改「优先级」
     * * 📝【2024-05-30 10:31:01】在「本地规则/修正」中修改「优先级」「耐久度」
     */
    @Override
    public Task getCurrentTask() {
        return currentTask;
    }

    /**
     * 设置当前任务
     * * 📝仅在「开始推理」之前设置，但在「直接推理」「概念推理」中均出现
     * * ⚠️并且，在两种推理中各含不同语义：「直接推理」作为唯一根据（不含任务链），而「概念推理」则是「任务链」的目标
     * * ✅已解决「在『组合规则』中设置『当前任务』」的例外
     */
    protected void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    @Override
    public void absorbedByMemory(Memory memory) {
        // * 🚩销毁「当前任务」
        drop(this.currentTask);
        // * 🚩从基类方法继续
        super.absorbedByMemory(memory);
    }
}
