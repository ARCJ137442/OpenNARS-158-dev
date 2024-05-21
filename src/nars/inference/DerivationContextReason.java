package nars.inference;

import java.util.ArrayList;
import java.util.LinkedList;

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
public class DerivationContextReason extends DerivationContext {

    public static interface IBuilder {
        public DerivationContextReason build();
    }

    /**
     * 用于构建「直接推理上下文」对象
     */
    public static class Builder extends DerivationContextReason implements IBuilder {
        public Builder(Memory memory) {
            super(memory);
        }

        public DerivationContextReason build() {
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
            if (this.getCurrentTask() == null) {
                throw new Error("currentTask: 不符预期的可空情况");
            }
            if (this.getCurrentTerm() == null) {
                throw new Error("currentTerm: 不符预期的可空情况");
            }
            if (this.getCurrentConcept() == null) {
                throw new Error("currentConcept: 不符预期的可空情况");
            }
            if (this.getCurrentBelief() == null && this.getCurrentBelief() != null) { // * 📝可空
                throw new Error("currentBelief: 不符预期的可空情况");
            }
            if (this.getCurrentBeliefLink() == null) {
                throw new Error("currentBeliefLink: 不符预期的可空情况");
            }
            if (this.getCurrentTaskLink() == null) {
                throw new Error("currentTaskLink: 不符预期的可空情况");
            }
            if (this.getNewStamp() != null && this.getNewStamp() == null) {
                // * 📝溯源其在这之前被赋值的场所：getBelief⇒processConcept
                throw new Error("newStamp: 不符预期的可空情况");
            }
            if (this.getSubstitute() != null) {
                throw new Error("substitute: 不符预期的可空情况");
            }
            if (this.getTermLinksToReason().isEmpty() && !this.getTermLinksToReason().isEmpty()) { // * 📝可空：有可能只有一个词项链
                throw new Error("termLinksToReason: 不符预期的可空情况");
            }
            return (DerivationContextReason) this;
        }
    }

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * The selected TaskLink
     */
    private TaskLink currentTaskLink = null;

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

    /**
     * 构造函数
     * * 🚩创建一个空的「概念推理上下文」，默认所有参数为空
     *
     * @param memory 所反向引用的「记忆区」对象
     */
    protected DerivationContextReason(final Memory memory) {
        super(memory);
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    protected DerivationContextReason(final Memory memory,
            final LinkedList<Task> newTasks,
            final ArrayList<String> exportStrings) {
        super(memory, newTasks, exportStrings);
    }

    /**
     * 「复制」推导上下文
     * * 🚩只搬迁引用，并不更改所有权
     */
    public DerivationContextReason clone() {
        // * 🚩创建新上下文，并随之迁移`final`变量
        final DerivationContextReason self = new DerivationContextReason(
                this.getMemory(),
                this.getNewTasks(),
                this.getExportStrings());
        // * 🚩搬迁引用
        // self.currentTerm = this.currentTerm;
        // self.currentConcept = this.currentConcept;
        self.currentTaskLink = this.currentTaskLink;
        // self.currentTask = this.currentTask;
        self.currentBeliefLink = this.currentBeliefLink;
        // self.currentBelief = this.currentBelief;
        // self.newStamp = this.newStamp;
        // self.substitute = this.substitute;
        // * 🚩返回新上下文
        return self;
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

    /**
     * 清理概念推导上下文
     */
    public void clear() {
        super.clear();
        // * 🚩清理独有变量
        // this.currentTerm = null;
        // this.currentConcept = null;
        this.currentTaskLink = null;
        // this.currentTask = null;
        this.currentBeliefLink = null;
        // this.currentBelief = null;
        // this.newStamp = null;
        // this.substitute = null;
    }
}
