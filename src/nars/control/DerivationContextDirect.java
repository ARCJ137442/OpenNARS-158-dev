package nars.control;

import nars.entity.Concept;
import nars.entity.Task;
import nars.main.Reasoner;
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
         * }
         */

        // * 🚩系列断言与赋值（实际使用中可删）
        if (self.getCurrentTask() == null)
            throw new AssertionError("currentTask: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new AssertionError("currentTerm: 不符预期的可空情况");
        if (self.getCurrentConcept() == null)
            throw new AssertionError("currentConcept: 不符预期的可空情况");
        // if (self.getCurrentBelief() != null)
        // throw new AssertionError("currentBelief: 不符预期的可空情况");
    }

    /**
     * 🆕通过完全的「可空性假设」构建
     * * 🚩每次构造后立即检查参数是否为空
     * * 🎯确保内部字段的可空性：当前任务、当前概念 不可能为空
     */
    public DerivationContextDirect(final Reasoner reasoner, final Task currentTask, final Concept currentConcept) {
        super(reasoner);
        this.currentTask = currentTask;
        setCurrentConcept(currentConcept);
        verify(this);
    }

    /**
     * 📝对「记忆区」的可变引用，只在「直接推理」中可变
     */
    public Memory mutMemory() {
        return this.getMemory();
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

    @Override
    public void absorbedByReasoner(Reasoner reasoner) {
        // * 🚩销毁「当前任务」
        drop(this.currentTask);
        // * 🚩从基类方法继续
        super.absorbedByReasoner(reasoner);
    }
}
