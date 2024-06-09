package nars.control;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.inference.RuleTables;
import nars.main.Reasoner;
import nars.storage.Memory;
import static nars.control.DerivationContext.drop;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 「转换推理上下文」
 * * 📄从「推理上下文」中派生，用于在「直接推理」「概念推理」之间的「转换推理」
 * * 📌唯一的理由仅仅只是「此时没有『当前信念』『当前信念链』与『待推理词项链表』」
 * * 📌类名源自「预备函数」{@link ProcessReason#preprocessConcept}
 * * 📝以{@link RuleTables#transformTask}
 * * 🚩此处的`currentBelief`总是`null`，实际上不使用（以免产生更复杂的类型）
 */
public class DerivationContextTransform implements DerivationContextConcept {

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
    private TaskLink currentTaskLink;

    /**
     * The selected belief
     *
     * * ️📝可空性：可空
     * * 📝可变性：可变 | 仅切换值，不修改内部 @ 切换信念/修正
     * * 📝所有权：具所有权
     *
     * * 🚩【2024-05-30 09:25:15】内部不被修改，同时「语句」允许被随意复制（内容固定，占用小）
     */
    protected Judgement currentBelief; // TODO: 字段分离（可变/不可变）

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
        // * 🚩构造核心
        this.core = new DerivationContextCore(reasoner, currentConcept);
        this.currentTaskLink = currentTaskLink;
        // * 🚩特有字段
        this.memory = reasoner.getMemory();
        // * 🚩检验
        verify(this);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    public Judgement getCurrentBelief() {
        return currentBelief;
    }

    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }

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
    public LinkedList<Task> getNewTasks() {
        return this.core.newTasks;
    }

    @Override
    public ArrayList<String> getExportStrings() {
        return this.core.exportStrings;
    }

    @Override
    public ArrayList<String> getStringsToRecord() {
        return this.core.stringsToRecord;
    }

    @Override
    public Concept getCurrentConcept() {
        return this.core.currentConcept;
    }

    @Override
    public void absorbedByReasoner(Reasoner reasoner) {
        // * 🚩销毁「当前信念」 | 变量值仅临时推理用
        this.currentBelief = null;
        drop(currentBelief);
        // * 🚩将「当前任务链」归还给「当前概念」（所有权转移）
        this.getCurrentConcept().__putTaskLinkBack(this.currentTaskLink);
        // * 🚩从基类方法继续
        this.core.absorbedByReasoner(reasoner);
    }
}
