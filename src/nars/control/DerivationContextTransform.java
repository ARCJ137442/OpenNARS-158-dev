package nars.control;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.storage.Memory;

/**
 * 「转换推理上下文」
 * * 📄从「推理上下文」中派生，用于在「直接推理」「概念推理」之间的「转换推理」
 * * 📌唯一的理由仅仅只是「此时没有『当前信念』『当前信念链』与『待推理词项链表』」
 * * 📌类名源自「预备函数」{@link ProcessReason#preprocessConcept}
 * * 📝以{@link RuleTables#transformTask}
 * * 🚩此处的`currentBelief`总是`null`，实际上不使用（以免产生更复杂的类型）
 */
public final class DerivationContextTransform implements DerivationContextConcept {

    // struct DerivationContextTransform

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

    // impl DerivationContextTransform

    /**
     * 用于构建「转换推理上下文」对象
     */
    public static void verify(DerivationContextTransform self) {
        // * 🚩系列断言与赋值（实际使用中可删）
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         * currentTaskLink
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
        // if (self.getCurrentBelief() != null) // * 📝可空
        // throw new AssertionError("currentBelief: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     */
    public DerivationContextTransform(
            final Reasoner reasoner,
            final Concept currentConcept,
            final TaskLink currentTaskLink) {
        // * 🚩构造核心
        this.core = new DerivationContextCore(reasoner, currentConcept);
        this.currentTaskLink = currentTaskLink;
        // * 🚩特有字段
        this.memory = reasoner.getMemory();
        // * 🚩检验
        verify(this);
    }

    // impl DerivationContextConcept for DerivationContextTransform

    @Override
    public TaskLink getCurrentTaskLink() {
        return this.currentTaskLink;
    }

    @Override
    public Judgement getCurrentBelief() {
        // ! 📌「转换推理」的「当前信念」始终为空
        // * 🚩【2024-06-09 11:03:54】妥协：诸多「导出结论」需要使用「当前信念」，但所幸「当前信念」始终允许为空（方便作为默认值）
        return null;
    }

    @Override
    public TermLink getBeliefLinkForBudgetInference() {
        // ! 📌「转换推理」的「当前信念链」始终为空
        // * 🚩【2024-06-09 11:03:54】妥协：诸多「预算推理」需要使用「当前信念链」，但「当前信念」在「概念推理」中不允许为空
        return null;
    }

    // impl DerivationContext for DerivationContextTransform

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
        // * 🚩处理所有「导出」
        for (Derivation derivation : this.core.derivations)
            this.handleDerivation(derivation);
        this.core.derivations.clear();
        // * 🚩将「当前任务链」归还给「当前概念」（所有权转移）
        this.getCurrentConcept().putTaskLinkBack(this.currentTaskLink);
        // * 🚩从基类方法继续
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
