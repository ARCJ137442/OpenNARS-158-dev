package nars.control;

import nars.entity.Concept;
import nars.entity.Task;
import nars.inference.Budget;
import nars.inference.BudgetInference;
import nars.language.Term;
import nars.storage.Memory;
import static nars.control.DerivationContext.drop;

/**
 * 🆕新的「直接推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务」的「直接推理」
 */
public final class DerivationContextDirect implements DerivationContext {

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
     * * 📝可空性：非空
     * * 📝可变性：可变 | 【2024-05-30 08:47:16】在「概念链接建立」的过程中需要
     * * 📝所有权：可变引用
     */
    private final Memory memory;

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
        // * 🚩构造核心
        this.core = new DerivationContextCore(reasoner, currentConcept);
        // * 🚩独有字段
        this.memory = reasoner.getMemory();
        this.currentTask = currentTask;
        verify(this);
    }

    @Override
    public Memory getMemory() {
        return memory;
    }

    /**
     * 📝对「记忆区」的可变引用，只在「直接推理」中可变
     */
    public Memory mutMemory() {
        return this.getMemory();
    }

    /**
     * 获取「已存在的概念」或（在记忆区）创建新概念
     * * 🎯让「概念推理」可以在「拿出概念」的时候运行，同时不影响具体推理过程
     * * 🚩先与「当前概念」做匹配，若没有再在记忆区中寻找
     * * 📌【2024-05-24 22:07:42】目前专供「推理规则」调用
     * * 📝【2024-06-26 20:45:59】目前所有逻辑纯只读：最多为「获取其中的信念」
     *
     * @param &m-self
     * @param term    [&]
     */
    public Concept getConceptOrCreate(Term term) {
        if (term.equals(this.getCurrentTerm()))
            return this.getCurrentConcept();
        else
            return this.getMemory().getConceptOrCreate(term);
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
        // * 🚩处理所有「导出」
        for (Derivation derivation : this.core.derivations)
            this.handleDerivation(derivation);
        this.core.derivations.clear();
        // * 🚩销毁「当前任务」
        drop(this.currentTask);
        // * 🚩继续销毁核心
        this.core.absorbedByReasoner(reasoner);
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
    public Concept getCurrentConcept() {
        return this.core.currentConcept;
    }

    // 惰性推理结果处理

    @Override
    public void sendDerivation(Derivation derivation) {
        this.core.sendDerivation(derivation);
    }

    @Override
    public void handleDerivation(Derivation derivation) {
        final Budget budget;
        switch (derivation.budget.type) {
            case ReviseDirect:
                budget = BudgetInference.reviseDirect(
                        derivation.budget.newBeliefTruth, derivation.budget.oldBeliefTruth,
                        derivation.budget.truth,
                        derivation.budget.currentTaskBudget);
                this.doublePremiseTaskRevision(
                        derivation.content,
                        derivation.truth, budget,
                        derivation.stamp);
                break;

            default:
                System.err.println("未支持的预算推理类型：" + derivation.budget.type);
        }
    }
}
