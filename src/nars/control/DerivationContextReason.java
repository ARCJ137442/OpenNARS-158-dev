package nars.control;

import java.util.ArrayList;
import java.util.LinkedList;

import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.RuleTables;
import nars.storage.Memory;

/**
 * 🆕新的「概念推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务链-信念链」的「概念推理」
 * * 📌类名源自入口函数{@link RuleTables#reason}
 */
public class DerivationContextReason extends DerivationContext {

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
    public DerivationContextReason(final Memory memory) {
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
